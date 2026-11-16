"""Regression tests: exact source-only MQ4 false-alert day concentration."""
import copy
import json
import unittest

from g2_mq4_false_alert_day_concentration import make_report, run
from g2_mq4_forward_error_burden import run as run_burden
from g2_mq4_receipt_lineage_ci import ROOT


class DayConcentrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.burden = run_burden()

    def test_authenticated_source_and_no_promotion(self):
        result = run()
        self.assertEqual(len(result["models"]), 5)
        self.assertEqual(len(result["calendar_days"]), 7)
        self.assertFalse(result["g2_qualified"])
        self.assertFalse(result["g3_qualified"])
        self.assertFalse(result["independent_tank_exposures"])
        self.assertFalse(result["threshold_is_safety_limit"])

    def test_saved_receipt_exact_semantic_replay(self):
        saved = json.loads((ROOT / "G2_MQ4_FALSE_ALERT_DAY_CONCENTRATION_2026-11-02.json").read_text(encoding="utf-8"))
        self.assertEqual(saved, run())

    def test_ridge_mq4_3_worst_day(self):
        model = make_report(self.burden)["models"]["RIDGE_MQ4_3"]
        self.assertEqual(model["false_positives"], 2367)
        self.assertEqual(model["reference_negatives"], 7900)
        self.assertEqual(model["worst_false_positive_day_or_ties"], ["2021-07-29"])
        self.assertEqual(model["worst_day_share_of_false_positives"],
                         {"numerator": 776, "denominator": 2367})
        self.assertEqual(model["daily_false_positives"]["2021-07-31"]["false_positives"], 613)

    def test_lodo_exact_fractions(self):
        model = make_report(self.burden)["models"]["RIDGE_MQ4_3"]
        self.assertEqual(model["leave_one_day_out_min_false_positive_rate"],
                         [{"excluded_day": "2021-07-29", "numerator": 1591, "denominator": 6943}])
        self.assertEqual(model["leave_one_day_out_max_false_positive_rate"],
                         [{"excluded_day": "2021-07-26", "numerator": 2367, "denominator": 6460}])

    def test_ridge_temperature_worst_day(self):
        model = make_report(self.burden)["models"]["RIDGE_MQ4_3_T_RH"]
        self.assertEqual(model["worst_false_positive_day_or_ties"], ["2021-07-25"])
        self.assertEqual(model["worst_day_share_of_false_positives"],
                         {"numerator": 1145, "denominator": 1955})

    def test_zero_alert_baseline_is_not_success(self):
        model = make_report(self.burden)["models"]["TRAIN_MEDIAN_REFERENCE"]
        self.assertEqual(model["false_positives"], 0)
        self.assertEqual(model["false_positive_days"], [])
        self.assertEqual(model["worst_false_positive_day_or_ties"], [])
        self.assertIsNone(model["worst_day_share_of_false_positives"])
        self.assertEqual(len(model["leave_one_day_out_min_false_positive_rate"]), 7)

    def mutate(self, edit, error):
        changed = copy.deepcopy(self.burden)
        edit(changed)
        with self.assertRaisesRegex(ValueError, error):
            make_report(changed)

    def test_false_promotion_rejected(self):
        self.mutate(lambda b: b.__setitem__("g2_qualified", True), "SOURCE_SCOPE")

    def test_false_safety_limit_rejected(self):
        self.mutate(lambda b: b.__setitem__("threshold_is_safety_limit", True), "SOURCE_SCOPE")

    def test_missing_day_rejected(self):
        self.mutate(lambda b: b["models"]["RIDGE_MQ4_3"]["days"].pop("2021-07-31"), "MODEL_SHAPE")

    def test_negative_false_positives_rejected(self):
        self.mutate(lambda b: b["models"]["RIDGE_MQ4_3"]["days"]["2021-07-31"].__setitem__("fp", -1), "DAY_COUNT")

    def test_boolean_false_positives_rejected(self):
        self.mutate(lambda b: b["models"]["RIDGE_MQ4_3"]["days"]["2021-07-31"].__setitem__("fp", True), "DAY_COUNT")

    def test_tampered_aggregate_rejected(self):
        self.mutate(lambda b: b["models"]["RIDGE_MQ4_3"]["aggregate"].__setitem__("fp", 1), "AGGREGATE_REPLAY")

    def test_tampered_day_confusion_rejected(self):
        self.mutate(lambda b: b["models"]["RIDGE_MQ4_3"]["days"]["2021-07-31"].__setitem__("rows", 1), "DAY_CONFUSION")

    def test_invalid_model_count_rejected(self):
        self.mutate(lambda b: b["models"].pop("RIDGE_MQ4_1"), "SOURCE_SHAPE")


    def test_cross_model_daily_row_transfer_rejected(self):
        # Conserves all aggregate counts while moving reference-negative
        # rows between two dates for just one model.
        def edit(b):
            days = b["models"]["RIDGE_MQ4_3"]["days"]
            days["2021-07-25"]["tn"] -= 1
            days["2021-07-25"]["rows"] -= 1
            days["2021-07-27"]["tn"] += 1
            days["2021-07-27"]["rows"] += 1
        self.mutate(edit, "CROSS_MODEL_DAY_REFERENCE")

    def test_cross_model_daily_label_transfer_rejected(self):
        # Conserves each day's row count and all aggregate confusion counts,
        # but moves one reference-positive label between days.
        def edit(b):
            days = b["models"]["RIDGE_MQ4_3"]["days"]
            days["2021-07-25"]["tp"] -= 1
            days["2021-07-25"]["tn"] += 1
            days["2021-07-25"]["reference_positive"] -= 1
            days["2021-07-27"]["tp"] += 1
            days["2021-07-27"]["tn"] -= 1
            days["2021-07-27"]["reference_positive"] += 1
        self.mutate(edit, "CROSS_MODEL_DAY_REFERENCE")


if __name__ == "__main__":
    unittest.main()
