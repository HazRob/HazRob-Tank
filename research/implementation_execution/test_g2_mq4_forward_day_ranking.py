"""Fail-closed reproducibility tests for descriptive MQ4 MAE rank reversals."""
import copy
import json
import unittest
from pathlib import Path
from g2_mq4_forward_day_ranking import build, SCHEMA

HERE = Path(__file__).parent
SOURCE = HERE / 'G2_MQ4_FORWARD_4PPM_2026-11-01.json'
STABILITY = HERE / 'G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json'
OUTPUT = HERE / 'G2_MQ4_FORWARD_DAY_RANKING_2026-11-01.json'

class Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = json.loads(SOURCE.read_text(encoding='utf-8'))
        cls.stability = json.loads(STABILITY.read_text(encoding='utf-8'))

    def test_pinned_full_output(self):
        self.assertEqual(build(self.source, self.stability),
                         json.loads(OUTPUT.read_text(encoding='utf-8')))

    def test_deterministic(self):
        self.assertEqual(build(self.source, self.stability),
                         build(self.source, self.stability))

    def test_source_identity_guard(self):
        changed = copy.deepcopy(self.source)
        changed['csv_sha256'] = '0' * 64
        with self.assertRaises(ValueError):
            build(changed, self.stability)

    def test_tampered_day_mae_rejected(self):
        changed = copy.deepcopy(self.source)
        changed['models']['RIDGE_MQ4_3']['days'][0]['mae'] += 0.1
        with self.assertRaises(ValueError):
            build(changed, self.stability)

    def test_tampered_pinned_stability_rejected(self):
        changed = copy.deepcopy(self.stability)
        changed['models']['RIDGE_MQ4_3']['pooled']['specificity'] = 0.1
        with self.assertRaisesRegex(ValueError, 'PINNED_STABILITY_MISMATCH'):
            build(self.source, changed)

    def test_promotion_rejected(self):
        changed = copy.deepcopy(self.source)
        changed['g2_qualified'] = True
        with self.assertRaises(ValueError):
            build(changed, self.stability)

    def test_pairwise_complete(self):
        out = build(self.source, self.stability)
        self.assertEqual(out['schema'], SCHEMA)
        self.assertEqual(out['pairwise_comparisons'], 70)
        self.assertEqual(len(out['pairwise_pooled_vs_day']), 10)
        self.assertEqual(len(out['daily']), 7)
        self.assertEqual(sum(out['day_first_place_counts'].values()), 7)
        for p in out['pairwise_pooled_vs_day'].values():
            self.assertEqual(p['agree_days'] + p['reverse_days'] + p['tied_days'], 7)

    def test_all_day_rankings_permutations(self):
        out = build(self.source, self.stability)
        for d in out['daily']:
            self.assertEqual(set(d['mae_ascending']), set(out['models']))
            self.assertEqual(len(d['mae_ascending']), 5)
            self.assertEqual(d['mae_ascending'],
                             sorted(out['models'],
                                    key=lambda n: (d['mae_by_model'][n], n)))

    def test_no_qualification_claims(self):
        out = build(self.source, self.stability)
        for k in ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
                  'blinded_prospective', 'traceable_calibration'):
            self.assertIs(out[k], False)

    def test_rank_reversals_exist(self):
        out = build(self.source, self.stability)
        self.assertGreater(out['pairwise_reversals'], 0)
        self.assertEqual(out['pooled_mae_ascending'][0], 'RIDGE_MQ4_1')
        self.assertLess(out['day_first_place_counts']['RIDGE_MQ4_1'], 7)

    def test_source_days_without_positives(self):
        out = build(self.source, self.stability)
        self.assertEqual([d['day'] for d in out['daily']
                          if d['reference_positives'] == 0],
                         ['2021-07-26', '2021-07-31'])

if __name__ == '__main__':
    unittest.main()
