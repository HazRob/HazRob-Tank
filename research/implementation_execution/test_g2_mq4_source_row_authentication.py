"""Synthetic-only regression tests; no original archive or real predictions supplied."""
import csv
import io
import unittest

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


if __name__ == "__main__":
    unittest.main()
