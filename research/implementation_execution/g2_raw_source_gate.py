"""Bind a G2 evidence claim to the original raw archive bytes.

A declared checksum and "verified_from_bytes" flag are not verification.
This gate requires a locally replayable immutable raw archive. It proves
byte-level integrity only, not gas identity, calibration or independence.
"""
import hashlib
import json
from pathlib import Path, PurePosixPath


def audit_raw_source_bytes(record, repo_root=Path(".")):
    """Fail closed if a G2 candidate cannot reproduce its raw-data SHA256.

    raw_manifest.json must contain raw_local_path (under research/g2/raw),
    raw_size_bytes, raw_sha256 and dataset_uri. Multi-file sources should be
    archived without changing the originals and pinned as a single file.
    """
    root = Path(repo_root).resolve()
    base = (root / "research" / "g2" / "evidence" /
            record["id"] / record["dataset_id"] / record["run_id"])
    try:
        manifest = json.loads((base / "raw_manifest.json").read_text(encoding="utf-8"))
    except (OSError, UnicodeError, ValueError, TypeError):
        return ["RAW_SOURCE_MANIFEST_UNREADABLE"]
    raw_path = manifest.get("raw_local_path") if isinstance(manifest, dict) else None
    size = manifest.get("raw_size_bytes") if isinstance(manifest, dict) else None
    if (not isinstance(raw_path, str) or not raw_path or "\\" in raw_path or
            raw_path.startswith("/") or
            any(part in ("", ".", "..") for part in raw_path.split("/")) or
            PurePosixPath(raw_path).parts[:3] != ("research", "g2", "raw") or
            len(PurePosixPath(raw_path).parts) < 4):
        return ["RAW_SOURCE_LOCAL_PATH_MISSING_OR_UNSAFE"]
    if isinstance(size, bool) or not isinstance(size, int) or size < 1:
        return ["RAW_SOURCE_SIZE_NOT_DOCUMENTED"]
    path = root.joinpath(*PurePosixPath(raw_path).parts)
    try:
        resolved = path.resolve(strict=True)
        resolved.relative_to(root / "research" / "g2" / "raw")
        # Refuse symlinks in any path component, not only the final file.
        if resolved != path.absolute() or not resolved.is_file():
            raise ValueError("unsafe or non-regular raw source")
        actual_size = resolved.stat().st_size
        if actual_size != size:
            return ["RAW_SOURCE_BYTE_SIZE_MISMATCH"]
        digest = hashlib.sha256()
        with resolved.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except (OSError, ValueError):
        return ["RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE"]
    if (digest.hexdigest() != record.get("raw_sha256") or
            digest.hexdigest() != manifest.get("raw_sha256")):
        return ["RAW_SOURCE_BYTE_DIGEST_MISMATCH"]
    return []
