"""Synthetic-only regression tests; no original archive or real predictions supplied."""
import csv
import io
import json
import unittest
import zipfile
from unittest.mock import patch

import g2_mq4_source_row_authentication as auth

from g2_mq4_source_row_authentication import (
    SOURCE_ID, authenticate_rows, authenticate_reference_labels, canonical_row_sha256,
    verify_original_archive, sha256,
)

COLS = ["datetime", "licor_ch4", "MQ4_1", "MQ4_2", "MQ4_3"] + [
    f"other_{i}" for i in range(15)
]
VALUES = [
    ["2021-07-21 11:06:00", "2000", "1", "2", "3"] + ["0"] * 15,
    ["2021-07-21 11:07:00", "3000", "2", "3", "4"] + ["0"] * 15,
    ["2021-07-22 11:06:00", "4000", "3", "4", "5"] + ["0"] * 15,
]


def fixture(values=VALUES):
    buf = io.StringIO(newline="")
    writer = csv.writer(buf, lineterminator="\n")
    writer.writerow(COLS)
    writer.writerows(values)
    raw = buf.getvalue().encode("utf-8")
    rows = []
    for i, vals in enumerate(values, 1):
        day = vals[0][:10]
        rows.append({
            "row_id": f"{SOURCE_ID}:{i:06d}",
            "test_day": day,
            "fold_test_day": day,
            "raw_row_sha256": canonical_row_sha256(COLS, vals),
        })
    return raw, rows


class SyntheticRowAuthenticationTests(unittest.TestCase):
    def test_valid_complete_source_match(self):
        raw, rows = fixture()
        report = authenticate_rows(raw, rows)
        self.assertEqual(report["authenticated_submitted_rows"], 3)
        self.assertTrue(report["complete_source_row_coverage"])
        self.assertFalse(report["g2_qualified"])
        self.assertFalse(report["g3_qualified"])
        self.assertFalse(report["model_predictions_independently_reproduced"])

    def test_valid_partial_does_not_claim_complete(self):
        raw, rows = fixture()
        report = authenticate_rows(raw, rows[:1], require_full=False)
        self.assertEqual(report["authenticated_submitted_rows"], 1)
        self.assertFalse(report["complete_source_row_coverage"])

    def test_missing_row_fails_closed(self):
        raw, rows = fixture()
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_COVERAGE_MISMATCH"):
            authenticate_rows(raw, rows[:-1])

    def test_duplicate_id_fails(self):
        raw, rows = fixture()
        with self.assertRaisesRegex(ValueError, "DUPLICATE_SUBMITTED_SOURCE_ROW"):
            authenticate_rows(raw, [rows[0], rows[0], rows[2]])

    def test_unrecognized_id_fails(self):
        raw, rows = fixture()
        rows[1]["row_id"] = f"{SOURCE_ID}:999999"
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_ID_MISMATCH"):
            authenticate_rows(raw, rows)

    def test_changed_day_fails(self):
        raw, rows = fixture()
        rows[1]["test_day"] = "2021-07-22"
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_DAY_MISMATCH"):
            authenticate_rows(raw, rows)

    def test_changed_fold_day_fails(self):
        raw, rows = fixture()
        rows[1]["fold_test_day"] = "2021-07-22"
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_DAY_MISMATCH"):
            authenticate_rows(raw, rows)

    def test_changed_digest_fails(self):
        raw, rows = fixture()
        rows[1]["raw_row_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_DIGEST_MISMATCH"):
            authenticate_rows(raw, rows)

    def test_source_cell_tamper_fails(self):
        raw, rows = fixture()
        mutated = raw.replace(b"3000,2,3,4", b"3001,2,3,4")
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_DIGEST_MISMATCH"):
            authenticate_rows(mutated, rows)

    def test_invalid_source_header_fails(self):
        raw, rows = fixture()
        mutated = raw.replace(b"datetime,", b"bad_time,", 1)
        with self.assertRaisesRegex(ValueError, "INVALID_SOURCE_HEADER"):
            authenticate_rows(mutated, rows)

    def test_short_source_row_fails(self):
        raw, rows = fixture()
        mutated = raw.replace(b"3000,2,3,4", b"3000,2,3")
        with self.assertRaisesRegex(ValueError, "SOURCE_COLUMN_COUNT_MISMATCH"):
            authenticate_rows(mutated, rows)

    def test_canonical_digest_independent_of_csv_quoting(self):
        raw, rows = fixture()
        quoted = raw.replace(b"3000,2,3,4", b'"3000",2,3,4')
        self.assertEqual(authenticate_rows(raw, rows),
                         authenticate_rows(quoted, rows))
        self.assertNotEqual(sha256(raw), sha256(quoted))

    def test_wrong_archive_hash_fails_before_unzipping(self):
        with self.assertRaisesRegex(ValueError, "ARCHIVE_SHA_MISMATCH"):
            verify_original_archive(b"not the original ZIP", b"{}")

    def test_empty_submission_fails(self):
        raw, _ = fixture()
        with self.assertRaisesRegex(ValueError, "NO_SUBMITTED_ROWS"):
            authenticate_rows(raw, [])

    def test_non_dict_submission_fails(self):
        raw, rows = fixture()
        rows[0] = None
        with self.assertRaisesRegex(ValueError, "INVALID_SUBMITTED_ROW"):
            authenticate_rows(raw, rows)

    def test_source_reference_labels_recomputed(self):
        raw, rows = fixture()
        for row, vals in zip(rows, VALUES):
            row["reference_positive_4ppm"] = float(vals[1]) / 1000.0 >= 4.0
        report = authenticate_reference_labels(raw, rows)
        self.assertEqual(report["reference_positive_rows"], 1)
        self.assertEqual(report["authenticated_reference_labels"], 3)
        self.assertFalse(report["g2_qualified"])
        self.assertFalse(report["model_predictions_independently_reproduced"])

    def test_truth_label_permutation_rejected_despite_same_marginal(self):
        raw, rows = fixture()
        for row, vals in zip(rows, VALUES):
            row["reference_positive_4ppm"] = float(vals[1]) / 1000.0 >= 4.0
        rows[0]["reference_positive_4ppm"] = True
        rows[2]["reference_positive_4ppm"] = False
        self.assertEqual(sum(r["reference_positive_4ppm"] for r in rows), 1)
        with self.assertRaisesRegex(ValueError, "SOURCE_REFERENCE_LABEL_MISMATCH"):
            authenticate_reference_labels(raw, rows)

    def test_reference_threshold_exactly_four_ppm_is_positive(self):
        raw, rows = fixture()
        for row, vals in zip(rows, VALUES):
            row["reference_positive_4ppm"] = float(vals[1]) / 1000.0 >= 4.0
        self.assertTrue(rows[2]["reference_positive_4ppm"])
        self.assertEqual(authenticate_reference_labels(raw, rows)["reference_positive_rows"], 1)

    def test_reference_threshold_below_four_ppm_is_negative(self):
        vals = [list(v) for v in VALUES]
        vals[2][1] = "3999.999"
        raw, rows = fixture(vals)
        for row in rows:
            row["reference_positive_4ppm"] = False
        self.assertEqual(authenticate_reference_labels(raw, rows)["reference_positive_rows"], 0)

    def test_reference_nonboolean_rejected(self):
        raw, rows = fixture()
        for row, vals in zip(rows, VALUES):
            row["reference_positive_4ppm"] = float(vals[1]) / 1000.0 >= 4.0
        rows[2]["reference_positive_4ppm"] = 1
        with self.assertRaisesRegex(ValueError, "INVALID_SUBMITTED_REFERENCE_LABEL"):
            authenticate_reference_labels(raw, rows)

    def test_reference_nonfinite_source_rejected(self):
        vals = [list(v) for v in VALUES]
        vals[1][1] = "nan"
        raw, rows = fixture(vals)
        for row in rows:
            row["reference_positive_4ppm"] = False
        with self.assertRaisesRegex(ValueError, "INVALID_SOURCE_REFERENCE_PPB"):
            authenticate_reference_labels(raw, rows)

    def test_reference_missing_source_rejected(self):
        vals = [list(v) for v in VALUES]
        vals[1][1] = ""
        raw, rows = fixture(vals)
        for row in rows:
            row["reference_positive_4ppm"] = False
        with self.assertRaisesRegex(ValueError, "INVALID_SOURCE_REFERENCE_PPB"):
            authenticate_reference_labels(raw, rows)

    def test_reference_incomplete_coverage_rejected(self):
        raw, rows = fixture()
        rows[0]["reference_positive_4ppm"] = False
        with self.assertRaisesRegex(ValueError, "SOURCE_ROW_COVERAGE_MISMATCH"):
            authenticate_reference_labels(raw, rows[:1])

    def test_reference_partial_coverage_not_certified(self):
        raw, rows = fixture()
        rows[0]["reference_positive_4ppm"] = False
        report = authenticate_reference_labels(raw, rows[:1], require_full=False)
        self.assertFalse(report["complete_reference_label_coverage"])
        self.assertFalse(report["g2_qualified"])


class SyntheticReplayMetadataTests(unittest.TestCase):
    """Synthetic ZIP and replay; no original MQ4 archive is bundled."""

    def setUp(self):
        self.raw, _ = fixture()
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr(auth.MEMBER, self.raw)
        self.archive = buffer.getvalue()
        for key, value in (
                ("ARCHIVE_SHA", auth.sha256(self.archive)),
                ("MEMBER_SHA", auth.sha256(self.raw)),
                ("PINNED_MEMBER_BYTES", len(self.raw))):
            p = patch.object(auth, key, value)
            p.start()
            self.addCleanup(p.stop)
        self.replay = {
            "schema": "hazrob.g2.mq4-original-zip-raw-replay.v1",
            "source_id": auth.SOURCE_ID,
            "archive_sha256": auth.ARCHIVE_SHA,
            "raw_member_sha256": auth.MEMBER_SHA,
            "raw_member_bytes": len(self.raw),
            "raw_replayed_in_this_execution": True,
            "summary_fields_verified": True,
            "g2_qualified": False,
            "g3_tank_qualified": False,
            "physical_exposure_independence_verified": False,
            "reference_calibration_certificate_verified": False,
            "blinded_holdout_verified": False,
            "derived": auth.derive(self.raw),
        }

    def verify(self):
        return auth.verify_original_archive(
            self.archive, json.dumps(self.replay).encode("utf-8"))

    def test_complete_synthetic_replay_accepted_without_g2(self):
        self.assertEqual(self.verify(), self.raw)
        self.assertFalse(self.replay["g2_qualified"])

    def test_replay_schema_change_rejected(self):
        self.replay["schema"] = "unversioned"
        with self.assertRaisesRegex(ValueError, "REPLAY_SCHEMA_MISMATCH"):
            self.verify()

    def test_replay_source_id_change_rejected(self):
        self.replay["source_id"] = "OTHER-SOURCE"
        with self.assertRaisesRegex(ValueError, "REPLAY_LINEAGE_MISMATCH"):
            self.verify()

    def test_replay_member_length_change_rejected(self):
        self.replay["raw_member_bytes"] += 1
        with self.assertRaisesRegex(ValueError, "REPLAY_MEMBER_SIZE_MISMATCH"):
            self.verify()

    def test_boolean_member_length_rejected(self):
        self.replay["raw_member_bytes"] = True
        with self.assertRaisesRegex(ValueError, "REPLAY_MEMBER_SIZE_MISMATCH"):
            self.verify()

    def test_replay_not_replayed_rejected(self):
        self.replay["raw_replayed_in_this_execution"] = False
        with self.assertRaisesRegex(ValueError, "REPLAY_COMPLETENESS_MISMATCH"):
            self.verify()

    def test_replay_summary_not_verified_rejected(self):
        self.replay["summary_fields_verified"] = False
        with self.assertRaisesRegex(ValueError, "REPLAY_COMPLETENESS_MISMATCH"):
            self.verify()

    def test_all_unsupported_scientific_promotions_rejected(self):
        for field in ("g2_qualified", "g3_tank_qualified",
                      "physical_exposure_independence_verified",
                      "reference_calibration_certificate_verified",
                      "blinded_holdout_verified"):
            with self.subTest(field=field):
                self.replay[field] = True
                with self.assertRaisesRegex(
                        ValueError, "REPLAY_UNSUPPORTED_SCIENTIFIC_PROMOTION"):
                    self.verify()
                self.replay[field] = False

    def test_missing_scientific_status_rejected(self):
        del self.replay["blinded_holdout_verified"]
        with self.assertRaisesRegex(
                ValueError, "REPLAY_UNSUPPORTED_SCIENTIFIC_PROMOTION"):
            self.verify()

    def test_replay_missing_derived_rejected(self):
        del self.replay["derived"]
        with self.assertRaisesRegex(ValueError, "REPLAY_DERIVED_MISSING"):
            self.verify()

    def test_replay_derived_mutation_rejected(self):
        self.replay["derived"]["rows"] += 1
        with self.assertRaisesRegex(
                ValueError, "SOURCE_REPLAY_DERIVATION_MISMATCH"):
            self.verify()

    def test_replay_member_sha_mutation_rejected(self):
        self.replay["raw_member_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "REPLAY_LINEAGE_MISMATCH"):
            self.verify()


if __name__ == "__main__":
    unittest.main()
