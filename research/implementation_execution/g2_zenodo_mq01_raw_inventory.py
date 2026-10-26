"""Acquire and inspect original Zenodo MQ-EXT-01 archive, without G2 promotion.

Research-only. The checksum is pinned from Zenodo record 17285312. A
download failure is not replaced by synthetic data or a GitHub mirror.
"""
import argparse
import csv
import hashlib
import json
import os
import tempfile
import time
import urllib.request
import zipfile
from pathlib import Path

RECORD_ID = "17285312"
FILENAME = "Food-Freshness-E-Nose-Dataset.zip"
PUBLISHED_MD5 = "008b8ccb62d7a7d51ff9ffcfd698a1e5"
SOURCE = "https://zenodo.org/records/" + RECORD_ID
DOWNLOAD = SOURCE + "/files/" + FILENAME + "?download=1"
MAX_ARCHIVE_BYTES = 30 * 1024 * 1024
MAX_TOTAL_UNCOMPRESSED_BYTES = 512 * 1024 * 1024

def download_verified(destination, url=DOWNLOAD):
    last_error = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "HazRob-Tank-G2-source-audit/1.0"})
            md5 = hashlib.md5()
            sha256 = hashlib.sha256()
            size = 0
            with urllib.request.urlopen(request, timeout=45) as response, open(destination, "wb") as out:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    size += len(chunk)
                    if size > MAX_ARCHIVE_BYTES:
                        raise ValueError("SOURCE_ARCHIVE_EXCEEDS_SIZE_LIMIT")
                    md5.update(chunk)
                    sha256.update(chunk)
                    out.write(chunk)
            if md5.hexdigest() != PUBLISHED_MD5:
                raise ValueError("ZENODO_PUBLISHED_MD5_MISMATCH: " + md5.hexdigest())
            return {"bytes": size, "md5": md5.hexdigest(), "sha256": sha256.hexdigest(),
                    "source_url": url, "record_url": SOURCE}
        except (OSError, ValueError) as exc:
            last_error = exc
            if isinstance(exc, ValueError):
                break
            if attempt < 2:
                time.sleep(attempt + 1)
    raise RuntimeError("ORIGINAL_ZENODO_DOWNLOAD_NOT_VERIFIED: " + str(last_error))

def inspect_archive(archive):
    members = []
    with zipfile.ZipFile(archive) as zf:
        files = [x for x in zf.infolist() if not x.is_dir()]
        total = sum(x.file_size for x in files)
        if total > MAX_TOTAL_UNCOMPRESSED_BYTES:
            raise ValueError("ZIP_UNCOMPRESSED_SIZE_LIMIT")
        for info in files:
            if info.filename.startswith("/") or ".." in Path(info.filename).parts:
                raise ValueError("ZIP_UNSAFE_MEMBER_PATH")
            # CRC is verified by reading each file to completion without extraction.
            with zf.open(info) as stream:
                while stream.read(1024 * 1024):
                    pass
            members.append({"path": info.filename, "bytes": info.file_size,
                            "compressed_bytes": info.compress_size, "crc32": f"{info.CRC:08x}"})
    return members

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--output-dir", required=True)
    args = ap.parse_args()
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temp:
        archive = Path(temp) / FILENAME
        provenance = download_verified(archive)
        members = inspect_archive(archive)
    manifest = {
        "schema": "hazrob.g2.original-source-inventory.v1",
        "dataset_id": "MQ-EXT-01",
        "source_kind": "PHYSICAL_MEASUREMENTS_REPORTED_NOT_INDEPENDENTLY_QUALIFIED",
        "archive": provenance,
        "file_count": len(members),
        "files": members,
        "g2_pass": False,
        "g3_tank": False,
        "note": "Original archive verified by published MD5 and computed SHA256; no inference on gas identity or ppm."
    }
    qualification = {
        "schema": "hazrob.g2.source-qualification.v1",
        "dataset_id": "MQ-EXT-01",
        "source_archive_sha256": provenance["sha256"],
        "sensor_models_reported": ["MQ2","MQ3","MQ4","MQ5","MQ6","MQ7","MQ8","MQ9","MQ135"],
        "application": "food freshness, not pure-gas concentration calibration",
        "physical_exposure_group_ids_verified": False,
        "gas_reference_ppm_verified": False,
        "signal_units_verified": False,
        "r0_measured_sensor_bound_verified": False,
        "status": "SOURCE_BYTES_VERIFIED_G2_GROUPING_AND_CALIBRATION_PENDING",
        "g2_qualified": False
    }
    (output / "raw_manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
    (output / "source_qualification.json").write_text(json.dumps(qualification, indent=2, ensure_ascii=False) + "\n")
    with (output / "archive_inventory.csv").open("w", newline="") as fp:
        writer = csv.DictWriter(fp, fieldnames=["path", "bytes", "compressed_bytes", "crc32"])
        writer.writeheader()
        writer.writerows(members)
    # Inventory paths are source facts, not verified exposure-group IDs.
    print("SOURCE_ARCHIVE_MEMBERS_BEGIN")
    for member in members:
        print(json.dumps({"path": member["path"], "bytes": member["bytes"]}, ensure_ascii=False))
    print("SOURCE_ARCHIVE_MEMBERS_END")
    print(json.dumps({"dataset_id": "MQ-EXT-01", "archive_sha256": provenance["sha256"],
                      "files": len(members), "g2_qualified": False}))

if __name__ == "__main__":
    main()
