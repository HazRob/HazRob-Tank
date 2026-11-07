#!/usr/bin/env python3
"""Research-only MQ4 strict chronological holdout audit of SHA-pinned source outputs.

The original benchmark's rotating leave-day-out scheme has ten future-fit folds.
This audit is descriptive; it neither certifies Tank sensors nor authorizes G2/G3.
"""
import argparse
import csv
import hashlib
import json
import math
from collections import defaultdict
from datetime import date, timedelta
from pathlib import Path

PINS = {
    'folds': '04d9974d207ff160e8e1bf8f8108a3665fff347de02c3763da96c05f26f72f15',
    'predictions': 'f6707daa79f17c674dfb36c7051420acf67543e854137aa724225228b0719506',
    'source_archive': '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9',
}
NAMES = ['TRAIN_MEDIAN_REFERENCE', 'RIDGE_MQ4_1', 'RIDGE_MQ4_3',
         'RIDGE_MQ4_3_T_RH', 'HGB_MQ4_3_T_RH']
DAYS = [(date(2021, 7, 21) + timedelta(days=i)).isoformat() for i in range(11)]


def chronological_fold(days, test_day, cal_day, n_train_days):
    """Derive eligibility from actual declared train/cal/test days, never pooled counts."""
    if len(days) != len(set(days)) or sorted(days) != list(days) or test_day not in days or cal_day not in days or cal_day == test_day:
        raise ValueError('BAD_FOLD_DAYS')
    train = [d for d in days if d not in (test_day, cal_day)]
    if len(train) != n_train_days:
        raise ValueError('TRAIN_DAY_COUNT')
    return {
        'test_day': test_day,
        'calibration_day': cal_day,
        'future_training_days': sum(d > test_day for d in train),
        'future_calibration': cal_day > test_day,
        'strictly_chronological': all(d < test_day for d in train) and cal_day < test_day,
    }


def evaluate_day(rows):
    """Evaluate 4-ppm source diagnostic with row-level truth and predictions."""
    if not rows:
        raise ValueError('EMPTY_DAY')
    ids = set()
    abs_errors = []
    tp = fn = fp = tn = covered = 0
    for row in rows:
        key = row['source_row_id']
        if key in ids:
            raise ValueError('DUPLICATE_ROW')
        ids.add(key)
        truth = float(row['reference_ch4_ppm'])
        pred = float(row['estimated_ch4_ppm_research'])
        width = float(row['calibration_halfwidth_ppm'])
        if not all(map(math.isfinite, (truth, pred, width))) or truth <= 0 or width < 0:
            raise ValueError('INVALID_MEASUREMENT')
        error = abs(truth - pred)
        abs_errors.append(error)
        pos = truth >= 4.0
        guess = pred >= 4.0
        tp += int(pos and guess)
        fn += int(pos and not guess)
        fp += int(not pos and guess)
        tn += int(not pos and not guess)
        covered += int(error <= width)
        if row['above_4ppm_reference'] != str(pos) or row['above_4ppm_research_estimate'] != str(guess):
            raise ValueError('ROW_LABEL_MISMATCH')
    n = len(rows)
    positives = tp + fn
    negatives = tn + fp
    return {
        'rows': n,
        'reference_positive_4ppm': positives,
        'reference_negative_4ppm': negatives,
        'tp': tp, 'fn': fn, 'fp': fp, 'tn': tn,
        'mae_ppm': round(sum(abs_errors) / n, 6),
        'interval_coverage': round(covered / n, 6),
        'specificity': round(tn / negatives, 6) if negatives else None,
        'sensitivity': round(tp / positives, 6) if positives else None,
        'sensitivity_estimable': bool(positives),
    }


def derive(folds_path, predictions_path):
    for name, path in [('folds', folds_path), ('predictions', predictions_path)]:
        if hashlib.sha256(Path(path).read_bytes()).hexdigest() != PINS[name]:
            raise ValueError('PINNED_OUTPUT_SHA_' + name)
    with open(folds_path, newline='', encoding='utf-8') as f:
        folds = list(csv.DictReader(f))
    if len(folds) != 55:
        raise ValueError('FOLD_COUNT')
    per_day = defaultdict(list)
    for fold in folds:
        if fold['model'] not in NAMES or fold['test_day'] not in DAYS:
            raise ValueError('UNKNOWN_MODEL_OR_DAY')
        per_day[fold['test_day']].append(fold)
    if sorted(per_day) != DAYS or any(sorted(x['model'] for x in v) != sorted(NAMES) for v in per_day.values()):
        raise ValueError('FOLD_COVERAGE')
    plans = []
    for idx, day in enumerate(DAYS):
        cal_day = DAYS[(idx - 1) % len(DAYS)]
        plan = chronological_fold(DAYS, day, cal_day, 9)
        for fold in per_day[day]:
            if fold['calibration_day'] != cal_day or int(fold['n_train']) + int(fold['n_cal']) + int(fold['n_test']) != 14186:
                raise ValueError('FOLD_PROTOCOL')
        plans.append(plan)
    eligible = [p['test_day'] for p in plans if p['strictly_chronological']]
    if eligible != ['2021-07-31']:
        raise ValueError('CHRONOLOGICAL_ELIGIBILITY')
    selected = defaultdict(list)
    total_rows = 0
    with open(predictions_path, newline='', encoding='utf-8') as f:
        for row in csv.DictReader(f):
            total_rows += 1
            if row['test_day'] == eligible[0]:
                selected[row['model']].append(row)
    if total_rows != 70930 or set(selected) != set(NAMES):
        raise ValueError('PREDICTION_SCOPE')
    models = []
    for name in NAMES:
        fold = next(f for f in per_day[eligible[0]] if f['model'] == name)
        score = evaluate_day(selected[name])
        if score['rows'] != int(fold['n_test']) or score['rows'] != 723:
            raise ValueError('FOLD_ROW_COUNT')
        if any(score[k] != int(fold[col]) for k, col in [('tp', 'tp4'), ('fn', 'fn4'), ('fp', 'fp4'), ('tn', 'tn4')]):
            raise ValueError('FOLD_CONFUSION_MISMATCH')
        if abs(score['mae_ppm'] - float(fold['mae_ppm'])) > 0.000001:
            raise ValueError('FOLD_MAE_MISMATCH')
        models.append({'model': name, **score})
    if any(m['reference_positive_4ppm'] != 0 for m in models):
        raise ValueError('UNEXPECTED_POSITIVE_SUPPORT')
    return {
        'schema': 'hazrob.g2.mq4-strict-chronological-source-holdout.v1',
        'source_archive_sha256': PINS['source_archive'],
        'source_fold_csv_sha256': PINS['folds'],
        'source_prediction_csv_sha256': PINS['predictions'],
        'original_source_rows': 14186,
        'historical_test_days': 11,
        'historical_folds_with_future_training': sum(p['future_training_days'] > 0 for p in plans),
        'historical_folds_with_future_calibration': sum(p['future_calibration'] for p in plans),
        'strictly_chronological_test_days': eligible,
        'chronological_test_day': eligible[0],
        'chronological_calibration_day': '2021-07-30',
        'chronological_train_days': DAYS[:9],
        'chronological_test_rows': 723,
        'chronological_positive_reference_rows': 0,
        'chronological_detection_sensitivity_estimable': False,
        'threshold_ppm': 4,
        'threshold_is_occupational_safety_limit': False,
        'models': models,
        'source_only_not_tank': True,
        'independent_physical_exposures_verified': False,
        'prospective_multi_day_generalization_verified': False,
        'g2_qualified': False,
        'g3_qualified': False,
        'limitations': 'Single last-day temporally eligible source fold contains zero >=4ppm positives; cannot estimate sensitivity. Prior folds use future training/calibration. Correlated minute rows are not independent; no Tank validation.'
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--folds', type=Path, required=True)
    p.add_argument('--predictions', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    result = derive(a.folds, a.predictions)
    a.out.write_text(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True) + '\n', encoding='utf-8')
    print('PASS pinned source chronological audit: 1/11 eligible day, 0/723 positive references; G2 blocked')

if __name__ == '__main__':
    main()
