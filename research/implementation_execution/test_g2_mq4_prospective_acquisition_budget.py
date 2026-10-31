"""Deterministic tests for research-only necessary acquisition group budgets."""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_prospective_acquisition_budget import finite_rank_minimum, plan


class MQ4ProspectiveAcquisitionBudgetTests(unittest.TestCase):
    def test_finite_rank_boundaries(self):
        self.assertEqual([finite_rank_minimum(p) for p in (0.5, 0.8, 0.9, 0.95)],
                         [1, 4, 9, 19])

    def test_historical_calendar_shortfalls_are_not_independent_evidence(self):
        r = plan()
        self.assertEqual([r['gap_results'][str(g)]
                          ['candidate_calendar_calibration_days_not_verified_exposures']
                          for g in (0, 1, 2)], [2, 1, 0])
        self.assertEqual([r['gap_results'][str(g)]['nominal_rank_budgets']['0.9']
                          ['shortfall_if_existing_calendar_days_were_valid_groups']
                          for g in (0, 1, 2)], [7, 8, 9])
        self.assertEqual(r['independent_exposure_records_available'], 0)

    def test_prospective_calendar_budget(self):
        r = plan()
        for level, expected in (('0.9', [18, 19, 20]), ('0.95', [28, 29, 30])):
            self.assertEqual([r['gap_results'][str(g)]['nominal_rank_budgets'][level]
                              ['prospective_contiguous_calendar_days_if_one_independent_group_per_day']
                              for g in (0, 1, 2)], expected)

    def test_gap_does_not_create_independent_groups(self):
        r = plan()
        self.assertEqual([r['gap_results'][str(g)]['nominal_rank_budgets']['0.9']
                          ['prospective_total_independent_exposure_groups_excluding_embargo']
                          for g in (0, 1, 2)], [18, 18, 18])

    def test_zero_historical_days_cannot_be_credited(self):
        r = plan(existing_candidate_days=0)
        self.assertTrue(all(r['gap_results'][str(g)]
                            ['candidate_calendar_calibration_days_not_verified_exposures'] == 0
                            for g in (0, 1, 2)))

    def test_reject_invalid_nominal(self):
        for p in (0, 1, -1, True, float('nan'), float('inf')):
            with self.subTest(p=p), self.assertRaisesRegex(ValueError, 'NOMINAL'):
                finite_rank_minimum(p)

    def test_reject_invalid_design(self):
        for kwargs, code in (({'min_training_groups': True}, 'TRAINING'),
                             ({'locked_test_groups': 0}, 'HOLDOUT'),
                             ({'gaps': (0, 0)}, 'GAPS'),
                             ({'gaps': (-1,)}, 'GAPS'),
                             ({'nominal_levels': (0.9, 0.9)}, 'NOMINAL'),
                             ({'existing_candidate_days': 3}, 'DAYS')):
            with self.subTest(kwargs=kwargs), self.assertRaisesRegex(ValueError, code):
                plan(**kwargs)

    def test_deterministic_and_fail_closed(self):
        a, b = plan(), plan()
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertTrue(all(not value for value in a['scientific_state'].values()))
        self.assertIn('not a validated coverage sample size', a['limitations'])
        self.assertTrue(all(v['rank_feasible_only_not_scientifically_qualified']
                            for g in a['gap_results'].values()
                            for v in g['nominal_rank_budgets'].values()))

    def test_pinned_reproducible_summary(self):
        pinned = Path(__file__).with_name('G2_MQ4_PROSPECTIVE_ACQUISITION_BUDGET_2026-10-31.json')
        self.assertEqual(json.loads(pinned.read_text(encoding='utf-8')), plan())


if __name__ == '__main__':
    unittest.main()
