"""Fail-closed MQ4 external raw numeric integrity audit (research only, not G2/G3)."""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from datetime import datetime
from pathlib import Path

SOURCE_ID = 'MQ-ACQ-2026-06'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
RAW_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
COLUMNS = ('licor_ch4', 'MQ4_1', 'MQ4_2', 'MQ4_3', 't', 'rh')
REQUIRED = ('datetime',) + COLUMNS


def audit_rows(rows, fieldnames):
    """Audit numeric completeness/ranges without inferring calibration or exposures.

    Reference methane is ppb; MQ4 channels are ohm. Missing, non-finite,
    nonpositive resistance, negative methane and RH outside [0,100] fail closed.
    """
    if not fieldnames or len(fieldnames) != len(set(fieldnames)) or not set(REQUIRED).issubset(fieldnames):
        raise ValueError('MQ4_NUMERIC_HEADER_INVALID')
    values = {key: [] for key in COLUMNS}
    previous = None
    n = 0
    for n, row in enumerate(rows, 1):
        if None in row or any(v is None for v in row.values()):
            raise ValueError(f'MQ4_NUMERIC_ROW_SHAPE_INVALID:{n}')
        stamp = row['datetime']
        try:
            parsed = datetime.strptime(stamp, '%Y-%m-%d %H:%M:%S')
        except (TypeError, ValueError) as exc:
            raise ValueError(f'MQ4_NUMERIC_TIMESTAMP_INVALID:{n}') from exc
        if parsed.strftime('%Y-%m-%d %H:%M:%S') != stamp or (previous is not None and parsed <= previous):
            raise ValueError(f'MQ4_NUMERIC_TIMESTAMP_NONCANONICAL_OR_DUPLICATE:{n}')
        previous = parsed
        for key in COLUMNS:
            raw = row[key]
            if not isinstance(raw, str) or not raw.strip():
                raise ValueError(f'MQ4_NUMERIC_MISSING:{key}:{n}')
            try:
                value = float(raw)
            except ValueError as exc:
                raise ValueError(f'MQ4_NUMERIC_NONNUMBER:{key}:{n}') from exc
            if not math.isfinite(value):
                raise ValueError(f'MQ4_NUMERIC_NONFINITE:{key}:{n}')
            if key.startswith('MQ4_') and value <= 0:
                raise ValueError(f'MQ4_NUMERIC_RESISTANCE_NONPOSITIVE:{key}:{n}')
            if key == 'licor_ch4' and value < 0:
                raise ValueError(f'MQ4_NUMERIC_METHANE_NEGATIVE:{n}')
            if key == 'rh' and not 0 <= value <= 100:
                raise ValueError(f'MQ4_NUMERIC_RH_OUT_OF_RANGE:{n}')
            values[key].append(value)
    if not n:
        raise ValueError('MQ4_NUMERIC_EMPTY_SOURCE')
    def repeated_run_metrics(v):
        longest = 1
        current = 1
        repeated_pairs = 0
        for i in range(1, len(v)):
            if v[i] == v[i - 1]:
                current += 1
                repeated_pairs += 1
                longest = max(longest, current)
            else:
                current = 1
        return {'consecutive_equal_pairs': repeated_pairs,
                'longest_identical_run': longest}

    return {
        'row_count': n,
        'numeric_fields': {
            key: {'min': min(v), 'max': max(v), 'distinct_values': len(set(v)),
                  'finite_values': len(v), 'invalid_values': 0,
                  **repeated_run_metrics(v)}
            for key, v in values.items()
        },
        'reference_methane_ppm_min': min(values['licor_ch4']) / 1000,
        'reference_methane_ppm_max': max(values['licor_ch4']) / 1000,
        'source_numeric_integrity_verified': True,
        'source_calibration_traceability_verified': False,
        'independent_exposure_groups_verified': False,
        'g2_qualified': False,
        'g3_tank_qualified': False,
    }


def audit_pinned_archive(path):
    archive = Path(path).read_bytes()
    if hashlib.sha256(archive).hexdigest() != ARCHIVE_SHA256:
        raise ValueError('MQ4_NUMERIC_ARCHIVE_SHA256_MISMATCH')
    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        if zf.namelist().count(MEMBER) != 1:
            raise ValueError('MQ4_NUMERIC_MEMBER_MISSING_OR_DUPLICATE')
        raw = zf.read(MEMBER)
    if hashlib.sha256(raw).hexdigest() != RAW_SHA256:
        raise ValueError('MQ4_NUMERIC_RAW_SHA256_MISMATCH')
    reader = csv.DictReader(io.StringIO(raw.decode('utf-8'), newline=''), strict=True)
    summary = audit_rows(reader, reader.fieldnames)
    if summary['row_count'] != 14186:
        raise ValueError('MQ4_NUMERIC_PINNED_ROW_COUNT_MISMATCH')
    return {
        'schema': 'hazrob.g2.mq4-raw-numeric-integrity.v1',
        'source_id': SOURCE_ID,
        'source_archive_sha256': ARCHIVE_SHA256,
        'source_member_sha256': RAW_SHA256,
        'source_member': MEMBER,
        'source_hashes_verified': True,
        'units': {'licor_ch4': 'ppb', 'MQ4_1': 'ohm', 'MQ4_2': 'ohm', 'MQ4_3': 'ohm',
                  't': 'degC', 'rh': 'percent'},
        **summary,
        'scope': 'Raw numeric syntax, finiteness, positivity, timestamp monotonicity and simple RH bounds only.',
        'limitations': 'Not evidence of calibration certificate, physically independent exposures, gas identification, training/holdout independence, or Tank G2/G3 qualification.',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    result = audit_pinned_archive(args.archive)
    args.output.write_text(json.dumps(result, sort_keys=True, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('MQ4 numeric integrity verified for pinned raw bytes; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
