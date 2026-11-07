"""Research-only MQ4 three-channel ridge vs single channels and day-rotation placebo.

Past-only day folds with fixed ridge penalty, never tuned on held-out labels.
Day-wise half-rotation of reference labels is a descriptive negative control, NOT
an independent randomized experiment or proof of a valid sensor calibration.
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
from datetime import datetime, timedelta
from pathlib import Path

ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
MEMBER_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
CHANNELS = ('MQ4_1', 'MQ4_2', 'MQ4_3')
MODELS = ('ridge3', *CHANNELS, 'training_mean', 'day_rotation_placebo_ridge3')
STATE = {k: False for k in ('g2_qualified', 'g3_tank_qualified',
                            'physical_independence_verified',
                            'reference_calibration_verified', 'blinded_holdout_verified')}


def require(ok, code):
    if not ok:
        raise ValueError('MQ4_MULTI_FORWARD_' + code)


def parse(raw, expected_sha=None):
    if expected_sha is not None:
        require(hashlib.sha256(raw).hexdigest() == expected_sha, 'MEMBER_SHA256')
    try:
        reader = csv.DictReader(io.StringIO(raw.decode('utf-8-sig'), newline=''), strict=True)
        required = ('datetime', 'licor_ch4', *CHANNELS)
        require(reader.fieldnames is not None and len(set(reader.fieldnames)) == len(reader.fieldnames)
                and all(c in reader.fieldnames for c in required), 'HEADER')
        grouped = defaultdict(list)
        prev = None
        for row in reader:
            require(None not in row and all(v is not None for v in row.values()), 'ROW_SHAPE')
            ts = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
            require(ts.second == 0 and (prev is None or ts > prev), 'TIME_ORDER')
            prev = ts
            values = tuple(float(row[c]) for c in ('licor_ch4', *CHANNELS))
            require(all(math.isfinite(v) for v in values), 'NONFINITE')
            grouped[ts.date()].append(values)
    except (UnicodeError, csv.Error, ValueError, TypeError, OverflowError, KeyError) as exc:
        if isinstance(exc, ValueError) and str(exc).startswith('MQ4_MULTI_FORWARD_'):
            raise
        raise ValueError('MQ4_MULTI_FORWARD_INVALID_CSV') from exc
    require(len(grouped) >= 5 and all(len(v) >= 3 for v in grouped.values()), 'GROUPS')
    return dict(grouped)


def solve3(a, b):
    """Deterministic pivoted 3x3 solve; strictly positive ridge ensures invertibility."""
    m = [list(row) + [v] for row, v in zip(a, b)]
    for k in range(3):
        p = max(range(k, 3), key=lambda i: abs(m[i][k]))
        require(math.isfinite(m[p][k]) and abs(m[p][k]) > 1e-14, 'SINGULAR')
        m[k], m[p] = m[p], m[k]
        for i in range(k+1, 3):
            f = m[i][k] / m[k][k]
            for j in range(k+1, 4):
                m[i][j] -= f * m[k][j]
            m[i][k] = 0.0
    out = [0.0]*3
    for i in (2, 1, 0):
        out[i] = (m[i][3] - sum(m[i][j]*out[j] for j in range(i+1, 3))) / m[i][i]
    require(all(math.isfinite(v) for v in out), 'COEFFICIENTS')
    return out


def fit(train, alpha):
    """Training-only standardized ridge (penalty alpha times mean squared coefficients)."""
    require(len(train) >= 3 and type(alpha) in (int, float) and
            math.isfinite(alpha) and 0 < alpha <= 100, 'ALPHA')
    n = len(train)
    my = statistics.mean(row[0] for row in train)
    means = [statistics.mean(row[j] for row in train) for j in range(1, 4)]
    scales = [math.sqrt(sum((r[j]-means[j-1])**2 for r in train)/n)
              for j in range(1, 4)]
    scales = [s if s > 0 else 1.0 for s in scales]
    z = [tuple((r[j]-means[j-1])/scales[j-1] for j in range(1, 4)) for r in train]
    a = [[sum(v[i]*v[j] for v in z)/n + (alpha if i == j else 0)
          for j in range(3)] for i in range(3)]
    b = [sum(v[j]*(r[0]-my) for v, r in zip(z, train))/n for j in range(3)]
    coef = solve3(a, b)
    single = []
    for j in range(1, 4):
        sxx = sum((r[j]-means[j-1])**2 for r in train)
        sxy = sum((r[j]-means[j-1])*(r[0]-my) for r in train)
        single.append(sxy/sxx if sxx > 0 else 0.0)
    return my, means, scales, coef, single


def predict(model, row):
    my, means, scales, coef, single = model
    ridge = my + sum(coef[j]*(row[j+1]-means[j])/scales[j] for j in range(3))
    singles = [my + single[j]*(row[j+1]-means[j]) for j in range(3)]
    return (ridge, *singles, my)


def rotated(groups):
    """Half-day cyclic label shift within each day; preserves day-level label distribution."""
    return {day: [((rows[(i+len(rows)//2) % len(rows)][0],) + row[1:])
                  for i, row in enumerate(rows)] for day, rows in groups.items()}


def evaluate_groups(groups, gaps=(0, 1, 2), min_train_days=3, alpha=0.1,
                    raw_member_sha256=None):
    require(bool(groups) and all(len(v) >= 3 for v in groups.values()), 'GROUPS')
    require(type(min_train_days) is int and min_train_days >= 2, 'MIN_TRAIN_DAYS')
    require(type(alpha) in (int, float) and math.isfinite(alpha) and 0 < alpha <= 100, 'ALPHA')
    gaps = tuple(gaps)
    require(bool(gaps) and len(set(gaps)) == len(gaps) and
            all(type(g) is int and 0 <= g <= 30 for g in gaps), 'GAPS')
    days = sorted(groups)
    common = [d for d in days if sum(t < d-timedelta(days=max(gaps)) for t in days) >= min_train_days]
    require(len(common) >= 2, 'INSUFFICIENT_FOLDS')
    shifted = rotated(groups)
    result = {'schema': 'hazrob.g2.mq4-multichannel-forward-control.v1',
              'raw_member_sha256': raw_member_sha256,
              'rows': sum(len(v) for v in groups.values()),
              'days': [d.isoformat() for d in days],
              'common_test_days': [d.isoformat() for d in common],
              'gaps': list(gaps), 'min_train_days': min_train_days,
              'ridge_alpha': float(alpha), 'models': list(MODELS),
              'method': 'past-only day folds; fixed training-standardized ridge; 3 univariate OLS; training mean; half-day label-rotation placebo',
              'scientific_state': dict(STATE), 'gaps_results': {},
              'limitations': 'Observational source MQ4 data, not independent gas exposures. Placebo preserves day-level labels and may preserve confounding; no blind group holdout, physical reference calibration, Tank transfer or certified ppm.'}
    for gap in gaps:
        folds = {}
        totals = {m: 0.0 for m in MODELS}
        n_total = 0
        for d in common:
            train_days = [t for t in days if t < d-timedelta(days=gap)]
            require(len(train_days) >= min_train_days, 'TRAIN_DAYS')
            train = [r for t in train_days for r in groups[t]]
            null_train = [r for t in train_days for r in shifted[t]]
            test, null_test = groups[d], shifted[d]
            model, null_model = fit(train, alpha), fit(null_train, alpha)
            errors = {m: 0.0 for m in MODELS}
            for row, shifted_row in zip(test, null_test):
                p = predict(model, row)
                for m, predicted in zip(MODELS[:5], p):
                    errors[m] += (row[0]-predicted)**2
                null_p = predict(null_model, shifted_row)[0]
                errors['day_rotation_placebo_ridge3'] += (shifted_row[0]-null_p)**2
            require(all(math.isfinite(v) and v >= 0 for v in errors.values()), 'NONFINITE_ERROR')
            for m in MODELS:
                totals[m] += errors[m]
            n_total += len(test)
            folds[d.isoformat()] = {
                'train_days': [t.isoformat() for t in train_days],
                'train_rows': len(train), 'test_rows': len(test),
                'rmse_ppb': {m: math.sqrt(errors[m]/len(test)) for m in MODELS},
                'ridge_standardized_coefficients': model[3],
                'day_rotation_placebo_ridge_standardized_coefficients': null_model[3]}
        baseline = totals['training_mean']
        ridge = totals['ridge3']
        null = totals['day_rotation_placebo_ridge3']
        day_skills = [1 - f['rmse_ppb']['ridge3']**2/f['rmse_ppb']['training_mean']**2
                      for f in folds.values() if f['rmse_ppb']['training_mean'] > 0]
        result['gaps_results'][str(gap)] = {
            'total_test_rows': n_total, 'calendar_test_days': len(common),
            'pooled_rmse_ppb': {m: math.sqrt(totals[m]/n_total) for m in MODELS},
            'ridge_skill_vs_training_mean': 1-ridge/baseline if baseline > 0 else None,
            'placebo_skill_vs_training_mean': 1-null/baseline if baseline > 0 else None,
            'ridge_skill_vs_MQ4_1': 1-ridge/totals['MQ4_1'] if totals['MQ4_1'] > 0 else None,
            'mean_defined_day_ridge_skill': statistics.mean(day_skills) if day_skills else None,
            'days_ridge_worse_than_training_mean': sum(f['rmse_ppb']['ridge3'] > f['rmse_ppb']['training_mean'] for f in folds.values()),
            'days_ridge_worse_than_MQ4_1': sum(f['rmse_ppb']['ridge3'] > f['rmse_ppb']['MQ4_1'] for f in folds.values()),
            'days_ridge_beats_placebo': sum(f['rmse_ppb']['ridge3'] < f['rmse_ppb']['day_rotation_placebo_ridge3'] for f in folds.values()),
            'folds': folds}
    return result


def evaluate(raw, gaps=(0, 1, 2), min_train_days=3, alpha=0.1, expected_member_sha=None):
    groups = parse(raw, expected_member_sha)
    return evaluate_groups(groups, gaps, min_train_days, alpha, hashlib.sha256(raw).hexdigest())


def evaluate_zip(archive, expected_archive_sha=ARCHIVE_SHA256,
                 expected_member_sha=MEMBER_SHA256, gaps=(0, 1, 2),
                 min_train_days=3, alpha=0.1):
    require(hashlib.sha256(archive).hexdigest() == expected_archive_sha, 'ARCHIVE_SHA256')
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            require(z.namelist().count(MEMBER) == 1, 'ZIP_MEMBER')
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, OSError, RuntimeError) as exc:
        raise ValueError('MQ4_MULTI_FORWARD_INVALID_ZIP') from exc
    return evaluate(raw, gaps, min_train_days, alpha, expected_member_sha)


def compact(report):
    out = json.loads(json.dumps(report))
    for gap in out['gaps_results'].values():
        gap.pop('folds')
    out['artifact_kind'] = 'source-derived observational benchmark summary; full folds reproducible from SHA256-pinned ZIP'
    return out


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--archive', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--gaps', type=int, nargs='+', default=[0, 1, 2])
    p.add_argument('--min-train-days', type=int, default=3)
    p.add_argument('--alpha', type=float, default=0.1)
    p.add_argument('--compact', action='store_true')
    args = p.parse_args()
    result = evaluate_zip(args.archive.read_bytes(), gaps=args.gaps,
                          min_train_days=args.min_train_days, alpha=args.alpha)
    args.output.write_text(json.dumps(compact(result) if args.compact else result,
                                      sort_keys=True, indent=2) + '\n', encoding='utf-8')
    print('MQ4 multichannel forward/placebo observational benchmark; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
