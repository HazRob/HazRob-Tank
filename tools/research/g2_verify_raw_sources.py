#!/usr/bin/env python3
"""G2 raw-source byte integrity verifier. Stdlib only; no scientific promotion.
Usage: python tools/research/g2_verify_raw_sources.py manifest.json --root data/raw
Manifest JSON: {"sources":[{"source_id":"MQ-EXT-01","path":"archive.zip","sha256":"64 lowercase hex digits"}]}
Paths must be relative to --root; symlinks and traversal are rejected.
Exit 0 only if every source exists and matches its declared SHA256.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys

def verify(manifest, root):
    out = []
    if any(part.is_symlink() for part in (root, *root.parents)):
        return {"schema": "hazrob-g2-byte-integrity/1", "scientific_qualification": False, "results": [{"source_id": "", "path": "", "status": "blocked", "error": "unsafe_root_symlink"}], "all_verified": False}
    seen_ids = set()
    seen_paths = set()
    seen_file_objects = set()  # Reject hardlink aliases of the same raw evidence.
    seen_digests = set()  # Byte-identical copies are not independent evidence.
    for entry in manifest.get("sources", []):
        if not isinstance(entry, dict):
            out.append({"source_id": "", "path": "", "status": "blocked", "error": "invalid_source_entry"})
            continue
        sid = entry.get("source_id", "")
        rel = entry.get("path", "")
        expected = entry.get("sha256", "")
        result = {"source_id": sid, "path": rel, "status": "blocked"}
        try:
            if not isinstance(sid, str) or not sid.strip() or any(ord(ch) < 32 or ord(ch) == 127 for ch in sid):
                raise ValueError("invalid_source_id")
            sid = sid.strip()
            result["source_id"] = sid
            if sid in seen_ids:
                raise ValueError("duplicate_source_id")
            seen_ids.add(sid)
            if not isinstance(rel, str) or not rel or any(ord(ch) < 32 or ord(ch) == 127 for ch in rel) or pathlib.PurePosixPath(rel).is_absolute() or ".." in pathlib.PurePosixPath(rel).parts or "\\" in rel or not pathlib.PurePosixPath(rel).parts:
                raise ValueError("unsafe_path")
            normalized_path = pathlib.PurePosixPath(rel).as_posix()
            if rel != normalized_path or any(part in ("", ".") for part in rel.split("/")):
                raise ValueError("unsafe_path")
            if normalized_path in seen_paths:
                raise ValueError("duplicate_source_path")
            seen_paths.add(normalized_path)
            if not isinstance(expected, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", expected):
                raise ValueError("invalid_sha256")
            path = root / rel
            if any(p.is_symlink() for p in (root / pathlib.PurePosixPath(rel).parts[0], *(root.joinpath(*pathlib.PurePosixPath(rel).parts[:i]) for i in range(2, len(pathlib.PurePosixPath(rel).parts) + 1)))) or not path.resolve().is_relative_to(root.resolve()):
                raise ValueError("unsafe_path")
            if not path.is_file():
                raise ValueError("missing_raw_bytes")
            before = path.stat()
            file_identity = (before.st_dev, before.st_ino)
            if file_identity in seen_file_objects:
                raise ValueError("duplicate_raw_file_identity")
            seen_file_objects.add(file_identity)
            digest = hashlib.sha256()
            byte_count = 0
            with path.open("rb") as f:
                opened = pathlib.os.fstat(f.fileno())
                if (before.st_dev, before.st_ino) != (opened.st_dev, opened.st_ino):
                    raise ValueError("raw_source_changed_during_read")
                for chunk in iter(lambda: f.read(1024 * 1024), b""):
                    digest.update(chunk)
                    byte_count += len(chunk)
                closed = pathlib.os.fstat(f.fileno())
            after = path.stat()
            if (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns) != (opened.st_dev, opened.st_ino, opened.st_size, opened.st_mtime_ns, opened.st_ctime_ns) or (opened.st_dev, opened.st_ino, opened.st_size, opened.st_mtime_ns, opened.st_ctime_ns) != (closed.st_dev, closed.st_ino, closed.st_size, closed.st_mtime_ns, closed.st_ctime_ns) or (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns) != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns) or byte_count != after.st_size:
                raise ValueError("raw_source_changed_during_read")
            actual = digest.hexdigest()
            result["actual_sha256"] = actual
            result["size_bytes"] = byte_count
            if actual in seen_digests:
                raise ValueError("duplicate_raw_content")
            seen_digests.add(actual)
            result["status"] = "verified" if actual.lower() == expected.lower() else "hash_mismatch"
        except (ValueError, OSError) as exc:
            result["error"] = str(exc)
        out.append(result)
    return {"schema": "hazrob-g2-byte-integrity/1", "scientific_qualification": False, "results": out, "all_verified": bool(out) and all(x["status"] == "verified" for x in out)}

def reject_duplicate_json_keys(pairs):
    """Reject ambiguous manifest keys rather than silently taking the last value."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate_manifest_key: " + key)
        result[key] = value
    return result

def reject_nonfinite_json_constant(value):
    """JSON manifests must not contain NaN or Infinity extensions."""
    raise ValueError("nonfinite_manifest_constant: " + value)

def main():
    p = argparse.ArgumentParser()
    p.add_argument("manifest", type=pathlib.Path)
    p.add_argument("--root", type=pathlib.Path, required=True)
    args = p.parse_args()
    try:
        data = json.loads(args.manifest.read_text(encoding="utf-8"), object_pairs_hook=reject_duplicate_json_keys, parse_constant=reject_nonfinite_json_constant)
        if not isinstance(data, dict) or not isinstance(data.get("sources"), list):
            raise ValueError("manifest.sources must be a list")
        result = verify(data, args.root)
    except (ValueError, OSError) as exc:
        print(json.dumps({"status": "blocked", "error": str(exc)}, sort_keys=True))
        return 2
    print(json.dumps(result, sort_keys=True, indent=2))
    return 0 if result["all_verified"] else 1

if __name__ == "__main__":
    sys.exit(main())
