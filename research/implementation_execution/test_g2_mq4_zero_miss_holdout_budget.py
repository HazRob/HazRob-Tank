"""Deterministic tests of hypothetical zero-miss binomial holdout design."""
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_zero_miss_holdout_budget import (
    min_zero_miss_groups, zero_miss_lower_bound, plan,
)


class ZeroMissHoldoutBudgetTests(unittest.TestCase):
    def test_exact_90_95_targets(self):
        self.assertEqual(min_zero_miss_groups(0.9, 0.95), 29)
        self.assertEqual(min_zero_miss_groups(0.95, 0.95), 59)

    def test_exact_boundary_and_minimality(self):
        for target, confidence in ((0.9, 0.95), (0.95, 0.95), (0.8, 0.9), (0.5, 0.75)):
            n = min_zero_miss_groups(target, confidence)
            t, alpha = Fraction(str(target)), 1-Fraction(str(confidence))
            self.assertLessEqual(t ** n, alpha)
            if n > 1:
                self.assertGreater(t ** (n-1), alpha)

    def test_six_held_out_groups_not_enough(self):
        self.assertAlmostEqual(zero_miss_lower_bound(6), 0.6069622310029172)
        self.assertLess(zero_miss_lower_bound(6), 0.9)

    def test_new_hypothetical_calendar_budgets(self):
        result = plan()
        self.assertEqual([result['levels']['0.9']['contiguous_calendar_days_one_independent_group_per_day'][str(g)]
                          for g in (0, 1, 2)], [41, 42, 43])
        self.assertEqual([result['levels']['0.95']['contiguous_calendar_days_one_independent_group_per_day'][str(g)]
                          for g in (0, 1, 2)], [81, 82, 83])
        self.assertEqual(result['levels']['0.9']['total_independent_groups_train_calibration_holdout'], 41)
        self.assertEqual(result['levels']['0.95']['total_independent_groups_train_calibration_holdout'], 81)

    def test_different_confidence_levels(self):
        self.assertGreater(min_zero_miss_groups(0.9, 0.99), min_zero_miss_groups(0.9, 0.95))
        self.assertLess(min_zero_miss_groups(0.8, 0.95), min_zero_miss_groups(0.9, 0.95))

    def test_reject_invalid_probability_and_integer(self):
        for p in (0, 1, True, -0.1, float('nan'), float('inf')):
            with self.subTest(p=p), self.assertRaises(ValueError):
                min_zero_miss_groups(p)
            with self.subTest(p=p), self.assertRaises(ValueError):
                min_zero_miss_groups(0.9, p)
        for n in (0, -1, True, 1.5):
            with self.subTest(n=n), self.assertRaises(ValueError):
                zero_miss_lower_bound(n)

    def test_reject_oversized_design(self):
        with self.assertRaisesRegex(ValueError, 'TOO_LARGE'):
            min_zero_miss_groups(0.99999, 0.95)

    def test_reject_invalid_planning(self):
        for kwargs in ({'min_training_groups': 1}, {'illustrative_holdout_groups': True},
                       {'gaps': (0, 0)}, {'gaps': (-1,)},
                       {'nominal_levels': (0.9, 0.9)}, {'confidence': float('nan')}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                plan(**kwargs)

    def test_deterministic_and_no_promotion(self):
        a, b = plan(), plan()
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertTrue(all(v is False for v in a['scientific_state'].values()))
        self.assertTrue(all(not x['scientifically_qualified'] for x in a['levels'].values()))
        self.assertEqual(a['independent_exposure_records_available'], 0)

    def test_pinned_reproducible_summary(self):
        path = Path(__file__).with_name('G2_MQ4_ZERO_MISS_HOLDOUT_BUDGET_2026-10-31.json')
        self.assertEqual(json.loads(path.read_text(encoding='utf-8')), plan())


if __name__ == '__main__':
    unittest.main()
