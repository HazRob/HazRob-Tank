"""External-source MQ4 forward-day support strata; NOT Tank G2/G3 qualification."""
import argparse
import json
from pathlib import Path
from g2_mq4_forward_day_stability import analyze
from g2_mq4_forward_day_ranking import build as build_ranking

SCHEMA = 'hazrob.g2.mq4-forward-day-support-strata.v1'
BASELINE = 'TRAIN_MEDIAN_REFERENCE'

def require(ok, code):
    if not ok:
        raise ValueError('MQ4_FORWARD_SUPPORT_STRATA_' + code)

def build(source, stability, ranking):
    require(analyze(source) == stability, 'PINNED_STABILITY_MISMATCH')
    require(build_ranking(source, stability) == ranking, 'PINNED_RANKING_MISMATCH')
    models, days = ranking['models'], ranking['daily']
    require(BASELINE in models and len(models) == 5 and len(days) == 7, 'SCOPE')
    groups = {
        'days_with_reference_positives': [d for d in days if d['reference_positives'] > 0],
        'days_without_reference_positives': [d for d in days if d['reference_positives'] == 0],
    }
    require([len(v) for v in groups.values()] == [5, 2], 'SUPPORT')
    strata = {}
    for label, selected in groups.items():
        mae = {name: round(sum(d['mae_by_model'][name] for d in selected) /
                           len(selected), 12) for name in models}
        ranks = {name: sum(d['mae_ascending'].index(name) + 1 for d in selected)
                 for name in models}
        strata[label] = {
            'calendar_days': [d['day'] for d in selected],
            'days': len(selected),
            'rows': sum(d['rows'] for d in selected),
            'reference_positives': sum(d['reference_positives'] for d in selected),
            'equal_day_weight_macro_mae_ppm': mae,
            'equal_day_weight_mae_ascending': sorted(models, key=lambda n: (mae[n], n)),
            'day_rank_sum': ranks,
            'day_rank_sum_ascending': sorted(models, key=lambda n: (ranks[n], n)),
        }
    comparisons = {}
    for name in models:
        if name == BASELINE:
            continue
        better = [d for d in days if d['mae_by_model'][name] < d['mae_by_model'][BASELINE]]
        worse = [d for d in days if d['mae_by_model'][name] > d['mae_by_model'][BASELINE]]
        tied = [d for d in days if d['mae_by_model'][name] == d['mae_by_model'][BASELINE]]
        zero_better = sum(d['reference_positives'] == 0 for d in better)
        comparisons[name] = {
            'days_better_than_baseline': len(better),
            'days_worse_than_baseline': len(worse),
            'days_tied_with_baseline': len(tied),
            'positive_support_days_better': sum(d['reference_positives'] > 0 for d in better),
            'zero_positive_support_days_better': zero_better,
            'better_on_all_zero_positive_days': zero_better == 2,
            'better_on_all_seven_days': len(better) == 7,
        }
        require(len(better) + len(worse) + len(tied) == 7, 'PAIR_ACCOUNTING')
    require(sum(s['rows'] for s in strata.values()) == stability['rows'] and
            sum(s['reference_positives'] for s in strata.values()) ==
            stability['reference_positives'], 'STRATA_ACCOUNTING')
    return {
        'schema': SCHEMA, 'source_zip_sha256': stability['source_zip_sha256'],
        'source_csv_sha256': stability['source_csv_sha256'],
        'source_receipt_schema': source['schema'],
        'baseline': BASELINE, 'calendar_days': [d['day'] for d in days],
        'strata': strata, 'paired_calendar_day_mae_vs_baseline': comparisons,
        'g2_qualified': False, 'g3_qualified': False,
        'independent_tank_exposures': False, 'blinded_prospective': False,
        'traceable_calibration': False,
        'limitations': ('External observational MQ4 days only; grouping is by whether a '
                        'calendar day contains any >=4 ppm reference rows, NOT a '
                        'row-level class-stratified MAE. The 4-ppm cut is a study '
                        'classification threshold, not a safety limit. Days share '
                        'training history and repeated measurements; no independence, '
                        'significance tests, intervals, Tank performance, alarm '
                        'fitness, or G2/G3 qualification is established.'),
    }

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--source-receipt', required=True, type=Path)
    p.add_argument('--stability-receipt', required=True, type=Path)
    p.add_argument('--ranking-receipt', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    result = build(json.loads(a.source_receipt.read_text(encoding='utf-8')),
                   json.loads(a.stability_receipt.read_text(encoding='utf-8')),
                   json.loads(a.ranking_receipt.read_text(encoding='utf-8')))
    a.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True,
                                   separators=(',', ':'), allow_nan=False) + '\n',
                        encoding='utf-8')
    print('MQ4 support-stratified source audit PASS; G2/G3 NOT QUALIFIED')

if __name__ == '__main__':
    main()
