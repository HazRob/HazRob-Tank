"""G1 software contract tests, synthetic metadata only."""
import unittest
from chemcore_next_contract_gate_v1 import Evidence, evaluate


def ev(group="g1", **changes):
    fields = dict(source_id="src", source_sha="sha", session_id="session",
                  device_id="tank", sensor_domain="MQ8", model_id="m1",
                  independent_group=group, score=0.8, validated=True,
                  feature_names=("MQ7_ratio",))
    fields.update(changes)
    return Evidence(**fields)


class EvidenceGateTests(unittest.TestCase):
    def test_accepts_two_distinct_groups(self):
        self.assertTrue(evaluate((ev(), ev("g2")), "MQ8").eligible)

    def test_rejects_same_group_even_with_two_models(self):
        self.assertEqual(evaluate((ev(), ev(model_id="m2")), "MQ8").reason,
                         "INSUFFICIENT_INDEPENDENT_GROUPS")

    def test_rejects_cross_domain(self):
        self.assertEqual(evaluate((ev(), ev("g2", sensor_domain="UCI_MOX")), "MQ8").reason,
                         "DOMAIN_MISMATCH")

    def test_rejects_missing_source(self):
        self.assertEqual(evaluate((ev(source_sha=""), ev("g2")), "MQ8").reason,
                         "MISSING_PROVENANCE")

    def test_rejects_unvalidated(self):
        self.assertEqual(evaluate((ev(), ev("g2", validated=False)), "MQ8").reason,
                         "UNVALIDATED_EVIDENCE")

    def test_rejects_nonfinite(self):
        self.assertEqual(evaluate((ev(), ev("g2", score=float("nan"))), "MQ8").reason,
                         "INVALID_SCORE")

    def test_rejects_boolean_score(self):
        self.assertEqual(evaluate((ev(), ev("g2", score=True)), "MQ8").reason,
                         "INVALID_SCORE")

    def test_rejects_missing_features(self):
        self.assertEqual(evaluate((ev(), ev("g2", feature_names=())), "MQ8").reason,
                         "MISSING_FEATURE_SCHEMA")

    def test_rejects_empty(self):
        self.assertEqual(evaluate((), "MQ8").reason, "NO_EVIDENCE")

    def test_requires_minimum_two(self):
        with self.assertRaises(ValueError):
            evaluate((ev(),), "MQ8", 1)


if __name__ == "__main__":
    unittest.main()
