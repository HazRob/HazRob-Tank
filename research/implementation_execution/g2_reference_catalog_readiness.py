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
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit

PINNED_CATALOG_BLOB = "1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f"
DATASET_CATEGORY = "EXTERNAL_MEASUREMENT_DATASET"
HEX64 = re.compile(r"^[0-9a-f]{64}$")
CANONICAL_ID = re.compile(r"^[A-Z0-9]+(?:-[A-Z0-9]+)*$")
CANONICAL_CATEGORY = re.compile(r"^[A-Z][A-Z0-9_]*$")


def git_blob_sha(data):
    """Compute the Git blob identity of the *exact* UTF-8 source bytes."""
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") +
                        b"\x00" + data).hexdigest()



def _replay_catalog_raw_bytes(row, repo_root):
    """Verify source archive bytes, not a catalogue's 'verified' assertion.

    This is still only a local byte replay: no authentication of the external
    publisher, physical standard, or independent exposure groups is implied.
    """
    raw_path = row.get("raw_local_path")
    size = row.get("raw_size_bytes")
    if (not isinstance(raw_path, str) or not raw_path or
            "\\" in raw_path or raw_path.startswith("/") or
            any(part in ("", ".", "..") for part in raw_path.split("/")) or
            PurePosixPath(raw_path).parts[:3] != ("research", "g2", "raw") or
            len(PurePosixPath(raw_path).parts) < 4):
        return "ORIGINAL_RAW_LOCAL_PATH_MISSING_OR_UNSAFE"
    if type(size) is not int or size < 1:
        return "ORIGINAL_RAW_BYTE_SIZE_NOT_DOCUMENTED"
    root = Path(repo_root).resolve()
    raw_root = root / "research" / "g2" / "raw"
    path = root.joinpath(*PurePosixPath(raw_path).parts)
    try:
        resolved = path.resolve(strict=True)
        resolved.relative_to(raw_root)
        if resolved != path.absolute() or not resolved.is_file():
            raise ValueError("non-regular or symlinked archive")
        if resolved.stat().st_size != size:
            return "ORIGINAL_RAW_BYTE_SIZE_MISMATCH"
        digest = hashlib.sha256()
        bytes_hashed = 0
        with resolved.open("rb") as stream:
            if stream.read(64).startswith(b"version https://git-lfs.github.com/spec/v1"):
                return "ORIGINAL_RAW_LFS_POINTER_NOT_MATERIALIZED"
            stream.seek(0)
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                bytes_hashed += len(chunk)
                digest.update(chunk)
    except (OSError, ValueError):
        return "ORIGINAL_RAW_BYTES_UNAVAILABLE_OR_UNSAFE"
    if bytes_hashed != size:
        return "ORIGINAL_RAW_BYTE_SIZE_MISMATCH"
    if digest.hexdigest() != row.get("raw_sha256"):
        return "ORIGINAL_RAW_SHA256_REPLAY_MISMATCH"
    return None


def _canonical_source_origin(row):
    """Conservatively identify a DOI or an HTTP(S) landing page.

    DOI resolver aliases and percent-encoded DOI paths must not split one
    dataset into artificial independent sources. For ordinary URLs, preserve
    case-sensitive paths and meaningful query parameters: only the host,
    default port, scheme, and trailing slash are normalized. This remains a
    source-review heuristic, not proof of shared physical exposures.
    """
    doi = row.get("doi")
    url = row.get("primary_url")
    value = doi if isinstance(doi, str) and doi.strip() else url
    if not isinstance(value, str) or not value.strip():
        return None
    value = value.strip()
    if value.lower().startswith("doi:"):
        value = value[4:].strip()
    if value.lower().startswith(("https://", "http://")):
        parsed = urlsplit(value)
        host = (parsed.hostname or "").lower().rstrip(".")
        if host in {"doi.org", "dx.doi.org", "www.doi.org", "www.dx.doi.org"}:
            candidate = unquote(parsed.path).strip("/ ")
            if re.fullmatch(r"10\.\d{4,9}/\S+", candidate, re.I):
                return "doi:" + candidate.lower()
        try:
            port = parsed.port
        except ValueError:
            return "url:" + value
        netloc = host + (":" + str(port) if port not in (None, 80, 443) else "")
        path = parsed.path.rstrip("/") or "/"
        return "url:" + netloc + path + ("?" + parsed.query if parsed.query else "")
    candidate = unquote(value).rstrip("/")
    if re.fullmatch(r"10\.\d{4,9}/\S+", candidate, re.I):
        return "doi:" + candidate.lower()
    return "url:" + value.lower().rstrip("/")


def _canonical_source_origins(row):
    """All stated origin identities, not DOI *instead of* landing URL.

    Each source contributes a set so DOI+URL aliases within one record cannot
    create a spurious duplicate. Shared DOI or shared landing page across
    *different* records is a conservative source-review blocker.
    """
    origins = set()
    for field in ("doi", "primary_url"):
        value = row.get(field)
        if isinstance(value, str) and value.strip():
            identity = _canonical_source_origin({field: value})
            if identity is not None:
                origins.add(identity)
    return origins


def audit_source_catalog(catalog, expected_total=80, expected_datasets=14, repo_root=Path(".")):
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
    # Classify the *entire* duplicate equivalence class as ambiguous.
    # Otherwise the first catalogue ID stays "ready" merely because of
    # lexical ordering, while the same physical archive is duplicated later.
    origin_counts = Counter(
        origin for row in datasets for origin in _canonical_source_origins(row))
    raw_hash_counts = Counter(
        row["raw_sha256"] for row in datasets
        if isinstance(row.get("raw_sha256"), str)
        and HEX64.fullmatch(row["raw_sha256"]))
    results = []
    for row in sorted(datasets, key=lambda entry: entry["id"]):
        blockers = []
        # A duplicate DOI/landing page or identical original raw bytes
        # blocks every member pending source-level provenance adjudication.
        # Distinct identifiers never establish independent physical exposure.
        origins = _canonical_source_origins(row)
        if any(origin_counts[origin] > 1 for origin in origins):
            blockers.append("DUPLICATE_ORIGINAL_DATASET_ORIGIN")
        raw_digest = row.get("raw_sha256")
        if (isinstance(raw_digest, str) and HEX64.fullmatch(raw_digest)
                and raw_hash_counts[raw_digest] > 1):
            blockers.append("DUPLICATE_ORIGINAL_RAW_BYTES")
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
        # A syntactically valid SHA256 and a self-declared 'verified' flag
        # must never mark a source ready without replaying the archive bytes.
        # Avoid needless I/O for already-blocked catalogue entries.
        if not blockers:
            replay_issue = _replay_catalog_raw_bytes(row, repo_root)
            if replay_issue:
                blockers.append(replay_issue)
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
