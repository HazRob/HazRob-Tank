"""Synthetic post-blind pairing controls; NOT original physical measurements."""
import csv
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
                   metrics="b,2,1,1\na,2,1,1\n")
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
        self.write(pred=("e1,a,A,A,B\n"
                         "e2,a,B,B,A\n"
                         "e3,b,A,B,A\n"
                         "e4,b,B,A,B\n"),
                   metrics="a,2,2,0\nb,2,0,2\n")
        result = self.replay()
        self.assertEqual(result["observed_delta"], 0)
        groups = result["paired_outcomes_by_declared_physical_group"]
        self.assertEqual([g["paired_net_model_advantage_count"] for g in groups],
                         [2, -2])
        self.assertEqual(result["paired_model_only_correct"], 2)
        self.assertEqual(result["paired_baseline_only_correct"], 2)

    def test_full_semantic_digest_tracks_optional_source_provenance(self):
        pred_header = PRED_HEADER.rstrip("\n") + ",session_id\n"
        metrics_header = METRIC_HEADER.rstrip("\n") + ",reference_trace_id\n"
        pred = "".join(line.rstrip("\n") + ",session-A\n"
                       for line in PRED_ROWS.splitlines(keepends=True))
        metrics = "".join(line.rstrip("\n") + ",ref-A\n"
                          for line in METRIC_ROWS.splitlines(keepends=True))
        self.write(pred=pred, metrics=metrics, pred_header=pred_header,
                   metrics_header=metrics_header)
        original = self.replay()
        self.pred.write_text(
            self.pred.read_text(encoding="utf-8").replace(
                "session-A", "session-B", 1), encoding="utf-8")
        altered_prediction = self.replay()
        self.assertEqual(original["prediction_rows_semantic_sha256"],
                         altered_prediction["prediction_rows_semantic_sha256"])
        self.assertNotEqual(original["prediction_full_rows_semantic_sha256"],
                            altered_prediction["prediction_full_rows_semantic_sha256"])
        self.assertEqual(original["metrics_full_rows_semantic_sha256"],
                         altered_prediction["metrics_full_rows_semantic_sha256"])
        self.metrics.write_text(
            self.metrics.read_text(encoding="utf-8").replace(
                "ref-A", "ref-B", 1), encoding="utf-8")
        altered_metric = self.replay()
        self.assertNotEqual(altered_prediction["metrics_full_rows_semantic_sha256"],
                            altered_metric["metrics_full_rows_semantic_sha256"])
        self.assertIn("NOT_AUTHENTICATION", original["full_row_digest_scope"])

    def test_full_semantic_digest_is_row_and_column_order_invariant(self):
        original = self.replay()
        for path in (self.pred, self.metrics):
            with path.open("r", newline="", encoding="utf-8") as handle:
                reader = csv.DictReader(handle)
                fields = list(reader.fieldnames)
                rows = list(reader)
            with path.open("w", newline="", encoding="utf-8") as handle:
                writer = csv.DictWriter(handle, fieldnames=list(reversed(fields)))
                writer.writeheader()
                writer.writerows(reversed(rows))
        reordered = self.replay()
        for field in ("prediction_full_rows_semantic_sha256",
                      "metrics_full_rows_semantic_sha256",
                      "prediction_rows_semantic_sha256",
                      "observed_delta"):
            self.assertEqual(original[field], reordered[field])
        self.assertNotEqual(original["predictions_csv_sha256"],
                            reordered["predictions_csv_sha256"])

    def test_full_semantic_digest_catches_optional_blank_to_value(self):
        pred_header = PRED_HEADER.rstrip("\n") + ",device_id\n"
        pred = "".join(line.rstrip("\n") + ",\n"
                       for line in PRED_ROWS.splitlines(keepends=True))
        self.write(pred=pred, pred_header=pred_header)
        original = self.replay()
        self.pred.write_text(
            self.pred.read_text(encoding="utf-8").replace(
                "e1,a,A,A,B,\n", "e1,a,A,A,B,device-1\n"),
            encoding="utf-8")
        changed = self.replay()
        self.assertEqual(original["prediction_rows_semantic_sha256"],
                         changed["prediction_rows_semantic_sha256"])
        self.assertNotEqual(original["prediction_full_rows_semantic_sha256"],
                            changed["prediction_full_rows_semantic_sha256"])

    def test_full_semantic_digest_tracks_added_metadata_column(self):
        original = self.replay()
        self.write(
            pred_header=PRED_HEADER.rstrip("\n") + ",source_sha256\n",
            pred="".join(line.rstrip("\n") + ",unverified\n"
                         for line in PRED_ROWS.splitlines(keepends=True)))
        altered = self.replay()
        self.assertEqual(original["prediction_rows_semantic_sha256"],
                         altered["prediction_rows_semantic_sha256"])
        self.assertNotEqual(original["prediction_full_rows_semantic_sha256"],
                            altered["prediction_full_rows_semantic_sha256"])


    def test_true_reference_class_paired_counts_reconciled(self):
        result = self.replay()
        classes = result["paired_outcomes_by_true_reference_class"]
        self.assertEqual(result["true_reference_class_count"], 2)
        self.assertEqual([c["true_reference"] for c in classes], ["A", "B"])
        self.assertEqual(
            [(c["exposures"], c["model_correct"], c["baseline_correct"],
              c["both_correct"], c["both_wrong"], c["model_only_correct"],
              c["baseline_only_correct"]) for c in classes],
            [(2, 2, 1, 1, 0, 1, 0), (2, 0, 1, 0, 1, 0, 1)])
        self.assertEqual(result["equal_true_reference_class_mean_delta"], 0)
        self.assertIn("NOT_INDEPENDENT_LABEL_AUTHENTICATION",
                      result["true_reference_class_diagnostic_scope"])

    def test_class_imbalance_reverses_pooled_and_equal_class_deltas(self):
        # The majority class favours the model, minority favours baseline.
        self.write(pred=("e1,a,A,A,B\n"
                         "e2,a,A,A,B\n"
                         "e3,a,A,A,B\n"
                         "e4,a,A,B,A\n"
                         "e5,b,B,A,B\n"),
                   metrics="a,4,3,1\nb,1,0,1\n")
        result = self.replay()
        self.assertAlmostEqual(result["observed_delta"], 0.2)
        self.assertAlmostEqual(
            result["equal_true_reference_class_mean_delta"], -0.25)
        classes = result["paired_outcomes_by_true_reference_class"]
        self.assertEqual([c["paired_model_minus_baseline_accuracy"]
                          for c in classes], [0.5, -1.0])

    def test_class_diagnostics_invariant_to_row_order_and_unicode_labels(self):
        self.write(pred=("e1,a,إيثانول,إيثانول,أسيتون\n"
                         "e2,a,أسيتون,إيثانول,أسيتون\n"
                         "e3,b,إيثانول,إيثانول,إيثانول\n"
                         "e4,b,أسيتون,إيثانول,إيثانول\n"))
        original = self.replay()
        rows = self.pred.read_text(encoding="utf-8").splitlines(keepends=True)
        self.pred.write_text(rows[0] + "".join(reversed(rows[1:])),
                             encoding="utf-8")
        reordered = self.replay()
        self.assertEqual(
            original["paired_outcomes_by_true_reference_class"],
            reordered["paired_outcomes_by_true_reference_class"])
        self.assertEqual(
            original["equal_true_reference_class_mean_delta"],
            reordered["equal_true_reference_class_mean_delta"])

    def test_invalid_protocol_rejected_by_shared_cluster_kernel(self):
        with self.assertRaisesRegex(ValueError, "PROTOCOL_INVALID"):
            replay_paired_predictions(self.pred, self.metrics, resamples=999)


    def test_true_class_group_support_full_crossed_design(self):
        result = self.replay()
        support = result["true_reference_class_support_by_declared_group"]
        self.assertEqual(
            [(r["group_id"], r["true_reference"], r["exposures"])
             for r in support],
            [("a", "A", 1), ("a", "B", 1),
             ("b", "A", 1), ("b", "B", 1)])
        self.assertEqual(result["true_reference_classes_confined_to_one_declared_group"], [])
        self.assertEqual(result["true_reference_class_min_declared_group_coverage"], 2)
        self.assertEqual(
            [r["declared_group_coverage_fraction"]
             for r in result["true_reference_class_declared_group_coverage"]],
            [1.0, 1.0])
        self.assertIn("NOT_AUTHENTICATED_GROUP_INDEPENDENCE",
                      result["true_reference_group_support_scope"])

    def test_true_class_single_group_confinement_is_flagged(self):
        # Perfectly reconciled pooled metrics cannot supply class-wise
        # cross-group evidence when each class occurs in only one group.
        self.write(pred=("e1,a,A,A,B\n"
                         "e2,a,A,B,A\n"
                         "e3,b,B,B,A\n"
                         "e4,b,B,A,B\n"),
                   metrics="a,2,1,1\nb,2,1,1\n")
        result = self.replay()
        self.assertEqual(result["observed_delta"], 0)
        self.assertEqual(result["true_reference_classes_confined_to_one_declared_group"],
                         ["A", "B"])
        self.assertEqual(result["true_reference_class_min_declared_group_coverage"], 1)
        self.assertEqual(
            [r["declared_group_coverage_fraction"]
             for r in result["true_reference_class_declared_group_coverage"]],
            [0.5, 0.5])

    def test_true_class_partial_group_support_reconciles(self):
        self.write(pred=("e1,a,A,A,B\n"
                         "e2,b,A,B,A\n"
                         "e3,b,B,B,B\n"
                         "e4,c,B,A,B\n"),
                   metrics="a,1,1,0\nb,2,1,2\nc,1,0,1\n")
        result = self.replay()
        self.assertEqual(result["groups"], 3)
        self.assertEqual(result["true_reference_class_min_declared_group_coverage"], 2)
        self.assertEqual(result["true_reference_classes_confined_to_one_declared_group"], [])
        for row in result["true_reference_class_declared_group_coverage"]:
            self.assertAlmostEqual(row["declared_group_coverage_fraction"], 2 / 3)
        self.assertEqual(
            sum(r["exposures"] for r in
                result["true_reference_class_support_by_declared_group"]), 4)

    def test_true_class_group_support_order_and_arabic_label_invariant(self):
        self.write(pred=("e1,a,إيثانول,إيثانول,أسيتون\n"
                         "e2,a,أسيتون,إيثانول,أسيتون\n"
                         "e3,b,إيثانول,إيثانول,إيثانول\n"
                         "e4,b,أسيتون,إيثانول,إيثانول\n"))
        before = self.replay()
        rows = self.pred.read_text(encoding="utf-8").splitlines(keepends=True)
        self.pred.write_text(rows[0] + "".join(reversed(rows[1:])),
                             encoding="utf-8")
        after = self.replay()
        for key in ("true_reference_class_support_by_declared_group",
                    "true_reference_class_declared_group_coverage",
                    "true_reference_classes_confined_to_one_declared_group",
                    "true_reference_class_min_declared_group_coverage"):
            self.assertEqual(before[key], after[key])


    def test_true_class_group_discordance_reconciles_all_four_cells(self):
        result = self.replay()
        cells = result["true_reference_class_support_by_declared_group"]
        self.assertEqual(
            [(r["group_id"], r["true_reference"], r["both_correct"],
              r["both_wrong"], r["model_only_correct"],
              r["baseline_only_correct"]) for r in cells],
            [("a", "A", 0, 0, 1, 0),
             ("a", "B", 0, 0, 0, 1),
             ("b", "A", 1, 0, 0, 0),
             ("b", "B", 0, 1, 0, 0)])
        self.assertEqual(
            [r["paired_net_model_advantage_count"] for r in cells],
            [1, -1, 0, 0])
        self.assertEqual(result["true_reference_classes_with_opposite_group_effects"], [])
        self.assertIn("NOT_INDEPENDENT_PHYSICAL_EVIDENCE",
                      result["class_group_discordance_scope"])

    def test_within_class_group_weighting_reverses_pooled_sign(self):
        # Same true class in two groups, but group sizes are unequal:
        # weighted +1/7 versus equal-group (1/3 - 1)/2 = -1/3.
        pred = "".join(
            f"e{i},a,A,A,B\n" for i in range(1, 5)
        ) + "".join(
            f"e{i},a,A,B,A\n" for i in range(5, 7)
        ) + "e7,b,A,B,A\n"
        self.write(pred=pred, metrics="a,6,4,2\nb,1,0,1\n")
        result = self.replay()
        row = result["true_reference_class_declared_group_coverage"][0]
        self.assertEqual(row["true_reference"], "A")
        self.assertAlmostEqual(row["exposure_weighted_delta"], 1 / 7)
        self.assertAlmostEqual(row["equal_observed_group_mean_delta"], -1 / 3)
        self.assertEqual(row["min_observed_group_delta"], -1)
        self.assertAlmostEqual(row["max_observed_group_delta"], 1 / 3)
        self.assertTrue(row["opposite_signed_group_effects"])
        self.assertTrue(row["equal_group_vs_exposure_weighted_sign_reversal"])
        self.assertEqual(result["true_reference_classes_with_opposite_group_effects"], ["A"])
        self.assertEqual(
            result["true_reference_classes_with_group_weighting_sign_reversal"],
            ["A"])

    def test_unobserved_class_group_cells_are_not_imputed(self):
        self.write(pred=("e1,a,A,A,B\n"
                         "e2,a,A,B,A\n"
                         "e3,b,B,B,A\n"
                         "e4,b,B,A,B\n"),
                   metrics="a,2,1,1\nb,2,1,1\n")
        result = self.replay()
        cells = result["true_reference_class_support_by_declared_group"]
        self.assertEqual(len(cells), 2)
        self.assertEqual(
            [(r["group_id"], r["true_reference"]) for r in cells],
            [("a", "A"), ("b", "B")])
        for row in result["true_reference_class_declared_group_coverage"]:
            self.assertTrue(row["confined_to_single_declared_group"])
            self.assertFalse(row["opposite_signed_group_effects"])
            self.assertFalse(row["equal_group_vs_exposure_weighted_sign_reversal"])
        self.assertIn("MISSING_GROUP_CLASS_CELLS_NOT_IMPUTED",
                      result["class_group_discordance_scope"])

    def test_class_group_discordance_invariant_to_row_order_and_arabic(self):
        self.write(pred=("e1,a,إيثانول,إيثانول,أسيتون\n"
                         "e2,a,أسيتون,إيثانول,أسيتون\n"
                         "e3,b,إيثانول,إيثانول,إيثانول\n"
                         "e4,b,أسيتون,إيثانول,إيثانول\n"))
        before = self.replay()
        lines = self.pred.read_text(encoding="utf-8").splitlines(keepends=True)
        self.pred.write_text(lines[0] + "".join(reversed(lines[1:])),
                             encoding="utf-8")
        after = self.replay()
        for key in ("true_reference_class_support_by_declared_group",
                    "true_reference_class_declared_group_coverage",
                    "true_reference_classes_with_opposite_group_effects",
                    "true_reference_classes_with_group_weighting_sign_reversal"):
            self.assertEqual(before[key], after[key])


    def test_declared_physical_sample_repeated_reads_stay_in_one_group(self):
        header = PRED_HEADER.rstrip("\n") + ",physical_sample_id\n"
        pred = ("e1,a,A,A,B,s1\n"
                "e2,a,B,A,B,s1\n"
                "e3,b,A,A,A,s2\n"
                "e4,b,B,A,A,s3\n")
        self.write(pred=pred, pred_header=header)
        result = self.replay()
        self.assertTrue(result["physical_sample_group_consistency_checked"])
        self.assertEqual(result["distinct_declared_physical_samples"], 3)
        self.assertEqual(
            result["repeated_exposure_rows_within_declared_physical_samples"], 1)
        self.assertEqual(result["max_exposure_rows_per_declared_physical_sample"], 2)
        self.assertIn("NOT_AUTHENTICATED_SAMPLE_PROVENANCE",
                      result["physical_sample_scope"])

    def test_declared_physical_sample_cannot_cross_test_groups(self):
        header = PRED_HEADER.rstrip("\n") + ",physical_sample_id\n"
        pred = ("e1,a,A,A,B,s1\n"
                "e2,a,B,A,B,s2\n"
                "e3,b,A,A,A,s1\n"
                "e4,b,B,A,A,s3\n")
        self.write(pred=pred, pred_header=header)
        with self.assertRaisesRegex(ValueError,
                                    "PAIRED_REPLAY_PHYSICAL_SAMPLE_GROUP_LEAKAGE"):
            self.replay()

    def test_partial_or_noncanonical_physical_sample_ids_rejected(self):
        header = PRED_HEADER.rstrip("\n") + ",physical_sample_id\n"
        for invalid in ("", "ｓ1", " s1"):
            with self.subTest(invalid=invalid):
                pred = (f"e1,a,A,A,B,{invalid}\n"
                        "e2,a,B,A,B,s2\n"
                        "e3,b,A,A,A,s3\n"
                        "e4,b,B,A,A,s4\n")
                self.write(pred=pred, pred_header=header)
                with self.assertRaisesRegex(
                        ValueError, "PAIRED_REPLAY_PHYSICAL_SAMPLE_ID_INVALID"):
                    self.replay()

    def test_physical_sample_checks_are_row_order_invariant(self):
        header = PRED_HEADER.rstrip("\n") + ",physical_sample_id\n"
        pred = ("e1,a,A,A,B,عينة-١\n"
                "e2,a,B,A,B,عينة-١\n"
                "e3,b,A,A,A,عينة-٢\n"
                "e4,b,B,A,A,عينة-٣\n")
        self.write(pred=pred, pred_header=header)
        before = self.replay()
        rows = self.pred.read_text(encoding="utf-8").splitlines(keepends=True)
        self.pred.write_text(rows[0] + "".join(reversed(rows[1:])),
                             encoding="utf-8")
        after = self.replay()
        for key in ("physical_sample_group_consistency_checked",
                    "distinct_declared_physical_samples",
                    "repeated_exposure_rows_within_declared_physical_samples",
                    "max_exposure_rows_per_declared_physical_sample"):
            self.assertEqual(before[key], after[key])

    def test_absent_sample_id_is_explicitly_unverified(self):
        result = self.replay()
        self.assertFalse(result["physical_sample_group_consistency_checked"])
        self.assertIsNone(result["distinct_declared_physical_samples"])
        self.assertIsNone(
            result["repeated_exposure_rows_within_declared_physical_samples"])


if __name__ == "__main__":
    unittest.main()
