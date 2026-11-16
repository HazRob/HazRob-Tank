"""Descriptive MQ4 forward-only day-vs-pooled MAE ranking audit; NOT Tank G2/G3."""
import argparse
import itertools
import json
import math
from pathlib import Path
from g2_mq4_forward_day_stability import analyze, DAYS, MODELS

SCHEMA = 'hazrob.g2.mq4-forward-day-ranking.v1'

def require(ok, code):
    if not ok:
        raise ValueError('MQ4_FORWARD_DAY_RANKING_' + code)

def build(source, pinned_stability):
    require(analyze(source) == pinned_stability, 'PINNED_STABILITY_MISMATCH')
    names = sorted(MODELS)
    require(set(source['models']) == set(names), 'MODEL_IDENTITY')
    pooled_mae = {name: source['models'][name]['aggregate']['mae'] for name in names}
    require(all(type(x) in (int, float) and math.isfinite(x) and x >= 0
                for x in pooled_mae.values()), 'POOLED_MAE')
    pooled_order = sorted(names, key=lambda name: (pooled_mae[name], name))
    day_wins = dict.fromkeys(names, 0)
    daily = []
    pairwise = {f'{a}__vs__{b}': {'pooled_better': a if pooled_mae[a] < pooled_mae[b] else b,
                                 'agree_days': 0, 'reverse_days': 0, 'tied_days': 0}
                for a, b in itertools.combinations(names, 2)
                if pooled_mae[a] != pooled_mae[b]}
    require(len(pairwise) == 10, 'POOLED_TIES_UNSUPPORTED')
    for i, day in enumerate(DAYS):
        values = {name: source['models'][name]['days'][i]['mae'] for name in names}
        require(all(type(x) in (int, float) and math.isfinite(x) and x >= 0
                    for x in values.values()), 'DAY_MAE')
        order = sorted(names, key=lambda name: (values[name], name))
        day_wins[order[0]] += 1
        for a, b in itertools.combinations(names, 2):
            p = pairwise[f'{a}__vs__{b}']
            if values[a] == values[b]:
                p['tied_days'] += 1
            elif (values[a] < values[b]) == (p['pooled_better'] == a):
                p['agree_days'] += 1
            else:
                p['reverse_days'] += 1
        daily.append({'day': day, 'rows': source['test_rows_by_day'][day],
                      'reference_positives': source['positive_by_test_day'][day],
                      'mae_ascending': order, 'mae_by_model': values})
    require(sum(day_wins.values()) == 7 and
            all(sum(p[k] for k in ('agree_days', 'reverse_days', 'tied_days')) == 7
                for p in pairwise.values()), 'DAY_ACCOUNTING')
    return {'schema': SCHEMA, 'source_zip_sha256': pinned_stability['source_zip_sha256'],
            'source_csv_sha256': pinned_stability['source_csv_sha256'],
            'calendar_days': DAYS, 'models': names,
            'pooled_mae_ascending': pooled_order, 'pooled_mae_by_model': pooled_mae,
            'day_first_place_counts': day_wins, 'daily': daily,
            'pairwise_pooled_vs_day': pairwise,
            'pairwise_comparisons': 70,
            'pairwise_reversals': sum(p['reverse_days'] for p in pairwise.values()),
            'g2_qualified': False, 'g3_qualified': False,
            'independent_tank_exposures': False, 'blinded_prospective': False,
            'traceable_calibration': False,
            'limitations': ('MAE ranks describe seven calendar days of an external observational '
                            'MQ4 forward-only receipt. Shared training history and repeated readings '
                            'make days and pairwise comparisons non-independent. Ranking does not '
                            'establish gas identity, ppm calibration, Tank performance, alarm '
                            'fitness, confidence intervals, or G2/G3 qualification.')}

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--source-receipt', required=True, type=Path)
    p.add_argument('--stability-receipt', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    out = build(json.loads(a.source_receipt.read_text(encoding='utf-8')),
                json.loads(a.stability_receipt.read_text(encoding='utf-8')))
    a.output.write_text(json.dumps(out, sort_keys=True, ensure_ascii=False,
                                   separators=(',', ':'), allow_nan=False) + '\n',
                        encoding='utf-8')
    print('MQ4 day ranking PASS; external source only; G2/G3 NOT QUALIFIED')

if __name__ == '__main__':
    main()
