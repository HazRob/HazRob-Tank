#!/usr/bin/env python3
"""Fail-closed G2 independent-evidence manifest audit (Python stdlib only).

Usage: python3 research/implementation_execution/audit_g2_independent_evidence.py
Optional: --manifest PATH --json
This audit checks provenance completeness, not scientific validity or sensor accuracy.
"""
import argparse
import json
from datetime import date
from pathlib import Path, PurePosixPath
import unicodedata

DEFAULT = Path(__file__).with_name("G2_INDEPENDENT_EVIDENCE.json")
def has_disallowed_unicode(value):
    """Reject non-NFKC identifiers, invisible formatting, and control characters."""
    return (unicodedata.normalize("NFKC", value) != value
            or any(unicodedata.category(char) in ("Cc", "Cf", "Cs") for char in value))

REQUIRED = ("source_id", "raw_data_path", "raw_sha256", "measurement_date",
            "instrument_id", "calibration_reference", "holdout_split_id")
def audit(data):
    errors = []
    if not isinstance(data, dict) or data.get("schema") != "hazrob.g2.independent-evidence.v1":
        return {"status": "BLOCKED", "record_count": 0, "errors": ["invalid manifest schema"], "scientifically_validated": False}
    records = data.get("records")
    if not isinstance(records, list):
        return {"status": "BLOCKED", "record_count": 0, "errors": ["records must be an array"], "scientifically_validated": False}
    if not records:
        errors.append("no independent physical measurement records")
    seen = set()
    raw_paths = {}
    raw_digests = {}
    for index, record in enumerate(records):
        if not isinstance(record, dict):
            errors.append(f"record {index}: expected object")
            continue
        missing = [k for k in REQUIRED if not isinstance(record.get(k), str) or not record[k].strip()]
        if missing:
            errors.append(f"record {index}: missing provenance: {','.join(missing)}")
        measured = record.get("measurement_date")
        try:
            if not isinstance(measured, str) or len(measured) != 10 or date.fromisoformat(measured).isoformat() != measured:
                raise ValueError("invalid date")
        except ValueError:
            errors.append(f"record {index}: invalid measurement_date (expected YYYY-MM-DD calendar date)")
        sha = record.get("raw_sha256", "")
        if not isinstance(sha, str) or len(sha) != 64 or any(c not in "0123456789abcdefABCDEF" for c in sha):
            errors.append(f"record {index}: invalid raw_sha256")
        if isinstance(sha, str) and len(sha) == 64 and all(c in "0123456789abcdefABCDEF" for c in sha):
            digest = sha.lower()
            if digest in raw_digests:
                errors.append(f"record {index}: duplicate raw_sha256 (first used by record {raw_digests[digest]})")
            else:
                raw_digests[digest] = index
        raw_path = record.get("raw_data_path")
        if isinstance(raw_path, str) and raw_path.strip():
            normalized = raw_path.replace("\\", "/")
            parts = PurePosixPath(normalized).parts
            if (normalized != raw_path or raw_path != raw_path.strip()
                    or raw_path.startswith(("/", "~"))
                    or any(part in ("", ".", "..") for part in normalized.split("/"))
                    or ":" in normalized.split("/")[0]
                    or has_disallowed_unicode(raw_path)):
                errors.append(f"record {index}: unsafe raw_data_path")
            elif raw_path in raw_paths:
                errors.append(f"record {index}: duplicate raw_data_path (first used by record {raw_paths[raw_path]})")
            else:
                raw_paths[raw_path] = index
        for field in ("source_id", "instrument_id", "calibration_reference", "holdout_split_id"):
            value = record.get(field)
            if isinstance(value, str) and (value != value.strip() or has_disallowed_unicode(value)):
                errors.append(f"record {index}: unsafe {field}")
        ident = record.get("source_id")
        if not isinstance(ident, str) or not ident.strip():
            errors.append(f"record {index}: invalid source_id")
        elif ident.strip() in seen:
            errors.append(f"record {index}: duplicate source_id")
        else:
            seen.add(ident.strip())
    return {"status": "BLOCKED" if errors else "PROVENANCE_REVIEW_REQUIRED",
            "record_count": len(records), "errors": errors,
            "scientifically_validated": False}
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    try:
        result = audit(json.loads(args.manifest.read_text(encoding="utf-8")))
    except (OSError, ValueError) as exc:
        result = {"status": "BLOCKED", "record_count": 0, "errors": [str(exc)], "scientifically_validated": False}
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
    return 0 if result["status"] == "PROVENANCE_REVIEW_REQUIRED" else 2
if __name__ == "__main__":
    raise SystemExit(main())
