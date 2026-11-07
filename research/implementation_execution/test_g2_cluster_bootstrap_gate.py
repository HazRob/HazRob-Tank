"""Regression tests for fail-closed G2 grouped-bootstrap evidence arithmetic.

All inputs here are synthetic software controls, not measured gas evidence.
"""
import unittest

from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy


INTERVAL = {
    "method": "GROUP_BOOTSTRAP",
    "seed": 20261026,
    "resamples": 2000,
    "confidence_level": 0.95,
    "resampling_unit": "group_id",
    "statistic": "pooled_accuracy",
    "quantile_method": "linear",
    "low": 0.0,
    "high": 1.0,
}
INVALID = ["GROUP_BOOTSTRAP_GROUP_COUNTS_INVALID"]


class BootstrapInputIntegrityTests(unittest.TestCase):
    def test_valid_complete_two_group_control(self):
        self.assertEqual(audit_group_bootstrap_accuracy(
            {"a": 1, "b": 1}, {"a": 1, "b": 0}, INTERVAL), [])

    def test_missing_correct_group_must_not_be_assumed_zero(self):
        self.assertEqual(audit_group_bootstrap_accuracy(
            {"a": 1, "b": 1}, {"a": 1}, INTERVAL), INVALID)

    def test_extra_correct_group_is_rejected(self):
        self.assertEqual(audit_group_bootstrap_accuracy(
            {"a": 1, "b": 1}, {"a": 1, "b": 0, "c": 0}, INTERVAL), INVALID)

    def test_non_mapping_inputs_fail_closed_without_exception(self):
        for counts, correct in ((None, {}), ({}, None),
                                (["a", "b"], {"a": 1, "b": 0}),
                                ({"a": 1, "b": 1}, [1, 0])):
            with self.subTest(counts=counts, correct=correct):
                self.assertEqual(audit_group_bootstrap_accuracy(
                    counts, correct, INTERVAL), INVALID)

    def test_invalid_or_mixed_group_ids_fail_closed_without_exception(self):
        for counts, correct in (
            ({1: 1, "b": 1}, {1: 1, "b": 0}),
            ({"": 1, "b": 1}, {"": 1, "b": 0}),
            ({"   ": 1, "b": 1}, {"   ": 1, "b": 0}),
        ):
            with self.subTest(counts=counts):
                self.assertEqual(audit_group_bootstrap_accuracy(
                    counts, correct, INTERVAL), INVALID)

    def test_invalid_count_or_numerator_fails_closed(self):
        for counts, correct in (
            ({"a": True, "b": 1}, {"a": 1, "b": 0}),
            ({"a": 1, "b": 1}, {"a": False, "b": 0}),
            ({"a": 1, "b": 1}, {"a": 2, "b": 0}),
            ({"a": 1, "b": 1}, {"a": -1, "b": 0}),
        ):
            with self.subTest(counts=counts, correct=correct):
                self.assertEqual(audit_group_bootstrap_accuracy(
                    counts, correct, INTERVAL), INVALID)


if __name__ == "__main__":
    unittest.main()
