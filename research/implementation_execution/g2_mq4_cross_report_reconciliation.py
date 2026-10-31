"""Fail-closed cross-check of two MQ4 research reports; NOT a raw replay or G2/G3 validation."""
import argparse
import json
from datetime import date
from pathlib import Path

SOURCE = 'MQ-ACQ-2026-06'
ARCHIVE = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
RAW = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
LAGS = (1, 5, 15, 30, 60, 120)


def check(condition, code):
    if not condition:
        raise ValueError('MQ4_CROSS_REPORT_' + code)


def integer(value):
    return type(value) is int and value >= 0


def unique_keys(pairs):
    d = {}
    for k, v in pairs:
        check(k not in d, 'DUPLICATE_JSON_KEY')
        d[k] = v
    return d


def nonfinite(value):
    raise ValueError('MQ4_CROSS_REPORT_NONFINITE_JSON_' + value)


def load_report(path):
    return json.loads(Path(path).read_text(encoding='utf-8'),
                      object_pairs_hook=unique_keys, parse_constant=nonfinite)


def valid_iso_day(value):
    if type(value) is not str or len(value) != 10 or value[4] != '-' or value[7] != '-':
        return False
    try:
        return date.fromisoformat(value).isoformat() == value
    except ValueError:
        return False


def reconcile(lodo, multi):
    check(type(lodo) is dict and type(multi) is dict, 'NOT_OBJECT')
    check(lodo.get('schema') == 'hazrob.g2.mq4-lodo-two-way-purge-raw-summary.v1' and
          multi.get('schema') == 'hazrob.g2.mq4-multiscale-day-context-embargo.v1', 'SCHEMA')
    check(lodo.get('source') == multi.get('source_id') == SOURCE, 'SOURCE')
    check(lodo.get('source_archive_sha256') == multi.get('source_archive_sha256') == ARCHIVE and
          lodo.get('raw_member_sha256') == multi.get('source_member_sha256') == RAW and
          lodo.get('raw_replay_verified') is True and
          multi.get('source_member_sha256_verified') is True, 'SOURCE_PIN')
    check(all(r.get('g2_qualified') is False and r.get('g3_tank_qualified') is False
              for r in (lodo, multi)), 'QUALIFICATION')
    check(lodo.get('physical_independence_verified') is False and
          lodo.get('model_training_verified') is False and
          multi.get('calendar_day_split_physical_independence_verified') is False and
          multi.get('model_use_of_lag_features_verified') is False, 'SCIENTIFIC_BOUNDARY')
    n, segments, eligible = (lodo.get(k) for k in
                             ('raw_rows', 'strict_minute_segments', 'eligible_rows_all_lags'))
    check(all(integer(v) for v in (n, segments, eligible)) and
          n > 0 and segments > 0 and eligible <= n, 'COUNTS')
    check(multi.get('row_count') == n and
          multi.get('contiguous_one_minute_segment_count') == segments and
          type(lodo.get('max_lag_minutes')) is int and
          lodo.get('max_lag_minutes') == max(LAGS), 'GLOBAL_COUNTS')
    lr = multi.get('lag_results')
    check(type(lr) is dict and set(lr) == {str(k) for k in LAGS}, 'LAGS')
    folds = lodo.get('folds')
    check(type(folds) is dict and len(folds) >= 2 and
          type(lodo.get('calendar_dates')) is int and
          lodo.get('calendar_dates') == len(folds), 'FOLDS')
    days = sorted(folds)
    check(all(valid_iso_day(d) for d in days), 'DATES')
    previous_available = None
    lag_one_cross_day = {}
    day_index = {day: i for i, day in enumerate(days)}
    for lag in LAGS:
        item = lr[str(lag)]
        check(type(item) is dict and type(item.get('lag_minutes')) is int and
              item.get('lag_minutes') == lag, 'LAG_ID')
        a = item.get('same_day_context_rows')
        b = item.get('cross_day_context_rows_requiring_embargo_if_day_holdout')
        c = item.get('lag_context_unavailable_rows')
        available = item.get('contiguous_lag_pairs')
        check(all(integer(v) for v in (a, b, c, available)) and
              a + b == available and available + c == n, 'LAG_ARITHMETIC')
        if lag == 1:
            check(available == n - segments, 'SEGMENT_LAG_ONE')
        if previous_available is not None:
            check(available <= previous_available, 'LAG_AVAILABLE_MONOTONIC')
        # With `segments` nonempty contiguous runs and n total rows, the
        # maximum lag-k pairs occur when all but one run contain one row.
        # This is a necessary combinatorial bound, not raw-signal validation.
        check(available <= max(0, n - segments + 1 - lag),
              'LAG_MAX_PAIRS_BY_SEGMENTS')
        previous_available = available
        distribution = item.get('cross_day_context_rows_by_later_date')
        check(type(distribution) is dict and set(distribution).issubset(set(days)) and
              all(integer(v) for v in distribution.values()) and
              sum(distribution.values()) == b, 'LAG_DISTRIBUTION')
        check(all(v <= lag for v in distribution.values()), 'CROSS_DAY_LAG_CAP')
        # A cross-midnight context pair is anchored to a later-day row;
        # its count cannot exceed that day's actual holdout-row count.
        check(all(v <= folds[day].get('holdout_total', -1)
                  for day, v in distribution.items()
                  if type(folds.get(day)) is dict),
              'CROSS_DAY_EXCEEDS_DAY_ROWS')
        for later_day, cross_rows in distribution.items():
            if cross_rows:
                prior_index = day_index[later_day] - 1
                check(prior_index >= 0 and
                      (date.fromisoformat(later_day) -
                       date.fromisoformat(days[prior_index])).days == 1,
                      'CROSS_DAY_NONADJACENT')
        if lag == 1:
            lag_one_cross_day = dict(distribution)
        else:
            check(all(not rows or lag_one_cross_day.get(day, 0) == 1
                      for day, rows in distribution.items()),
                  'CROSS_DAY_NO_LAG_ONE_CONTINUITY')
        fraction = item.get('cross_day_fraction_of_available_pairs')
        expected = b / available if available else None
        check((fraction is None and expected is None) or
              (type(fraction) in (int, float) and expected is not None and
               abs(fraction - expected) < 1e-12), 'LAG_FRACTION')
    check(lr[str(max(LAGS))]['contiguous_lag_pairs'] == eligible, 'ELIGIBILITY')
    max_cross = lr[str(max(LAGS))]['cross_day_context_rows_by_later_date']
    total_rows = total_eligible = total_train = total_excluded = total_purged = 0
    for i, day in enumerate(days):
        f = folds[day]
        keys = ('holdout_total', 'holdout_timestamp_eligible',
                'holdout_excluded_training_context', 'holdout_remaining',
                'train_timestamp_eligible', 'train_purged_holdout_context',
                'train_remaining', 'holdout_history_ineligible')
        check(type(f) is dict and all(integer(f.get(k)) for k in keys), 'FOLD_COUNTS')
        check(f['holdout_total'] > 0, 'FOLD_EMPTY_CALENDAR_DAY')
        check(f['holdout_timestamp_eligible'] + f['holdout_history_ineligible'] == f['holdout_total'] and
              f['holdout_remaining'] + f['holdout_excluded_training_context'] == f['holdout_timestamp_eligible'] and
              f['train_remaining'] + f['train_purged_holdout_context'] == f['train_timestamp_eligible'],
              'FOLD_ARITHMETIC')
        check(f['train_timestamp_eligible'] + f['holdout_timestamp_eligible'] == eligible,
              'FOLD_TRAIN_COMPLEMENT')
        check(f['holdout_excluded_training_context'] == max_cross.get(day, 0),
              'HOLDOUT_BOUNDARY')
        next_day = days[i + 1] if i + 1 < len(days) else None
        check(f['train_purged_holdout_context'] == max_cross.get(next_day, 0),
              'TRAIN_BOUNDARY')
        total_rows += f['holdout_total']
        total_eligible += f['holdout_timestamp_eligible']
        total_train += f['train_timestamp_eligible']
        total_excluded += f['holdout_excluded_training_context']
        total_purged += f['train_purged_holdout_context']
    check(total_rows == n and total_eligible == eligible and
          total_train == eligible * (len(days) - 1), 'FOLD_TOTALS')
    check(total_excluded == total_purged ==
          lr[str(max(LAGS))]['cross_day_context_rows_requiring_embargo_if_day_holdout'] and
          total_excluded == lodo.get('sum_holdout_excluded_across_folds') and
          total_purged == lodo.get('sum_train_purged_across_folds'), 'PURGE_TOTALS')
    return {
        'schema': 'hazrob.g2.mq4-cross-report-reconciliation.v1',
        'source_id': SOURCE, 'source_archive_sha256': ARCHIVE,
        'source_member_sha256': RAW, 'row_count': n,
        'calendar_date_count': len(days), 'strict_minute_segment_count': segments,
        'max_lag_minutes': max(LAGS), 'timestamp_eligible_rows_all_lags': eligible,
        'holdout_context_exclusions_across_folds': total_excluded,
        'training_context_purges_across_folds': total_purged,
        'reconciled_lags': list(LAGS), 'summary_arithmetic_reconciled': True,
        'original_raw_bytes_replayed_by_this_audit': False,
        'physical_exposure_independence_verified': False,
        'model_training_or_accuracy_verified': False,
        'g2_qualified': False, 'g3_tank_qualified': False,
        'limitations': 'Checks two published summaries only. Shared upstream assumptions may remain; source-level replay requires original raw ZIP. No calibrated gas identity, independent exposure or scientific promotion.'
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--lodo', type=Path, required=True)
    p.add_argument('--multiscale', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    result = reconcile(load_report(args.lodo), load_report(args.multiscale))
    args.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True,
                                      indent=2) + '\n', encoding='utf-8')
    print('MQ4 summaries reconciled; original raw NOT replayed; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
