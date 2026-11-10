#!/usr/bin/env python3
"""Fail-closed G2 independent-evidence manifest audit (Python stdlib only).

Usage: python3 research/implementation_execution/audit_g2_independent_evidence.py
Optional: --manifest PATH --json
This audit checks provenance completeness, not scientific validity or sensor accuracy.
"""
import argparse
import json
from pathlib import Path

DEFAULT = Path(__file__).with_name("G2_INDEPENDENT_EVIDENCE.json")
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
    for index, record in enumerate(records):
        if not isinstance(record, dict):
            errors.append(f"record {index}: expected object")
            continue
        missing = [k for k in REQUIRED if not isinstance(record.get(k), str) or not record[k].strip()]
        if missing:
            errors.append(f"record {index}: missing provenance: {','.join(missing)}")
        sha = record.get("raw_sha256", "")
        if not isinstance(sha, str) or len(sha) != 64 or any(c not in "0123456789abcdefABCDEF" for c in sha):
            errors.append(f"record {index}: invalid raw_sha256")
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
