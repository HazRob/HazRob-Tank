#!/usr/bin/env python3
"""Read-only SHA-256 verification of G2 raw files; not scientific qualification."""
import argparse
import hashlib
import json
from pathlib import Path

def verify(manifest, root):
    errors = []
    records = manifest.get("records", []) if isinstance(manifest, dict) else None
    if not isinstance(records, list) or not records:
        return {"status": "BLOCKED", "verified_files": 0,
                "errors": ["no independent raw records"], "scientifically_validated": False}
    try:
        root = Path(root).resolve(strict=True)
        if not root.is_dir():
            raise NotADirectoryError(str(root))
    except (OSError, RuntimeError) as exc:
        return {"status": "BLOCKED", "verified_files": 0,
                "errors": [f"invalid raw-data root: {exc}"],
                "scientifically_validated": False}
    verified = 0
    seen_paths = set()
    seen_file_ids = set()
    for i, record in enumerate(records):
        if not isinstance(record, dict):
            errors.append(f"record {i}: invalid record")
            continue
        path, digest = record.get("raw_data_path"), record.get("raw_sha256")
        if (not isinstance(path, str) or not path or not isinstance(digest, str)
                or len(digest) != 64 or any(c not in "0123456789abcdefABCDEF" for c in digest)):
            errors.append(f"record {i}: invalid raw file metadata")
            continue
        if ("\\" in path or path.startswith(("/", "~")) or ":" in path.split("/")[0]
                or any(x in ("", ".", "..") for x in path.split("/"))):
            errors.append(f"record {i}: unsafe path")
            continue
        try:
            resolved = root.joinpath(*path.split("/")).resolve(strict=True)
            if not resolved.is_relative_to(root) or not resolved.is_file():
                raise ValueError("not a regular file inside root")
            if resolved in seen_paths:
                raise ValueError("duplicate raw file path")
            file_stat = resolved.stat()
            file_id = (file_stat.st_dev, file_stat.st_ino)
            if file_id in seen_file_ids:
                raise ValueError("duplicate raw file identity (hard link)")
            h = hashlib.sha256()
            with resolved.open("rb") as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b""):
                    h.update(block)
            if h.hexdigest() != digest.lower():
                raise ValueError("SHA-256 mismatch")
            seen_paths.add(resolved)
            seen_file_ids.add(file_id)
            verified += 1
        except (OSError, ValueError, RuntimeError) as exc:
            errors.append(f"record {i}: {exc}")
    return {"status": "BLOCKED" if errors else "RAW_INTEGRITY_VERIFIED_ONLY",
            "verified_files": verified, "errors": errors, "scientifically_validated": False}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    try:
        data = json.loads(args.manifest.read_text(encoding="utf-8"))
        result = verify(data, args.root)
    except (OSError, ValueError) as exc:
        result = {"status": "BLOCKED", "verified_files": 0, "errors": [str(exc)],
                  "scientifically_validated": False}
    print(json.dumps(result, sort_keys=True, indent=2))
    return 0 if result["status"] == "RAW_INTEGRITY_VERIFIED_ONLY" else 2

if __name__ == "__main__":
    raise SystemExit(main())
