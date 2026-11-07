"""Deterministic metadata tests: external MQ4 receipts, no Tank G2 promotion."""
import copy
import unittest

from g2_mq4_cross_receipt_raw_forward import audit, compare
from g2_mq4_receipt_lineage_ci import strict_json
from g2_mq4_cross_receipt_raw_forward import RAW, FORWARD


class MQ4CrossReceiptTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = strict_json(RAW.read_bytes())
        cls.forward = strict_json(FORWARD.read_bytes())

    def mutate(self, which, action):
        raw, forward = copy.deepcopy(self.raw), copy.deepcopy(self.forward)
        action(raw if which == "raw" else forward)
        with self.assertRaises(ValueError):
            compare(raw, forward)

    def test_actual_pinned_receipts_and_lineage(self):
        result = audit()
        self.assertEqual(result["matched_test_days"], 7)
        self.assertEqual(result["matched_test_rows"], 9325)
        self.assertEqual(result["reference_positives"], 1425)
        self.assertFalse(result["g2_qualified"])
        self.assertFalse(result["g3_qualified"])
        self.assertFalse(result["original_zip_replayed_in_ci"])

    def test_zip_digest_conflict(self):
        self.mutate("forward", lambda x: x.__setitem__("zip_sha256", "0" * 64))

    def test_csv_digest_conflict(self):
        self.mutate("forward", lambda x: x.__setitem__("csv_sha256", "0" * 64))

    def test_source_row_conflict(self):
        self.mutate("forward", lambda x: x.__setitem__("source_rows", 1))

    def test_day_count_conflict(self):
        self.mutate("forward", lambda x: x["test_rows_by_day"].__setitem__("2021-07-26", 1))

    def test_day_missing(self):
        self.mutate("forward", lambda x: x["test_rows_by_day"].pop("2021-07-26"))

    def test_day_duplicate(self):
        self.mutate("forward", lambda x: x["test_days"].append("2021-07-26"))

    def test_false_positive_impossible_below_threshold(self):
        self.mutate("forward", lambda x: x["positive_by_test_day"].__setitem__("2021-07-26", 1))

    def test_mixed_range_cannot_have_zero_positives(self):
        self.mutate("forward", lambda x: x["positive_by_test_day"].__setitem__("2021-07-25", 0))

    def test_mixed_range_cannot_have_all_positives(self):
        self.mutate("forward", lambda x: x["positive_by_test_day"].__setitem__("2021-07-25", 1436))

    def test_positive_sum_conflict(self):
        self.mutate("forward", lambda x: x["positive_by_test_day"].__setitem__("2021-07-29", 460))

    def test_approval_claim_conflict(self):
        self.mutate("forward", lambda x: x.__setitem__("g2_qualified", True))

    def test_bool_row_count_rejected(self):
        self.mutate("forward", lambda x: x["test_rows_by_day"].__setitem__("2021-07-26", True))

    def test_reference_min_above_threshold_requires_all_positive(self):
        self.mutate("raw", lambda x: x["daily_compact"]["2021-07-25"].__setitem__("ref_min_ppm", 4.0))

    def test_reference_max_below_threshold_requires_zero_positive(self):
        self.mutate("raw", lambda x: x["daily_compact"]["2021-07-25"].__setitem__("ref_max_ppm", 3.99))


if __name__ == "__main__":
    unittest.main()
