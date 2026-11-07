"""Research-only split-conformal finite-rank feasibility for MQ4 calendar-day groups.

No model, intervals, empirical coverage, independent exposures or G2/G3 promotion.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from collections import Counter
from datetime import date, datetime, timedelta
from pathlib import Path

ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
MEMBER_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
STATE = {k: False for k in ('g2_qualified', 'g3_tank_qualified',
                            'physical_independence_verified',
                            'reference_calibration_verified', 'blinded_holdout_verified')}


def require(ok, code):
    if not ok:
        raise ValueError('MQ4_DAY_CONFORMAL_' + code)


def read_days(raw, expected_sha=None):
    if expected_sha is not None:
        require(hashlib.sha256(raw).hexdigest() == expected_sha, 'MEMBER_SHA256')
    try:
        reader = csv.DictReader(io.StringIO(raw.decode('utf-8-sig'), newline=''), strict=True)
        require(reader.fieldnames is not None and len(set(reader.fieldnames)) == len(reader.fieldnames)
                and 'datetime' in reader.fieldnames, 'HEADER')
        counts = Counter()
        previous = None
        for row in reader:
            require(None not in row and all(value is not None for value in row.values()), 'ROW_SHAPE')
            timestamp = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
            require(timestamp.second == 0 and (previous is None or timestamp > previous), 'TIME_ORDER')
            counts[timestamp.date()] += 1
            previous = timestamp
    except (UnicodeError, csv.Error, ValueError, TypeError, OverflowError, KeyError) as exc:
        if isinstance(exc, ValueError) and str(exc).startswith('MQ4_DAY_CONFORMAL_'):
            raise
        raise ValueError('MQ4_DAY_CONFORMAL_INVALID_CSV') from exc
    require(len(counts) >= 5 and all(n >= 3 for n in counts.values()), 'GROUPS')
    return dict(counts)


def required_groups(nominal):
    """Finite split-conformal rank requires ceil((n+1)*p) <= n.

    Exchangeability is unverified: the count alone never proves coverage.
    """
    require(type(nominal) in (float, int) and math.isfinite(nominal)
            and 0 < nominal < 1, 'NOMINAL')
    n = max(1, math.ceil(nominal / (1 - nominal) - 1e-12))
    while math.ceil((n + 1) * nominal - 1e-12) > n:
        n += 1
    return n


def analyze(counts, holdout_days=6, min_train_days=3, gaps=(0, 1, 2),
            nominal_levels=(0.9, 0.95), raw_member_sha256=None):
    require(isinstance(counts, dict) and len(counts) >= 5 and
            all(isinstance(d, date) and type(n) is int and n >= 3
                for d, n in counts.items()), 'GROUPS')
    require(type(holdout_days) is int and holdout_days >= 1, 'HOLDOUT_DAYS')
    require(type(min_train_days) is int and min_train_days >= 2, 'TRAIN_DAYS')
    gaps, nominal_levels = tuple(gaps), tuple(nominal_levels)
    require(bool(gaps) and len(set(gaps)) == len(gaps) and
            all(type(g) is int and 0 <= g <= 30 for g in gaps), 'GAPS')
    require(bool(nominal_levels) and len(set(nominal_levels)) == len(nominal_levels), 'NOMINALS')
    required = {str(float(p)): required_groups(p) for p in nominal_levels}
    days = sorted(counts)
    require(holdout_days < len(days), 'HOLDOUT_DAYS')
    tests = days[-holdout_days:]
    require(all(sum(t < d-timedelta(days=max(gaps)) for t in days) >= min_train_days
                for d in tests), 'INSUFFICIENT_PAST_DAYS')
    report = {
        'schema': 'hazrob.g2.mq4-day-conformal-rank-feasibility.v1',
        'raw_member_sha256': raw_member_sha256,
        'source_rows': sum(counts.values()),
        'source_days': [d.isoformat() for d in days],
        'rows_by_day': {d.isoformat(): counts[d] for d in days},
        'test_days': [d.isoformat() for d in tests],
        'holdout_days': holdout_days,
        'min_train_days': min_train_days,
        'gaps': list(gaps),
        'nominal_levels': list(nominal_levels),
        'minimum_exchangeable_calibration_groups_for_finite_threshold': required,
        'gap_results': {},
        'scientific_state': dict(STATE),
        'limitations': ('Calendar days are at most candidate groups, not verified independent '
                        'physical exposures or exchangeable calibration groups. This is only '
                        'the finite-rank feasibility bound for split conformal, not actual '
                        'intervals, observed coverage, a safety guarantee, or G2/G3 evidence.')
    }
    for gap in gaps:
        # Locked holdout cannot recycle earlier test days for later calibration.
        locked_past = [d for d in days if d < tests[0] - timedelta(days=gap)]
        locked_cal = len(locked_past) - min_train_days
        require(locked_cal >= 0, 'INSUFFICIENT_PAST_DAYS')
        locked = {
            'eligible_past_days_before_locked_test_block': len(locked_past),
            'maximum_calendar_calibration_days': locked_cal,
            'largest_finite_rank_fraction_if_exchangeable': locked_cal/(locked_cal+1) if locked_cal else 0.0,
            'finite_nominal_feasible': {k: locked_cal >= v for k, v in required.items()},
        }
        folds = {}
        for test in tests:
            past = [d for d in days if d < test - timedelta(days=gap)]
            require(len(past) >= min_train_days, 'INSUFFICIENT_PAST_DAYS')
            max_cal = len(past) - min_train_days
            finite_upper = max_cal/(max_cal+1) if max_cal else 0.0
            folds[test.isoformat()] = {
                'eligible_past_days': len(past),
                'minimum_train_days': min_train_days,
                'maximum_calendar_calibration_days': max_cal,
                'largest_finite_rank_fraction_if_exchangeable': finite_upper,
                'finite_nominal_feasible': {k: max_cal >= v for k, v in required.items()},
            }
        report['gap_results'][str(gap)] = {
            'locked_holdout_block_calibration_capacity': locked,
            'max_calendar_calibration_days_over_tests': max(
                f['maximum_calendar_calibration_days'] for f in folds.values()),
            'all_test_days_finite_nominal_feasible': {
                k: all(f['finite_nominal_feasible'][k] for f in folds.values())
                for k in required},
            'folds': folds,
        }
    return report


def analyze_zip(archive, expected_archive_sha=ARCHIVE_SHA256,
                expected_member_sha=MEMBER_SHA256, **kwargs):
    require(hashlib.sha256(archive).hexdigest() == expected_archive_sha, 'ARCHIVE_SHA256')
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            require(z.namelist().count(MEMBER) == 1, 'ZIP_MEMBER')
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, OSError, RuntimeError) as exc:
        raise ValueError('MQ4_DAY_CONFORMAL_INVALID_ZIP') from exc
    return analyze(read_days(raw, expected_member_sha),
                   raw_member_sha256=hashlib.sha256(raw).hexdigest(), **kwargs)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--holdout-days', type=int, default=6)
    parser.add_argument('--min-train-days', type=int, default=3)
    parser.add_argument('--gaps', type=int, nargs='+', default=[0, 1, 2])
    args = parser.parse_args()
    report = analyze_zip(args.archive.read_bytes(), holdout_days=args.holdout_days,
                         min_train_days=args.min_train_days, gaps=args.gaps)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    print('Day-group conformal rank feasibility evaluated; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
