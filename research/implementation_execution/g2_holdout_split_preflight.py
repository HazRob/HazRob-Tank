#!/usr/bin/env python3
"""G2 independent physical holdout split preflight. Research-only; no scientific approval."""
import argparse
import csv
import hashlib
import json
from pathlib import Path

REQUIRED = ("sample_id", "device_id", "session_id", "day_id", "partition", "raw_sha256")
PARTITIONS = {"train", "validation", "holdout"}

def audit(rows):
    errors, seen, groups = [], set(), {}
    counts = {p: 0 for p in sorted(PARTITIONS)}
    for n, row in enumerate(rows, 2):
        missing = [k for k in REQUIRED if not str(row.get(k, "")).strip()]
        if missing:
            errors.append(f"row {n}: missing {','.join(missing)}")
            continue
        sid, partition = row["sample_id"], row["partition"]
        if sid in seen:
            errors.append(f"row {n}: duplicate sample_id {sid}")
        seen.add(sid)
        if partition not in PARTITIONS:
            errors.append(f"row {n}: invalid partition {partition}")
            continue
        digest = row["raw_sha256"]
        if len(digest) != 64 or any(c not in "0123456789abcdefABCDEF" for c in digest):
            errors.append(f"row {n}: invalid raw_sha256")
        counts[partition] += 1
        # Conservative grouping: each device, day, and exposure session is disjoint.
        for key in ("device_id", "day_id", "session_id"):
            identity = (key, row[key])
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
        reader = csv.DictReader(f)
        if not reader.fieldnames or any(k not in reader.fieldnames for k in REQUIRED):
            raise SystemExit("missing required CSV columns")
        result = audit(list(reader))
    result["manifest_sha256"] = hashlib.sha256(raw).hexdigest()
    output = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(output, encoding="utf-8")
    print(output, end="")
    raise SystemExit(1 if result["errors"] else 0)

if __name__ == "__main__":
    main()
