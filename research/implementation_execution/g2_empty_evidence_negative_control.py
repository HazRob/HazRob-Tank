#!/usr/bin/env python3
"""Research-only negative control for independent G2 evidence.

Run: python3 research/implementation_execution/g2_empty_evidence_negative_control.py
Presence of records is never equivalent to scientific qualification.
"""
import json
from pathlib import Path

EVIDENCE = Path(__file__).with_name("G2_INDEPENDENT_EVIDENCE.json")
SCHEMA = "hazrob.g2.independent-evidence.v1"


def reject_nested_approval_assertions(value):
    """Prevent approval claims hidden inside nested raw-evidence objects."""
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ("g2_approved", "g3_approved"):
                raise ValueError(f"Forbidden approval assertion: {key}")
            reject_nested_approval_assertions(child)
    elif isinstance(value, list):
        for child in value:
            reject_nested_approval_assertions(child)


def has_independent_records(payload):
    if not isinstance(payload, dict):
        raise ValueError("Evidence register must be a JSON object")
    if payload.get("schema") != SCHEMA:
        raise ValueError("Unexpected evidence schema")
    reject_nested_approval_assertions(payload)
    records = payload.get("records")
    if not isinstance(records, list):
        raise ValueError("Evidence records must be a list")
    return len(records) > 0


def reject_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate evidence JSON key: {key}")
        result[key] = value
    return result


def load_register(path=EVIDENCE):
    return json.loads(
        Path(path).read_text(encoding="utf-8"),
        object_pairs_hook=reject_duplicate_keys,
        parse_constant=lambda value: (_ for _ in ()).throw(ValueError(value)),
    )


def main():
    assert not has_independent_records({"schema": SCHEMA, "records": []})
    payload = load_register()
    if has_independent_records(payload):
        print("Records present; provenance, independence, and holdout qualification still required; no G2 approval")
    else:
        print("PASS: empty independent evidence register; scientific G2 promotion blocked")


if __name__ == "__main__":
    main()
