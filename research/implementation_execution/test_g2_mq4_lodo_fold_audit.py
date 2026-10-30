"""Synthetic regression tests only; NOT physical gas validation."""
import json
import sys
import unittest
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_lodo_fold_audit import lodo_context, validate_predictors


class MQ4LODOFoldContextTests(unittest.TestCase):
    def test_two_day_multilag_union_and_direction(self):
        start = datetime(2021, 7, 21, 23)
        times = [start + timedelta(minutes=i) for i in range(181)]
        r = lodo_context(times, lags=(1, 60, 120))
        self.assertEqual(r['row_count'], 181)
        self.assertEqual(r['calendar_date_count'], 2)
        self.assertEqual([r['lag_results'][str(lag)]['cross_calendar_day_context_pairs'] for lag in (1, 60, 120)], [1, 60, 60])
        self.assertEqual([r['lag_results'][str(lag)]['sum_cross_fold_context_rows_over_lodo_folds'] for lag in (1, 60, 120)], [2, 120, 120])
        early = r['lodo_folds']['2021-07-21']
        late = r['lodo_folds']['2021-07-22']
        self.assertEqual((early['unique_train_to_holdout_context_rows_any_lag'], early['unique_holdout_to_train_context_rows_any_lag']), (0, 120))
        self.assertEqual((late['unique_train_to_holdout_context_rows_any_lag'], late['unique_holdout_to_train_context_rows_any_lag']), (120, 0))
        self.assertEqual(early['unique_cross_fold_context_rows_any_lag'], 120)
        self.assertEqual(late['unique_cross_fold_context_rows_any_lag'], 120)
        self.assertFalse(r['calendar_day_physical_independence_verified'])
        self.assertFalse(r['g2_qualified'])

    def test_three_days_middle_holdout_has_both_directions(self):
        start = datetime(2021, 7, 21, 23)
        times = [start + timedelta(minutes=i) for i in range(60 + 1440 + 60)]
        r = lodo_context(times, lags=(60,))
        middle = r['lodo_folds']['2021-07-22']
        self.assertEqual(middle['unique_train_to_holdout_context_rows_any_lag'], 60)
        self.assertEqual(middle['unique_holdout_to_train_context_rows_any_lag'], 60)
        self.assertEqual(middle['unique_cross_fold_context_rows_any_lag'], 120)

    def test_midnight_gap_prevents_cross_fold_context(self):
        times = [datetime(2021, 7, 21, 23, 59), datetime(2021, 7, 22, 0, 1)]
        r = lodo_context(times, lags=(1,))
        self.assertEqual(r['strict_minute_segment_count'], 2)
        self.assertEqual(r['lag_results']['1']['cross_calendar_day_context_pairs'], 0)
        self.assertEqual(sum(v['unique_cross_fold_context_rows_any_lag'] for v in r['lodo_folds'].values()), 0)

    def test_compensating_nonminute_intervals_are_not_contiguous(self):
        times = [datetime(2021, 7, 21, 23, 59), datetime(2021, 7, 21, 23, 59, 30), datetime(2021, 7, 22, 0, 1)]
        r = lodo_context(times, lags=(1, 2))
        self.assertEqual(r['strict_minute_segment_count'], 3)
        self.assertEqual(r['lag_results']['2']['cross_calendar_day_context_pairs'], 0)

    def test_reference_and_timestamp_as_predictors_fail_closed(self):
        for manifest in (('licor_ch4',), ('MQ4_1', 'licor_ch4'), ('datetime',),
                         ('MQ4_1', 'MQ4_1'), (), ('MQ4_1', 'mq4_2'), ('MQ4_1', None)):
            with self.subTest(manifest=manifest):
                with self.assertRaisesRegex(ValueError, 'PREDICTOR_MANIFEST'):
                    validate_predictors(manifest)
        self.assertEqual(validate_predictors(('MQ4_1', 'MQ4_3')), ['MQ4_1', 'MQ4_3'])

    def test_reject_invalid_lags_and_timestamp_order(self):
        times = [datetime(2021, 7, 21, 23, 59), datetime(2021, 7, 22, 0, 0)]
        for lags in ((), (0,), (True,), (1, 1), (2, 1), (1.5,)):
            with self.subTest(lags=lags):
                with self.assertRaisesRegex(ValueError, 'LAGS_INVALID'):
                    lodo_context(times, lags=lags)
        with self.assertRaisesRegex(ValueError, 'NONMONOTONIC'):
            lodo_context([times[0], times[1], times[1]])
        with self.assertRaisesRegex(ValueError, 'NONMONOTONIC'):
            lodo_context(times[::-1])

    def test_single_day_not_a_lodo_split(self):
        with self.assertRaisesRegex(ValueError, 'MULTIPLE_DAYS'):
            lodo_context([datetime(2021, 7, 21), datetime(2021, 7, 21, 0, 1)])

    def test_deterministic_json_replay(self):
        times = [datetime(2021, 7, 21, 23, 59) + timedelta(minutes=i) for i in range(7)]
        a = lodo_context(times, lags=(1, 5))
        b = lodo_context(times, lags=(1, 5))
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertFalse(a['actual_train_test_split_verified'])
        self.assertFalse(a['lag_features_used_by_a_model_verified'])


if __name__ == '__main__':
    unittest.main()
