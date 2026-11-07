"""Read-only reconciliation of pinned G2 source receipt claims.

This audits metadata disagreement, not original bytes, independent exposures,
gas identity, concentration, technique G2, or Tank G3.
"""
import argparse
import json
import re
from pathlib import Path

KIND = "EXTERNAL_MEASUREMENT_DATASET"
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def strict_json(path):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("DUPLICATE_JSON_KEY:" + key)
            result[key] = value
        return result

    def no_constant(token):
        raise ValueError("NONFINITE_JSON_VALUE:" + token)

    return json.loads(Path(path).read_text(encoding="utf-8"),
                      object_pairs_hook=unique, parse_constant=no_constant)


def by_id(rows, label):
    if not isinstance(rows, list):
        raise ValueError(label + "_ROWS_INVALID")
    result = {}
    for row in rows:
        if (not isinstance(row, dict) or not isinstance(row.get("id"), str)
                or not row["id"] or row["id"] in result):
            raise ValueError(label + "_ROW_OR_ID_INVALID")
        result[row["id"]] = row
    return result


def reconcile(readiness, candidates):
    """Report stale receipt claims; never promote a readiness flag."""
    if (not isinstance(readiness, dict) or not isinstance(candidates, dict)
            or readiness.get("schema") != "hazrob.g2.central-catalog-readiness.v1"
            or candidates.get("schema") != "hazrob.g2.source-candidates.v1"
            or readiness.get("scope") != "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL"
            or readiness.get("source_git_blob_sha1") != candidates.get("catalog_blob_sha")
            or not isinstance(readiness.get("source_git_blob_sha1"), str)
            or not re.fullmatch(r"[0-9a-f]{40}", readiness["source_git_blob_sha1"])):
        raise ValueError("SNAPSHOT_SCHEMA_OR_CATALOG_IDENTITY_MISMATCH")
    old = by_id(readiness.get("datasets"), "READINESS")
    all_candidates = by_id(candidates.get("candidates"), "CANDIDATES")
    new = {key: row for key, row in all_candidates.items()
           if row.get("category") == KIND}
    if (len(old) != 14 or len(new) != 14 or set(old) != set(new)
            or type(readiness.get("external_measurement_datasets")) is not int
            or readiness["external_measurement_datasets"] != 14):
        raise ValueError("EXTERNAL_DATASET_ID_SET_MISMATCH")
    issues = []
    for sid in sorted(old):
        before, after = old[sid], new[sid]
        if (before.get("catalog_source_review_ready") is True
                or after.get("measurement_qualification") is True):
            raise ValueError("SOURCE_QUALIFICATION_CHANGE_REQUIRES_INDEPENDENT_AUDIT:" + sid)
        digest = after.get("source_sha256")
        if digest is not None and (not isinstance(digest, str)
                                   or not HEX64.fullmatch(digest)):
            raise ValueError("CANDIDATE_SOURCE_DIGEST_INVALID:" + sid)
        claimed = after.get("raw_file_status") == "ORIGINAL_ARCHIVE_SHA256_AND_PUBLISHER_MD5_VERIFIED"
        if claimed and not digest:
            raise ValueError("CLAIMED_ORIGINAL_ARCHIVE_WITHOUT_SHA256:" + sid)
        previous_status = before.get("source_status")
        previous_raw = before.get("raw_file_status")
        if claimed and (previous_raw != "RAW_BYTES_SHA256_VERIFIED"
                        or previous_status in ("archive_listed_not_downloaded",
                                               "metadata_verified_raw_not_downloaded")):
            issues.append({
                "dataset_id": sid,
                "issue": "REPORTED_ORIGINAL_ARCHIVE_RECEIPT_NOT_REFLECTED_IN_PINNED_READINESS",
                "readiness_source_status": previous_status,
                "readiness_raw_status": previous_raw,
                "candidate_raw_status": after["raw_file_status"],
                "candidate_claimed_sha256": digest,
                "candidate_claimed_ci_run_id": after.get("verified_raw_source_ci_run_id"),
                "effect": "SOURCE_REVIEW_REMAINS_BLOCKED_PENDING_RAW_REPLAY_AND_NEW_PINNED_CATALOG_AUDIT",
            })
    return {
        "schema": "hazrob.g2.source-snapshot-drift.v1",
        "scope": "SOURCE_CLAIMS_ONLY_NO_G2_OR_G3_PROMOTION",
        "pinned_catalog_blob_sha1": readiness["source_git_blob_sha1"],
        "external_datasets_reconciled": len(old),
        "discrepancy_count": len(issues),
        "discrepancies": issues,
        "g2_qualified_by_this_audit": 0,
        "g3_qualified_by_this_audit": 0,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--readiness", default="research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json")
    ap.add_argument("--candidates", default="research/implementation_execution/G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json")
    ap.add_argument("--output", required=True)
    args = ap.parse_args()
    result = reconcile(strict_json(args.readiness), strict_json(args.candidates))
    Path(args.output).write_text(json.dumps(result, indent=2, ensure_ascii=False,
                                           sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"datasets": result["external_datasets_reconciled"],
                      "discrepancies": result["discrepancy_count"],
                      "g2_qualified": 0}))


if __name__ == "__main__":
    main()
