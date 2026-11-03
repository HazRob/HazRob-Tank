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
    for entry in manifest.get("sources", []):
        if not isinstance(entry, dict):
            out.append({"source_id": "", "path": "", "status": "blocked", "error": "invalid_source_entry"})
            continue
        sid = entry.get("source_id", "")
        rel = entry.get("path", "")
        expected = entry.get("sha256", "")
        result = {"source_id": sid, "path": rel, "status": "blocked"}
        try:
            if not isinstance(rel, str) or not rel or pathlib.PurePosixPath(rel).is_absolute() or ".." in pathlib.PurePosixPath(rel).parts or "\\" in rel or pathlib.PurePosixPath(rel).parts[0] == ".":
                raise ValueError("unsafe_path")
            if not isinstance(expected, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", expected):
                raise ValueError("invalid_sha256")
            path = root / rel
            if any(p.is_symlink() for p in (root / pathlib.PurePosixPath(rel).parts[0], *(root.joinpath(*pathlib.PurePosixPath(rel).parts[:i]) for i in range(2, len(pathlib.PurePosixPath(rel).parts) + 1)))) or not path.resolve().is_relative_to(root.resolve()):
                raise ValueError("unsafe_path")
            if not path.is_file():
                raise ValueError("missing_raw_bytes")
            digest = hashlib.sha256()
            with path.open("rb") as f:
                for chunk in iter(lambda: f.read(1024 * 1024), b""):
                    digest.update(chunk)
            actual = digest.hexdigest()
            result["actual_sha256"] = actual
            result["status"] = "verified" if actual.lower() == expected.lower() else "hash_mismatch"
        except (ValueError, OSError) as exc:
            result["error"] = str(exc)
        out.append(result)
    return {"schema": "hazrob-g2-byte-integrity/1", "scientific_qualification": False, "results": out, "all_verified": bool(out) and all(x["status"] == "verified" for x in out)}

def main():
    p = argparse.ArgumentParser()
    p.add_argument("manifest", type=pathlib.Path)
    p.add_argument("--root", type=pathlib.Path, required=True)
    args = p.parse_args()
    try:
        data = json.loads(args.manifest.read_text(encoding="utf-8"))
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
