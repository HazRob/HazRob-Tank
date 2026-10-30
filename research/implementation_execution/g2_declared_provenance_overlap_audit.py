"""Research-only audit of declared session/batch/device overlap across G2 groups.

Overlapping declared identifiers are diagnostics, NOT proof of experimental
leakage. Missing metadata is explicitly unverified, never treated as clean.
"""
import argparse
import csv
import hashlib
import io
import json
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

FIELDS = ('session_id', 'exposure_batch_id', 'device_id')


def _valid_id(value):
    return (isinstance(value, str) and bool(value.strip())
            and value == value.strip()
            and unicodedata.normalize('NFC', value) == value
            and not any(unicodedata.category(c).startswith('C')
                        or unicodedata.category(c) in ('Zl', 'Zp')
                        or (unicodedata.category(c) == 'Zs' and c != ' ')
                        for c in value))


def _reject_nfkc_aliases(rows, columns):
    """Fail closed on compatibility-equivalent identifiers; never silently merge groups.

    NFKC is used only to detect ambiguity, not to rewrite scientific source IDs.
    """
    critical = {'group_id', *FIELDS}
    folded_headers = {}
    for column in columns:
        folded = unicodedata.normalize('NFKC', column)
        if (folded in folded_headers and folded_headers[folded] != column
                or (folded != column and folded in critical)):
            raise ValueError('G2_PROVENANCE_HEADER_ALIAS_NFKC')
        folded_headers[folded] = column
    for field in ('group_id', *FIELDS):
        if field not in columns:
            continue
        folded_values = {}
        for row in rows:
            value = row[field]
            if not _valid_id(value):
                raise ValueError(f'G2_PROVENANCE_ID_INVALID:{field}')
            folded = unicodedata.normalize('NFKC', value)
            if folded in folded_values and folded_values[folded] != value:
                raise ValueError(f'G2_PROVENANCE_ID_ALIAS_NFKC:{field}')
            folded_values[folded] = value


def audit_declared_overlap(rows, columns):
    """Return deterministic overlap counts for *declared* provenance only.

    A device may legitimately be used in multiple groups. Sessions/batches
    crossing groups warrant protocol review, not automatic rejection.
    """
    if (not columns or len(columns) != len(set(columns))
            or 'group_id' not in columns
            or any(not _valid_id(c) for c in columns)):
        raise ValueError('G2_PROVENANCE_HEADER_INVALID')
    rows = list(rows)
    if not rows:
        raise ValueError('G2_PROVENANCE_EMPTY')
    for row in rows:
        if (set(row) != set(columns)
                or any(not _valid_id(row.get(key)) for key in ('group_id',))):
            raise ValueError('G2_PROVENANCE_ROW_INVALID')
    _reject_nfkc_aliases(rows, columns)
    result = {'schema': 'hazrob.g2.declared-group-overlap.v1',
              'rows': len(rows),
              'declared_groups': sorted({r['group_id'] for r in rows}),
              'by_provenance_field': {},
              'potential_session_or_batch_group_overlap': None,
              'source_provenance_authenticated': False,
              'independent_physical_exposures_verified': False,
              'g2_qualified': False,
              'g3_qualified': False,
              'scope': ('DECLARED_METADATA_ONLY;GROUP_OVERLAP_IS_NOT_PROOF_OF_LEAKAGE;'
                        'DEVICE_REUSE_IS_NOT_AUTOMATIC_LEAKAGE;'
                        'MISSING_METADATA_IS_UNVERIFIED;NOT_G2_OR_G3_APPROVAL')}
    relevant_flags = []
    for field in FIELDS:
        if field not in columns:
            result['by_provenance_field'][field] = {
                'checked': False, 'distinct_ids': None,
                'ids_spanning_multiple_declared_groups': None,
                'cross_group_id_count': None,
            }
            continue
        group_sets = defaultdict(set)
        for row in rows:
            value = row[field]
            if not _valid_id(value):
                raise ValueError(f'G2_PROVENANCE_ID_INVALID:{field}')
            group_sets[value].add(row['group_id'])
        overlaps = sorted(value for value, groups in group_sets.items()
                          if len(groups) > 1)
        result['by_provenance_field'][field] = {
            'checked': True, 'distinct_ids': len(group_sets),
            'ids_spanning_multiple_declared_groups': overlaps,
            'cross_group_id_count': len(overlaps),
        }
        if field in ('session_id', 'exposure_batch_id'):
            relevant_flags.append(bool(overlaps))
    if len(relevant_flags) == 2:
        result['potential_session_or_batch_group_overlap'] = any(relevant_flags)
    # Explicitly preserve uncertainty if either relevant provenance column is absent.
    # Exact repeated *declared* records cannot be counted as independent
    # physical samples without authenticated sample/exposure provenance.
    # These diagnostics do not infer whether repetition is legitimate.
    payload = [sorted(row.items()) for row in rows]
    serialized = [json.dumps(items, ensure_ascii=False,
                             separators=(',', ':')) for items in payload]
    frequencies = Counter(serialized)
    result['unique_declared_rows'] = len(frequencies)
    result['exact_duplicate_declared_rows'] = sum(n - 1 for n in frequencies.values())
    result['repeated_declared_row_patterns'] = sum(n > 1 for n in frequencies.values())
    result['declared_rows_per_group'] = dict(sorted(Counter(
        row['group_id'] for row in rows).items()))
    result['duplicate_metadata_is_not_independent_evidence'] = True
    result['declared_rows_semantic_sha256'] = hashlib.sha256(
        json.dumps(sorted(payload), ensure_ascii=False,
                   separators=(',', ':')).encode('utf-8')).hexdigest()
    return result


def audit_csv(path):
    """Read exact-width CSV; never silently pad or discard provenance cells.

    DictReader fills missing trailing fields with None and places surplus cells
    under a None key. Both can hide incomplete or misaligned evidence rows.
    """
    raw = Path(path).read_bytes()
    try:
        with io.StringIO(raw.decode('utf-8-sig'), newline='') as handle:
            reader = csv.reader(handle, strict=True)
            columns = next(reader, None)
            if not columns:
                raise ValueError('G2_PROVENANCE_HEADER_INVALID')
            rows = []
            for record_number, values in enumerate(reader, start=2):
                if len(values) != len(columns):
                    raise ValueError(
                        'G2_PROVENANCE_CSV_ROW_WIDTH_INVALID:'
                        f'record={record_number}:expected={len(columns)}:'
                        f'actual={len(values)}')
                rows.append(dict(zip(columns, values)))
    except (UnicodeError, csv.Error) as exc:
        raise ValueError('G2_PROVENANCE_CSV_INVALID') from exc
    return audit_declared_overlap(rows, columns)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--predictions', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit_csv(args.predictions)
    args.output.write_text(json.dumps(result, sort_keys=True, ensure_ascii=False,
                                       indent=2) + '\n', encoding='utf-8')
    print('Declared G2 group overlap audited; independent evidence NOT verified')


if __name__ == '__main__':
    main()
