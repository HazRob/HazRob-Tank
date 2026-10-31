"""Synthetic declared-sample true-class ambiguity regressions (not G2 evidence)."""
import tempfile
import unittest
from pathlib import Path

from g2_paired_prediction_replay import replay_paired_predictions

PRED_HEADER = (
    "exposure_id,group_id,true_reference,predicted_reference,"
    "baseline_predicted_reference,physical_sample_id\n"
)
METRIC = "group_id,sample_count,correct_count,baseline_correct_count\na,2,1,1\nb,2,1,1\n"
MIXED = (
    "e1,a,A,A,B,s1\n"
    "e2,a,B,A,B,s1\n"
    "e3,b,A,A,A,s2\n"
    "e4,b,B,A,A,s3\n"
)


class DeclaredSampleTrueClassAuditTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.pred = Path(temporary.name) / "predictions.csv"
        self.metrics = Path(temporary.name) / "metrics.csv"
        self.metrics.write_text(METRIC, encoding="utf-8")

    def replay(self, rows=MIXED, header=PRED_HEADER):
        self.pred.write_text(header + rows, encoding="utf-8")
        return replay_paired_predictions(self.pred, self.metrics, resamples=1000)

    def test_mixed_reference_classes_for_same_sample_are_flagged(self):
        result = self.replay()
        self.assertTrue(result["physical_sample_true_class_consistency_checked"])
        self.assertEqual(result["declared_samples_with_multiple_true_reference_classes"], ["s1"])
        self.assertEqual(result["declared_samples_with_single_true_reference_class"], 2)
        diag = result["declared_physical_sample_equal_weight_diagnostic"]
        self.assertEqual(diag["mixed_true_reference_declared_sample_count"], 1)
        self.assertFalse(diag["all_declared_samples_single_true_reference_class"])
        self.assertIn("NOT_AUTHENTICATED_SAMPLE", result["sample_true_class_scope"])

    def test_same_class_repeated_reads_are_not_misflagged(self):
        rows = (
            "e1,a,A,A,B,s1\n"
            "e2,a,A,B,A,s1\n"
            "e3,b,A,A,A,s2\n"
            "e4,b,B,A,A,s3\n"
        )
        result = self.replay(rows)
        self.assertEqual(result["declared_samples_with_multiple_true_reference_classes"], [])
        self.assertEqual(result["declared_samples_with_single_true_reference_class"], 3)
        self.assertTrue(result["declared_physical_sample_equal_weight_diagnostic"]
                        ["all_declared_samples_single_true_reference_class"])

    def test_missing_sample_column_is_not_misreported_as_consistent(self):
        header = PRED_HEADER.replace(",physical_sample_id", "")
        rows = "\n".join(",".join(line.split(",")[:-1])
                         for line in MIXED.strip().splitlines()) + "\n"
        result = self.replay(rows, header)
        self.assertFalse(result["physical_sample_true_class_consistency_checked"])
        self.assertIsNone(result["declared_samples_with_multiple_true_reference_classes"])
        self.assertIsNone(result["declared_samples_with_single_true_reference_class"])
        self.assertIsNone(result["declared_physical_sample_equal_weight_diagnostic"])

    def test_row_order_invariant_with_arabic_labels(self):
        rows = (
            "e1,a,إيثانول,إيثانول,أسيتون,عينة-١\n"
            "e2,a,أسيتون,إيثانول,أسيتون,عينة-١\n"
            "e3,b,إيثانول,إيثانول,إيثانول,عينة-٢\n"
            "e4,b,أسيتون,إيثانول,إيثانول,عينة-٣\n"
        )
        before = self.replay(rows)
        after = self.replay("".join(reversed(rows.splitlines(keepends=True))))
        self.assertEqual(before["declared_samples_with_multiple_true_reference_classes"],
                         after["declared_samples_with_multiple_true_reference_classes"])
        self.assertEqual(before["declared_samples_with_multiple_true_reference_classes"],
                         ["عينة-١"])

    def test_cross_group_reuse_still_fails_closed(self):
        rows = MIXED.replace("e3,b,A,A,A,s2", "e3,b,A,A,A,s1")
        with self.assertRaisesRegex(ValueError, "PHYSICAL_SAMPLE_GROUP_LEAKAGE"):
            self.replay(rows)


if __name__ == "__main__":
    unittest.main()
