"""Deterministic regressions on pinned external MQ4 receipt; not Tank G2/G3."""
import copy
import json
import unittest
from pathlib import Path
from g2_mq4_forward_day_stability import analyze, DAYS, MODELS, rates

RECEIPT = Path(__file__).with_name('G2_MQ4_FORWARD_4PPM_2026-11-01.json')


class Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = json.loads(RECEIPT.read_text(encoding='utf-8'))

    def mutate_reject(self, mutation):
        r = copy.deepcopy(self.source)
        mutation(r)
        with self.assertRaises(ValueError):
            analyze(r)

    def test_pinned_receipt_recomputes(self):
        a = analyze(self.source)
        self.assertEqual((a['rows'], a['reference_positives'], a['days_with_positives']),
                         (9325, 1425, 5))
        self.assertEqual(a['days_without_positives'], [DAYS[1], DAYS[-1]])
        self.assertEqual(set(a['models']), MODELS)
        self.assertFalse(a['g2_qualified'])

    def test_undefined_sensitivity_not_100(self):
        a = analyze(self.source)
        for m in a['models'].values():
            self.assertEqual(m['days_sensitivity_undefined'], [DAYS[1], DAYS[-1]])
            self.assertIsNone(m['days'][1]['sensitivity'])
            self.assertIsNone(m['days'][-1]['sensitivity'])

    def test_reproducible_output(self):
        self.assertEqual(json.dumps(analyze(self.source), sort_keys=True),
                         json.dumps(analyze(self.source), sort_keys=True))

    def test_no_promotion(self):
        self.mutate_reject(lambda r: r.update(g2_qualified=True))
        self.mutate_reject(lambda r: r.update(independent_tank_exposures=True))
        self.mutate_reject(lambda r: r.update(threshold_4ppm_is_safety_limit=True))

    def test_source_sha(self):
        self.mutate_reject(lambda r: r.update(csv_sha256='0'*64))

    def test_day_row_tamper(self):
        self.mutate_reject(lambda r: r['test_rows_by_day'].__setitem__(DAYS[0], 2))

    def test_truth_tamper(self):
        self.mutate_reject(lambda r: r['positive_by_test_day'].__setitem__(DAYS[0], 0))

    def test_fold_confusion_tamper(self):
        self.mutate_reject(lambda r: r['models']['RIDGE_MQ4_3']['days'][0].__setitem__('fn', 247))

    def test_marginal_tamper(self):
        self.mutate_reject(lambda r: r['models']['RIDGE_MQ4_3']['aggregate'].__setitem__('fp', 1))

    def test_mae_tamper(self):
        self.mutate_reject(lambda r: r['models']['RIDGE_MQ4_3']['days'][0].__setitem__('mae', 1e99))

    def test_boolean_is_not_integer(self):
        self.mutate_reject(lambda r: r['models']['RIDGE_MQ4_3']['days'][0].__setitem__('fn', True))

    def test_null_when_no_positive_support(self):
        self.assertIsNone(rates({'n':3,'pos':0,'tp':0,'fn':0,'fp':1,'tn':2})['sensitivity'])


    def test_pinned_day_stability_artifact_matches_recalculation(self):
        pin = json.loads(Path(__file__).with_name(
            'G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json').read_text(encoding='utf-8'))
        actual = analyze(self.source)
        self.assertEqual(pin['schema'], actual['schema'])
        self.assertEqual(pin['calendar_days'], actual['calendar_days'])
        self.assertEqual(pin['days_without_positives'], actual['days_without_positives'])
        self.assertFalse(pin['g2_qualified'])
        self.assertEqual(set(pin['models']), set(actual['models']))
        for name in actual['models']:
            for key in ('min_day_sensitivity', 'max_day_false_positive_rate',
                        'macro_sensitivity_defined_days',
                        'macro_specificity_defined_days'):
                self.assertAlmostEqual(pin['models'][name][key], actual['models'][name][key], places=12)
            self.assertEqual(pin['models'][name]['days_sensitivity_undefined'],
                             actual['models'][name]['days_sensitivity_undefined'])
            for a, b in zip(pin['models'][name]['days'], actual['models'][name]['days']):
                self.assertEqual((a['day'], a['pos'], a['fn'], a['fp']),
                                 (b['day'], b['pos'], b['fn'], b['fp']))


if __name__ == '__main__':
    unittest.main()
