"""Reconcile the G2 technique evidence gate with the pinned central source matrix.

The central catalogue is a private cross-project reference library. Its
research-only, non-sensitive readiness projection is pinned by Git blob SHA
inside Tank; do not copy the full private source catalogue into this repo.

A source-ready entry is NEVER a scientific G2 pass. The highest possible
outcome from this module is leaving an independently audited technique as
a candidate requiring human review.
"""
import json
import re
from pathlib import Path

from g2_reference_catalog_readiness import PINNED_CATALOG_BLOB, git_blob_sha

PINNED_SOURCE_MATRIX_BLOB = "bd69304fc35175b90199c6cc6ad4a88fc10ae93f"
SOURCE_ID = re.compile(r"MQ-EXT-[0-9]{2}\Z")
SOURCE_PREFIX = re.compile(r"MQ-EXT-", re.IGNORECASE)


def _unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("G2_SOURCE_MATRIX_DUPLICATE_JSON_KEY")
        result[key] = value
    return result


def _reject_constant(value):
    raise ValueError("G2_SOURCE_MATRIX_NONFINITE_NUMBER:" + value)


def load_pinned_source_matrix(path):
    """Validate immutable source-readiness bytes and all 14 source rows.

    A checksum of the public derived matrix binds it to the reviewed source
    inventory without publishing the private 80-entry reference catalogue.
    Re-pinning requires a new source audit, not an arbitrary flag edit.
    """
    raw = Path(path).read_bytes()
    if git_blob_sha(raw) != PINNED_SOURCE_MATRIX_BLOB:
        raise ValueError("G2_SOURCE_MATRIX_GIT_BLOB_MISMATCH")
    try:
        matrix = json.loads(raw.decode("utf-8"),
                            object_pairs_hook=_unique_object,
                            parse_constant=_reject_constant)
    except (UnicodeError, ValueError) as exc:
        raise ValueError("G2_SOURCE_MATRIX_JSON_INVALID") from exc
    if (not isinstance(matrix, dict) or
            matrix.get("schema") != "hazrob.g2.central-catalog-readiness.v1" or
            matrix.get("scope") != "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL" or
            matrix.get("source_git_blob_sha1") != PINNED_CATALOG_BLOB or
            type(matrix.get("catalog_entries")) is not int or
            matrix["catalog_entries"] != 80 or
            type(matrix.get("external_measurement_datasets")) is not int or
            matrix["external_measurement_datasets"] != 14 or
            not isinstance(matrix.get("datasets"), list) or
            len(matrix["datasets"]) != 14):
        raise ValueError("G2_SOURCE_MATRIX_SCHEMA_OR_CATALOG_BINDING_INVALID")
    by_id = {}
    for row in matrix["datasets"]:
        if not isinstance(row, dict):
            raise ValueError("G2_SOURCE_MATRIX_DATASET_ROW_INVALID")
        sid = row.get("id")
        ready = row.get("catalog_source_review_ready")
        blockers = row.get("blockers")
        if (not isinstance(sid, str) or not SOURCE_ID.fullmatch(sid) or
                sid in by_id or type(ready) is not bool or
                not isinstance(blockers, list) or
                any(not isinstance(issue, str) or not issue for issue in blockers) or
                (ready and blockers) or (not ready and not blockers)):
            raise ValueError("G2_SOURCE_MATRIX_DATASET_ROW_INVALID")
        by_id[sid] = row
    ready_count = sum(row["catalog_source_review_ready"] for row in by_id.values())
    if (type(matrix.get("source_review_ready")) is not int or
            matrix["source_review_ready"] != ready_count or
            type(matrix.get("source_review_blocked")) is not int or
            matrix["source_review_blocked"] != 14 - ready_count):
        raise ValueError("G2_SOURCE_MATRIX_COUNTS_INVALID")
    return by_id


def reconcile_catalog_source_evidence(results, evidence_records, source_matrix):
    """Fail closed for known central dataset bindings; do not grant G2.

    A record with dataset_id=MQ-EXT-NN, or an explicit catalog_source_id,
    must not be a review candidate while that source is unqualified.
    Duplicate evidence records are all examined so ordering cannot conceal
    a blocked source. Non-catalogue original Tank experiments remain governed
    by their separate raw-byte, physical-group and reference-traceability gates.
    """
    by_technique = {}
    for record in evidence_records:
        if isinstance(record, dict) and isinstance(record.get("id"), str):
            by_technique.setdefault(record["id"], []).append(record)
    output = []
    for result in results:
        row = {**result, "issues": list(result["issues"])}
        for evidence in by_technique.get(row["id"], []):
            dataset_id = evidence.get("dataset_id")
            has_explicit = "catalog_source_id" in evidence
            declared = evidence.get("catalog_source_id")
            known_prefix = (isinstance(dataset_id, str) and
                            SOURCE_PREFIX.match(dataset_id) is not None)
            if not known_prefix and not has_explicit:
                continue
            sid = declared if has_explicit else dataset_id
            if not isinstance(sid, str) or not SOURCE_ID.fullmatch(sid):
                row["issues"].append("CATALOG_SOURCE_BINDING_INVALID")
                continue
            if known_prefix and dataset_id != sid:
                row["issues"].append("CATALOG_SOURCE_ID_CONFLICT")
            if sid not in source_matrix:
                row["issues"].append("CATALOG_SOURCE_NOT_IN_PINNED_MATRIX:" + sid)
            elif source_matrix[sid]["catalog_source_review_ready"] is not True:
                row["issues"].append("CATALOG_SOURCE_NOT_REVIEW_READY:" + sid)
        if row["issues"]:
            row["issues"] = sorted(set(row["issues"]))
            row["status"] = "G2_BLOCKED"
        output.append(row)
    return output
