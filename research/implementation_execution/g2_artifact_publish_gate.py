"""Research-only G2 publication guard: never a scientific qualification."""
import argparse
from collections import Counter
import hashlib
import json
import re
from pathlib import Path
from g2_acquisition_triage import (SOURCE_ACTIONS, CAVEAT, build_triage,
                                   CANONICAL_REGISTRY)

def _strict(path):
    def unique(pairs):
        result = {}
        for k, v in pairs:
            if k in result:
                raise ValueError("G2_PUBLISH_DUPLICATE_KEY")
            result[k] = v
        return result
    def reject(_):
        raise ValueError("G2_PUBLISH_NONFINITE")
    data = Path(path).read_bytes()
    obj = json.loads(data.decode("utf-8"), object_pairs_hook=unique,
                     parse_constant=reject)
    if not isinstance(obj, dict):
        raise ValueError("G2_PUBLISH_NOT_OBJECT")
    return data, obj

def verify_publication(audit_path, triage_path, manifest_path):
    audit_bytes, audit = _strict(audit_path)
    triage_bytes, triage = _strict(triage_path)
    _, manifest = _strict(manifest_path)
    names = {"g2_audit_results.json": audit_bytes,
             "g2_acquisition_triage.json": triage_bytes}
    if (manifest.get("schema") != "hazrob.g2.replay-integrity.v1" or
            manifest.get("scope") != "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL" or
            not isinstance(manifest.get("artifacts"), dict) or
            set(manifest["artifacts"]) != set(names)):
        raise ValueError("G2_PUBLISH_MANIFEST_INVALID")
    for name, raw in names.items():
        item = manifest["artifacts"][name]
        if (not isinstance(item, dict) or item.get("byte_identical") is not True
                or type(item.get("bytes")) is not int or item["bytes"] != len(raw)
                or item.get("sha256") != hashlib.sha256(raw).hexdigest()):
            raise ValueError("G2_PUBLISH_BYTES_MISMATCH:" + name)
    if (audit.get("schema") != "hazrob.g2.audit.v1"
            or audit.get("scope") != "new V4 independent source qualification"
            or triage.get("schema") != "hazrob.g2.acquisition-triage.v1"
            or triage.get("scope") != "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL"
            or not isinstance(triage.get("input_sha256"), dict)
            or triage["input_sha256"].get("audit") !=
               hashlib.sha256(audit_bytes).hexdigest()):
        raise ValueError("G2_PUBLISH_LINEAGE_MISMATCH")
    records, techniques, sources = (audit.get("records"),
                                     triage.get("techniques"), triage.get("sources"))
    if (not isinstance(records, list) or len(records) != 80
            or not isinstance(techniques, list) or len(techniques) != 80
            or not isinstance(sources, list) or len(sources) != 14
            or any(not isinstance(row, dict) or not isinstance(row.get("id"), str)
                   for row in records + techniques + sources)
            or len({row["id"] for row in records}) != 80
            or len({row["id"] for row in techniques}) != 80
            or len({row["id"] for row in sources}) != 14
            or {row["id"] for row in records} != {row["id"] for row in techniques}):
        raise ValueError("G2_PUBLISH_SCOPE_INVALID")
    statuses = {row["id"]: row.get("status") for row in records}
    blocked = sum(status == "G2_BLOCKED" for status in statuses.values())
    candidates = 80 - blocked
    source_blocked = sum(row.get("ready_for_source_review") is False for row in sources)
    expected = ((audit.get("g2_blocked"), blocked),
                (audit.get("g2_pass_candidates"), candidates),
                (triage.get("g2_blocked"), blocked),
                (triage.get("g2_pass_candidates_requiring_review"), candidates),
                (triage.get("external_sources_blocked"), source_blocked),
                (triage.get("techniques_total"), 80),
                (triage.get("external_sources_total"), 14))
    if (any(status not in ("G2_BLOCKED", "G2_PASS_CANDIDATE_REQUIRES_REVIEW")
            for status in statuses.values())
            or any(row.get("status") != statuses[row["id"]] for row in techniques)
            or any(type(row.get("ready_for_source_review")) is not bool for row in sources)
            or any(type(value) is not int or value != expected_value
                   for value, expected_value in expected)):
        raise ValueError("G2_PUBLISH_COUNTS_OR_STATUS_MISMATCH")
    # A self-consistent, rehashed triage must not rewrite the underlying
    # audit's per-technique blockers, nor its blocker-frequency summaries.
    # Publication is a separate trust boundary from triage generation.
    def blockers(row, key):
        values = row.get(key)
        if (not isinstance(values, list)
                or any(not isinstance(x, str) or not x.strip() for x in values)
                or len(values) != len(set(values))):
            raise ValueError("G2_PUBLISH_BLOCKERS_INVALID:" + key)
        return set(values)

    audit_by_id = {row["id"]: row for row in records}
    technique_counts = Counter()
    for row in techniques:
        actual = blockers(row, "blockers")
        original = blockers(audit_by_id[row["id"]], "issues")
        if actual != original or (row["status"] == "G2_BLOCKED") != bool(actual):
            raise ValueError("G2_PUBLISH_TECHNIQUE_BLOCKER_MISMATCH:" + row["id"])
        technique_counts.update(actual)
    source_counts = Counter()
    for row in sources:
        actual = blockers(row, "blockers")
        if bool(actual) == row["ready_for_source_review"]:
            raise ValueError("G2_PUBLISH_SOURCE_BLOCKER_MISMATCH:" + row["id"])
        source_counts.update(actual)
    if (triage.get("technique_blocker_counts") != dict(technique_counts)
            or triage.get("source_blocker_counts") != dict(source_counts)):
        raise ValueError("G2_PUBLISH_BLOCKER_FREQUENCY_MISMATCH")
    # Acquisition instructions are operational evidence-collection guidance.
    # A rehashed report must not silently change or omit them while preserving
    # its source blocker totals. Reject unknown source blocker labels as well.
    if set(source_counts) - set(SOURCE_ACTIONS):
        raise ValueError("G2_PUBLISH_UNKNOWN_SOURCE_BLOCKER")
    expected_actions = [
        {"blocker": key, "affected_sources": source_counts[key],
         "action": SOURCE_ACTIONS[key]}
        for key in SOURCE_ACTIONS if source_counts[key]
    ]
    if triage.get("acquisition_actions") != expected_actions:
        raise ValueError("G2_PUBLISH_ACQUISITION_ACTION_MISMATCH")
    if triage.get("caveat") != CAVEAT:
        raise ValueError("G2_PUBLISH_SCIENTIFIC_CAVEAT_MISMATCH")
    # The manifest hashes the output files; these five digests identify the
    # controlling input bytes. This boundary validates shape and audit binding,
    # not the physical authenticity of the referenced measurements.
    lineage = triage["input_sha256"]
    required = {"audit", "source_matrix", "ledger",
                "independent_evidence", "canonical_registry"}
    if (set(lineage) != required or
            any(not isinstance(value, str) or
                re.fullmatch(r"[0-9a-f]{64}", value) is None
                for value in lineage.values())):
        raise ValueError("G2_PUBLISH_INPUT_DIGEST_SET_INVALID")
    return {"g2_blocked": blocked, "g2_pass_candidates": candidates,
            "source_blocked": source_blocked}


def verify_controlling_evidence(audit_path, triage_path, source_matrix_path,
                                ledger_path, evidence_path):
    """Independently bind published triage to the five controlling input bytes.

    A self-consistent replay manifest and plausible SHA256 strings do not prove
    that the published triage was derived from the actual source/ledger/register.
    The CI publication boundary rechecks all five bytes and recomputes triage.
    This is software provenance only, never a scientific G2/G3 promotion.
    """
    _, triage = _strict(triage_path)
    inputs = {
        "audit": audit_path,
        "source_matrix": source_matrix_path,
        "ledger": ledger_path,
        "independent_evidence": evidence_path,
        "canonical_registry": CANONICAL_REGISTRY,
    }
    actual_digests = {
        name: hashlib.sha256(Path(path).read_bytes()).hexdigest()
        for name, path in inputs.items()
    }
    if triage.get("input_sha256") != actual_digests:
        raise ValueError("G2_PUBLISH_CONTROLLING_INPUT_SHA256_MISMATCH")
    try:
        expected = build_triage(audit_path, source_matrix_path,
                                ledger_path, evidence_path)
    except (ValueError, TypeError, KeyError, OSError, UnicodeError) as exc:
        raise ValueError("G2_PUBLISH_CONTROLLING_EVIDENCE_INVALID") from exc
    if triage != expected:
        raise ValueError("G2_PUBLISH_CONTROLLING_EVIDENCE_REPLAY_MISMATCH")
    return actual_digests

def main():
    parser = argparse.ArgumentParser()
    for flag in ("audit", "triage", "manifest"):
        parser.add_argument("--" + flag, required=True, type=Path)
    parser.add_argument("--source-matrix", required=True, type=Path)
    parser.add_argument("--ledger", required=True, type=Path)
    parser.add_argument("--evidence", required=True, type=Path)
    args = parser.parse_args()
    result = verify_publication(args.audit, args.triage, args.manifest)
    verify_controlling_evidence(args.audit, args.triage, args.source_matrix,
                                args.ledger, args.evidence)
    print("G2 research publication guard PASS:", result)

if __name__ == "__main__":
    main()
