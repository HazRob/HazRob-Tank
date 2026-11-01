"""Deterministic fail-closed MQ4 source-day support-strata tests."""
import copy
import json
import unittest
from pathlib import Path
from g2_mq4_forward_day_support_strata import build, SCHEMA

HERE = Path(__file__).parent
def load(name):
    return json.loads((HERE / name).read_text(encoding='utf-8'))

class Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = load('G2_MQ4_FORWARD_4PPM_2026-11-01.json')
        cls.stability = load('G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json')
        cls.ranking = load('G2_MQ4_FORWARD_DAY_RANKING_2026-11-01.json')

    def result(self):
        return build(self.source, self.stability, self.ranking)

    def test_pinned_output(self):
        self.assertEqual(self.result(), load('G2_MQ4_FORWARD_DAY_SUPPORT_STRATA_2026-11-01.json'))

    def test_deterministic(self):
        self.assertEqual(json.dumps(self.result(), sort_keys=True),
                         json.dumps(self.result(), sort_keys=True))

    def test_source_mutation_rejected(self):
        x = copy.deepcopy(self.source)
        x['models']['RIDGE_MQ4_1']['days'][0]['mae'] += 0.01
        with self.assertRaises(ValueError):
            build(x, self.stability, self.ranking)

    def test_stability_mutation_rejected(self):
        x = copy.deepcopy(self.stability)
        x['models']['RIDGE_MQ4_1']['pooled']['sensitivity'] = 0.1
        with self.assertRaises(ValueError):
            build(self.source, x, self.ranking)

    def test_ranking_mutation_rejected(self):
        x = copy.deepcopy(self.ranking)
        x['pairwise_reversals'] = 0
        with self.assertRaisesRegex(ValueError, 'PINNED_RANKING_MISMATCH'):
            build(self.source, self.stability, x)

    def test_source_identity_rejected(self):
        x = copy.deepcopy(self.source)
        x['zip_sha256'] = '0' * 64
        with self.assertRaises(ValueError):
            build(x, self.stability, self.ranking)

    def test_promotion_claim_rejected(self):
        x = copy.deepcopy(self.source)
        x['g2_qualified'] = True
        with self.assertRaises(ValueError):
            build(x, self.stability, self.ranking)

    def test_partition_and_support(self):
        x = self.result()
        self.assertEqual(x['schema'], SCHEMA)
        p = x['strata']['days_with_reference_positives']
        z = x['strata']['days_without_reference_positives']
        self.assertEqual((p['days'], z['days']), (5, 2))
        self.assertEqual((p['rows'], z['rows']), (7162, 2163))
        self.assertEqual((p['reference_positives'], z['reference_positives']), (1425, 0))

    def test_macro_mae_winners(self):
        x = self.result()['strata']
        self.assertEqual(x['days_with_reference_positives']['equal_day_weight_mae_ascending'][0],
                         'RIDGE_MQ4_1')
        self.assertEqual(x['days_without_reference_positives']['equal_day_weight_mae_ascending'][0],
                         'TRAIN_MEDIAN_REFERENCE')

    def test_baseline_counts(self):
        x = self.result()['paired_calendar_day_mae_vs_baseline']
        self.assertEqual(x['RIDGE_MQ4_1']['days_better_than_baseline'], 5)
        self.assertEqual(x['RIDGE_MQ4_1']['positive_support_days_better'], 4)
        self.assertEqual(x['RIDGE_MQ4_1']['zero_positive_support_days_better'], 1)
        for v in x.values():
            self.assertEqual(sum(v[k] for k in ('days_better_than_baseline',
                             'days_worse_than_baseline', 'days_tied_with_baseline')), 7)
            self.assertFalse(v['better_on_all_zero_positive_days'])
            self.assertFalse(v['better_on_all_seven_days'])

    def test_rank_sums(self):
        for g in self.result()['strata'].values():
            self.assertEqual(sum(g['day_rank_sum'].values()), g['days'] * 15)
            self.assertEqual(set(g['equal_day_weight_mae_ascending']),
                             set(self.ranking['models']))

    def test_non_promotion(self):
        x = self.result()
        for k in ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
                  'blinded_prospective', 'traceable_calibration'):
            self.assertIs(x[k], False)
        self.assertIn('NOT a', x['limitations'])

if __name__ == '__main__':
    unittest.main()
