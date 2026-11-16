"""Cross-check the same MQ-EXT-10 ethanol bytes via folder and ZIP intake.

Research-only source integrity; liquid solution v/v is not gas headspace ppm.
This tool never promotes a technique to G2 or G3.
"""
import argparse
import hashlib
import json
from pathlib import Path

from g2_mq_ext10_ethanol_preflight import run as inspect_folder, require
from g2_mq_ext10_archive_intake import inspect_zip

SCHEMA = "hazrob.g2.mq-ext10-folder-zip-parity.v1"
FIELDS = (
    "concentration_code", "repetition", "nominal_liquid_ethanol_percent_vv",
    "rows", "sha256", "numeric_trace_sha256", "ranges",
)


def _canonical_manifest(receipt):
    manifest = receipt["file_manifest"]
    keyed = {}
    for row in manifest:
        key = (row["concentration_code"], row["repetition"])
        require(key not in keyed, "PARITY_DUPLICATE_SAMPLE_KEY")
        keyed[key] = {field: row[field] for field in FIELDS}
    return keyed


def _digest(rows):
    ordered = [rows[key] for key in sorted(rows)]
    payload = json.dumps(ordered, sort_keys=True, ensure_ascii=True,
                         separators=(",", ":"), allow_nan=False).encode("ascii")
    return hashlib.sha256(payload).hexdigest()


def run(raw_root=None, raw_zip=None):
    """Require paired inputs; compare physical sample bytes, not only labels."""
    require((raw_root is None) == (raw_zip is None),
            "PARITY_BOTH_FOLDER_AND_ZIP_REQUIRED")
    result = {
        "schema": SCHEMA,
        "source_id": "MQ-EXT-10",
        "source_version_doi": "10.17632/vpc887d53s.3",
        "liquid_percent_vv_is_gas_ppm": False,
        "headspace_ethanol_ppm_measured": False,
        "independent_tank_exposures": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "source_status": "PAIRED_ORIGINAL_RAW_NOT_AVAILABLE",
        "samples_compared": 0,
        "folder_manifest_sha256": None,
        "zip_manifest_sha256": None,
        "archive_sha256_computed": None,
        "publisher_archive_sha256_verified": False,
        "blockers": [
            "PAIRED_ORIGINAL_RAW_NOT_AVAILABLE",
            "PUBLISHER_ARCHIVE_SHA256_NOT_INDEPENDENTLY_VERIFIED",
            "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
            "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED",
            "R0_CALIBRATION_NOT_VERIFIED",
        ],
    }
    if raw_root is None:
        return result
    folder = inspect_folder(Path(raw_root))
    archive = inspect_zip(Path(raw_zip))
    a = _canonical_manifest(folder)
    b = _canonical_manifest(archive)
    require(set(a) == set(b), "PARITY_SAMPLE_MEMBERSHIP_MISMATCH")
    for key in sorted(a):
        require(a[key]["sha256"] == b[key]["sha256"],
                "PARITY_SOURCE_BYTES_MISMATCH")
        require(a[key] == b[key], "PARITY_PARSED_SAMPLE_MISMATCH")
    require(folder["concentration_code_counts"] == archive["concentration_code_counts"],
            "PARITY_LEVEL_COUNTS_MISMATCH")
    require(folder["reported_ethanol_count_complete"] ==
            archive["reported_ethanol_count_complete"],
            "PARITY_REPORTED_COUNT_MISMATCH")
    require(folder["all_six_published_liquid_levels_present"] ==
            archive["all_six_published_liquid_levels_present"],
            "PARITY_LEVEL_COVERAGE_MISMATCH")
    folder_sha, zip_sha = _digest(a), _digest(b)
    require(folder_sha == zip_sha, "PARITY_MANIFEST_DIGEST_MISMATCH")
    result.update({
        "source_status": "PAIRED_SOURCE_BYTES_MATCH_UNQUALIFIED",
        "samples_compared": len(a),
        "folder_manifest_sha256": folder_sha,
        "zip_manifest_sha256": zip_sha,
        "archive_sha256_computed": archive["archive_sha256_computed"],
        "reported_ethanol_count_complete": folder["reported_ethanol_count_complete"],
        "all_six_published_liquid_levels_present":
            folder["all_six_published_liquid_levels_present"],
        "concentration_code_counts": folder["concentration_code_counts"],
        "blockers": [
            "PUBLISHER_ARCHIVE_SHA256_NOT_INDEPENDENTLY_VERIFIED",
            "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
            "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED",
            "R0_CALIBRATION_NOT_VERIFIED",
        ] + (["INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS"]
             if not folder["reported_ethanol_count_complete"] else [])
          + (["MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS"]
             if not folder["all_six_published_liquid_levels_present"] else []),
    })
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw-root", type=Path, help="Original Ethanol folder (paired)")
    ap.add_argument("--raw-zip", type=Path, help="Original publisher ZIP (paired)")
    ap.add_argument("--output", type=Path)
    args = ap.parse_args()
    encoded = json.dumps(run(args.raw_root, args.raw_zip), sort_keys=True,
                         ensure_ascii=False, separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
