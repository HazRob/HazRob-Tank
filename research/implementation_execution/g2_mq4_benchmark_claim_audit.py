"""Fail-closed reconciliation of external MQ4 benchmark claims with raw LiCor CH4.

Research-only: verifies original reference labels and confusion arithmetic, NOT
model predictions, calibration traceability, physical independence or Tank G3.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from datetime import datetime
from pathlib import Path

ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
RAW_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
SUMMARY_SHA256 = '0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
THRESHOLD_PPB = 4000.0  # 4 ppm, source-only diagnostic, NOT safety alarm


def _digest(data):
    return hashlib.sha256(data).hexdigest()


def _unique_object(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError('G2_MQ4_SUMMARY_DUPLICATE_KEY:' + key)
        out[key] = value
    return out


def _no_constant(value):
    raise ValueError('G2_MQ4_SUMMARY_NONFINITE:' + value)


def _number(value, name, minimum=None, maximum=None):
    if isinstance(value, bool) or not isinstance(value, (float, int)) or not math.isfinite(value):
        raise ValueError('G2_MQ4_NUMBER_INVALID:' + name)
    if minimum is not None and value < minimum or maximum is not None and value > maximum:
        raise ValueError('G2_MQ4_NUMBER_RANGE:' + name)
    return value


def _count(value, name):
    _number(value, name, 0)
    if not isinstance(value, int):
        raise ValueError('G2_MQ4_COUNT_NOT_INTEGER:' + name)
    return value


def audit(archive_bytes, summary_bytes, enforce_pins=True):
    archive_sha, summary_sha = _digest(archive_bytes), _digest(summary_bytes)
    if enforce_pins and (archive_sha != ARCHIVE_SHA256 or summary_sha != SUMMARY_SHA256):
        raise ValueError('G2_MQ4_PINNED_INPUT_MISMATCH')
    try:
        with zipfile.ZipFile(io.BytesIO(archive_bytes)) as z:
            if z.namelist().count(MEMBER) != 1:
                raise ValueError('G2_MQ4_RAW_MEMBER_MISSING_OR_DUPLICATE')
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, RuntimeError, OSError) as exc:
        raise ValueError('G2_MQ4_ARCHIVE_INVALID') from exc
    raw_sha = _digest(raw)
    if enforce_pins and raw_sha != RAW_SHA256:
        raise ValueError('G2_MQ4_RAW_SHA_MISMATCH')
    try:
        reader = csv.reader(io.StringIO(raw.decode('utf-8-sig'), newline=''), strict=True)
        header = next(reader)
        required = ('datetime', 'licor_ch4', 'MQ4_1', 'MQ4_2', 'MQ4_3')
        if len(set(header)) != len(header) or not set(required).issubset(header):
            raise ValueError('G2_MQ4_RAW_HEADER_INVALID')
        indices = {k: header.index(k) for k in required}
        n = positive = 0
        days = set()
        previous = None
        min_ppb = float('inf')
        max_ppb = float('-inf')
        for values in reader:
            if len(values) != len(header):
                raise ValueError('G2_MQ4_RAW_ROW_WIDTH_INVALID')
            timestamp = datetime.fromisoformat(values[indices['datetime']])
            if previous is not None and timestamp <= previous:
                raise ValueError('G2_MQ4_RAW_TIMESTAMP_NONMONOTONIC')
            previous = timestamp
            days.add(timestamp.date().isoformat())
            ch4 = float(values[indices['licor_ch4']])
            _number(ch4, 'licor_ch4_ppb', 0)
            for key in ('MQ4_1', 'MQ4_2', 'MQ4_3'):
                _number(float(values[indices[key]]), key, 0)
                if float(values[indices[key]]) == 0:
                    raise ValueError('G2_MQ4_RAW_SENSOR_ZERO')
            n += 1
            positive += ch4 >= THRESHOLD_PPB
            min_ppb = min(min_ppb, ch4)
            max_ppb = max(max_ppb, ch4)
    except (UnicodeError, csv.Error, OverflowError, TypeError, ValueError) as exc:
        if isinstance(exc, ValueError) and str(exc).startswith('G2_MQ4_'):
            raise
        raise ValueError('G2_MQ4_RAW_PARSE_INVALID') from exc
    if not n:
        raise ValueError('G2_MQ4_RAW_EMPTY')
    try:
        summary = json.loads(summary_bytes.decode('utf-8-sig'),
                             object_pairs_hook=_unique_object, parse_constant=_no_constant)
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise ValueError('G2_MQ4_SUMMARY_INVALID') from exc
    if (summary.get('source_rows') != n or summary.get('days') != len(days)
            or summary.get('sensor_instances') != 3 or summary.get('distinct_gases') != 1):
        raise ValueError('G2_MQ4_SUMMARY_SOURCE_SCOPE_MISMATCH')
    if (summary.get('role') != 'EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME'
            or summary.get('evidence_policy', {}).get('tank_g3_validated') != 0
            or summary.get('four_ppm_diagnostic', {}).get('not_safety_threshold') is not True):
        raise ValueError('G2_MQ4_SCIENTIFIC_BOUNDARY_INVALID')
    if summary['four_ppm_diagnostic'].get('source_reference_positive') != positive:
        raise ValueError('G2_MQ4_REFERENCE_POSITIVE_MISMATCH')
    models = summary.get('models')
    if not isinstance(models, list) or not models:
        raise ValueError('G2_MQ4_MODELS_MISSING')
    names = set()
    checked = []
    for m in models:
        name = m['model']
        if not isinstance(name, str) or not name or name in names:
            raise ValueError('G2_MQ4_MODEL_NAME_INVALID')
        names.add(name)
        if (_count(m['original_source_test_predictions'], name + ':predictions') != n
                or _count(m['test_days'], name + ':days') != len(days)):
            raise ValueError('G2_MQ4_MODEL_SCOPE_MISMATCH:' + name)
        tp, fn, fp, tn = (_count(m['total_4ppm_' + key], name + ':' + key)
                          for key in ('true_positive', 'false_negative', 'false_positive', 'true_negative'))
        if tp + fn != positive or tn + fp != n - positive:
            raise ValueError('G2_MQ4_CONFUSION_REFERENCE_MISMATCH:' + name)
        coverage = _number(m['pooled_interval_coverage'], name + ':coverage', 0, 1)
        _number(m['pooled_mae_ppm'], name + ':mae', 0)
        checked.append({'model': name, 'declared_test_rows': n,
                        'true_positive': tp, 'false_negative': fn,
                        'false_positive': fp, 'true_negative': tn,
                        'sensitivity_4ppm': tp / positive if positive else None,
                        'false_negative_rate_4ppm': fn / positive if positive else None,
                        'false_positive_rate_4ppm': fp / (n - positive) if n > positive else None,
                        'declared_interval90_coverage': coverage,
                        'declared_interval90_undercoverage': coverage < 0.9})
    if 'HGB_MQ4_3_T_RH' in names:
        hgb = next(m for m in checked if m['model'] == 'HGB_MQ4_3_T_RH')
        diag = summary['four_ppm_diagnostic']
        for field, key in (('hgb_true_positive', 'true_positive'), ('hgb_false_negative', 'false_negative'),
                           ('hgb_false_positive', 'false_positive'), ('hgb_true_negative', 'true_negative')):
            if diag.get(field) != hgb[key]:
                raise ValueError('G2_MQ4_HGB_DIAGNOSTIC_MISMATCH:' + field)
        for field, key in (('hgb_sensitivity', 'sensitivity_4ppm'),
                           ('hgb_false_positive_rate', 'false_positive_rate_4ppm')):
            if not math.isclose(_number(diag[field], field, 0, 1), hgb[key], rel_tol=1e-12, abs_tol=1e-12):
                raise ValueError('G2_MQ4_HGB_RATE_MISMATCH:' + field)
    return {'schema': 'hazrob.g2.mq4.external-benchmark-claim-reconciliation.v1',
            'archive_sha256': archive_sha, 'raw_member_sha256': raw_sha,
            'summary_sha256': summary_sha, 'reference_unit': 'ppb',
            'diagnostic_threshold_ppb': THRESHOLD_PPB, 'source_rows': n,
            'source_days': len(days), 'source_reference_positive_4ppm': positive,
            'source_reference_negative_4ppm': n - positive,
            'source_min_ch4_ppm': min_ppb / 1000,
            'source_max_ch4_ppm': max_ppb / 1000,
            'model_claim_arithmetic': checked,
            'reference_labels_independently_recounted_from_raw': True,
            'model_predictions_independently_replayed': False,
            'reported_error_and_interval_metrics_independently_recomputed': False,
            'source_physical_independence_authenticated': False,
            'source_calibration_traceability_authenticated': False,
            'tank_g2_qualified': False, 'tank_g3_qualified': False,
            'not_safety_alarm': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--summary', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.archive.read_bytes(), args.summary.read_bytes())
    args.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    print('MQ4 source truth and declared confusion arithmetic reconciled; no G2/G3 approval')

if __name__ == '__main__':
    main()
