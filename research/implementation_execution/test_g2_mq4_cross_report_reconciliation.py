"""Synthetic and pinned-summary regressions. No raw replay or scientific G2 approval."""
import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_cross_report_reconciliation import ARCHIVE, RAW, LAGS, load_report, reconcile, check_lag_pair_convexity


def synthetic():
    n = 181
    a, b = '2021-07-21', '2021-07-22'
    lag_results = {}
    for lag in LAGS:
        cross, available = min(lag, 60), n - lag
        lag_results[str(lag)] = {
            'lag_minutes': lag, 'contiguous_lag_pairs': available,
            'same_day_context_rows': available - cross,
            'cross_day_context_rows_requiring_embargo_if_day_holdout': cross,
            'cross_day_context_rows_by_later_date': {b: cross},
            'lag_context_unavailable_rows': lag,
            'cross_day_fraction_of_available_pairs': cross / available,
        }
    lodo = {
        'schema': 'hazrob.g2.mq4-lodo-two-way-purge-raw-summary.v1',
        'source': 'MQ-ACQ-2026-06', 'source_archive_sha256': ARCHIVE,
        'raw_member_sha256': RAW, 'raw_replay_verified': True,
        'raw_rows': n, 'strict_minute_segments': 1, 'calendar_dates': 2,
        'max_lag_minutes': 120, 'eligible_rows_all_lags': 61,
        'sum_holdout_excluded_across_folds': 60, 'sum_train_purged_across_folds': 60,
        'model_training_verified': False, 'physical_independence_verified': False,
        'g2_qualified': False, 'g3_tank_qualified': False,
        'folds': {
            a: {'holdout_total': 60, 'holdout_timestamp_eligible': 0,
                'holdout_excluded_training_context': 0, 'holdout_remaining': 0,
                'train_timestamp_eligible': 61, 'train_purged_holdout_context': 60,
                'train_remaining': 1, 'holdout_history_ineligible': 60},
            b: {'holdout_total': 121, 'holdout_timestamp_eligible': 61,
                'holdout_excluded_training_context': 60, 'holdout_remaining': 1,
                'train_timestamp_eligible': 0, 'train_purged_holdout_context': 0,
                'train_remaining': 0, 'holdout_history_ineligible': 60},
        },
    }
    multi = {
        'schema': 'hazrob.g2.mq4-multiscale-day-context-embargo.v1',
        'source_id': 'MQ-ACQ-2026-06', 'source_archive_sha256': ARCHIVE,
        'source_member_sha256': RAW, 'source_member_sha256_verified': True,
        'row_count': n, 'contiguous_one_minute_segment_count': 1,
        'lag_results': lag_results,
        'calendar_day_split_physical_independence_verified': False,
        'model_use_of_lag_features_verified': False,
        'g2_qualified': False, 'g3_tank_qualified': False,
    }
    return lodo, multi


class MQ4CrossReportTests(unittest.TestCase):
    def test_synthetic_reconciliation_and_scientific_boundary(self):
        l, m = synthetic()
        r = reconcile(l, m)
        self.assertEqual((r['row_count'], r['timestamp_eligible_rows_all_lags'],
                          r['holdout_context_exclusions_across_folds']), (181, 61, 60))
        self.assertFalse(r['original_raw_bytes_replayed_by_this_audit'])
        self.assertFalse(r['g2_qualified'])
        self.assertFalse(r['g3_tank_qualified'])

    def test_deterministic_replay(self):
        l, m = synthetic()
        self.assertEqual(json.dumps(reconcile(l, m), sort_keys=True),
                         json.dumps(reconcile(l, m), sort_keys=True))

    def test_reject_row_count_mismatch(self):
        l, m = synthetic()
        m['row_count'] += 1
        with self.assertRaisesRegex(ValueError, 'GLOBAL_COUNTS'):
            reconcile(l, m)

    def test_reject_lag_arithmetic(self):
        l, m = synthetic()
        m['lag_results']['5']['same_day_context_rows'] += 1
        with self.assertRaisesRegex(ValueError, 'LAG_ARITHMETIC'):
            reconcile(l, m)

    def test_reject_cross_day_distribution(self):
        l, m = synthetic()
        m['lag_results']['60']['cross_day_context_rows_by_later_date']['2021-07-22'] -= 1
        with self.assertRaisesRegex(ValueError, 'LAG_DISTRIBUTION'):
            reconcile(l, m)

    def test_reject_holdout_boundary_mismatch(self):
        l, m = synthetic()
        f = l['folds']['2021-07-22']
        f['holdout_excluded_training_context'] = 59
        f['holdout_remaining'] = 2
        with self.assertRaisesRegex(ValueError, 'HOLDOUT_BOUNDARY'):
            reconcile(l, m)

    def test_reject_training_purge_mismatch(self):
        l, m = synthetic()
        f = l['folds']['2021-07-21']
        f['train_purged_holdout_context'] = 59
        f['train_remaining'] = 2
        with self.assertRaisesRegex(ValueError, 'TRAIN_BOUNDARY'):
            reconcile(l, m)

    def test_reject_unsupported_qualification(self):
        l, m = synthetic()
        m['g2_qualified'] = True
        with self.assertRaisesRegex(ValueError, 'QUALIFICATION'):
            reconcile(l, m)

    def test_reject_false_replay_and_source_tamper(self):
        l, m = synthetic()
        l['raw_replay_verified'] = False
        with self.assertRaisesRegex(ValueError, 'SOURCE_PIN'):
            reconcile(l, m)
        l, m = synthetic()
        m['source_member_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'SOURCE_PIN'):
            reconcile(l, m)

    def test_reject_boolean_count(self):
        l, m = synthetic()
        l['raw_rows'] = True
        with self.assertRaisesRegex(ValueError, 'COUNTS'):
            reconcile(l, m)

    def test_duplicate_json_keys_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / 'dup.json'
            p.write_text('{"x":1,"x":2}', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'DUPLICATE_JSON_KEY'):
                load_report(p)

    def test_nonfinite_json_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / 'nan.json'
            p.write_text('{"x":NaN}', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'NONFINITE_JSON'):
                load_report(p)

    def test_actual_repository_pinned_reports(self):
        root = Path(__file__).resolve().parent
        a = root / 'G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json'
        b = root / 'G2_MQ4_MULTISCALE_DAY_CONTEXT_EMBARGO_2026-10-30.json'
        result = reconcile(load_report(a), load_report(b))
        self.assertEqual(result['row_count'], 14186)
        self.assertEqual(result['timestamp_eligible_rows_all_lags'], 12981)
        self.assertEqual(result['holdout_context_exclusions_across_folds'], 1200)
        self.assertEqual(result['training_context_purges_across_folds'], 1200)


    def test_committed_result_is_byte_identical_replay(self):
        root = Path(__file__).resolve().parent
        a = root / 'G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json'
        b = root / 'G2_MQ4_MULTISCALE_DAY_CONTEXT_EMBARGO_2026-10-30.json'
        saved = root / 'G2_MQ4_CROSS_REPORT_RECONCILIATION_2026-10-31.json'
        result = reconcile(load_report(a), load_report(b))
        self.assertEqual(saved.read_text(encoding='utf-8'),
                         json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')


    def test_reject_float_calendar_count(self):
        l, m = synthetic()
        l['calendar_dates'] = 2.0
        with self.assertRaisesRegex(ValueError, 'FOLDS'):
            reconcile(l, m)

    def test_reject_float_max_lag(self):
        l, m = synthetic()
        l['max_lag_minutes'] = 120.0
        with self.assertRaisesRegex(ValueError, 'GLOBAL_COUNTS'):
            reconcile(l, m)

    def test_reject_invalid_iso_calendar_date(self):
        l, m = synthetic()
        l['folds']['2021-02-30'] = l['folds'].pop('2021-07-21')
        with self.assertRaisesRegex(ValueError, 'DATES'):
            reconcile(l, m)

    def test_reject_boolean_lag_identifier(self):
        l, m = synthetic()
        m['lag_results']['1']['lag_minutes'] = True
        with self.assertRaisesRegex(ValueError, 'LAG_ID'):
            reconcile(l, m)

    def test_reject_lag_one_segment_identity(self):
        l, m = synthetic()
        l['strict_minute_segments'] = 2
        m['contiguous_one_minute_segment_count'] = 2
        with self.assertRaisesRegex(ValueError, 'SEGMENT_LAG_ONE'):
            reconcile(l, m)

    def test_reject_lag_pair_count_increasing_with_lag(self):
        l, m = synthetic()
        item = m['lag_results']['15']
        item['contiguous_lag_pairs'] = 177
        item['same_day_context_rows'] = 162
        item['lag_context_unavailable_rows'] = 4
        with self.assertRaisesRegex(ValueError, 'LAG_AVAILABLE_MONOTONIC'):
            reconcile(l, m)

    def test_reject_more_cross_midnight_pairs_than_lag(self):
        l, m = synthetic()
        item = m['lag_results']['5']
        item['cross_day_context_rows_requiring_embargo_if_day_holdout'] = 6
        item['cross_day_context_rows_by_later_date']['2021-07-22'] = 6
        item['same_day_context_rows'] = 170
        item['cross_day_fraction_of_available_pairs'] = 6 / 176
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_LAG_CAP'):
            reconcile(l, m)

    def test_reject_per_fold_train_complement_when_global_total_matches(self):
        l, m = synthetic()
        first = l['folds']['2021-07-21']
        second = l['folds']['2021-07-22']
        first['train_timestamp_eligible'] = 60
        first['train_remaining'] = 0
        second['train_timestamp_eligible'] = 1
        second['train_remaining'] = 1
        with self.assertRaisesRegex(ValueError, 'FOLD_TRAIN_COMPLEMENT'):
            reconcile(l, m)


    def test_reject_cross_day_lag_across_missing_calendar_date(self):
        l, m = synthetic()
        old, new = '2021-07-22', '2021-07-24'
        l['folds'][new] = l['folds'].pop(old)
        for item in m['lag_results'].values():
            item['cross_day_context_rows_by_later_date'][new] = (
                item['cross_day_context_rows_by_later_date'].pop(old))
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_NONADJACENT'):
            reconcile(l, m)

    def test_reject_long_lag_crossing_without_lag_one_continuity(self):
        l, m = synthetic()
        item = m['lag_results']['1']
        item['same_day_context_rows'] += 1
        item['cross_day_context_rows_requiring_embargo_if_day_holdout'] = 0
        item['cross_day_context_rows_by_later_date'] = {}
        item['cross_day_fraction_of_available_pairs'] = 0.0
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_NO_LAG_ONE_CONTINUITY'):
            reconcile(l, m)

    def test_reject_cross_day_rows_assigned_to_first_observed_date(self):
        l, m = synthetic()
        item = m['lag_results']['1']
        item['cross_day_context_rows_by_later_date'] = {'2021-07-21': 1}
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_NONADJACENT'):
            reconcile(l, m)


    def test_reject_lag_pair_count_above_segment_combinatorial_maximum(self):
        l, m = synthetic()
        item = m['lag_results']['5']
        item['contiguous_lag_pairs'] += 1
        item['same_day_context_rows'] += 1
        item['lag_context_unavailable_rows'] -= 1
        item['cross_day_fraction_of_available_pairs'] = 5 / item['contiguous_lag_pairs']
        # Still below lag-one pairs: monotonicity alone would miss this.
        with self.assertRaisesRegex(ValueError, 'LAG_MAX_PAIRS_BY_SEGMENTS'):
            reconcile(l, m)

    def test_reject_cross_day_pairs_exceeding_later_day_rows(self):
        l, m = synthetic()
        first, second = l['folds']['2021-07-21'], l['folds']['2021-07-22']
        # At the maximum lag only one row crosses the boundary, whereas
        # the shorter 60-minute lag declares 60 cross-boundary rows.
        # The latter cannot fit in the later day's 50 actual rows.
        max_item = m['lag_results']['120']
        max_item['cross_day_context_rows_requiring_embargo_if_day_holdout'] = 1
        max_item['cross_day_context_rows_by_later_date']['2021-07-22'] = 1
        max_item['same_day_context_rows'] = max_item['contiguous_lag_pairs'] - 1
        max_item['cross_day_fraction_of_available_pairs'] = 1 / max_item['contiguous_lag_pairs']
        l['sum_holdout_excluded_across_folds'] = 1
        l['sum_train_purged_across_folds'] = 1
        first.update(holdout_total=131, holdout_timestamp_eligible=60,
                     holdout_history_ineligible=71, holdout_remaining=60,
                     train_timestamp_eligible=1, train_purged_holdout_context=1,
                     train_remaining=0)
        second.update(holdout_total=50, holdout_timestamp_eligible=1,
                      holdout_history_ineligible=49,
                      holdout_excluded_training_context=1, holdout_remaining=0,
                      train_timestamp_eligible=60, train_remaining=60)
        # The 60-minute lag still declares 60 later-day cross-context rows.
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_EXCEEDS_DAY_ROWS'):
            reconcile(l, m)

    def test_reject_declared_calendar_day_with_zero_source_rows(self):
        l, m = synthetic()
        first, second = l['folds']['2021-07-21'], l['folds']['2021-07-22']
        first['holdout_total'] = 0
        first['holdout_history_ineligible'] = 0
        second['holdout_total'] = 181
        second['holdout_history_ineligible'] = 120
        with self.assertRaisesRegex(ValueError, 'FOLD_EMPTY_CALENDAR_DAY'):
            reconcile(l, m)


    def test_reject_lag_pair_count_below_segment_combinatorial_minimum(self):
        l, m = synthetic()
        item = m['lag_results']['5']
        item['contiguous_lag_pairs'] -= 1
        item['same_day_context_rows'] -= 1
        item['lag_context_unavailable_rows'] += 1
        item['cross_day_fraction_of_available_pairs'] = (
            5 / item['contiguous_lag_pairs'])
        # One uninterrupted 181-row segment has exactly 176 lag-5 pairs.
        # The old upper bound and monotonicity checks both accepted 175.
        with self.assertRaisesRegex(ValueError, 'LAG_MIN_PAIRS_BY_SEGMENTS'):
            reconcile(l, m)

    def test_reject_cross_day_pairs_exceeding_previous_day_rows(self):
        l, m = synthetic()
        # A lag-5 crossing declares five distinct preceding-day rows,
        # but only four source rows are now declared for that day.
        # Later-day bounds alone cannot detect this contradiction.
        first = l['folds']['2021-07-21']
        second = l['folds']['2021-07-22']
        first['holdout_total'] = 4
        first['holdout_history_ineligible'] = 4
        second['holdout_total'] = 177
        second['holdout_history_ineligible'] = 116
        # Preserve fold arithmetic and global row totals, exposing only
        # the impossible previous-day capacity in the summary metadata.
        with self.assertRaisesRegex(ValueError, 'CROSS_DAY_EXCEEDS_PRIOR_DAY_ROWS'):
            reconcile(l, m)



    def test_accept_convex_sparse_lag_pairs(self):
        # Segment lengths 10 and 171: P(k) = sum(max(0, length-k)).
        knots = [(k, max(0, 10-k) + max(0, 171-k)) for k in LAGS]
        self.assertIsNone(check_lag_pair_convexity(knots))

    def test_reject_nonconvex_sparse_lag_pairs(self):
        # Every P(k) is monotone and inside its independent segment bounds.
        # Yet P(5)=174, P(15)=156 requires the average surviving-segment
        # count to increase from 1.25 to 1.8, which is impossible.
        knots = [(1, 179), (5, 174), (15, 156), (30, 141),
                 (60, 111), (120, 51)]
        with self.assertRaisesRegex(ValueError, 'LAG_SEGMENT_CONVEXITY'):
            check_lag_pair_convexity(knots)

    def test_pinned_mq4_lag_pairs_satisfy_convexity(self):
        root = Path(__file__).resolve().parent
        p = root / 'G2_MQ4_MULTISCALE_DAY_CONTEXT_EMBARGO_2026-10-30.json'
        report = load_report(p)
        knots = [(k, report['lag_results'][str(k)]['contiguous_lag_pairs'])
                 for k in LAGS]
        self.assertIsNone(check_lag_pair_convexity(knots))


if __name__ == '__main__':
    unittest.main()
