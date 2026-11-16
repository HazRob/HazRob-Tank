"""Descriptive MQ4 forward-only day stability; NOT Tank G2/G3 qualification."""
import argparse
import json
import math
from pathlib import Path

ZIP_SHA = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
CSV_SHA = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
DAYS = [f'2021-07-{n:02d}' for n in range(25, 32)]
MODELS = {'TRAIN_MEDIAN_REFERENCE', 'RIDGE_MQ4_1', 'RIDGE_MQ4_3',
          'RIDGE_MQ4_3_T_RH', 'HGB_MQ4_3_T_RH'}
NO_PROMOTION = ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
                'blinded_prospective', 'traceable_calibration',
                'threshold_4ppm_is_safety_limit')


def require(ok, code):
    if not ok:
        raise ValueError('MQ4_FORWARD_DAY_STABILITY_' + code)


def rate(num, den):
    return num / den if den else None


def counts(rows):
    return {k: sum(x[k] for x in rows) for k in ('n', 'pos', 'tp', 'fn', 'fp', 'tn')}


def rates(c):
    neg = c['n'] - c['pos']
    sens, spec = rate(c['tp'], c['pos']), rate(c['tn'], neg)
    return {'sensitivity': sens, 'specificity': spec,
            'false_positive_rate': rate(c['fp'], neg),
            'balanced_accuracy': (sens + spec)/2 if sens is not None and spec is not None else None}


def analyze(source):
    require(isinstance(source, dict) and source.get('schema') ==
            'hazrob.g2.mq4-forward-4ppm-source.v1', 'SCHEMA')
    require(source.get('zip_sha256') == ZIP_SHA and
            source.get('csv_sha256') == CSV_SHA, 'SOURCE_IDENTITY')
    require(source.get('source_rows') == 14186 and
            source.get('source_only_not_tank') is True and
            all(source.get(k) is False for k in NO_PROMOTION), 'SCIENCE_GUARD')
    require(source.get('test_days') == DAYS and
            set(source.get('models', {})) == MODELS, 'MODEL_DAYS')
    nday, pday = source.get('test_rows_by_day'), source.get('positive_by_test_day')
    require(isinstance(nday, dict) and isinstance(pday, dict) and
            set(nday) == set(pday) == set(DAYS), 'DAY_COUNTS')
    require(all(type(nday[d]) is int and type(pday[d]) is int and
                0 <= pday[d] <= nday[d] and nday[d] > 0 for d in DAYS), 'DAY_VALUES')
    require(sum(nday.values()) == 9325 and sum(pday.values()) == 1425, 'SOURCE_TOTALS')
    out = {'schema': 'hazrob.g2.mq4-forward-day-stability.v1',
           'source_zip_sha256': ZIP_SHA, 'source_csv_sha256': CSV_SHA,
           'source_receipt_schema': source['schema'], 'calendar_days': DAYS,
           'rows': sum(nday.values()), 'reference_positives': sum(pday.values()),
           'days_with_positives': sum(pday[d] > 0 for d in DAYS),
           'days_without_positives': [d for d in DAYS if pday[d] == 0],
           'models': {}, 'g2_qualified': False, 'g3_qualified': False,
           'independent_tank_exposures': False, 'blinded_prospective': False,
           'traceable_calibration': False,
           'limitations': 'External observational MQ4 source only. Calendar days share training history and may be dependent. No independent Tank exposure, reference calibration, blinded holdout, safety alarm qualification, or independent-cluster confidence claim.'}
    for name in sorted(MODELS):
        obj = source['models'][name]
        require(isinstance(obj, dict) and isinstance(obj.get('days'), list) and
                len(obj['days']) == len(DAYS) and isinstance(obj.get('aggregate'), dict),
                'MODEL_SCHEMA')
        rows = []
        for day, f in zip(DAYS, obj['days']):
            require(isinstance(f, dict) and f.get('day') == day and
                    all(type(f.get(k)) is int for k in ('pos', 'fn', 'fp')) and
                    type(f.get('mae')) in (float, int) and
                    math.isfinite(f['mae']) and f['mae'] >= 0, 'FOLD_SCHEMA')
            n, pos = nday[day], pday[day]
            require(f['pos'] == pos and 0 <= f['fn'] <= pos and
                    0 <= f['fp'] <= n-pos, 'FOLD_CONFUSION')
            rows.append({'day': day, 'n': n, 'pos': pos, 'tp': pos-f['fn'],
                         'fn': f['fn'], 'fp': f['fp'], 'tn': n-pos-f['fp'],
                         'mae': f['mae']})
        c, agg = counts(rows), obj['aggregate']
        require(all(type(agg.get(k)) is int and agg[k] == c[k] for k in c),
                'AGGREGATE_COUNTS')
        require(type(agg.get('mae')) in (float, int) and
                math.isfinite(agg['mae']) and math.isclose(
                    agg['mae'], sum(f['mae']*f['n'] for f in rows)/c['n'],
                    rel_tol=1e-11, abs_tol=1e-11), 'AGGREGATE_MAE')
        pooled = rates(c)
        require(all(type(agg.get(k)) in (float, int) and
                    math.isfinite(agg[k]) and math.isclose(agg[k], pooled[k],
                    rel_tol=1e-11, abs_tol=1e-11)
                    for k in ('sensitivity', 'specificity')), 'AGGREGATE_RATES')
        per_day = [{**{'day': f['day'], 'n': f['n'], 'pos': f['pos'],
                        'fn': f['fn'], 'fp': f['fp']}, **rates(f)} for f in rows]
        sensitivity_days = [x['sensitivity'] for x in per_day if x['sensitivity'] is not None]
        specificity_days = [x['specificity'] for x in per_day if x['specificity'] is not None]
        loo = []
        for f in rows:
            rem = {k: c[k]-f[k] for k in c}
            loo.append({'excluded_day': f['day'], **rates(rem)})
        out['models'][name] = {
            'pooled': pooled,
            'macro_sensitivity_defined_days': sum(sensitivity_days)/len(sensitivity_days),
            'macro_specificity_defined_days': sum(specificity_days)/len(specificity_days),
            'min_day_sensitivity': min(sensitivity_days),
            'max_day_false_positive_rate': max(x['false_positive_rate'] for x in per_day
                                                if x['false_positive_rate'] is not None),
            'days_sensitivity_undefined': [x['day'] for x in per_day
                                           if x['sensitivity'] is None],
            'days_with_any_false_negatives': sum(x['fn'] > 0 for x in rows),
            'days_with_any_false_positives': sum(x['fp'] > 0 for x in rows),
            'leave_one_day_out_pooled_sensitivity_range': [
                min(x['sensitivity'] for x in loo if x['sensitivity'] is not None),
                max(x['sensitivity'] for x in loo if x['sensitivity'] is not None)],
            'leave_one_day_out_pooled_specificity_range': [
                min(x['specificity'] for x in loo if x['specificity'] is not None),
                max(x['specificity'] for x in loo if x['specificity'] is not None)],
            'days': per_day, 'leave_one_day_out': loo}
    return out


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--receipt', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    result = analyze(json.loads(a.receipt.read_text(encoding='utf-8')))
    a.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True,
                                   separators=(',', ':'), allow_nan=False)+'\n', encoding='utf-8')
    print('MQ4 forward source day-stability PASS; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
