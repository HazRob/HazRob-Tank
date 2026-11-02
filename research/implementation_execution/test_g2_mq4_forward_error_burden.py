"""Deterministic regressions for source-only MQ4 alert burden (not Tank G2)."""
import copy
import unittest

from g2_mq4_forward_error_burden import evaluate, run, summary
from g2_mq4_receipt_lineage_ci import ROOT, FILENAMES, strict_json


class ErrorBurdenTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = strict_json((ROOT / FILENAMES["source"]).read_bytes())

    def test_authenticated_source_replay(self):
        r = run()
        self.assertEqual(r["rows"], 9325)
        self.assertEqual(r["reference_positives"], 1425)
        self.assertEqual(len(r["models"]), 5)
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["g3_qualified"])

    def test_model_confusion_and_precision(self):
        r = evaluate(self.source)
        a = r["models"]["RIDGE_MQ4_3"]["aggregate"]
        self.assertEqual((a["tp"], a["fn"], a["fp"], a["tn"]),
                         (1289, 136, 2367, 5533))
        self.assertEqual(a["precision_ppv"], round(1289 / 3656, 8))
        self.assertEqual(a["false_positives_per_1000_reference_negatives"],
                         round(2367 * 1000 / 7900, 6))

    def test_baseline_never_alarms_and_precision_undefined(self):
        r = evaluate(self.source)
        a = r["models"]["TRAIN_MEDIAN_REFERENCE"]["aggregate"]
        self.assertEqual(a["predicted_positive"], 0)
        self.assertIsNone(a["precision_ppv"])
        self.assertEqual(a["sensitivity"], 0)
        self.assertEqual(a["specificity"], 1)

    def test_zero_positive_days_are_not_false_negatives(self):
        r = evaluate(self.source)
        self.assertEqual(r["models"]["RIDGE_MQ4_3"]["zero_reference_positive_days"],
                         ["2021-07-26", "2021-07-31"])
        self.assertEqual(
            r["models"]["RIDGE_MQ4_3"]["false_positive_alerts_on_zero_positive_days"],
            613)
        self.assertIsNone(
            r["models"]["RIDGE_MQ4_3"]["days"]["2021-07-31"]["sensitivity"])

    def test_single_class_day_balanced_accuracy_undefined(self):
        a = summary(10, 0, 0, 3)
        self.assertIsNone(a["balanced_accuracy"])
        self.assertIsNone(a["sensitivity"])
        self.assertEqual(a["false_positives_per_1000_reference_negatives"], 300)

    def test_empty_day_denominators_not_invented(self):
        a = summary(0, 0, 0, 0)
        self.assertIsNone(a["precision_ppv"])
        self.assertIsNone(a["sensitivity"])
        self.assertIsNone(a["specificity"])
        self.assertIsNone(a["balanced_accuracy"])

    def mutate(self, edit, pattern):
        source = copy.deepcopy(self.source)
        edit(source)
        with self.assertRaisesRegex(ValueError, pattern):
            evaluate(source)

    def test_tampered_aggregate_fails(self):
        self.mutate(
            lambda s: s["models"]["RIDGE_MQ4_3"]["aggregate"].__setitem__("tp", 1290),
            "AGGREGATE_RECONCILIATION")

    def test_negative_daily_false_positive_fails(self):
        self.mutate(
            lambda s: s["models"]["RIDGE_MQ4_3"]["days"][0].__setitem__("fp", -1),
            "COUNTS_NOT_NONNEGATIVE_INTEGERS")

    def test_impossible_false_negative_fails(self):
        self.mutate(
            lambda s: s["models"]["RIDGE_MQ4_3"]["days"][0].__setitem__("fn", 247),
            "CONFUSION_OUT_OF_RANGE")

    def test_boolean_false_positive_fails(self):
        self.mutate(
            lambda s: s["models"]["RIDGE_MQ4_3"]["days"][0].__setitem__("fp", True),
            "COUNTS_NOT_NONNEGATIVE_INTEGERS")

    def test_wrong_day_order_fails(self):
        self.mutate(
            lambda s: s["models"]["RIDGE_MQ4_3"]["days"][0].__setitem__(
                "day", "2021-07-31"), "DAY_ALIGNMENT")

    def test_unsupported_safety_claim_fails(self):
        self.mutate(
            lambda s: s.__setitem__("threshold_4ppm_is_safety_limit", True),
            "SOURCE_SCOPE")


if __name__ == "__main__":
    unittest.main()
