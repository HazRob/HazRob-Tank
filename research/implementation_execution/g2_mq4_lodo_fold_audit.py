"""Research-only leave-one-calendar-day-out (LODO) lag-context audit.

Not a physical exposure split, trained model, calibration, or G2/G3 qualification.
"""
import argparse
import csv
import hashlib
import io
import json
import zipfile
from datetime import datetime
from pathlib import Path

MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
RAW_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
LAGS = (1, 5, 15, 30, 60, 120)
PREDICTORS = ('MQ4_1', 'MQ4_2', 'MQ4_3')
TARGET = 'licor_ch4'


def validate_predictors(predictors):
    if (not isinstance(predictors, (tuple, list)) or not predictors or
            any(type(x) is not str for x in predictors) or
            len(predictors) != len(set(predictors)) or
            any(x not in PREDICTORS for x in predictors)):
        raise ValueError('MQ4_PREDICTOR_MANIFEST_INVALID_OR_REFERENCE_LEAKAGE')
    return list(predictors)


def lodo_context(timestamps, *, lags=LAGS, predictors=PREDICTORS):
    """Exact feature-context fold crossings, conditional on lag-feature use.

    LODO here means one *calendar date* held out. It is not an authenticated
    physically independent holdout. Only strictly consecutive minute samples
    are eligible to provide lag context. Unique row unions are per-fold.
    """
    predictors = validate_predictors(predictors)
    if (not timestamps or any(type(t) is not datetime or t.tzinfo is not None for t in timestamps)):
        raise ValueError('MQ4_TIMESTAMPS_INVALID')
    if (not lags or any(type(x) is not int or x <= 0 for x in lags) or
            tuple(sorted(set(lags))) != tuple(lags)):
        raise ValueError('MQ4_LAGS_INVALID')
    dates = [t.date().isoformat() for t in timestamps]
    days = sorted(set(dates))
    if len(days) < 2:
        raise ValueError('MQ4_LODO_REQUIRES_MULTIPLE_DAYS')
    segment = [0]
    for i in range(1, len(timestamps)):
        delta = (timestamps[i] - timestamps[i - 1]).total_seconds()
        if delta <= 0:
            raise ValueError('MQ4_NONMONOTONIC_TIMESTAMPS')
        segment.append(segment[-1] + (delta != 60))
    folds = {d: {'holdout_date': d, 'train_to_holdout_context_rows': set(),
                 'holdout_to_train_context_rows': set(), 'per_lag': {}} for d in days}
    global_by_lag = {}
    for lag in lags:
        cross_pairs = []
        for i in range(lag, len(timestamps)):
            j = i - lag
            if segment[i] == segment[j] and dates[i] != dates[j]:
                cross_pairs.append((i, dates[j], dates[i]))
        global_by_lag[str(lag)] = {'cross_calendar_day_context_pairs': len(cross_pairs)}
        crossing_fold_sum = 0
        for d in days:
            train_to_holdout = {i for i, source_day, later_day in cross_pairs
                                if later_day == d and source_day != d}
            holdout_to_train = {i for i, source_day, later_day in cross_pairs
                                if source_day == d and later_day != d}
            folds[d]['train_to_holdout_context_rows'].update(train_to_holdout)
            folds[d]['holdout_to_train_context_rows'].update(holdout_to_train)
            folds[d]['per_lag'][str(lag)] = {
                'train_to_holdout_context_rows': len(train_to_holdout),
                'holdout_to_train_context_rows': len(holdout_to_train),
                'cross_fold_context_rows': len(train_to_holdout | holdout_to_train),
            }
            crossing_fold_sum += len(train_to_holdout) + len(holdout_to_train)
        if crossing_fold_sum != 2 * len(cross_pairs):
            raise AssertionError('MQ4_LODO_PAIR_RECONCILIATION_FAILED')
        global_by_lag[str(lag)]['sum_cross_fold_context_rows_over_lodo_folds'] = crossing_fold_sum
    result_folds = {}
    for d in days:
        f = folds[d]
        result_folds[d] = {
            'holdout_row_count': dates.count(d),
            'unique_train_to_holdout_context_rows_any_lag': len(f['train_to_holdout_context_rows']),
            'unique_holdout_to_train_context_rows_any_lag': len(f['holdout_to_train_context_rows']),
            'unique_cross_fold_context_rows_any_lag': len(f['train_to_holdout_context_rows'] | f['holdout_to_train_context_rows']),
            'per_lag': f['per_lag'],
        }
    return {
        'schema': 'hazrob.g2.mq4-lodo-calendar-fold-context.v1',
        'row_count': len(timestamps), 'calendar_date_count': len(days),
        'strict_minute_segment_count': segment[-1] + 1,
        'predictor_manifest': predictors, 'reference_target': TARGET,
        'reference_target_used_as_predictor': False,
        'lag_results': global_by_lag, 'lodo_folds': result_folds,
        'actual_train_test_split_verified': False,
        'calendar_day_physical_independence_verified': False,
        'lag_features_used_by_a_model_verified': False,
        'g2_qualified': False, 'g3_tank_qualified': False,
    }


def audit_pinned_archive(path):
    archive = Path(path).read_bytes()
    if hashlib.sha256(archive).hexdigest() != ARCHIVE_SHA256:
        raise ValueError('MQ4_ARCHIVE_SHA256_MISMATCH')
    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        if zf.namelist().count(MEMBER) != 1:
            raise ValueError('MQ4_MEMBER_MISSING_OR_DUPLICATED')
        raw = zf.read(MEMBER)
    if hashlib.sha256(raw).hexdigest() != RAW_SHA256:
        raise ValueError('MQ4_RAW_SHA256_MISMATCH')
    reader = csv.DictReader(io.StringIO(raw.decode('utf-8'), newline=''), strict=True)
    if not reader.fieldnames or len(reader.fieldnames) != len(set(reader.fieldnames)) or not set(('datetime', TARGET, *PREDICTORS)).issubset(reader.fieldnames):
        raise ValueError('MQ4_CSV_HEADER_INVALID')
    timestamps = []
    for row in reader:
        if None in row or any(v is None for v in row.values()):
            raise ValueError('MQ4_CSV_ROW_INVALID')
        stamp = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
        if stamp.strftime('%Y-%m-%d %H:%M:%S') != row['datetime']:
            raise ValueError('MQ4_TIMESTAMP_NONCANONICAL')
        timestamps.append(stamp)
    result = lodo_context(timestamps)
    if result['row_count'] != 14186 or result['strict_minute_segment_count'] != 11 or result['lag_results']['60']['cross_calendar_day_context_pairs'] != 600:
        raise ValueError('MQ4_PINNED_RECONCILIATION_FAILED')
    return {
        'source_id': 'MQ-ACQ-2026-06',
        'source_archive_sha256': ARCHIVE_SHA256,
        'source_member_sha256': RAW_SHA256,
        'source_member': MEMBER,
        'source_hashes_verified': True,
        **result,
        'units': {'reference_target': 'ppb', 'MQ4_channels': 'ohm'},
        'method': 'For each leave-one-calendar-day-out fold, count strictly contiguous lag-feature contexts that cross the hypothetical train/holdout boundary. Report union of affected later rows over all lags, without double counting.',
        'limitations': 'Hypothetical LODO only. A calendar day is not an independent exposure. No actual training, feature manifest, split, device calibration, source exposure identity or G2/G3 scientific approval established.',
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--archive', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    result = audit_pinned_archive(args.archive)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print('MQ4 hypothetical LODO context audit complete; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
