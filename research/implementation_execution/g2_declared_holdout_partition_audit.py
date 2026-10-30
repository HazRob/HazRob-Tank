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

from g2_declared_provenance_overlap_audit import audit_declared_overlap, _valid_id

PARTITIONS = frozenset(('train', 'validation', 'calibration', 'test'))
KEYS = ('group_id', 'session_id', 'exposure_batch_id',
        'exposure_id', 'physical_sample_id', 'device_id')


def _spanning(rows, key):
    groups = defaultdict(set)
    for row in rows:
        groups[row[key]].add(row['partition'])
    return sorted(identifier for identifier, splits in groups.items()
                  if len(splits) > 1)



def _identity_component_test_support(rows, columns):
    """Conservative declared-ID connectivity, excluding device reuse.

    Test components connected to train/calibration/validation are not counted
    as clean test-only components. These are declarations, not physical proof.
    """
    groups = sorted({row['group_id'] for row in rows})
    parent = {g: g for g in groups}

    def root(g):
        while parent[g] != g:
            parent[g] = parent[parent[g]]
            g = parent[g]
        return g

    def join(a, b):
        a, b = root(a), root(b)
        if a != b:
            parent[max(a, b)] = min(a, b)

    for field in ('session_id', 'exposure_batch_id',
                  'exposure_id', 'physical_sample_id'):
        if field not in columns:
            continue
        first = {}
        for row in rows:
            key = row[field]
            if key in first:
                join(first[key], row['group_id'])
            else:
                first[key] = row['group_id']

    by_root = defaultdict(list)
    for group in groups:
        by_root[root(group)].append(group)
    partitions_by_group = defaultdict(set)
    for row in rows:
        partitions_by_group[row['group_id']].add(row['partition'])

    components = sorted((sorted(c) for c in by_root.values()),
                        key=lambda c: c[0])
    test_components = []
    test_only = []
    cross_partition = []
    for members in components:
        labels = set().union(*(partitions_by_group[g] for g in members))
        if 'test' in labels:
            test_components.append(members)
            if labels == {'test'}:
                test_only.append(members)
            else:
                cross_partition.append({
                    'groups': members, 'partitions': sorted(labels),
                })
    return {
        'declared_identity_fields_used': [
            k for k in ('session_id', 'exposure_batch_id',
                        'exposure_id', 'physical_sample_id') if k in columns
        ],
        'declared_identity_test_component_count': len(test_components),
        'declared_identity_test_only_component_count': len(test_only),
        'declared_identity_test_cross_partition_components': cross_partition,
        'test_has_two_disconnected_test_only_declared_identity_components': (
            len(test_only) >= 2
        ),
        'declared_identity_components_are_not_authenticated_experiments': True,
    }


def audit_declared_holdout(rows, columns):
    """Inspect declared partition overlap; do not infer physical independence."""
    rows = list(rows)
    # Reject compatibility aliases before the shared provenance header gate.
    if any(c != unicodedata.normalize('NFKC', c)
           and unicodedata.normalize('NFKC', c)
           in ('partition', 'exposure_id', 'physical_sample_id')
           for c in columns):
        raise ValueError('G2_HOLDOUT_CRITICAL_HEADER_ALIAS')
    base = audit_declared_overlap(rows, columns)
    # Optional physical IDs are evidence-bearing: reject empty and
    # compatibility-equivalent aliases instead of silently conflating them.
    for field in ('exposure_id', 'physical_sample_id'):
        if field in columns:
            folded_ids = {}
            for row in rows:
                value = row[field]
                if not _valid_id(value):
                    raise ValueError('G2_HOLDOUT_OPTIONAL_ID_INVALID:' + field)
                folded = unicodedata.normalize('NFKC', value)
                if folded in folded_ids and folded_ids[folded] != value:
                    raise ValueError('G2_HOLDOUT_OPTIONAL_ID_ALIAS_NFKC:' + field)
                folded_ids[folded] = value
    out = {
        'schema': 'hazrob.g2.declared-holdout-partition.v1',
        'checked': False,
        'reason': None,
        'partition_labels': None,
        'declared_rows_per_partition': None,
        'declared_groups_per_partition': None,
        'calibration_partition_declared': None,
        'calibration_partition_absent_requires_g2_protocol_review': None,
        'test_has_two_declared_groups_for_cluster_uncertainty': None,
        'declared_identity_test_component_count': None,
        'declared_identity_test_only_component_count': None,
        'declared_identity_test_cross_partition_components': None,
        'test_has_two_disconnected_test_only_declared_identity_components': None,
        'declared_identity_components_are_not_authenticated_experiments': True,
        'declared_group_counts_do_not_verify_physical_independence': True,
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
    # Calibration/threshold selection must not be silently folded into train
    # or test. Counts are of *declared* groups, not authenticated exposures.
    out['declared_rows_per_partition'] = {
        label: sum(r['partition'] == label for r in rows) for label in labels
    }
    out['declared_groups_per_partition'] = {
        label: len({r['group_id'] for r in rows if r['partition'] == label})
        for label in labels
    }
    out['calibration_partition_declared'] = 'calibration' in labels
    out['calibration_partition_absent_requires_g2_protocol_review'] = (
        'calibration' not in labels
    )
    out['test_has_two_declared_groups_for_cluster_uncertainty'] = (
        out['declared_groups_per_partition'].get('test', 0) >= 2
    )
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
    out.update(_identity_component_test_support(rows, columns))
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
