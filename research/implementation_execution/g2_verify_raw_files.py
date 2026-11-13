#!/usr/bin/env python3
"""Fail-closed, byte-level G2 evidence manifest preflight; NOT scientific validation."""
import argparse
import hashlib
import json
import pathlib
import re
import sys

FIELDS = ("source_id", "raw_data_path", "raw_sha256", "measurement_date",
          "instrument_id", "calibration_reference", "holdout_split_id")
HEX = re.compile(r"[0-9a-fA-F]{64}\Z")
def verify(manifest, root):
    errors = []
    try:
        data = json.loads(pathlib.Path(manifest).read_text(encoding="utf-8"))
    except (OSError, ValueError, UnicodeError) as exc:
        return ["manifest unreadable or invalid: " + str(exc)]
    records = data.get("records") if isinstance(data, dict) else None
    if not isinstance(records, list) or not records:
        return ["no independent physical records (BLOCKED)"]
    root = pathlib.Path(root).resolve()
    ids, hashes, paths = set(), set(), set()
    for i, record in enumerate(records):
        prefix = "records[%d]" % i
        if not isinstance(record, dict):
            errors.append(prefix + ": not an object")
            continue
        if any(not isinstance(record.get(k), str) or not record[k].strip() for k in FIELDS):
            errors.append(prefix + ": missing/invalid required metadata")
            continue
        source, digest = record["source_id"], record["raw_sha256"].lower()
        if source in ids:
            errors.append(prefix + ": duplicate source_id")
        ids.add(source)
        if not HEX.fullmatch(record["raw_sha256"]):
            errors.append(prefix + ": invalid SHA-256")
            continue
        if digest in hashes:
            errors.append(prefix + ": duplicate raw SHA-256")
        hashes.add(digest)
        relative = pathlib.PurePosixPath(record["raw_data_path"])
        if relative.is_absolute() or ".." in relative.parts or "\\" in record["raw_data_path"]:
            errors.append(prefix + ": unsafe path")
            continue
        try:
            target = (root / str(relative)).resolve(strict=False)
            safe_file = target.is_relative_to(root) and target.is_file()
        except (OSError, ValueError, RuntimeError) as exc:
            errors.append(prefix + ": unresolvable raw path: " + str(exc))
            continue
        if not safe_file:
            errors.append(prefix + ": raw file absent or outside root")
            continue
        try:
            if target.stat().st_size == 0:
                errors.append(prefix + ": empty raw file")
                continue
        except (OSError, ValueError) as exc:
            errors.append(prefix + ": raw file stat failure: " + str(exc))
            continue
        if target in paths:
            errors.append(prefix + ": duplicate raw file path")
        paths.add(target)
        sha = hashlib.sha256()
        try:
            with target.open("rb") as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    sha.update(chunk)
        except OSError as exc:
            errors.append(prefix + ": read failure: " + str(exc))
            continue
        if sha.hexdigest() != digest:
            errors.append(prefix + ": raw byte SHA-256 mismatch")
    return errors

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest")
    parser.add_argument("raw_root", help="directory containing original raw files")
    args = parser.parse_args()
    errors = verify(args.manifest, args.raw_root)
    for error in errors:
        print("BLOCKED:", error, file=sys.stderr)
    if errors:
        return 2
    print("INTEGRITY_PREFLIGHT_PASSED; PROVENANCE_REVIEW_REQUIRED; scientifically_validated=false")
    return 0

if __name__ == "__main__":
    sys.exit(main())
