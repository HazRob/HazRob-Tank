"""Synthetic-only negative and positive controls; never physical gas evidence."""
import hashlib
import tempfile
import unittest
from pathlib import Path

from g2_paired_delta_diagnostic import (paired_cluster_accuracy_delta as replay,
                                        replay_per_group_csv)


class PairedDeltaDiagnosticTests(unittest.TestCase):
    def test_identical_model_and_baseline_has_zero_paired_delta(self):
        result = replay({"session-a": 2, "session-b": 5},
                        {"session-a": 1, "session-b": 4},
                        {"session-a": 1, "session-b": 4}, resamples=1000)
        self.assertEqual((result["observed_delta"], result["interval_low"],
                          result["interval_high"]), (0, 0, 0))
        self.assertEqual(result["bootstrap_nonpositive_fraction_not_p_value"], 1)
        self.assertIn("NOT_G2_OR_G3_APPROVAL", result["scope"])

    def test_perfect_paired_improvement_has_unit_interval(self):
        result = replay({"a": 3, "b": 7}, {"a": 3, "b": 7},
                        {"a": 0, "b": 0}, resamples=1000)
        self.assertEqual((result["observed_delta"], result["interval_low"],
                          result["interval_high"]), (1, 1, 1))
        self.assertEqual(result["bootstrap_nonpositive_fraction_not_p_value"], 0)

    def test_deterministic_and_invariant_to_input_dict_order(self):
        counts = {"a": 2, "b": 5, "c": 4}
        model = {"a": 2, "b": 2, "c": 4}
        baseline = {"a": 1, "b": 3, "c": 1}
        first = replay(counts, model, baseline, resamples=1200)
        second = replay(dict(reversed(list(counts.items()))),
                        dict(reversed(list(model.items()))),
                        dict(reversed(list(baseline.items()))), resamples=1200)
        self.assertEqual(first, second)
        self.assertAlmostEqual(first["observed_delta"], 3 / 11)

    def test_missing_or_extra_baseline_group_fails_closed(self):
        for baseline in ({"a": 0}, {"a": 0, "b": 0, "c": 0}):
            with self.subTest(baseline=baseline):
                with self.assertRaisesRegex(ValueError, "PHYSICAL_GROUPS_INVALID"):
                    replay({"a": 1, "b": 1}, {"a": 1, "b": 0}, baseline)

    def test_noncanonical_or_invalid_physical_group_ids_fail_closed(self):
        for ids in (("a", "ａ"), ("a", "a\u200b"), ("a", "b "), ("a", 1)):
            with self.subTest(ids=ids):
                counts = {ids[0]: 1, ids[1]: 1}
                with self.assertRaisesRegex(ValueError, "PHYSICAL_GROUPS_INVALID"):
                    replay(counts, dict.fromkeys(counts, 1), dict.fromkeys(counts, 0))

    def test_invalid_numerators_and_boolean_counts_fail_closed(self):
        for counts, model, baseline in (
            ({"a": True, "b": 1}, {"a": 1, "b": 1}, {"a": 0, "b": 0}),
            ({"a": 1, "b": 1}, {"a": 2, "b": 1}, {"a": 0, "b": 0}),
            ({"a": 1, "b": 1}, {"a": 1, "b": 1}, {"a": -1, "b": 0}),
        ):
            with self.subTest(counts=counts, model=model, baseline=baseline):
                with self.assertRaisesRegex(ValueError, "COUNTS_INVALID"):
                    replay(counts, model, baseline)

    def test_csv_replay_has_byte_lineage_and_rejects_duplicate_group(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "metrics_per_group.csv"
            content = ("group_id,sample_count,correct_count,baseline_correct_count\n"
                       "a,3,2,1\nb,5,4,3\n")
            path.write_text(content, encoding="utf-8")
            result = replay_per_group_csv(path, resamples=1000)
            self.assertEqual(result["source_csv_sha256"],
                             hashlib.sha256(content.encode()).hexdigest())
            self.assertAlmostEqual(result["observed_delta"], 2 / 8)
            path.write_text(content + "a,1,1,0\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "CSV_ROW_INVALID"):
                replay_per_group_csv(path)

    def test_csv_replay_rejects_ambiguous_header_and_noncanonical_count(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "metrics_per_group.csv"
            path.write_text(("group_id,sample_count,correct_count,correct_count,baseline_correct_count\n"
                             "a,3,2,2,1\nb,5,4,4,3\n"), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "CSV_HEADER_INVALID"):
                replay_per_group_csv(path)
            path.write_text(("group_id,sample_count,correct_count,baseline_correct_count\n"
                             "a,03,2,1\nb,5,4,3\n"), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "CSV_ROW_INVALID"):
                replay_per_group_csv(path)

    def test_protocol_rejects_bad_resamples_seed_or_confidence(self):
        for kwargs in ({"seed": True}, {"resamples": 999},
                       {"resamples": 100001}, {"confidence_level": 0.90},
                       {"confidence_level": float("nan")}):
            with self.subTest(kwargs=kwargs):
                with self.assertRaisesRegex(ValueError, "PROTOCOL_INVALID"):
                    replay({"a": 1, "b": 1}, {"a": 1, "b": 1},
                           {"a": 0, "b": 0}, **kwargs)


    def test_group_heterogeneity_and_leave_one_out_are_exact(self):
        # Three independent synthetic groups, unequal sizes, mixed effects.
        result = replay({"a": 2, "b": 8, "c": 10},
                        {"a": 2, "b": 3, "c": 9},
                        {"a": 0, "b": 5, "c": 9}, resamples=1000)
        self.assertEqual(result["observed_delta"], 0)
        self.assertAlmostEqual(result["model_accuracy"], 14 / 20)
        self.assertAlmostEqual(result["baseline_accuracy"], 14 / 20)
        self.assertEqual((result["group_positive_delta_count"],
                          result["group_negative_delta_count"],
                          result["group_tied_delta_count"]), (1, 1, 1))
        self.assertAlmostEqual(result["group_delta_min"], -2 / 8)
        self.assertAlmostEqual(result["group_delta_max"], 1)
        self.assertAlmostEqual(result["leave_one_group_out_delta_min"], -2 / 18)
        self.assertAlmostEqual(result["leave_one_group_out_delta_max"], 2 / 12)
        self.assertAlmostEqual(result["largest_group_exposure_fraction"], 0.5)
        self.assertAlmostEqual(result["kish_effective_group_count_descriptive_only"],
                               20**2 / (2**2 + 8**2 + 10**2))

    def test_one_dominant_group_exposes_low_effective_group_count(self):
        result = replay({"a": 1, "b": 99},
                        {"a": 1, "b": 0},
                        {"a": 0, "b": 99}, resamples=1000)
        self.assertAlmostEqual(result["observed_delta"], -0.98)
        self.assertAlmostEqual(result["largest_group_exposure_fraction"], 0.99)
        self.assertLess(result["kish_effective_group_count_descriptive_only"], 1.03)
        self.assertEqual(result["group_positive_delta_count"], 1)
        self.assertEqual(result["group_negative_delta_count"], 1)
        self.assertAlmostEqual(result["leave_one_group_out_delta_min"], -1)
        self.assertAlmostEqual(result["leave_one_group_out_delta_max"], 1)

    def test_diagnostics_are_deterministic_under_group_permutation(self):
        original = replay({"z": 3, "a": 4, "m": 2},
                          {"z": 2, "a": 4, "m": 0},
                          {"z": 1, "a": 1, "m": 2}, resamples=1000)
        reordered = replay({"m": 2, "z": 3, "a": 4},
                           {"m": 0, "z": 2, "a": 4},
                           {"m": 2, "z": 1, "a": 1}, resamples=1000)
        self.assertEqual(original, reordered)
        self.assertIn("NOT_G2_OR_G3_APPROVAL", original["scope"])


    def test_exposure_weighted_and_equal_group_directions_can_disagree(self):
        # Synthetic group a is small but strongly positive; b is large
        # and mildly negative. This is a weighting diagnostic, not G2 evidence.
        result = replay({"a": 1, "b": 100},
                        {"a": 1, "b": 30},
                        {"a": 0, "b": 50}, resamples=1000)
        self.assertAlmostEqual(result["observed_delta"], -19 / 101)
        self.assertAlmostEqual(result["equal_physical_group_mean_delta"], 0.4)
        self.assertTrue(result["exposure_vs_equal_group_direction_disagreement"])
        self.assertAlmostEqual(result["exposure_vs_equal_group_delta_gap"],
                               -19 / 101 - 0.4)
        self.assertEqual(result["leave_one_group_out_direction_reversal_count"], 1)
        self.assertAlmostEqual(result["leave_one_group_out_max_absolute_shift"],
                               1 + 19 / 101)
        self.assertIn("NOT_G2_OR_G3_APPROVAL", result["scope"])

    def test_equal_group_sizes_have_identical_macro_and_pooled_deltas(self):
        result = replay({"a": 4, "b": 4},
                        {"a": 4, "b": 0},
                        {"a": 0, "b": 4}, resamples=1000)
        self.assertEqual(result["observed_delta"], 0)
        self.assertEqual(result["equal_physical_group_mean_delta"], 0)
        self.assertEqual(result["exposure_vs_equal_group_delta_gap"], 0)
        self.assertFalse(result["exposure_vs_equal_group_direction_disagreement"])
        self.assertEqual(result["leave_one_group_out_direction_reversal_count"], 0)

    def test_weighting_diagnostics_stable_under_group_permutation(self):
        a = replay({"z": 100, "a": 1, "m": 5},
                   {"z": 30, "a": 1, "m": 4},
                   {"z": 50, "a": 0, "m": 1}, resamples=1000)
        b = replay({"m": 5, "z": 100, "a": 1},
                   {"m": 4, "z": 30, "a": 1},
                   {"m": 1, "z": 50, "a": 0}, resamples=1000)
        self.assertEqual(a, b)

    def test_csv_replay_rejects_nfkc_alias_in_extra_header(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "metrics_per_group.csv"
            path.write_text(
                "group_id,sample_count,correct_count,baseline_correct_count,ｅxtra\n"
                "a,3,2,1,x\nb,5,4,3,y\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "CSV_HEADER_INVALID"):
                replay_per_group_csv(path, resamples=1000)

    def test_csv_replay_rejects_nfkc_alias_of_required_header(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "metrics_per_group.csv"
            path.write_text(
                "group_id,ｇroup_id,sample_count,correct_count,baseline_correct_count\n"
                "a,a,3,2,1\nb,b,5,4,3\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "CSV_HEADER_INVALID"):
                replay_per_group_csv(path, resamples=1000)

    def test_csv_replay_preserves_canonical_arabic_extra_header(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "metrics_per_group.csv"
            path.write_text(
                "group_id,sample_count,correct_count,baseline_correct_count,الموقع\n"
                "a,3,2,1,صنعاء\nb,5,4,3,تعز\n", encoding="utf-8")
            result = replay_per_group_csv(path, resamples=1000)
            self.assertAlmostEqual(result["observed_delta"], 2 / 8)


if __name__ == "__main__":
    unittest.main()
