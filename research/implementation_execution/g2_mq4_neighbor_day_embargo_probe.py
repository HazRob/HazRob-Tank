"""MQ4 observational source benchmark: calendar-day LODO with adjacent-day embargo.

Research-only, no calibration, no independent exposure evidence, no G2/G3 promotion.
No non-standard Python dependencies. Original source archive is NOT committed.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import statistics
import zipfile
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from g2_mq4_grouped_numeric_probe import ARCHIVE_SHA256, MEMBER_SHA256, MEMBER, CHANNELS


def require(ok, code):
    if not ok:
        raise ValueError('MQ4_NEIGHBOR_EMBARGO_' + code)


def parse_source(raw, expected_member_sha=None):
    if expected_member_sha is not None:
        require(hashlib.sha256(raw).hexdigest() == expected_member_sha, 'MEMBER_SHA256')
    try:
        reader = csv.DictReader(io.StringIO(raw.decode('utf-8-sig'), newline=''), strict=True)
        required = ('datetime', 'licor_ch4', *CHANNELS)
        require(reader.fieldnames is not None and len(set(reader.fieldnames)) == len(reader.fieldnames)
                and all(c in reader.fieldnames for c in required), 'COLUMNS')
        grouped = defaultdict(list)
        prev = None
        for row in reader:
            require(None not in row and all(v is not None for v in row.values()), 'CSV_SHAPE')
            timestamp = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
            require(timestamp.second == 0 and (prev is None or timestamp > prev), 'TIME_ORDER')
            prev = timestamp
            values = tuple(float(row[c]) for c in ('licor_ch4', *CHANNELS))
            require(all(math.isfinite(v) for v in values), 'FINITE')
            grouped[timestamp.date()].append(values)
    except (UnicodeError, csv.Error, OverflowError, TypeError, KeyError, ValueError) as exc:
        if isinstance(exc, ValueError) and str(exc).startswith('MQ4_NEIGHBOR_EMBARGO_'):
            raise
        raise ValueError('MQ4_NEIGHBOR_EMBARGO_INVALID_CSV') from exc
    require(len(grouped) >= 5 and all(len(rows) >= 3 for rows in grouped.values()), 'GROUPS')
    return dict(grouped)


def evaluate(raw, embargo_days=(0, 1, 2), expected_member_sha=None):
    groups = parse_source(raw, expected_member_sha)
    days = sorted(groups)
    embargo_days = tuple(embargo_days)
    require(bool(embargo_days) and len(set(embargo_days)) == len(embargo_days)
            and all(type(e) is int and 0 <= e <= 30 for e in embargo_days), 'EMBARGO_WIDTHS')
    result = {
        'schema': 'hazrob.g2.mq4-neighbor-day-embargo-probe.v1',
        'raw_member_sha256': hashlib.sha256(raw).hexdigest(),
        'rows': sum(len(v) for v in groups.values()),
        'days': [d.isoformat() for d in days],
        'day_rows': {d.isoformat(): len(groups[d]) for d in days},
        'method': 'calendar-day LODO univariate OLS with symmetric calendar-day embargo; fold-specific training-mean baseline',
        'embargo_days': list(embargo_days),
        'channels': {},
        'scientific_state': {'g2_qualified': False, 'g3_tank_qualified': False,
                             'physical_independence_verified': False,
                             'reference_calibration_verified': False,
                             'blinded_holdout_verified': False},
        'limitations': 'Calendar-day exclusion is not independent physical exposure holdout. Source-reported licor_ch4 ppb and uncalibrated raw MQ4 channels; no Tank transfer or gas identity/ppm validation.'
    }
    for col, channel in enumerate(CHANNELS, start=1):
        results = {}
        for width in embargo_days:
            fold_results = {}
            sse_model = sse_baseline = 0.0
            rmse_days = []
            negative_skill_days = 0
            for holdout in days:
                excluded = [d for d in days if abs((d - holdout).days) <= width]
                train_days = [d for d in days if d not in excluded]
                require(len(train_days) >= 2, 'INSUFFICIENT_TRAIN_DAYS')
                train = [row for d in train_days for row in groups[d]]
                test = groups[holdout]
                require(len(train) >= 3, 'INSUFFICIENT_TRAIN_ROWS')
                mx = statistics.mean(row[col] for row in train)
                my = statistics.mean(row[0] for row in train)
                xx = sum((row[col] - mx) ** 2 for row in train)
                xy = sum((row[col] - mx) * (row[0] - my) for row in train)
                slope = xy / xx if xx > 0 else 0.0
                sm = sum((row[0] - (my + slope * (row[col] - mx))) ** 2 for row in test)
                sb = sum((row[0] - my) ** 2 for row in test)
                require(math.isfinite(sm) and math.isfinite(sb), 'NONFINITE_ERROR')
                sse_model += sm
                sse_baseline += sb
                rmse = math.sqrt(sm / len(test))
                rmse_days.append(rmse)
                negative_skill_days += sm > sb
                fold_results[holdout.isoformat()] = {
                    'train_days': [d.isoformat() for d in train_days],
                    'excluded_days': [d.isoformat() for d in excluded],
                    'train_rows': len(train), 'test_rows': len(test),
                    'rmse_model_ppb': rmse,
                    'rmse_train_mean_baseline_ppb': math.sqrt(sb / len(test)),
                    'skill_vs_train_mean': (1 - sm / sb) if sb > 0 else None
                }
            n = sum(len(v) for v in groups.values())
            results[str(width)] = {
                'pooled_rmse_model_ppb': math.sqrt(sse_model / n),
                'pooled_rmse_baseline_ppb': math.sqrt(sse_baseline / n),
                'pooled_skill_vs_train_mean': (1 - sse_model / sse_baseline) if sse_baseline > 0 else None,
                'macro_mean_day_rmse_model_ppb': statistics.mean(rmse_days),
                'worst_day_rmse_model_ppb': max(rmse_days),
                'days_worse_than_training_mean': negative_skill_days,
                'folds': fold_results
            }
        result['channels'][channel] = results
    return result


def evaluate_zip(archive, expected_archive_sha=ARCHIVE_SHA256,
                 expected_member_sha=MEMBER_SHA256, embargo_days=(0, 1, 2)):
    require(hashlib.sha256(archive).hexdigest() == expected_archive_sha, 'ARCHIVE_SHA256')
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            require(z.namelist().count(MEMBER) == 1, 'ZIP_MEMBER')
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, OSError, RuntimeError) as exc:
        raise ValueError('MQ4_NEIGHBOR_EMBARGO_INVALID_ZIP') from exc
    return evaluate(raw, embargo_days=embargo_days, expected_member_sha=expected_member_sha)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--archive', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--embargo-days', type=int, nargs='+', default=[0, 1, 2])
    args = p.parse_args()
    report = evaluate_zip(args.archive.read_bytes(), embargo_days=args.embargo_days)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + '\n', encoding='utf-8')
    print('MQ4 adjacent-day embargo research probe complete; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
