#!/usr/bin/env python3
"""Fail-closed inventory of independent G2 evidence. Stdlib only.

Usage: python3 research/implementation_execution/g2_evidence_inventory.py
       python3 research/implementation_execution/g2_evidence_inventory.py --evidence path.json
Exit 0 = inventory successfully produced (NOT G2 approval).
Exit 2 = malformed evidence / unsafe record; exit 3 = unreadable input.
"""
import argparse
import json
import math
import pathlib
import sys

SCHEMA = "hazrob.g2.independent-evidence.v1"


def unique_keys(pairs):
    """Reject ambiguous JSON objects instead of silently retaining the last key."""
    obj = {}
    for key, value in pairs:
        if key in obj:
            raise ValueError("duplicate JSON object key: " + key)
        obj[key] = value
    return obj

def finite_float(value):
    """Reject exponent-overflow numbers that json.loads otherwise turns into infinity."""
    number = float(value)
    if not math.isfinite(number):
        raise ValueError("non-finite JSON numeric literal: " + value)
    return number


def inspect(path):
    try:
        data = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_keys, parse_float=finite_float,
                          parse_constant=lambda value: (_ for _ in ()).throw(ValueError("non-finite JSON constant: " + value)))
    except (OSError, UnicodeError, ValueError) as exc:
        print(json.dumps({"status": "INPUT_ERROR", "detail": str(exc)}, ensure_ascii=False))
        return 3
    if not isinstance(data, dict) or data.get("schema") != SCHEMA or not isinstance(data.get("records"), list):
        print(json.dumps({"status": "INVALID_SCHEMA", "expected": SCHEMA}))
        return 2
    records = data["records"]
    if any(not isinstance(row, dict) for row in records):
        print(json.dumps({"status": "INVALID_RECORD", "reason": "records must be objects"}))
        return 2
    # Presence is not authenticity: acceptance requires separate provenance,
    # leakage, checksum, calibration and blind-holdout gates.
    print(json.dumps({
        "schema": SCHEMA,
        "record_count": len(records),
        "independent_evidence_present": bool(records),
        "g2_approved": False,
        "status": "NO_INDEPENDENT_RECORDS" if not records else "RECORDS_PRESENT_NOT_VALIDATED",
        "required_next_gate": "physical intake, provenance, raw SHA-256, group separation, blind holdout and external replication"
    }, ensure_ascii=False, sort_keys=True))
    return 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=pathlib.Path,
                        default=pathlib.Path(__file__).with_name("G2_INDEPENDENT_EVIDENCE.json"))
    args = parser.parse_args()
    sys.exit(inspect(args.evidence))
