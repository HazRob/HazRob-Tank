"""Fail-closed G2 metric-name controls; synthetic metadata, not Tank evidence."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_evidence_audit import audit_record
from test_g2_evidence_audit import eligible_fixture


class G2MetricNameAllowlistTests(unittest.TestCase):
    def test_supported_exact_metric_names_do_not_raise_name_blocker(self):
        names = (
            "classification_accuracy", "balanced_accuracy", "precision",
            "recall", "specificity", "sensitivity", "f1_score", "roc_auc",
            "positive_predictive_value", "negative_predictive_value",
            "false_positive_rate", "false_negative_rate",
            "rmse", "mae", "matthews_corrcoef",
        )
        for name in names:
            with self.subTest(metric=name):
                record = eligible_fixture()
                record["metric_name"] = name
                result = audit_record(record)
                self.assertNotIn(
                    "UNSUPPORTED_OR_NONCANONICAL_METRIC_NAME", result["issues"])
                self.assertEqual(result["status"], "G2_BLOCKED")

    def test_unrecognized_and_near_aliases_fail_closed(self):
        names = (
            "accuracy", "Accuracy", "classification_accuracy ",
            " classification_accuracy", "CLASSIFICATION_ACCURACY",
            "f1-score", "r2_score", "auc", "RMSE", "matthews_corrcoef\t",
            "ｒｍｓｅ",
        )
        for name in names:
            with self.subTest(metric=name):
                record = eligible_fixture()
                record["metric_name"] = name
                result = audit_record(record)
                self.assertIn(
                    "UNSUPPORTED_OR_NONCANONICAL_METRIC_NAME", result["issues"])
                self.assertEqual(result["status"], "G2_BLOCKED")

    def test_malformed_metric_names_do_not_raise_or_promote(self):
        for name in (None, "", "   ", 0, False, [], {}, ["rmse"]):
            with self.subTest(metric=repr(name)):
                record = eligible_fixture()
                record["metric_name"] = name
                result = audit_record(record)
                self.assertIn("INVALID_METRIC_NAME", result["issues"])
                self.assertEqual(result["status"], "G2_BLOCKED")

    def test_unknown_metric_remains_blocked_even_with_valid_numeric_domain(self):
        record = eligible_fixture()
        record.update(metric_name="custom_metric", metric_value=0.8,
                      baseline_metric_value=0.5)
        record["confidence_interval"].update(low=0.7, high=0.9)
        result = audit_record(record)
        self.assertIn("UNSUPPORTED_OR_NONCANONICAL_METRIC_NAME", result["issues"])
        self.assertEqual(result["status"], "G2_BLOCKED")


if __name__ == "__main__":
    unittest.main()
