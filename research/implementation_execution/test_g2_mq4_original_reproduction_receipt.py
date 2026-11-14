"""Deterministic CI receipt integrity tests; original source ZIP is NOT in CI."""
import copy
import json
import unittest
from pathlib import Path

RECEIPT = Path(__file__).with_name("G2_MQ4_ORIGINAL_SOURCE_PAIRED_REPRODUCTION_2026-11-01.json")
NAMES = ["TRAIN_MEDIAN_REFERENCE", "RIDGE_MQ4_1", "RIDGE_MQ4_3",
         "RIDGE_MQ4_3_T_RH", "HGB_MQ4_3_T_RH"]
PINS = {
    "source_archive_sha256": "7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9",
    "source_csv_member_sha256": "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d",
    "benchmark_script_sha256": "367658f9884ff8355c89d803f67eb7ced78ec7299759d58ddd1742ba9a1b368b",
    "source_summary_sha256": "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea",
}
NO_PROMOTION = ("independent_exposure_groups_verified", "instrument_calibration_certificate_verified",
                "independent_model_implementation_verified", "prospective_validation_verified",
                "paired_inferential_test_authorized", "g2_qualified", "g3_qualified")


def audit_receipt(r):
    if not isinstance(r, dict) or r.get("schema") != "hazrob.g2.mq4-original-archive-recomputed-paired-research.v1":
        raise ValueError("SCHEMA")
    if any(r.get(k) != v for k, v in PINS.items()):
        raise ValueError("SOURCE_LINEAGE")
    if (r.get("source_rows") != 14186 or r.get("prediction_rows") != 70930
            or r.get("model_count") != 5 or r.get("folds_per_model") != 11):
        raise ValueError("SOURCE_COUNTS")
    if r.get("retrospective_future_training_folds") != 10 or r.get("retrospective_future_calibration_folds") != 1:
        raise ValueError("FUTURE_FIT_DISCLOSURE")
    if any(r.get(k) is not False for k in NO_PROMOTION):
        raise ValueError("UNSUPPORTED_PROMOTION")
    models = r.get("models")
    if not isinstance(models, list) or [m.get("model") for m in models] != NAMES:
        raise ValueError("MODEL_IDENTITIES")
    for m in models:
        try:
            tp, fn, fp, tn = [int(m[k]) for k in (
                "total_4ppm_true_positive", "total_4ppm_false_negative",
                "total_4ppm_false_positive", "total_4ppm_true_negative")]
        except (ValueError, KeyError, TypeError) as exc:
            raise ValueError("MODEL_COUNTS") from exc
        if min(tp, fn, fp, tn) < 0 or tp + fn != 2346 or fp + tn != 11840:
            raise ValueError("MODEL_MARGINALS")
    pairs = r.get("paired_error_discordance_descriptive")
    if not isinstance(pairs, dict) or len(pairs) != 10:
        raise ValueError("PAIR_SCOPE")
    for i, a in enumerate(NAMES):
        for b in NAMES[i + 1:]:
            p = pairs.get(a + "|" + b)
            keys = ("both_correct", "a_correct_b_wrong", "a_wrong_b_correct", "both_wrong")
            if not isinstance(p, dict) or set(p) != set(keys):
                raise ValueError("PAIR_FIELDS")
            if any(type(p[k]) is not int or p[k] < 0 for k in keys) or sum(p.values()) != 14186:
                raise ValueError("PAIR_TOTAL")
            ma, mb = models[NAMES.index(a)], models[NAMES.index(b)]
            ea = int(ma["total_4ppm_false_negative"]) + int(ma["total_4ppm_false_positive"])
            eb = int(mb["total_4ppm_false_negative"]) + int(mb["total_4ppm_false_positive"])
            if p["a_wrong_b_correct"] + p["both_wrong"] != ea or p["a_correct_b_wrong"] + p["both_wrong"] != eb:
                raise ValueError("PAIR_MARGINALS")
    return True


class MQ4OriginalSourceReceiptTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.valid = json.loads(RECEIPT.read_text(encoding="utf-8"))

    def test_pinned_receipt(self):
        self.assertTrue(audit_receipt(self.valid))

    def test_source_lineage_mutation(self):
        r = copy.deepcopy(self.valid)
        r["source_csv_member_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SOURCE_LINEAGE"):
            audit_receipt(r)

    def test_paired_total_mutation(self):
        r = copy.deepcopy(self.valid)
        next(iter(r["paired_error_discordance_descriptive"].values()))["both_correct"] += 1
        with self.assertRaisesRegex(ValueError, "PAIR_TOTAL"):
            audit_receipt(r)

    def test_paired_marginal_mutation(self):
        r = copy.deepcopy(self.valid)
        p = next(iter(r["paired_error_discordance_descriptive"].values()))
        p["both_correct"] -= 1
        p["a_wrong_b_correct"] += 1
        with self.assertRaisesRegex(ValueError, "PAIR_MARGINALS"):
            audit_receipt(r)

    def test_g2_promotion_mutation(self):
        r = copy.deepcopy(self.valid)
        r["g2_qualified"] = True
        with self.assertRaisesRegex(ValueError, "UNSUPPORTED_PROMOTION"):
            audit_receipt(r)

    def test_future_training_flag_mutation(self):
        r = copy.deepcopy(self.valid)
        r["retrospective_future_training_folds"] = 0
        with self.assertRaisesRegex(ValueError, "FUTURE_FIT_DISCLOSURE"):
            audit_receipt(r)

    def test_incomplete_pair_set(self):
        r = copy.deepcopy(self.valid)
        r["paired_error_discordance_descriptive"].pop(next(iter(r["paired_error_discordance_descriptive"])))
        with self.assertRaisesRegex(ValueError, "PAIR_SCOPE"):
            audit_receipt(r)


if __name__ == "__main__":
    unittest.main()
