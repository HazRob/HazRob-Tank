"""Replay the original bytes claimed for each sensor-specific R0 reference.

A boolean source_bytes_verified flag or an R0 digest string is not evidence
that the calibration archive is present. This is a byte-integrity check only;
it cannot authenticate a reference gas or establish ppm traceability.
"""
import hashlib
import re
from pathlib import Path, PurePosixPath

HEX64 = re.compile(r"^[0-9a-f]{64}$")
LFS_HEADER = b"version https://git-lfs.github.com/spec/v1"


def audit_r0_source_bytes(record, repo_root=Path(".")):
    processing = record.get("signal_processing")
    if not isinstance(processing, dict) or processing.get("feature_formula") != "RS_OVER_R0":
        return []
    references = processing.get("r0_references")
    if references is None:
        references = [processing.get("r0_reference")]
    elif isinstance(references, dict):
        references = list(references.values())
    else:
        return ["R0_RAW_REFERENCE_LAYOUT_INVALID"]
    if not references or any(not isinstance(ref, dict) for ref in references):
        return ["R0_RAW_REFERENCE_LAYOUT_INVALID"]

    root = Path(repo_root).resolve()
    raw_root = root / "research" / "g2" / "raw"
    issues = []
    for ref in references:
        raw_path = ref.get("raw_local_path")
        size = ref.get("raw_size_bytes")
        digest = ref.get("raw_sha256")
        if (not isinstance(raw_path, str) or not raw_path or
                "\\" in raw_path or raw_path.startswith("/") or
                any(part in ("", ".", "..") for part in raw_path.split("/")) or
                PurePosixPath(raw_path).parts[:3] != ("research", "g2", "raw") or
                len(PurePosixPath(raw_path).parts) < 4):
            issues.append("R0_RAW_SOURCE_PATH_MISSING_OR_UNSAFE")
            continue
        if isinstance(size, bool) or not isinstance(size, int) or size < 1:
            issues.append("R0_RAW_SOURCE_SIZE_NOT_DOCUMENTED")
            continue
        if not isinstance(digest, str) or not HEX64.fullmatch(digest):
            issues.append("R0_RAW_SOURCE_DIGEST_INVALID")
            continue
        path = root.joinpath(*PurePosixPath(raw_path).parts)
        try:
            resolved = path.resolve(strict=True)
            resolved.relative_to(raw_root)
            if resolved != path.absolute() or not resolved.is_file():
                raise ValueError("unsafe or non-regular R0 archive")
            if resolved.stat().st_size != size:
                issues.append("R0_RAW_SOURCE_BYTE_SIZE_MISMATCH")
                continue
            actual = hashlib.sha256()
            bytes_hashed = 0
            with resolved.open("rb") as stream:
                if stream.read(64).startswith(LFS_HEADER):
                    issues.append("R0_RAW_SOURCE_GIT_LFS_POINTER_NOT_MATERIALIZED")
                    continue
                stream.seek(0)
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    bytes_hashed += len(chunk)
                    actual.update(chunk)
            # Recheck the bytes actually hashed: stat() precedes open() and
            # cannot guarantee that an archive stayed the same size while read.
            if bytes_hashed != size:
                issues.append("R0_RAW_SOURCE_BYTE_SIZE_MISMATCH")
                continue
            if actual.hexdigest() != digest:
                issues.append("R0_RAW_SOURCE_BYTE_DIGEST_MISMATCH")
        except (OSError, ValueError):
            issues.append("R0_RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE")
    return sorted(set(issues))
