"""Fail-closed audit of a pinned external MQ4 raw-day *receipt*, not the raw ZIP.

The GitHub blob SHA-1 binds the exact committed receipt bytes. The ZIP/CSV
SHA-256 values inside the receipt are provenance references, not evidence
that CI has independently reopened those source bytes. No G2/G3 promotion.
"""
import argparse
import hashlib
import json
import math
from datetime import date
from pathlib import Path

HERE = Path(__file__).resolve().parent
RECEIPT = HERE / 'G2_MQ4_RAW_DAY_DIAGNOSTICS_2026-11-02.json'
PINNED_GIT_BLOB_SHA1 = '7ac634ff77518748c434bbbafa6934ce61548ca4'
ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
MEMBER_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
SENSORS = ('MQ4_1', 'MQ4_2', 'MQ4_3')
FALSE_FLAGS = ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
               'traceable_calibration', 'blinded_prospective')
TOP_KEYS = {'schema', 'archive_sha256', 'member_sha256', 'source', 'rows',
            'calendar_days', 'sensor_names', 'daily_compact', 'summary',
            'limitations', *FALSE_FLAGS}
SUMMARY_KEYS = {'days_positive_pearson', 'days_negative_pearson',
                'min_day_pearson', 'max_day_pearson',
                'min_day_distinct_fraction', 'max_day_distinct_fraction'}


def require(condition, code):
    if not condition:
        raise ValueError('G2_MQ4_RAW_RECEIPT_' + code)


def no_duplicate_pairs(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'DUPLICATE_JSON_KEY')
        result[key] = value
    return result


def reject_constant(value):
    raise ValueError('G2_MQ4_RAW_RECEIPT_NONFINITE_JSON_' + value)


def strict_loads(raw):
    return json.loads(raw.decode('utf-8'), object_pairs_hook=no_duplicate_pairs,
                      parse_constant=reject_constant)


def real(value):
    return type(value) in (int, float) and math.isfinite(value)


def git_blob_sha1(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode('ascii') + bytes([0]) + raw).hexdigest()


def derive(obj):
    require(type(obj) is dict and set(obj) == TOP_KEYS, 'TOP_SCHEMA')
    require(obj['schema'] == 'hazrob.g2.mq4-raw-day-diagnostics.v1', 'SCHEMA')
    require(obj['source'] == 'external_observational_minnesota_MQ4_not_tank',
            'SOURCE_CLASS')
    require(obj['archive_sha256'] == ARCHIVE_SHA256 and
            obj['member_sha256'] == MEMBER_SHA256, 'SOURCE_PINS')
    require(obj['sensor_names'] == list(SENSORS), 'SENSOR_NAMES')
    require(type(obj['rows']) is int and obj['rows'] == 14186 and
            type(obj['calendar_days']) is int and obj['calendar_days'] == 11,
            'SOURCE_SCOPE')
    require(type(obj['limitations']) is str and len(obj['limitations']) > 100,
            'LIMITATIONS')
    for flag in FALSE_FLAGS:
        require(obj[flag] is False, 'UNSUPPORTED_CLAIM_' + flag.upper())
    days = obj['daily_compact']
    summary = obj['summary']
    require(type(days) is dict and len(days) == 11, 'DAY_SCOPE')
    require(type(summary) is dict and set(summary) == set(SENSORS),
            'SUMMARY_SENSORS')
    all_sign_agree = 0
    positive_days, negative_days = [], []
    no_positive, mixed_range, no_negative = [], [], []
    ratio = {}
    total = 0
    ordered = sorted(days)
    for day in ordered:
        try:
            require(date.fromisoformat(day).isoformat() == day, 'DATE')
        except (ValueError, TypeError):
            raise ValueError('G2_MQ4_RAW_RECEIPT_DATE') from None
        entry = days[day]
        require(type(entry) is dict and set(entry) == {'n', 'ref_min_ppm',
                'ref_max_ppm', 'mq4'}, 'DAY_FIELDS')
        n = entry['n']
        lo, hi = entry['ref_min_ppm'], entry['ref_max_ppm']
        require(type(n) is int and n > 1, 'DAY_N')
        require(real(lo) and real(hi) and 0 <= lo <= hi, 'REFERENCE_RANGE')
        total += n
        require(type(entry['mq4']) is dict and set(entry['mq4']) == set(SENSORS),
                'DAY_SENSORS')
        signs = []
        counts = {}
        for sensor in SENSORS:
            pair = entry['mq4'][sensor]
            require(type(pair) is list and len(pair) == 2, 'PAIR')
            count, correlation = pair
            require(type(count) is int and 1 <= count <= n, 'CARDINALITY')
            require(real(correlation) and -1 <= correlation <= 1, 'PEARSON')
            counts[sensor] = count
            signs.append(1 if correlation > 0 else -1 if correlation < 0 else 0)
        if len(set(signs)) == 1:
            all_sign_agree += 1
            if signs[0] > 0:
                positive_days.append(day)
            elif signs[0] < 0:
                negative_days.append(day)
        if hi < 4:
            no_positive.append(day)
        elif lo >= 4:
            no_negative.append(day)
        else:
            mixed_range.append(day)
        ratio[day] = round(counts['MQ4_1'] / counts['MQ4_3'], 4)
    require(total == obj['rows'], 'ROW_SUM')
    for sensor in SENSORS:
        item = summary[sensor]
        require(type(item) is dict and set(item) == SUMMARY_KEYS,
                'SUMMARY_FIELDS')
        plus = [d for d in ordered if days[d]['mq4'][sensor][1] > 0]
        minus = [d for d in ordered if days[d]['mq4'][sensor][1] < 0]
        require(item['days_positive_pearson'] == plus and
                item['days_negative_pearson'] == minus, 'SIGN_LINEAGE')
        correlations = [days[d]['mq4'][sensor][1] for d in ordered]
        fractions = [days[d]['mq4'][sensor][0] / days[d]['n'] for d in ordered]
        expected = {'min_day_pearson': min(correlations),
                    'max_day_pearson': max(correlations),
                    'min_day_distinct_fraction': round(min(fractions), 5),
                    'max_day_distinct_fraction': round(max(fractions), 5)}
        for key, value in expected.items():
            require(real(item[key]) and abs(item[key] - value) < 0.000011,
                    'SUMMARY_LINEAGE_' + key.upper())
    return {
        'schema': 'hazrob.g2.mq4-raw-receipt-audit.v1',
        'pinned_git_blob_sha1': PINNED_GIT_BLOB_SHA1,
        'source_scope': {'rows': total, 'calendar_days': len(days),
                         'physical_mq4_units': len(SENSORS)},
        'correlation_sign_agreement_days': all_sign_agree,
        'all_sensor_positive_correlation_days': positive_days,
        'all_sensor_negative_correlation_days': negative_days,
        'reference_4ppm_range_support': {
            'guaranteed_no_positive_days': no_positive,
            'range_crosses_threshold_days': mixed_range,
            'guaranteed_no_negative_days': no_negative,
            'note': 'Range support only, not row-level positive/negative counts or sensitivity.'
        },
        'daily_distinct_value_count_ratio_mq4_1_to_mq4_3': ratio,
        'ratio_min': min(ratio.values()), 'ratio_max': max(ratio.values()),
        'g2_qualified': False, 'g3_qualified': False,
        'independent_tank_exposures': False,
        'traceable_calibration': False, 'blinded_prospective': False,
        'limitations': ('Derived from a pinned compact JSON receipt, not replayed original ZIP. '
                        'Daily correlations are dependent observational summaries, not '
                        'independent exposures, causal effects, calibration, gas identification, '
                        'certified ppm, alarm limits or scientific G2/G3 qualification.')
    }


def audit_bytes(raw):
    require(git_blob_sha1(raw) == PINNED_GIT_BLOB_SHA1, 'BYTE_PIN')
    return derive(strict_loads(raw))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--receipt', type=Path, default=RECEIPT)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit_bytes(args.receipt.read_bytes())
    rendered = json.dumps(result, ensure_ascii=False, indent=2,
                          sort_keys=True, allow_nan=False) + chr(10)
    if args.output:
        args.output.write_text(rendered, encoding='utf-8')
    else:
        print(rendered, end='')
    print('MQ4 compact receipt integrity PASS; no original ZIP replay; no G2/G3',
          file=__import__('sys').stderr)


if __name__ == '__main__':
    main()
