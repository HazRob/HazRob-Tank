"""Read-only ZIP intake for MQ-EXT-10 Mendeley v3 (research, not G2 approval).

The publisher's archive checksum is not independently supplied; computing a
SHA256 for downloaded bytes does not authenticate publisher provenance.
"""
import argparse
import hashlib
import io
import json
import re
import stat
import unicodedata
import zipfile
from pathlib import Path

from g2_mq_ext10_ethanol_preflight import (
    MAX_SOURCE_BYTES_PER_FILE, LIQUID_PERCENT_BY_CODE,
    parse_ethanol_filename, parse_numeric_rows, liquid_level_coverage, require,
)

SCHEMA = "hazrob.g2.mq-ext10-archive-intake.v1"
MAX_ARCHIVE_BYTES = 250_000_000
MAX_ENTRIES = 1000
MAX_UNCOMPRESSED_BYTES = 500_000_000
EXTENSIONS = {".txt", ".csv", ".dat"}


def _safe_parts(info):
    name = info.filename
    require(isinstance(name, str) and name and "\\" not in name and "\x00" not in name,
            "ARCHIVE_UNSAFE_PATH")
    require(not name.startswith("/") and not re.match(r"^[A-Za-z]:", name),
            "ARCHIVE_UNSAFE_PATH")
    parts = name.rstrip("/").split("/")
    require(all(p and p not in (".", "..") for p in parts), "ARCHIVE_UNSAFE_PATH")
    # Invisible Unicode control/format characters can spoof source member
    # paths even when the ZIP checksum is valid. Ordinary multilingual
    # filenames remain allowed.
    require(not any(unicodedata.category(ch).startswith("C") for ch in name),
            "ARCHIVE_UNSAFE_PATH")
    # NFKC can turn compatibility punctuation into path separators or dot
    # segments. Validate each original component before canonicalizing paths.
    normalized_parts = [unicodedata.normalize("NFKC", p) for p in parts]
    require(all(p not in (".", "..") and "/" not in p and "\\" not in p
                for p in normalized_parts), "ARCHIVE_UNSAFE_PATH")
    mode = (info.external_attr >> 16) & 0xffff
    require(not stat.S_ISLNK(mode), "ARCHIVE_SYMLINK_REJECTED")
    require(not (info.flag_bits & 1), "ARCHIVE_ENCRYPTED_ENTRY")
    require(info.file_size >= 0 and info.compress_size >= 0, "ARCHIVE_INVALID_SIZE")
    return parts


def inspect_zip(path, expected_rows=3330):
    """Inspect original archive bytes in memory, without extracting to disk."""
    path = Path(path)
    require(path.is_file() and not path.is_symlink(), "ARCHIVE_NOT_REGULAR_FILE")
    require(path.stat().st_size <= MAX_ARCHIVE_BYTES, "ARCHIVE_TOO_LARGE")
    archive_bytes = path.read_bytes()
    require(len(archive_bytes) <= MAX_ARCHIVE_BYTES, "ARCHIVE_TOO_LARGE")
    archive_digest = hashlib.sha256(archive_bytes).hexdigest()
    seen_paths = set()
    seen_file_paths = set()
    seen_parent_paths = set()
    seen_samples = set()
    seen_bytes = set()
    seen_numeric = set()
    manifest = []
    try:
        with zipfile.ZipFile(io.BytesIO(archive_bytes)) as zf:
            entries = zf.infolist()
            require(0 < len(entries) <= MAX_ENTRIES, "ARCHIVE_ENTRY_COUNT")
            require(sum(i.file_size for i in entries) <= MAX_UNCOMPRESSED_BYTES,
                    "ARCHIVE_UNCOMPRESSED_LIMIT")
            for info in entries:
                parts = _safe_parts(info)
                # NFKC-equivalent archive paths must not masquerade as
                # distinct source members. Keep original paths in receipts.
                key = unicodedata.normalize("NFKC", "/".join(parts)).casefold()
                require(key not in seen_paths, "ARCHIVE_DUPLICATE_MEMBER_PATH")
                # An implicit directory may not also be a regular file, in
                # either member ordering. Explicit directories are allowed.
                segments = key.split("/")
                parent_paths = {"/".join(segments[:i])
                                for i in range(1, len(segments))}
                require(not (parent_paths & seen_file_paths),
                        "ARCHIVE_FILE_DIRECTORY_CONFLICT")
                require(info.is_dir() or key not in seen_parent_paths,
                        "ARCHIVE_FILE_DIRECTORY_CONFLICT")
                seen_paths.add(key)
                seen_parent_paths.update(parent_paths)
                if not info.is_dir():
                    seen_file_paths.add(key)
                folder_parts = parts if info.is_dir() else parts[:-1]
                # A fullwidth/case-variant Ethanol directory must not be
                # silently excluded as unrelated wine/source material.
                require(not any(unicodedata.normalize("NFKC", p).casefold() == "ethanol"
                                and p != "Ethanol" for p in folder_parts),
                        "ARCHIVE_ETHANOL_FOLDER_ALIAS")
                if info.is_dir():
                    # A directory entry carrying data is not an ordinary ZIP
                    # directory and must not hide an uninspected source member.
                    require(info.file_size == 0, "ARCHIVE_DIRECTORY_NONEMPTY")
                    continue
                if "Ethanol" not in parts[:-1]:
                    # Even excluded wine/auxiliary members are part of the
                    # original archive. Drain them in bounded chunks to check
                    # CRC and decompressed length, without treating their data
                    # as ethanol measurements or retaining it in memory.
                    consumed = 0
                    with zf.open(info) as handle:
                        while True:
                            chunk = handle.read(64 * 1024)
                            if not chunk:
                                break
                            consumed += len(chunk)
                            require(consumed <= info.file_size,
                                    "ARCHIVE_NON_ETHANOL_FILE_SIZE_MISMATCH")
                    require(consumed == info.file_size,
                            "ARCHIVE_NON_ETHANOL_FILE_SIZE_MISMATCH")
                    continue  # Published wine folders are not ethanol samples.
                require(parts[-2] == "Ethanol" and parts.count("Ethanol") == 1,
                        "ARCHIVE_NESTED_ETHANOL_SAMPLE")
                filename = parts[-1]
                suffix = Path(filename).suffix.lower()
                require(suffix in EXTENSIONS, "ARCHIVE_ETHANOL_EXTENSION")
                code, repetition = parse_ethanol_filename(Path(filename).stem)
                sample_key = (code, repetition)
                require(sample_key not in seen_samples, "DUPLICATE_CONCENTRATION_REPETITION")
                require(info.file_size <= MAX_SOURCE_BYTES_PER_FILE,
                        "ARCHIVE_ETHANOL_FILE_TOO_LARGE")
                with zf.open(info) as handle:
                    data = handle.read(MAX_SOURCE_BYTES_PER_FILE + 1)
                require(len(data) <= MAX_SOURCE_BYTES_PER_FILE and len(data) == info.file_size,
                        "ARCHIVE_ETHANOL_FILE_SIZE_MISMATCH")
                parsed = parse_numeric_rows(data, expected_rows=expected_rows)
                raw_sha = hashlib.sha256(data).hexdigest()
                numeric_sha = parsed["numeric_trace_sha256"]
                require(raw_sha not in seen_bytes, "DUPLICATE_MEASUREMENT_BYTES")
                require(numeric_sha not in seen_numeric,
                        "DUPLICATE_MEASUREMENT_NUMERIC_TRACE")
                seen_samples.add(sample_key)
                seen_bytes.add(raw_sha)
                seen_numeric.add(numeric_sha)
                manifest.append({
                    "archive_member": info.filename,
                    "concentration_code": code,
                    "nominal_liquid_ethanol_percent_vv": LIQUID_PERCENT_BY_CODE[code],
                    "repetition": repetition,
                    "rows": parsed["rows"],
                    "ranges": parsed["ranges"],
                    "sha256": raw_sha,
                    "numeric_trace_sha256": numeric_sha,
                })
    except (zipfile.BadZipFile, EOFError, RuntimeError, OSError) as exc:
        raise ValueError("MQ_EXT10_ARCHIVE_CORRUPT_OR_UNREADABLE") from exc
    require(bool(manifest), "ARCHIVE_NO_ETHANOL_FILES")
    require(len(manifest) <= 65, "ARCHIVE_MORE_ETHANOL_THAN_REPORTED")
    # A count of 65 alone is not evidence of the six published liquid
    # concentration levels: 65 uniquely named C1 replicates could pass it.
    counts, all_levels_present = liquid_level_coverage(manifest)
    count_complete = len(manifest) == 65
    manifest.sort(key=lambda x: (x["concentration_code"], x["repetition"]))
    return {
        "schema": SCHEMA,
        "source_id": "MQ-EXT-10",
        "source_version_doi": "10.17632/vpc887d53s.3",
        "source_status": ("ARCHIVE_BYTES_SCANNED_UNQUALIFIED" if count_complete and all_levels_present
                          else "REPORTED_COUNT_MISSING_CONCENTRATION_LEVELS" if count_complete
                          else "PARTIAL_ARCHIVE_BYTES_SCANNED_UNQUALIFIED"),
        "archive_sha256_computed": archive_digest,
        "publisher_archive_sha256_verified": False,
        "files_scanned": len(manifest),
        "rows_scanned": sum(x["rows"] for x in manifest),
        "reported_ethanol_measurements": 65,
        "concentration_code_counts": counts,
        "all_six_published_liquid_levels_present": all_levels_present,
        "reported_ethanol_count_complete": count_complete,
        "headspace_ethanol_ppm_measured": False,
        "liquid_percent_vv_is_gas_ppm": False,
        "independent_physical_groups_verified": False,
        "independent_tank_exposures": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "blockers": [
            "PUBLISHER_ARCHIVE_SHA256_NOT_INDEPENDENTLY_VERIFIED",
            "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
            "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED",
            "R0_CALIBRATION_NOT_VERIFIED",
        ] + (["INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS"] if not count_complete else [])
          + (["MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS"] if not all_levels_present else []),
        "file_manifest": manifest,
    }


def run(raw_zip=None):
    if raw_zip is not None:
        return inspect_zip(raw_zip)
    return {
        "schema": SCHEMA,
        "source_id": "MQ-EXT-10",
        "source_version_doi": "10.17632/vpc887d53s.3",
        "source_status": "ORIGINAL_ARCHIVE_NOT_PRESENT_IN_THIS_RUN",
        "archive_sha256_computed": None,
        "publisher_archive_sha256_verified": False,
        "files_scanned": 0,
        "rows_scanned": 0,
        "headspace_ethanol_ppm_measured": False,
        "liquid_percent_vv_is_gas_ppm": False,
        "independent_tank_exposures": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "blockers": ["ORIGINAL_ARCHIVE_NOT_AVAILABLE", "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
                     "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED", "R0_CALIBRATION_NOT_VERIFIED"],
        "file_manifest": [],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw-zip", type=Path, help="Optional original archive; never bundled in CI")
    ap.add_argument("--output", type=Path)
    args = ap.parse_args()
    encoded = json.dumps(run(args.raw_zip), ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
