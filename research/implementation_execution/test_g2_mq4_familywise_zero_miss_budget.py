"""Deterministic family-wise holdout planning regressions; synthetic only."""
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_familywise_zero_miss_budget import (
    min_familywise_zero_miss_groups, plan,
)


class FamilywiseZeroMissBudgetTests(unittest.TestCase):
    def test_80_claims_90_and_95(self):
        self.assertEqual(min_familywise_zero_miss_groups(0.9), 71)
        self.assertEqual(min_familywise_zero_miss_groups(0.95), 144)

    def test_one_claim_recovers_unadjusted_bounds(self):
        self.assertEqual(min_familywise_zero_miss_groups(0.9, claims=1), 29)
        self.assertEqual(min_familywise_zero_miss_groups(0.95, claims=1), 59)

    def test_exact_minimality(self):
        for coverage, conf, claims in ((0.9, 0.95, 80), (0.95, 0.95, 80),
                                      (0.9, 0.95, 1), (0.8, 0.9, 10)):
            n = min_familywise_zero_miss_groups(coverage, conf, claims)
            target = Fraction(str(coverage))
            alpha = (1 - Fraction(str(conf))) / claims
            self.assertLessEqual(target ** n, alpha)
            self.assertGreater(target ** (n - 1), alpha)

    def test_more_claims_or_confidence_never_reduces_n(self):
        self.assertGreater(min_familywise_zero_miss_groups(0.9, claims=80),
                           min_familywise_zero_miss_groups(0.9, claims=1))
        self.assertGreater(min_familywise_zero_miss_groups(0.9, 0.99, 80),
                           min_familywise_zero_miss_groups(0.9, 0.95, 80))

    def test_illustrative_totals_not_80_times_exposures(self):
        output = plan()
        self.assertEqual(output['levels']['0.9']['illustrative_train_calibration_holdout_groups'], 83)
        self.assertEqual(output['levels']['0.95']['illustrative_train_calibration_holdout_groups'], 166)
        self.assertEqual(output['levels']['0.9']['illustrative_days_one_verified_independent_group_per_day'],
                         {'0': 83, '1': 84, '2': 85})
        self.assertEqual(output['levels']['0.95']['illustrative_days_one_verified_independent_group_per_day'],
                         {'0': 166, '1': 167, '2': 168})

    def test_existing_six_groups_not_sufficient(self):
        output = plan()
        for result in output['levels'].values():
            self.assertLess(result['previous_six_group_simultaneous_lower_bound_if_all_covered'], 0.3)
            self.assertFalse(result['all_claims_scientifically_qualified'])

    def test_invalid_probabilities_and_claims_fail_closed(self):
        for bad in (0, 1, True, float('nan'), float('inf'), -0.5):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_zero_miss_groups(bad)
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_zero_miss_groups(0.9, bad)
        for bad in (0, -1, True, 1.5, 10001):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_zero_miss_groups(0.9, claims=bad)

    def test_oversized_target_rejected(self):
        with self.assertRaisesRegex(ValueError, 'TOO_LARGE'):
            min_familywise_zero_miss_groups(0.99999)

    def test_invalid_plan_inputs_rejected(self):
        for kwargs in ({'training_groups': 1}, {'illustrative_holdout_groups': 0},
                       {'gaps': (0, 0)}, {'gaps': (-1,)},
                       {'nominal_levels': (0.9, 0.9)}, {'claims': True}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                plan(**kwargs)

    def test_deterministic_no_promotion(self):
        a, b = plan(), plan()
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertFalse(a['g2_qualified'])
        self.assertFalse(a['g3_qualified'])
        self.assertEqual(a['independent_physical_holdout_records_available'], 0)

    def test_pinned_summary(self):
        path = Path(__file__).with_name('G2_MQ4_FAMILYWISE_ZERO_MISS_BUDGET_2026-10-31.json')
        self.assertEqual(json.loads(path.read_text(encoding='utf-8')), plan())


if __name__ == '__main__':
    unittest.main()
