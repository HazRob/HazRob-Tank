"""Synthetic-only regression tests for MQ4 paired prediction intake.

No synthetic row is evidence of a physical exposure or a source prediction.
"""
import copy
import hashlib
import json
import unittest

from g2_mq4_pairwise_frechet_bounds import EVIDENCE, NAMES, SOURCE
from g2_mq4_paired_prediction_intake import SCHEMA, audit

SOURCE_RAW = SOURCE.read_bytes()
EVIDENCE_RAW = EVIDENCE.read_bytes()
SOURCE_SHA = hashlib.sha256(SOURCE_RAW).hexdigest()


def sample():
    return {
        "schema": SCHEMA,
        "origin": "EXTERNAL_SOURCE_RESEARCH_ONLY",
        "source_summary_sha256": SOURCE_SHA,
        "rows": [
            {
                "row_id": "synthetic-001",
                "test_day": "2021-07-22",
                "fold_test_day": "2021-07-22",
                "train_days": ["2021-07-21"],
                "raw_row_sha256": "a" * 64,
                "reference_positive_4ppm": True,
                "predicted_positive_4ppm": {name: (i % 2 == 0) for i, name in enumerate(NAMES)},
            },
            {
                "row_id": "synthetic-002",
                "test_day": "2021-07-22",
                "fold_test_day": "2021-07-22",
                "train_days": ["2021-07-21"],
                "raw_row_sha256": "b" * 64,
                "reference_positive_4ppm": False,
                "predicted_positive_4ppm": {name: (i % 2 == 1) for i, name in enumerate(NAMES)},
            },
        ],
    }


def check(obj, *, require_full=False, source=SOURCE_RAW, evidence=EVIDENCE_RAW):
    return audit(json.dumps(obj, sort_keys=True).encode(), source, evidence,
                 require_full=require_full)


class PairedIntakeTests(unittest.TestCase):
    def test_synthetic_arithmetic_and_no_promotion(self):
        result = check(sample())
        self.assertEqual(result["submitted_rows"], 2)
        self.assertEqual(result["submitted_positive_rows"], 1)
        self.assertEqual(result["model_confusion_counts_tp_fn_fp_tn"][NAMES[0]], [1, 0, 1, 0])
        self.assertEqual(result["model_confusion_counts_tp_fn_fp_tn"][NAMES[1]], [0, 1, 1, 0])
        self.assertEqual(result["pairwise_submitted_rows_both_correct_a_correct_b_wrong_a_wrong_b_correct_both_wrong"][NAMES[0] + "|" + NAMES[1]], [0, 1, 0, 1])
        self.assertFalse(result["original_raw_archive_row_hashes_independently_verified"])
        self.assertFalse(result["paired_inferential_test_authorized"])
        self.assertFalse(result["g2_qualified"])
        self.assertFalse(result["g3_qualified"])

    def test_future_training_flagged_not_called_prospective(self):
        obj = sample()
        for row in obj["rows"]:
            row["train_days"] = ["2021-07-23"]
        self.assertEqual(check(obj)["test_days_with_future_training"], ["2021-07-22"])

    def test_full_source_rejects_synthetic_partial_rows(self):
        with self.assertRaisesRegex(ValueError, "INCOMPLETE_SOURCE_ROWS"):
            check(sample(), require_full=True)

    def test_duplicate_row_id(self):
        obj = sample()
        obj["rows"][1]["row_id"] = obj["rows"][0]["row_id"]
        with self.assertRaisesRegex(ValueError, "DUPLICATE_ROW_ID"):
            check(obj)

    def test_missing_model(self):
        obj = sample()
        del obj["rows"][0]["predicted_positive_4ppm"][NAMES[0]]
        with self.assertRaisesRegex(ValueError, "INVALID_MODEL_PREDICTIONS"):
            check(obj)

    def test_extra_model(self):
        obj = sample()
        obj["rows"][0]["predicted_positive_4ppm"]["UNVERIFIED"] = False
        with self.assertRaisesRegex(ValueError, "INVALID_MODEL_PREDICTIONS"):
            check(obj)

    def test_nonboolean_prediction(self):
        obj = sample()
        obj["rows"][0]["predicted_positive_4ppm"][NAMES[0]] = 1
        with self.assertRaisesRegex(ValueError, "INVALID_MODEL_PREDICTIONS"):
            check(obj)

    def test_nonboolean_truth(self):
        obj = sample()
        obj["rows"][0]["reference_positive_4ppm"] = 1
        with self.assertRaisesRegex(ValueError, "INVALID_REFERENCE_LABEL"):
            check(obj)

    def test_invalid_hash(self):
        obj = sample()
        obj["rows"][0]["raw_row_sha256"] = "abc"
        with self.assertRaisesRegex(ValueError, "INVALID_RAW_ROW_HASH"):
            check(obj)

    def test_test_fold_mismatch(self):
        obj = sample()
        obj["rows"][0]["fold_test_day"] = "2021-07-23"
        with self.assertRaisesRegex(ValueError, "TEST_FOLD_MISMATCH"):
            check(obj)

    def test_same_day_training_rejected(self):
        obj = sample()
        obj["rows"][0]["train_days"] = ["2021-07-22"]
        with self.assertRaisesRegex(ValueError, "TRAIN_TEST_DAY_OVERLAP"):
            check(obj)

    def test_duplicate_training_day_rejected(self):
        obj = sample()
        obj["rows"][0]["train_days"] = ["2021-07-21", "2021-07-21"]
        with self.assertRaisesRegex(ValueError, "TRAIN_TEST_DAY_OVERLAP"):
            check(obj)

    def test_inconsistent_fold_provenance(self):
        obj = sample()
        obj["rows"][1]["train_days"] = ["2021-07-20"]
        with self.assertRaisesRegex(ValueError, "INCONSISTENT_FOLD_PROVENANCE"):
            check(obj)

    def test_invalid_calendar_day(self):
        obj = sample()
        obj["rows"][0]["test_day"] = "2021-02-30"
        with self.assertRaisesRegex(ValueError, "INVALID_ISO_DAY"):
            check(obj)

    def test_empty_rows_rejected(self):
        obj = sample()
        obj["rows"] = []
        with self.assertRaisesRegex(ValueError, "MISSING_PAIRED_ROWS"):
            check(obj)

    def test_source_lineage_mismatch(self):
        obj = sample()
        obj["source_summary_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SOURCE_LINEAGE_MISMATCH"):
            check(obj)

    def test_source_bytes_mutated(self):
        with self.assertRaisesRegex(ValueError, "SOURCE_SHA_MISMATCH"):
            check(sample(), source=SOURCE_RAW + b" ")

    def test_nonempty_evidence_rejected(self):
        evidence = b'{"schema":"hazrob.g2.independent-evidence.v1","records":[{"unverified":true}]}'
        with self.assertRaisesRegex(ValueError, "UNVERIFIED_INDEPENDENT_EVIDENCE"):
            check(sample(), evidence=evidence)

    def test_duplicate_json_key_rejected(self):
        raw = json.dumps(sample()).encode()
        raw = raw.replace(b'"origin": "EXTERNAL_SOURCE_RESEARCH_ONLY"',
                          b'"origin": "EXTERNAL_SOURCE_RESEARCH_ONLY", "origin": "EXTERNAL_SOURCE_RESEARCH_ONLY"')
        with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
            audit(raw, SOURCE_RAW, EVIDENCE_RAW, require_full=False)

    def test_nonfinite_json_rejected(self):
        raw = json.dumps(sample()).encode().replace(b'"rows":', b'"bogus": NaN, "rows":')
        with self.assertRaisesRegex(ValueError, "NONFINITE_JSON_NUMBER"):
            audit(raw, SOURCE_RAW, EVIDENCE_RAW, require_full=False)

    def test_invalid_origin(self):
        obj = sample()
        obj["origin"] = "TANK_G3_VERIFIED"
        with self.assertRaisesRegex(ValueError, "INVALID_ORIGIN"):
            check(obj)


if __name__ == "__main__":
    unittest.main()
