"""Synthetic/fail-closed exact miss-tolerant familywise planning tests."""
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_familywise_nonzero_miss_budget import (
    binomial_upper_tail_for_at_most_misses,
    min_familywise_miss_tolerant_groups, plan,
)
from g2_mq4_familywise_zero_miss_budget import min_familywise_zero_miss_groups


class FamilywiseNonzeroMissTests(unittest.TestCase):
    def test_80_claim_90_targets(self):
        self.assertEqual([min_familywise_miss_tolerant_groups(.9, k)
                          for k in (0, 1, 2, 5)], [71, 94, 113, 165])

    def test_80_claim_95_targets(self):
        self.assertEqual([min_familywise_miss_tolerant_groups(.95, k)
                          for k in (0, 1, 2, 5)], [144, 191, 231, 336])

    def test_single_claim_crosscheck(self):
        self.assertEqual([min_familywise_miss_tolerant_groups(.9, k, claims=1)
                          for k in (0, 1, 2, 5)], [29, 46, 61, 103])

    def test_zero_miss_matches_existing_exact_rank(self):
        for p in (.8, .9, .95):
            for count in (1, 5, 80):
                self.assertEqual(min_familywise_miss_tolerant_groups(p, 0, claims=count),
                                 min_familywise_zero_miss_groups(p, claims=count))

    def test_exact_small_binomial(self):
        self.assertEqual(binomial_upper_tail_for_at_most_misses(3, 1, .5),
                         Fraction(1, 2))
        self.assertEqual(binomial_upper_tail_for_at_most_misses(3, 0, .5),
                         Fraction(1, 8))

    def test_exact_minimality(self):
        for p in (.9, .95):
            for k in (0, 1, 2, 5):
                n = min_familywise_miss_tolerant_groups(p, k)
                alpha = Fraction(1, 1600)
                self.assertLessEqual(binomial_upper_tail_for_at_most_misses(n, k, p),
                                     alpha)
                self.assertGreater(binomial_upper_tail_for_at_most_misses(n-1, k, p),
                                   alpha)

    def test_more_allowed_misses_cost_more_groups(self):
        ns = [min_familywise_miss_tolerant_groups(.9, k) for k in (0, 1, 2, 5)]
        self.assertEqual(ns, sorted(ns))
        self.assertEqual(len(set(ns)), 4)

    def test_more_claims_cost_more_groups(self):
        self.assertGreater(min_familywise_miss_tolerant_groups(.9, 1, claims=80),
                           min_familywise_miss_tolerant_groups(.9, 1, claims=1))

    def test_invalid_counts_fail_closed(self):
        for bad in (-1, True, 1.5, 21):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_miss_tolerant_groups(.9, bad)
        for bad in (0, True, 10001):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_miss_tolerant_groups(.9, 0, claims=bad)
        with self.assertRaises(ValueError):
            binomial_upper_tail_for_at_most_misses(3, 3, .9)

    def test_invalid_probabilities_fail_closed(self):
        for bad in (0, 1, True, float('nan'), float('inf'), -0.1):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                min_familywise_miss_tolerant_groups(bad, 1)

    def test_unattainable_target_fails_closed(self):
        with self.assertRaisesRegex(ValueError, 'TOO_LARGE'):
            min_familywise_miss_tolerant_groups(.99999, 5)

    def test_pinned_json_and_no_promotion(self):
        a = plan()
        pinned = json.loads(Path(__file__).with_name(
            'G2_MQ4_FAMILYWISE_NONZERO_MISS_BUDGET_2026-10-31.json'
        ).read_text(encoding='utf-8'))
        self.assertEqual(a, pinned)
        self.assertEqual(a, plan())
        self.assertEqual(a['physical_independent_evidence_records'], 0)
        self.assertFalse(a['g2_qualified'])
        self.assertFalse(a['g3_qualified'])
        self.assertFalse(a['original_mq4_zip_replayed_this_iteration'])


if __name__ == '__main__':
    unittest.main()
