#!/usr/bin/env python3
"""G2 independent physical holdout split preflight. Research-only; no scientific approval."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import unicodedata

def canonical_id(value):
    return unicodedata.normalize('NFKC', value).strip()


def invalid_id(value):
    """Reject identifiers empty after normalization or containing control/format characters."""
    normalized = canonical_id(value)
    return not normalized or any(unicodedata.category(ch) in ("Cc", "Cf", "Cs") for ch in normalized)


REQUIRED = ("sample_id", "device_id", "session_id", "day_id", "partition", "raw_sha256")
PARTITIONS = {"train", "validation", "holdout"}

def audit(rows):
    errors, seen, groups, raw_groups = [], set(), {}, {}
    counts = {p: 0 for p in sorted(PARTITIONS)}
    for n, row in enumerate(rows, 2):
        missing = [k for k in REQUIRED if row.get(k) is None or not isinstance(row.get(k), str) or not row[k].strip()]
        if missing:
            errors.append(f"row {n}: missing {','.join(missing)}")
            continue
        bad_ids = [key for key in ("sample_id", "device_id", "session_id", "day_id") if invalid_id(row[key])]
        if bad_ids:
            errors.append(f"row {n}: invalid normalized identifiers {','.join(bad_ids)}")
            continue
        sid, partition = canonical_id(row["sample_id"]), row["partition"].strip()
        if sid in seen:
            errors.append(f"row {n}: duplicate sample_id {sid}")
        seen.add(sid)
        if partition not in PARTITIONS:
            errors.append(f"row {n}: invalid partition {partition}")
            continue
        digest = row["raw_sha256"].strip()
        if len(digest) != 64 or any(c not in "0123456789abcdefABCDEF" for c in digest):
            errors.append(f"row {n}: invalid raw_sha256")
        # Identical raw payloads must not cross partitions.
        if len(digest) == 64 and all(c in "0123456789abcdefABCDEF" for c in digest):
            digest_key = digest.lower()
            prior_raw = raw_groups.setdefault(digest_key, partition)
            if prior_raw != partition:
                errors.append(f"row {n}: raw_sha256 leaks {prior_raw} -> {partition}")
        counts[partition] += 1
        # Conservative grouping: each device, day, and exposure session is disjoint.
        for key in ("device_id", "day_id", "session_id"):
            identity = (key, canonical_id(row[key]))
            prior = groups.setdefault(identity, partition)
            if prior != partition:
                errors.append(f"row {n}: {key}={row[key]} leaks {prior} -> {partition}")
    if not counts["holdout"]:
        errors.append("no independent holdout samples")
    if not counts["train"] or not counts["validation"]:
        errors.append("train and validation partitions are required")
    return {"status": "FAIL" if errors else "PASS_SPLIT_INTEGRITY_ONLY",
            "counts": counts, "errors": sorted(set(errors)),
            "scientific_g2_approved": False}

def main():
    p = argparse.ArgumentParser()
    p.add_argument("manifest", type=Path, help="CSV with required columns")
    p.add_argument("--output", type=Path, help="optional JSON audit output")
    args = p.parse_args()
    raw = args.manifest.read_bytes()
    with args.manifest.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f, strict=True)
        try:
            header = reader.fieldnames
        except csv.Error as exc:
            raise SystemExit(f"malformed CSV header: {exc}") from exc
        if not header or any(not isinstance(k, str) or not k.strip() for k in header):
            raise SystemExit("empty CSV column name")
        if any(any(unicodedata.category(ch) in ("Cc", "Cf", "Cs") for ch in unicodedata.normalize("NFKC", k)) for k in header):
            raise SystemExit("CSV header contains control or format characters")
        if len(header) != len(set(header)) or len({unicodedata.normalize("NFKC", k).strip().casefold() for k in header}) != len(header):
            raise SystemExit("duplicate CSV columns")
        if any(k not in reader.fieldnames for k in REQUIRED):
            raise SystemExit("missing required CSV columns")
        try:
            rows = list(reader)
        except csv.Error as exc:
            raise SystemExit(f"malformed CSV: {exc}") from exc
        if any(None in row for row in rows):
            raise SystemExit("CSV rows contain extra fields")
        result = audit(rows)
    result["manifest_sha256"] = hashlib.sha256(raw).hexdigest()
    output = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(output, encoding="utf-8")
    print(output, end="")
    raise SystemExit(1 if result["errors"] else 0)

if __name__ == "__main__":
    main()
