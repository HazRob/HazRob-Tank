"""Deterministic synthetic and pinned-receipt checks; original ZIP is not in CI."""
import json
import unittest
from pathlib import Path

from g2_mq4_chronological_holdout_audit import (
    DAYS, NAMES, chronological_fold, evaluate_day,
)

RECEIPT = Path(__file__).with_name('G2_MQ4_CHRONOLOGICAL_HOLDOUT_2026-11-01.json')


def row(key='1', truth='2.0', pred='2.5', width='1.0', label='False', guess='False'):
    return {
        'source_row_id': key, 'reference_ch4_ppm': truth,
        'estimated_ch4_ppm_research': pred, 'calibration_halfwidth_ppm': width,
        'above_4ppm_reference': label, 'above_4ppm_research_estimate': guess,
    }


class ChronologicalHoldoutTests(unittest.TestCase):
    def test_pinned_source_receipt_and_blocked_status(self):
        r = json.loads(RECEIPT.read_text(encoding='utf-8'))
        self.assertEqual(r['source_prediction_csv_sha256'],
                         'f6707daa79f17c674dfb36c7051420acf67543e854137aa724225228b0719506')
        self.assertEqual(r['strictly_chronological_test_days'], ['2021-07-31'])
        self.assertEqual(r['historical_folds_with_future_training'], 10)
        self.assertEqual(r['historical_folds_with_future_calibration'], 1)
        self.assertEqual(r['chronological_positive_reference_rows'], 0)
        self.assertIs(r['chronological_detection_sensitivity_estimable'], False)
        self.assertIs(r['g2_qualified'], False)
        self.assertIs(r['g3_qualified'], False)
        self.assertEqual([m['model'] for m in r['models']], NAMES)
        self.assertEqual([m['fp'] for m in r['models']], [0, 579, 613, 337, 234])
        for m in r['models']:
            self.assertEqual(m['rows'], 723)
            self.assertIsNone(m['sensitivity'])
            self.assertIs(m['sensitivity_estimable'], False)
            self.assertEqual(m['tp'] + m['fn'], 0)
            self.assertEqual(m['tn'] + m['fp'], 723)

    def test_only_last_day_chronological(self):
        eligible = [d for i, d in enumerate(DAYS)
                    if chronological_fold(DAYS, d, DAYS[(i-1)%11], 9)['strictly_chronological']]
        self.assertEqual(eligible, ['2021-07-31'])

    def test_future_calibration_detected_first_day(self):
        p = chronological_fold(DAYS, DAYS[0], DAYS[-1], 9)
        self.assertTrue(p['future_calibration'])
        self.assertFalse(p['strictly_chronological'])

    def test_future_training_detected_middle_day(self):
        p = chronological_fold(DAYS, DAYS[5], DAYS[4], 9)
        self.assertGreater(p['future_training_days'], 0)
        self.assertFalse(p['strictly_chronological'])

    def test_last_day_has_no_future_fit(self):
        p = chronological_fold(DAYS, DAYS[-1], DAYS[-2], 9)
        self.assertEqual(p['future_training_days'], 0)
        self.assertFalse(p['future_calibration'])
        self.assertTrue(p['strictly_chronological'])

    def test_invalid_partition_rejected(self):
        with self.assertRaisesRegex(ValueError, 'BAD_FOLD_DAYS'):
            chronological_fold(DAYS, DAYS[-1], DAYS[-1], 9)
        with self.assertRaisesRegex(ValueError, 'TRAIN_DAY_COUNT'):
            chronological_fold(DAYS, DAYS[-1], DAYS[-2], 8)

    def test_zero_positive_support_never_becomes_perfect_sensitivity(self):
        score = evaluate_day([row()])
        self.assertEqual(score['tn'], 1)
        self.assertIsNone(score['sensitivity'])
        self.assertFalse(score['sensitivity_estimable'])

    def test_positive_support_yields_defined_sensitivity(self):
        score = evaluate_day([row('a', '4.0', '4.1', '0.2', 'True', 'True'),
                              row('b', '4.2', '3.8', '0.5', 'True', 'False')])
        self.assertEqual((score['tp'], score['fn']), (1, 1))
        self.assertEqual(score['sensitivity'], 0.5)

    def test_4ppm_exact_boundary(self):
        score = evaluate_day([row('a', '4.0', '4.0', '0.0', 'True', 'True')])
        self.assertEqual(score['tp'], 1)

    def test_duplicate_rows_rejected(self):
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_ROW'):
            evaluate_day([row(), row()])

    def test_source_label_swaps_rejected(self):
        with self.assertRaisesRegex(ValueError, 'ROW_LABEL_MISMATCH'):
            evaluate_day([row(label='True')])

    def test_estimate_label_swaps_rejected(self):
        with self.assertRaisesRegex(ValueError, 'ROW_LABEL_MISMATCH'):
            evaluate_day([row(guess='True')])

    def test_nonfinite_or_negative_rejected(self):
        for bad in [row(truth='nan'), row(pred='inf'), row(width='-0.1')]:
            with self.subTest(bad=bad), self.assertRaisesRegex(ValueError, 'INVALID_MEASUREMENT'):
                evaluate_day([bad])

    def test_empty_day_rejected(self):
        with self.assertRaisesRegex(ValueError, 'EMPTY_DAY'):
            evaluate_day([])


if __name__ == '__main__':
    unittest.main()
