"""Causal forward-calendar-day MQ4 observational stress probe (research only).

Train exclusively on past calendar days; never on test/future days. All gaps
share the same evaluation days to avoid changing the test population.
Original reference ZIP remains external to the repository. Not G2/G3 evidence.
"""
import argparse
import hashlib
import io
import json
import math
import statistics
import zipfile
from datetime import timedelta
from pathlib import Path

from g2_mq4_neighbor_day_embargo_probe import parse_source
from g2_mq4_grouped_numeric_probe import ARCHIVE_SHA256, MEMBER_SHA256, MEMBER, CHANNELS


def require(condition, code):
    if not condition:
        raise ValueError('MQ4_FORWARD_CHAIN_' + code)


def evaluate_grouped(groups, gaps=(0, 1, 2), min_train_days=3, raw_sha256=None):
    gaps = tuple(gaps)
    require(bool(gaps) and len(gaps) == len(set(gaps))
            and all(type(g) is int and 0 <= g <= 30 for g in gaps), 'GAPS')
    require(type(min_train_days) is int and min_train_days >= 2, 'MIN_TRAIN_DAYS')
    require(bool(groups) and all(len(rows) >= 3 for rows in groups.values()), 'GROUPS')
    days = sorted(groups)
    max_gap = max(gaps)
    common_test_days = [test for test in days if sum(d < test - timedelta(days=max_gap)
                          for d in days) >= min_train_days]
    require(len(common_test_days) >= 2, 'INSUFFICIENT_FORWARD_FOLDS')
    report = {
        'schema': 'hazrob.g2.mq4-forward-calendar-day.v1',
        'raw_member_sha256': raw_sha256,
        'rows': sum(len(rows) for rows in groups.values()),
        'days': [d.isoformat() for d in days],
        'common_test_days': [d.isoformat() for d in common_test_days],
        'gaps': list(gaps), 'min_train_days': min_train_days,
        'method': 'expanding past-calendar-day OLS; calendar-day embargo; same test days for all gaps; training-only mean baseline',
        'channels': {},
        'scientific_state': {'g2_qualified': False, 'g3_tank_qualified': False,
                             'physical_independence_verified': False,
                             'reference_calibration_verified': False,
                             'blinded_holdout_verified': False},
        'limitations': 'Observational, source-reported methane ppb and raw MQ4 channels; no certified independent physical exposures, blinded holdout, Tank transfer or gas identity calibration.'
    }
    for col, channel in enumerate(CHANNELS, start=1):
        gap_results = {}
        for gap in gaps:
            folds = {}
            sum_model = sum_baseline = 0.0
            total_test = 0
            worse_days = 0
            for test_day in common_test_days:
                train_days = [d for d in days if d < test_day - timedelta(days=gap)]
                require(len(train_days) >= min_train_days, 'INSUFFICIENT_TRAIN_DAYS')
                train = [row for day in train_days for row in groups[day]]
                test = groups[test_day]
                require(len(train) >= 3, 'INSUFFICIENT_TRAIN_ROWS')
                mx = statistics.mean(row[col] for row in train)
                my = statistics.mean(row[0] for row in train)
                xx = sum((row[col] - mx)**2 for row in train)
                xy = sum((row[col] - mx)*(row[0] - my) for row in train)
                slope = xy / xx if xx > 0 else 0.0
                require(math.isfinite(slope), 'NONFINITE_SLOPE')
                sse_model = sum((row[0] - (my + slope*(row[col] - mx)))**2 for row in test)
                sse_baseline = sum((row[0] - my)**2 for row in test)
                require(math.isfinite(sse_model) and math.isfinite(sse_baseline), 'NONFINITE_ERROR')
                sum_model += sse_model
                sum_baseline += sse_baseline
                total_test += len(test)
                worse_days += sse_model > sse_baseline
                folds[test_day.isoformat()] = {
                    'train_days': [d.isoformat() for d in train_days],
                    'train_rows': len(train), 'test_rows': len(test),
                    'rmse_model_ppb': math.sqrt(sse_model/len(test)),
                    'rmse_train_mean_baseline_ppb': math.sqrt(sse_baseline/len(test)),
                    'slope_ppb_per_raw_channel_unit': slope
                }
            gap_results[str(gap)] = {
                'pooled_rmse_model_ppb': math.sqrt(sum_model/total_test),
                'pooled_rmse_baseline_ppb': math.sqrt(sum_baseline/total_test),
                'pooled_skill_vs_train_mean': 1 - sum_model/sum_baseline if sum_baseline > 0 else None,
                'days_worse_than_training_mean': worse_days,
                'total_test_rows': total_test,
                'folds': folds
            }
        report['channels'][channel] = gap_results
    return report


def evaluate(raw, gaps=(0, 1, 2), min_train_days=3, expected_member_sha=None):
    groups = parse_source(raw, expected_member_sha=expected_member_sha)
    return evaluate_grouped(groups, gaps, min_train_days, hashlib.sha256(raw).hexdigest())


def evaluate_zip(archive, expected_archive_sha=ARCHIVE_SHA256,
                 expected_member_sha=MEMBER_SHA256, gaps=(0, 1, 2), min_train_days=3):
    require(hashlib.sha256(archive).hexdigest() == expected_archive_sha, 'ARCHIVE_SHA256')
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            require(z.namelist().count(MEMBER) == 1, 'ZIP_MEMBER')
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, OSError, RuntimeError) as exc:
        raise ValueError('MQ4_FORWARD_CHAIN_INVALID_ZIP') from exc
    return evaluate(raw, gaps, min_train_days, expected_member_sha)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--archive', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--gaps', type=int, nargs='+', default=[0, 1, 2])
    parser.add_argument('--min-train-days', type=int, default=3)
    args = parser.parse_args()
    result = evaluate_zip(args.archive.read_bytes(), gaps=args.gaps, min_train_days=args.min_train_days)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False)+'\n', encoding='utf-8')
    print('MQ4 forward-chaining observational probe complete; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
