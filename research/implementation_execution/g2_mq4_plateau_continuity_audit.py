"""Research-only, gap-aware MQ4 constant-reading diagnostics; never G2/G3 promotion."""
import argparse
import csv
import hashlib
import io
import json
import zipfile
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path

from g2_mq4_numeric_integrity_audit import (
    ARCHIVE_SHA256, RAW_SHA256, MEMBER, SOURCE_ID, REQUIRED, audit_rows,
)

CHANNELS = ('MQ4_1', 'MQ4_2', 'MQ4_3')


def audit_plateaus(rows, fieldnames):
    """Only 60-second adjacent rows count as contiguous; no exposure independence assumed."""
    rows = list(rows)
    integrity = audit_rows(rows, fieldnames)  # fail closed before deriving any metrics
    dates = [datetime.strptime(r['datetime'], '%Y-%m-%d %H:%M:%S') for r in rows]
    reference = [float(r['licor_ch4']) for r in rows]  # ppb
    result = {}
    gaps = sum(dates[i] - dates[i - 1] != timedelta(seconds=60)
               for i in range(1, len(dates)))
    for channel in CHANNELS:
        readings = [float(r[channel]) for r in rows]
        day_pairs = defaultdict(int)
        equal_pairs = 0
        runs = []
        start = 0

        def finish(end):
            if end > start:
                ref_slice = reference[start:end + 1]
                runs.append({'length': end - start + 1,
                             'reference_span_ppb': max(ref_slice) - min(ref_slice)})

        for i in range(1, len(rows)):
            contiguous = dates[i] - dates[i - 1] == timedelta(seconds=60)
            if contiguous and readings[i] == readings[i - 1]:
                equal_pairs += 1
                day_pairs[dates[i].date().isoformat()] += 1
            else:
                finish(i - 1)
                start = i
        finish(len(rows) - 1)
        result[channel] = {
            'strict_minute_equal_pairs': equal_pairs,
            'noncontiguous_equal_pairs_excluded': integrity['numeric_fields'][channel]['consecutive_equal_pairs'] - equal_pairs,
            'constant_runs_at_least_two': len(runs),
            'longest_strict_minute_run': max((r['length'] for r in runs), default=1),
            'max_reference_span_ppb_in_constant_run': max((r['reference_span_ppb'] for r in runs), default=0.0),
            'constant_runs_with_reference_span_at_least_100ppb': sum(r['reference_span_ppb'] >= 100 for r in runs),
            'strict_equal_pairs_by_later_row_day': dict(sorted(day_pairs.items())),
        }
        assert sum(day_pairs.values()) == equal_pairs
        assert 0 <= equal_pairs <= len(rows) - 1 - gaps
    return {
        'schema': 'hazrob.g2.mq4-gap-aware-plateau.v1',
        'source_id': SOURCE_ID,
        'row_count': len(rows),
        'non_60_second_gaps': gaps,
        'strict_minute_adjacent_pairs': len(rows) - 1 - gaps,
        'channels': result,
        'source_calibration_traceability_verified': False,
        'independent_exposure_groups_verified': False,
        'g2_qualified': False,
        'g3_tank_qualified': False,
        'interpretation': 'Descriptive raw-channel plateau statistics only; no causal sensor diagnosis, independent exposure, calibration, or gas identification.',
    }


def audit_pinned_archive(path):
    archive = Path(path).read_bytes()
    if hashlib.sha256(archive).hexdigest() != ARCHIVE_SHA256:
        raise ValueError('MQ4_PLATEAU_ARCHIVE_SHA256_MISMATCH')
    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        if zf.namelist().count(MEMBER) != 1:
            raise ValueError('MQ4_PLATEAU_MEMBER_MISSING_OR_DUPLICATE')
        raw = zf.read(MEMBER)
    if hashlib.sha256(raw).hexdigest() != RAW_SHA256:
        raise ValueError('MQ4_PLATEAU_RAW_SHA256_MISMATCH')
    reader = csv.DictReader(io.StringIO(raw.decode('utf-8'), newline=''), strict=True)
    result = audit_plateaus(reader, reader.fieldnames)
    if result['row_count'] != 14186:
        raise ValueError('MQ4_PLATEAU_ROW_COUNT_MISMATCH')
    return {'source_archive_sha256': ARCHIVE_SHA256,
            'source_member_sha256': RAW_SHA256,
            'source_member': MEMBER,
            'source_hashes_verified': True,
            **result}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    record = audit_pinned_archive(args.archive)
    args.output.write_text(json.dumps(record, ensure_ascii=False, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    print('MQ4 plateau diagnostic complete; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
