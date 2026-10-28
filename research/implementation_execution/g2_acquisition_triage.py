"""Deterministic G2 evidence-acquisition triage, not scientific qualification.

Reads only the already-audited research outputs. Never upgrades a technique,
infers physical independence, or turns a bibliography into ppm calibration.
"""
import argparse
from collections import Counter
import json
import math
from pathlib import Path

from g2_catalog_link_gate import load_pinned_source_matrix
from g2_evidence_audit import load_canonical_technique_ids

HERE = Path(__file__).resolve().parent
CANONICAL_REGISTRY = HERE.parent / "PROGRESS_80.json"
PINNED_MATRIX = HERE / "G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json"


SOURCE_ACTIONS = {
    "ORIGINAL_RAW_SHA256_NOT_DOCUMENTED": "Acquire original archive bytes; record size and SHA256.",
    "ORIGINAL_RAW_BYTES_NOT_CATALOG_VERIFIED": "Replay original archive bytes and verify SHA256.",
    "INDEPENDENT_PHYSICAL_GROUPS_NOT_VERIFIED": "Document source device/session/exposure groups and holdout isolation.",
    "PHYSICAL_REFERENCE_TRACEABILITY_NOT_VERIFIED": "Obtain concentration reference, instrument ID, units and traceable calibration.",
    "MEASUREMENT_QUALIFICATION_NOT_VERIFIED": "Audit source measurement protocol and provenance independently.",
}


def _unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("DUPLICATE_JSON_KEY")
        result[key] = value
    return result


def _reject_constant(value):
    raise ValueError("NONFINITE_JSON_NUMBER")


def _finite_float(token):
    value = float(token)
    if not math.isfinite(value):
        raise ValueError("NONFINITE_JSON_NUMBER")
    return value


def _finite_integer(token):
    value = int(token)
    try:
        if not math.isfinite(float(value)):
            raise ValueError("NONFINITE_JSON_NUMBER")
    except OverflowError as exc:
        raise ValueError("NONFINITE_JSON_NUMBER") from exc
    return value


def read_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"),
                      object_pairs_hook=_unique_pairs,
                      parse_constant=_reject_constant,
                      parse_float=_finite_float,
                      parse_int=_finite_integer)


def build_triage(audit_path, source_matrix_path):
    """Require the exact pinned source bytes, not a substituted 14-row matrix."""
    load_pinned_source_matrix(source_matrix_path)
    return make_triage(read_json(audit_path), read_json(source_matrix_path))


def make_triage(audit, matrix):
    """Fail closed on inconsistent counts and IDs; report blockers, never passes."""
    if (not isinstance(audit, dict) or
            audit.get("schema") != "hazrob.g2.audit.v1" or
            not isinstance(audit.get("records"), list) or
            len(audit["records"]) != 80 or
            not isinstance(matrix, dict) or
            matrix.get("schema") != "hazrob.g2.central-catalog-readiness.v1" or
            not isinstance(matrix.get("datasets"), list) or
            len(matrix["datasets"]) != 14 or
            audit.get("scope") != "new V4 independent source qualification" or
            matrix.get("scope") != "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL"):
        raise ValueError("G2_TRIAGE_INPUT_SCHEMA_OR_SCOPE_INVALID")
    # A plausible 80-by-14 summary is not evidence of the canonical 80
    # techniques or the reviewed central source inventory.
    canonical_ids = set(load_canonical_technique_ids(CANONICAL_REGISTRY))
    pinned_sources = set(load_pinned_source_matrix(PINNED_MATRIX))
    if matrix != read_json(PINNED_MATRIX):
        raise ValueError("G2_TRIAGE_PINNED_MATRIX_CONTENT_MISMATCH")
    techniques = audit["records"]
    sources = matrix["datasets"]
    if (any(not isinstance(r, dict) or not isinstance(r.get("id"), str) or
            not isinstance(r.get("issues"), list) or
            any(not isinstance(x, str) or not x.strip() for x in r["issues"]) or
            r.get("status") not in ("G2_BLOCKED", "G2_PASS_CANDIDATE_REQUIRES_REVIEW") or
            ((r["status"] == "G2_BLOCKED") != bool(r["issues"]))
            for r in techniques) or
            len({r["id"] for r in techniques}) != 80 or
            {r["id"] for r in techniques} != canonical_ids or
            any(not isinstance(r, dict) or not isinstance(r.get("id"), str) or
                type(r.get("catalog_source_review_ready")) is not bool or
                not isinstance(r.get("blockers"), list) or
                any(not isinstance(x, str) or not x.strip() for x in r["blockers"]) or
                r["catalog_source_review_ready"] == bool(r["blockers"])
                for r in sources) or
            len({r["id"] for r in sources}) != 14 or
            {r["id"] for r in sources} != pinned_sources):
        raise ValueError("G2_TRIAGE_RECORDS_INVALID")
    blocked = sum(r["status"] == "G2_BLOCKED" for r in techniques)
    candidates = 80 - blocked
    source_blocked = sum(not r["catalog_source_review_ready"] for r in sources)
    if (type(audit.get("g2_blocked")) is not int or
            type(audit.get("g2_pass_candidates")) is not int or
            audit["g2_blocked"] != blocked or
            audit["g2_pass_candidates"] != candidates or
            type(matrix.get("source_review_ready")) is not int or
            type(matrix.get("source_review_blocked")) is not int or
            matrix["source_review_ready"] != 14 - source_blocked or
            matrix["source_review_blocked"] != source_blocked):
        raise ValueError("G2_TRIAGE_COUNT_MISMATCH")
    source_counts = Counter(b for r in sources for b in set(r["blockers"]))
    unknown = set(source_counts) - set(SOURCE_ACTIONS)
    if unknown:
        raise ValueError("G2_TRIAGE_UNKNOWN_SOURCE_BLOCKERS:" + ",".join(sorted(unknown)))
    technique_counts = Counter(b for r in techniques for b in set(r["issues"]))
    return {
        "schema": "hazrob.g2.acquisition-triage.v1",
        "scope": "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL",
        "techniques_total": 80,
        "g2_blocked": blocked,
        "g2_pass_candidates_requiring_review": candidates,
        "external_sources_total": 14,
        "external_sources_blocked": source_blocked,
        "source_blocker_counts": dict(sorted(source_counts.items())),
        "technique_blocker_counts": dict(sorted(technique_counts.items())),
        "acquisition_actions": [
            {"blocker": key, "affected_sources": source_counts[key],
             "action": SOURCE_ACTIONS[key]}
            for key in SOURCE_ACTIONS if source_counts[key]
        ],
        "sources": [
            {"id": r["id"], "ready_for_source_review": r["catalog_source_review_ready"],
             "blockers": sorted(set(r["blockers"]))}
            for r in sorted(sources, key=lambda x: x["id"])
        ],
        "techniques": [
            {"id": r["id"], "status": r["status"], "blockers": sorted(set(r["issues"]))}
            for r in sorted(techniques, key=lambda x: x["id"])
        ],
        "caveat": "No software test or source inventory proves independent physical exposures, traceable ppm or Tank MQ8 G3.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--audit", required=True, type=Path)
    parser.add_argument("--source-matrix", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    result = build_triage(args.audit, args.source_matrix)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                           encoding="utf-8")
    print("g2_blocked={g2_blocked} source_blocked={external_sources_blocked}".format(**result))


if __name__ == "__main__":
    main()
