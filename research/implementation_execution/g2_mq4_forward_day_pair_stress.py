"""Descriptive leave-two-calendar-days-out MQ4 stress test; never Tank G2/G3 evidence."""
import argparse
import itertools
import json
from pathlib import Path
from g2_mq4_forward_day_stability import analyze, DAYS, MODELS, rates

SCHEMA = 'hazrob.g2.mq4-forward-day-pair-stress.v1'

def require(condition, code):
    if not condition:
        raise ValueError('MQ4_DAY_PAIR_STRESS_' + code)

def build(source, pinned_stability):
    # Full structural equality, not just selected fields: fail closed on a modified receipt.
    expected = analyze(source)
    require(pinned_stability == expected, 'PINNED_STABILITY_MISMATCH')
    require(pinned_stability['calendar_days'] == DAYS and
            set(pinned_stability['models']) == MODELS, 'DAY_MODEL_IDENTITY')
    result = {
        'schema': SCHEMA,
        'source_zip_sha256': pinned_stability['source_zip_sha256'],
        'source_csv_sha256': pinned_stability['source_csv_sha256'],
        'calendar_days': DAYS,
        'excluded_calendar_days_per_stress': 2,
        'remaining_calendar_days_per_stress': 5,
        'combinations_per_model': 21,
        'models': {},
        'g2_qualified': False,
        'g3_qualified': False,
        'independent_tank_exposures': False,
        'blinded_prospective': False,
        'traceable_calibration': False,
        'limitations': ('Descriptive leave-two-calendar-days-out sensitivity analysis on a '
                        'pinned external observational MQ4 forward-only receipt. These '
                        'overlapping subsets share data and training histories; they are '
                        'not independent validation folds, confidence intervals, '
                        'Tank measurements, or a safety-alarm assessment.')
    }
    for name in sorted(MODELS):
        rows = pinned_stability['models'][name]['days']
        require(len(rows) == 7 and [x['day'] for x in rows] == DAYS, 'DAY_ORDER')
        subsets = []
        for excluded in itertools.combinations(range(len(DAYS)), 2):
            kept = [x for i, x in enumerate(rows) if i not in excluded]
            n = sum(x['n'] for x in kept)
            pos = sum(x['pos'] for x in kept)
            fn = sum(x['fn'] for x in kept)
            fp = sum(x['fp'] for x in kept)
            neg = n - pos
            require(n > 0 and pos > 0 and neg > 0 and
                    0 <= fn <= pos and 0 <= fp <= neg, 'SUBSET_SUPPORT')
            metrics = rates({'n': n, 'pos': pos, 'tp': pos-fn,
                             'fn': fn, 'fp': fp, 'tn': neg-fp})
            subsets.append({
                'excluded_days': [DAYS[i] for i in excluded],
                'remaining_rows': n,
                'remaining_reference_positives': pos,
                'remaining_reference_negatives': neg,
                'false_negatives': fn,
                'false_positives': fp,
                **metrics
            })
        require(len(subsets) == 21, 'COMBINATION_COUNT')
        worst_sens = min(subsets, key=lambda x: (x['sensitivity'], x['excluded_days']))
        worst_spec = min(subsets, key=lambda x: (x['specificity'], x['excluded_days']))
        best_sens = max(x['sensitivity'] for x in subsets)
        best_spec = max(x['specificity'] for x in subsets)
        result['models'][name] = {
            'sensitivity_range': [worst_sens['sensitivity'], best_sens],
            'specificity_range': [worst_spec['specificity'], best_spec],
            'worst_sensitivity_excluded_days': worst_sens['excluded_days'],
            'worst_specificity_excluded_days': worst_spec['excluded_days'],
            'subsets': subsets
        }
    return result

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--source-receipt', required=True, type=Path)
    p.add_argument('--stability-receipt', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    result = build(json.loads(a.source_receipt.read_text(encoding='utf-8')),
                   json.loads(a.stability_receipt.read_text(encoding='utf-8')))
    a.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True,
                                   separators=(',', ':'), allow_nan=False) + '\n',
                        encoding='utf-8')
    print('MQ4 two-day stress PASS; external source only; G2/G3 NOT QUALIFIED')

if __name__ == '__main__':
    main()
