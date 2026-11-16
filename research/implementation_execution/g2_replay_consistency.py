"""Byte-for-byte replay gate for G2 research artifacts; no scientific approval.

Two separately executed audit+triage passes must produce identical output bytes.
The manifest records artifact digests but does not establish physical authenticity.
"""
import argparse
import hashlib
import json
from pathlib import Path


def _match(first, second, label):
    original = Path(first).read_bytes()
    replay = Path(second).read_bytes()
    if original != replay:
        raise ValueError("G2_REPLAY_BYTES_MISMATCH:" + label)
    return {"bytes": len(original), "sha256": hashlib.sha256(original).hexdigest(),
            "byte_identical": True}


def verify_replay(audit_first, audit_second, triage_first, triage_second):
    """Reject differences in either G2 artifact, including whitespace or ordering."""
    return {
        "schema": "hazrob.g2.replay-integrity.v1",
        "scope": "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL",
        "artifacts": {
            "g2_audit_results.json": _match(audit_first, audit_second, "audit"),
            "g2_acquisition_triage.json": _match(triage_first, triage_second, "triage"),
        },
        "caveat": "Identical software replays do not prove original physical measurements or traceable ppm.",
    }


def main():
    parser = argparse.ArgumentParser()
    for flag in ("audit-first", "audit-second", "triage-first", "triage-second", "output"):
        parser.add_argument("--" + flag, required=True, type=Path)
    args = parser.parse_args()
    result = verify_replay(args.audit_first, args.audit_second,
                           args.triage_first, args.triage_second)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n",
                           encoding="utf-8")
    print("G2 byte-identical audit and triage replays verified (research only).")


if __name__ == "__main__":
    main()
