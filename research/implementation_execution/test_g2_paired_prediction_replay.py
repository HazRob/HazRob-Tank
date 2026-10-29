"""Synthetic post-blind pairing controls; NOT original physical measurements."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from g2_paired_prediction_replay import replay_paired_predictions


PRED_HEADER = (
    "exposure_id,group_id,true_reference,predicted_reference,"
    "baseline_predicted_reference\n"
)
METRIC_HEADER = (
    "group_id,sample_count,correct_count,baseline_correct_count\n"
)
PRED_ROWS = (
    "e1,a,A,A,B\n"
    "e2,a,B,A,B\n"
    "e3,b,A,A,A\n"
    "e4,b,B,A,A\n"
)
METRIC_ROWS = "a,2,1,1\nb,2,1,1\n"


class PairedPredictionReplayTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        self.pred = root / "predictions_blind.csv"
        self.metrics = root / "metrics_per_group.csv"
        self.write()

    def write(self, pred=PRED_ROWS, metrics=METRIC_ROWS,
              pred_header=PRED_HEADER, metrics_header=METRIC_HEADER):
        self.pred.write_text(pred_header + pred, encoding="utf-8")
        self.metrics.write_text(metrics_header + metrics, encoding="utf-8")

    def replay(self):
        return replay_paired_predictions(self.pred, self.metrics, resamples=1000)

    def test_exact_paired_discordance_and_group_metrics(self):
        result = self.replay()
        self.assertEqual(result["test_exposures"], 4)
        self.assertEqual(result["groups"], 2)
        self.assertEqual(result["observed_delta"], 0)
        self.assertEqual(result["paired_both_correct"], 1)
        self.assertEqual(result["paired_both_wrong"], 1)
        self.assertEqual(result["paired_model_only_correct"], 1)
        self.assertEqual(result["paired_baseline_only_correct"], 1)
        self.assertTrue(result["paired_outcomes_recomputed_from_same_exposure_rows"])
        self.assertTrue(result["per_group_metrics_recomputed_from_prediction_rows"])
        self.assertIn("NOT_PHYSICAL_GROUP_INDEPENDENCE", result["pairing_scope"])
        self.assertIn("NOT_G2_OR_G3_APPROVAL", result["scope"])

    def test_exact_input_byte_sha256_and_assignment_digest(self):
        result = self.replay()
        self.assertEqual(result["predictions_csv_sha256"],
                         hashlib.sha256(self.pred.read_bytes()).hexdigest())
        self.assertEqual(result["metrics_per_group_csv_sha256"],
                         hashlib.sha256(self.metrics.read_bytes()).hexdigest())
        expected = json.dumps([["e1", "a"], ["e2", "a"], ["e3", "b"],
                               ["e4", "b"]], ensure_ascii=False,
                              separators=(",", ":")).encode("utf-8")
        self.assertEqual(result["exposure_group_assignment_sha256"],
                         hashlib.sha256(expected).hexdigest())

    def test_metric_numerator_or_denominator_forgery_rejected(self):
        for replacement in ("a,2,2,1\nb,2,1,1\n",
                            "a,3,1,1\nb,2,1,1\n",
                            "a,2,1,0\nb,2,1,1\n"):
            with self.subTest(replacement=replacement):
                self.write(metrics=replacement)
                with self.assertRaisesRegex(ValueError, "METRIC_COUNT_MISMATCH"):
                    self.replay()

    def test_duplicate_exposure_across_groups_rejected(self):
        self.write(pred=PRED_ROWS.replace("e3,b,", "e1,b,"))
        with self.assertRaisesRegex(ValueError, "DUPLICATE_EXPOSURE_ID"):
            self.replay()

    def test_missing_extra_or_duplicate_metric_groups_rejected(self):
        for rows, expected in (
            ("a,2,1,1\n", "METRIC_GROUP_MISMATCH"),
            ("a,2,1,1\nb,2,1,1\nc,1,1,1\n", "METRIC_GROUP_MISMATCH"),
            ("a,2,1,1\na,2,1,1\nb,2,1,1\n", "DUPLICATE_METRIC_GROUP"),
        ):
            with self.subTest(rows=rows):
                self.write(metrics=rows)
                with self.assertRaisesRegex(ValueError, expected):
                    self.replay()

    def test_noncanonical_identity_and_header_alias_rejected(self):
        self.write(pred=PRED_ROWS.replace("e3,b,", "ｅ3,b,"))
        with self.assertRaisesRegex(ValueError, "PHYSICAL_ID_INVALID"):
            self.replay()
        self.write(pred_header=PRED_HEADER.replace("group_id", "ｇroup_id"))
        with self.assertRaisesRegex(ValueError, "CSV_HEADER_INVALID"):
            self.replay()

    def test_noncanonical_integer_or_malformed_csv_rejected(self):
        self.write(metrics="a,02,1,1\nb,2,1,1\n")
        with self.assertRaisesRegex(ValueError, "METRIC_COUNT_INVALID"):
            self.replay()
        self.write(pred=PRED_ROWS + "e5,a,A\n")
        with self.assertRaisesRegex(ValueError, "CSV_ROW_INVALID"):
            self.replay()
        self.write(pred=PRED_ROWS + "e5,a,A,A,A,EXTRA\n")
        with self.assertRaisesRegex(ValueError, "CSV_ROW_INVALID"):
            self.replay()

    def test_row_order_invariance_of_arithmetic_and_assignment(self):
        original = self.replay()
        self.write(pred="".join(reversed(PRED_ROWS.splitlines(keepends=True))),
                   metrics="b,2,1,1\na,2,1,1\n")
        reordered = self.replay()
        for field in ("observed_delta", "interval_low", "interval_high",
                      "paired_both_correct", "paired_model_only_correct",
                      "paired_baseline_only_correct",
                      "exposure_group_assignment_sha256"):
            self.assertEqual(original[field], reordered[field])
        self.assertNotEqual(original["predictions_csv_sha256"],
                            reordered["predictions_csv_sha256"])

    def test_arabic_truth_labels_preserved(self):
        self.write(pred=("e1,a,إيثانول,إيثانول,أسيتون\n"
                         "e2,a,أسيتون,إيثانول,أسيتون\n"
                         "e3,b,إيثانول,إيثانول,إيثانول\n"
                         "e4,b,أسيتون,إيثانول,إيثانول\n"))
        result = self.replay()
        self.assertEqual(result["paired_model_only_correct"], 1)
        self.assertEqual(result["paired_baseline_only_correct"], 1)

    def test_paired_outcomes_reconciled_for_each_declared_group(self):
        result = self.replay()
        groups = result["paired_outcomes_by_declared_physical_group"]
        self.assertEqual([g["group_id"] for g in groups], ["a", "b"])
        self.assertEqual(groups[0], {
            "group_id": "a", "exposures": 2,
            "both_correct": 0, "both_wrong": 0,
            "model_only_correct": 1, "baseline_only_correct": 1,
            "paired_net_model_advantage_count": 0,
        })
        self.assertEqual(groups[1], {
            "group_id": "b", "exposures": 2,
            "both_correct": 1, "both_wrong": 1,
            "model_only_correct": 0, "baseline_only_correct": 0,
            "paired_net_model_advantage_count": 0,
        })

    def test_semantic_prediction_fingerprint_is_order_invariant(self):
        original = self.replay()
        self.write(pred="".join(reversed(PRED_ROWS.splitlines(keepends=True))),
                   metrics="b,2,1,1\\na,2,1,1\\n")
        reordered = self.replay()
        self.assertEqual(original["prediction_rows_semantic_sha256"],
                         reordered["prediction_rows_semantic_sha256"])
        self.assertEqual(original["paired_outcomes_by_declared_physical_group"],
                         reordered["paired_outcomes_by_declared_physical_group"])

    def test_semantic_fingerprint_detects_wrong_label_changes_without_count_change(self):
        original = self.replay()
        self.write(pred=PRED_ROWS.replace("e2,a,B,A,B", "e2,a,B,C,B"))
        changed = self.replay()
        self.assertEqual(original["exposure_group_assignment_sha256"],
                         changed["exposure_group_assignment_sha256"])
        self.assertEqual(original["paired_outcomes_by_declared_physical_group"],
                         changed["paired_outcomes_by_declared_physical_group"])
        self.assertNotEqual(original["prediction_rows_semantic_sha256"],
                            changed["prediction_rows_semantic_sha256"])

    def test_per_group_opposing_discordance_not_hidden_by_pooled_tie(self):
        # Two groups have opposite paired net advantages: pooled delta zero.
        self.write(pred=("e1,a,A,A,B\\n"
                         "e2,a,B,B,A\\n"
                         "e3,b,A,B,A\\n"
                         "e4,b,B,A,B\\n"),
                   metrics="a,2,2,0\\nb,2,0,2\\n")
        result = self.replay()
        self.assertEqual(result["observed_delta"], 0)
        groups = result["paired_outcomes_by_declared_physical_group"]
        self.assertEqual([g["paired_net_model_advantage_count"] for g in groups],
                         [2, -2])
        self.assertEqual(result["paired_model_only_correct"], 2)
        self.assertEqual(result["paired_baseline_only_correct"], 2)

    def test_invalid_protocol_rejected_by_shared_cluster_kernel(self):
        with self.assertRaisesRegex(ValueError, "PROTOCOL_INVALID"):
            replay_paired_predictions(self.pred, self.metrics, resamples=999)


if __name__ == "__main__":
    unittest.main()
