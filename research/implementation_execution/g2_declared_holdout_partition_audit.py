"""Research-only, fail-closed diagnostic for declared G2 train/test holdout partitions.

Partition labels and provenance identifiers are declarations, not independently
authenticated physical experiments. No result is a G2 or G3 approval.
"""
import argparse
import csv
import io
import json
import unicodedata
from collections import defaultdict
from pathlib import Path

from g2_declared_provenance_overlap_audit import audit_declared_overlap

PARTITIONS = frozenset(('train', 'validation', 'test'))
KEYS = ('group_id', 'session_id', 'exposure_batch_id',
        'exposure_id', 'physical_sample_id', 'device_id')


def _spanning(rows, key):
    groups = defaultdict(set)
    for row in rows:
        groups[row[key]].add(row['partition'])
    return sorted(identifier for identifier, splits in groups.items()
                  if len(splits) > 1)


def audit_declared_holdout(rows, columns):
    """Inspect declared partition overlap; do not infer physical independence."""
    rows = list(rows)
    # Reject compatibility aliases before the shared provenance header gate.
    if any(c != 'partition' and unicodedata.normalize('NFKC', c) == 'partition'
           for c in columns):
        raise ValueError('G2_HOLDOUT_PARTITION_HEADER_ALIAS')
    base = audit_declared_overlap(rows, columns)
    out = {
        'schema': 'hazrob.g2.declared-holdout-partition.v1',
        'checked': False,
        'reason': None,
        'partition_labels': None,
        'cross_partition_ids': None,
        'cross_partition_components': None,
        'potential_partition_overlap': None,
        'device_reuse_is_not_automatic_leakage': True,
        'independent_physical_exposures_verified': False,
        'source_provenance_authenticated': False,
        'g2_qualified': False,
        'g3_qualified': False,
        'declared_rows_semantic_sha256': base['declared_rows_semantic_sha256'],
    }
    if 'partition' not in columns:
        out['reason'] = 'PARTITION_COLUMN_ABSENT'
        return out
    if any(row['partition'] not in PARTITIONS for row in rows):
        raise ValueError('G2_HOLDOUT_PARTITION_LABEL_INVALID')
    labels = sorted({row['partition'] for row in rows})
    out['partition_labels'] = labels
    if not {'session_id', 'exposure_batch_id'}.issubset(columns):
        out['reason'] = 'SESSION_OR_BATCH_PROVENANCE_ABSENT'
        return out
    if not {'train', 'test'}.issubset(labels):
        out['reason'] = 'TRAIN_TEST_NOT_BOTH_DECLARED'
        return out
    spans = {key: _spanning(rows, key) if key in columns else None
             for key in KEYS}
    component_result = base['declared_session_batch_connectivity']
    if not component_result['checked']:
        out['reason'] = 'CONNECTIVITY_UNVERIFIED'
        return out
    by_group = defaultdict(set)
    for row in rows:
        by_group[row['group_id']].add(row['partition'])
    spanning_components = []
    for component in component_result['components']:
        component_labels = set().union(*(by_group[g] for g in component))
        if len(component_labels) > 1:
            spanning_components.append({
                'groups': component,
                'partitions': sorted(component_labels),
            })
    # Device reuse is reported, but never by itself counted as leakage.
    suspicious = ('group_id', 'session_id', 'exposure_batch_id',
                  'exposure_id', 'physical_sample_id')
    out.update({
        'checked': True,
        'reason': None,
        'cross_partition_ids': spans,
        'cross_partition_components': spanning_components,
        'potential_partition_overlap': (
            any(bool(spans[k]) for k in suspicious if spans[k] is not None)
            or bool(spanning_components)),
        'optional_exposure_id_checked': 'exposure_id' in columns,
        'optional_physical_sample_id_checked': 'physical_sample_id' in columns,
        'no_detected_overlap_is_not_physical_independence_evidence': True,
    })
    return out


def audit_csv(path):
    raw = Path(path).read_bytes()
    try:
        with io.StringIO(raw.decode('utf-8-sig'), newline='') as stream:
            reader = csv.reader(stream, strict=True)
            columns = next(reader, None)
            if not columns:
                raise ValueError('G2_HOLDOUT_CSV_HEADER_INVALID')
            rows = []
            for record, values in enumerate(reader, start=2):
                if len(values) != len(columns):
                    raise ValueError('G2_HOLDOUT_CSV_WIDTH_INVALID:record='
                                     + str(record))
                rows.append(dict(zip(columns, values)))
    except (UnicodeError, csv.Error) as exc:
        raise ValueError('G2_HOLDOUT_CSV_FORMAT_INVALID') from exc
    return audit_declared_holdout(rows, columns)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--declared-csv', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit_csv(args.declared_csv)
    args.output.write_text(
        json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2)
        + '\n', encoding='utf-8')
    print('Declared holdout overlap diagnostic only; G2/G3 not qualified')


if __name__ == '__main__':
    main()
