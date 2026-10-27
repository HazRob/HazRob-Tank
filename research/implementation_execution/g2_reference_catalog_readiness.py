"""Deterministic source-readiness inventory for the central HazRob reference bank.

This is a *catalogue* audit, not G2 evidence qualification. An externally
measured dataset can be useful for research while still lacking independent
physical-group, raw-byte, and reference-traceability evidence for G2.
"""
import argparse
import hashlib
import json
import math
import re
from collections import Counter
from pathlib import Path

PINNED_CATALOG_BLOB = "1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f"
DATASET_CATEGORY = "EXTERNAL_MEASUREMENT_DATASET"
HEX64 = re.compile(r"^[0-9a-f]{64}$")
CANONICAL_ID = re.compile(r"^[A-Z0-9]+(?:-[A-Z0-9]+)*$")
CANONICAL_CATEGORY = re.compile(r"^[A-Z][A-Z0-9_]*$")


def git_blob_sha(data):
    """Compute the Git blob identity of the *exact* UTF-8 source bytes."""
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") +
                        b"\x00" + data).hexdigest()


def audit_source_catalog(catalog, expected_total=80, expected_datasets=14):
    """Return a source-only review matrix; never G2 technique pass decisions.

    Strict booleans are essential: strings such as 'claim_not_verified' are
    truthy in Python but do not establish independent measured evidence.
    """
    if (not isinstance(catalog, dict) or
            not isinstance(catalog.get("primary_catalog_entries"), list) or
            not isinstance(catalog.get("counts_by_type"), dict)):
        raise ValueError("CENTRAL_CATALOG_SCHEMA_INVALID")
    entries = catalog["primary_catalog_entries"]
    if (len(entries) != expected_total or
            type(catalog.get("record_count")) is not int or
            catalog["record_count"] != expected_total or
            any(not isinstance(row, dict) or
                not isinstance(row.get("id"), str) or
                not CANONICAL_ID.fullmatch(row["id"]) or
                not isinstance(row.get("category"), str) or
                not CANONICAL_CATEGORY.fullmatch(row["category"]) for row in entries)):
        raise ValueError("CENTRAL_CATALOG_ENTRY_COUNT_OR_SCHEMA_INVALID")
    # The declared category inventory is part of source provenance. A correct
    # external-dataset count cannot conceal a changed non-dataset category or
    # an invented/omitted category. JSON 14.0 and true are not integer counts.
    declared = catalog["counts_by_type"]
    actual = dict(Counter(row["category"] for row in entries))
    if (any(not isinstance(key, str) or
            not CANONICAL_CATEGORY.fullmatch(key) or
            type(value) is not int or value < 0
            for key, value in declared.items()) or declared != actual):
        raise ValueError("CENTRAL_CATALOG_CATEGORY_INVENTORY_MISMATCH")
    ids = [row["id"] for row in entries]
    if len(set(ids)) != len(ids):
        raise ValueError("CENTRAL_CATALOG_DUPLICATE_ID")
    datasets = [row for row in entries if row["category"] == DATASET_CATEGORY]
    if (len(datasets) != expected_datasets or
            catalog["counts_by_type"].get(DATASET_CATEGORY) != expected_datasets):
        raise ValueError("CENTRAL_CATALOG_DATASET_COUNT_MISMATCH")
    results = []
    for row in sorted(datasets, key=lambda entry: entry["id"]):
        blockers = []
        if row.get("measurement_qualification") is not True:
            blockers.append("MEASUREMENT_QUALIFICATION_NOT_VERIFIED")
        if row.get("experiment_independence") != "PHYSICAL_GROUPS_VERIFIED":
            blockers.append("INDEPENDENT_PHYSICAL_GROUPS_NOT_VERIFIED")
        if row.get("raw_file_status") != "RAW_BYTES_SHA256_VERIFIED":
            blockers.append("ORIGINAL_RAW_BYTES_NOT_CATALOG_VERIFIED")
        # A source-archive SHA must be a real SHA256 of the original bytes;
        # a Git mirror blob SHA1, DOI, or declared 'verified' flag is not it.
        if not isinstance(row.get("raw_sha256"), str) or not HEX64.fullmatch(row["raw_sha256"]):
            blockers.append("ORIGINAL_RAW_SHA256_NOT_DOCUMENTED")
        if row.get("physical_reference_traceability") is not True:
            blockers.append("PHYSICAL_REFERENCE_TRACEABILITY_NOT_VERIFIED")
        results.append({
            "id": row["id"],
            "title": row.get("title"),
            "source_status": row.get("source_status"),
            "raw_file_status": row.get("raw_file_status"),
            "experiment_independence": row.get("experiment_independence"),
            "measurement_qualification": row.get("measurement_qualification"),
            "catalog_source_review_ready": not blockers,
            "blockers": blockers,
            "notes": "CATALOG_ONLY; NOT A G2 PASS OR TANK G3 VALIDATION",
        })
    return {
        "schema": "hazrob.g2.central-catalog-readiness.v1",
        "scope": "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL",
        "catalog_entries": len(entries),
        "external_measurement_datasets": len(datasets),
        "source_review_ready": sum(row["catalog_source_review_ready"] for row in results),
        "source_review_blocked": sum(not row["catalog_source_review_ready"] for row in results),
        "datasets": results,
    }


def _unique_object_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("CENTRAL_CATALOG_DUPLICATE_JSON_KEY")
        result[key] = value
    return result


def _reject_nonfinite(token):
    raise ValueError("CENTRAL_CATALOG_NONFINITE_JSON_NUMBER")


def _finite_json_float(token):
    value = float(token)
    if not math.isfinite(value):
        raise ValueError("CENTRAL_CATALOG_NONFINITE_JSON_NUMBER")
    return value


def audit_catalog_bytes(data, expected_blob=PINNED_CATALOG_BLOB):
    if git_blob_sha(data) != expected_blob:
        raise ValueError("CENTRAL_CATALOG_GIT_BLOB_MISMATCH")
    try:
        catalog = json.loads(data.decode("utf-8"),
                             object_pairs_hook=_unique_object_pairs,
                             parse_constant=_reject_nonfinite,
                             parse_float=_finite_json_float)
    except (UnicodeError, ValueError) as exc:
        raise ValueError("CENTRAL_CATALOG_JSON_INVALID") from exc
    result = audit_source_catalog(catalog)
    result["source_git_blob_sha1"] = expected_blob
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--expected-blob", default=PINNED_CATALOG_BLOB)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = audit_catalog_bytes(args.catalog.read_bytes(), args.expected_blob)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print("catalog_source_review_ready={source_review_ready} "
          "catalog_source_review_blocked={source_review_blocked}".format(**result))


if __name__ == "__main__":
    main()
