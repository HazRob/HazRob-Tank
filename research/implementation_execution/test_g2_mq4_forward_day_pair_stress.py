"""Deterministic, fail-closed tests for MQ4 day-pair descriptive stress (not G2)."""
import copy
import json
import unittest
from pathlib import Path
from g2_mq4_forward_day_pair_stress import build, SCHEMA
from g2_mq4_forward_day_stability import DAYS, MODELS

HERE = Path(__file__).parent
SOURCE = HERE / 'G2_MQ4_FORWARD_4PPM_2026-11-01.json'
PIN = HERE / 'G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json'
OUTPUT = HERE / 'G2_MQ4_FORWARD_DAY_PAIR_STRESS_2026-11-01.json'

class Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = json.loads(SOURCE.read_text(encoding='utf-8'))
        cls.pin = json.loads(PIN.read_text(encoding='utf-8'))

    def test_complete_pinned_output_matches_recalculation(self):
        self.assertEqual(build(self.source, self.pin),
                         json.loads(OUTPUT.read_text(encoding='utf-8')))

    def test_reproducibility(self):
        a = json.dumps(build(self.source, self.pin), sort_keys=True)
        b = json.dumps(build(self.source, self.pin), sort_keys=True)
        self.assertEqual(a, b)

    def test_full_structural_pin_not_only_selected_fields(self):
        altered = copy.deepcopy(self.pin)
        altered['models']['RIDGE_MQ4_3']['pooled']['specificity'] = 0.99
        with self.assertRaisesRegex(ValueError, 'PINNED_STABILITY_MISMATCH'):
            build(self.source, altered)

    def test_altered_day_false_positive_count_rejected(self):
        altered = copy.deepcopy(self.pin)
        altered['models']['HGB_MQ4_3_T_RH']['days'][0]['fp'] += 1
        with self.assertRaises(ValueError):
            build(self.source, altered)

    def test_altered_leave_one_day_out_rejected(self):
        altered = copy.deepcopy(self.pin)
        altered['models']['RIDGE_MQ4_3']['leave_one_day_out'][0]['specificity'] = 0.9
        with self.assertRaises(ValueError):
            build(self.source, altered)

    def test_source_promotion_claim_rejected(self):
        altered = copy.deepcopy(self.source)
        altered['g2_qualified'] = True
        with self.assertRaises(ValueError):
            build(altered, self.pin)

    def test_source_model_confusion_tamper_rejected(self):
        altered = copy.deepcopy(self.source)
        altered['models']['RIDGE_MQ4_3']['days'][0]['fp'] += 1
        with self.assertRaises(ValueError):
            build(altered, self.pin)

    def test_exact_combinatorics_and_support(self):
        out = build(self.source, self.pin)
        self.assertEqual(out['schema'], SCHEMA)
        self.assertEqual(set(out['models']), MODELS)
        for obj in out['models'].values():
            self.assertEqual(len(obj['subsets']), 21)
            self.assertEqual(len({tuple(x['excluded_days']) for x in obj['subsets']}), 21)
            for x in obj['subsets']:
                self.assertEqual(len(x['excluded_days']), 2)
                self.assertEqual(x['remaining_rows'],
                                 x['remaining_reference_positives'] +
                                 x['remaining_reference_negatives'])
                self.assertGreater(x['remaining_reference_positives'], 0)
                self.assertGreater(x['remaining_reference_negatives'], 0)

    def test_no_qualification_claim(self):
        out = build(self.source, self.pin)
        for key in ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
                    'blinded_prospective', 'traceable_calibration'):
            self.assertIs(out[key], False)

    def test_ranges_are_descriptive_and_bounded(self):
        out = build(self.source, self.pin)
        for obj in out['models'].values():
            for key in ('sensitivity_range', 'specificity_range'):
                lo, hi = obj[key]
                self.assertLessEqual(0, lo)
                self.assertLessEqual(lo, hi)
                self.assertLessEqual(hi, 1)
            self.assertTrue(set(obj['worst_sensitivity_excluded_days']) <= set(DAYS))
            self.assertTrue(set(obj['worst_specificity_excluded_days']) <= set(DAYS))

    def test_ridge_three_source_sensitivity_range(self):
        out = build(self.source, self.pin)
        lo, hi = out['models']['RIDGE_MQ4_3']['sensitivity_range']
        self.assertAlmostEqual(lo, 0.7961019490254873, places=12)
        self.assertEqual(hi, 1)

    def test_baseline_is_not_sensitivity_success(self):
        out = build(self.source, self.pin)
        self.assertEqual(out['models']['TRAIN_MEDIAN_REFERENCE']['sensitivity_range'], [0, 0])
        self.assertEqual(out['models']['TRAIN_MEDIAN_REFERENCE']['specificity_range'], [1, 1])

if __name__ == '__main__':
    unittest.main()
