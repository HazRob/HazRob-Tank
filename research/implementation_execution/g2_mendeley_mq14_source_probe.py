"""Inspect original Mendeley MQ-EXT-14 metadata; never infer MQ model/ppm/G2.

Research-only acquisition probe. No synthetic fallback and no automatic
promotion. Mendeley API metadata is source description, not verified raw data.
"""
import argparse
import hashlib
import json
import urllib.error
import urllib.request
from pathlib import Path

DATASET = "78y8kv6hz7"
VERSION = "1"
SOURCE = f"https://data.mendeley.com/datasets/{DATASET}/{VERSION}"
API = f"https://api.data.mendeley.com/datasets/{DATASET}?version={VERSION}"
ALT_API = f"https://api.data.mendeley.com/datasets/{DATASET}/snapshot?version={VERSION}"
MAX_METADATA_BYTES = 2_000_000


def inspect_metadata(payload):
    if not isinstance(payload, dict):
        raise ValueError("SOURCE_METADATA_NOT_OBJECT")
    files = payload.get("files", [])
    if not isinstance(files, list):
        raise ValueError("SOURCE_FILES_NOT_LIST")
    inventory = []
    for item in files:
        if not isinstance(item, dict):
            raise ValueError("SOURCE_FILE_METADATA_NOT_OBJECT")
        inventory.append({
            "name": item.get("filename") or item.get("name") or item.get("file_name"),
            "id": item.get("id"),
            "size": item.get("size") or item.get("size_in_bytes"),
            "content_type": item.get("content_type") or (item.get("content_details") or {}).get("content_type"),
            "publisher_sha256_claim": (item.get("content_details") or {}).get("sha256_hash"),
            "raw_sha256_verified": False,
        })
    return {
        "schema": "hazrob.g2.mq14.source-metadata-probe.v1",
        "dataset_id": "MQ-EXT-14", "dataset_url": SOURCE,
        "doi": "10.17632/78y8kv6hz7.1",
        "files_reported": len(inventory), "file_inventory": inventory,
        "reported_description": str(payload.get("description", ""))[:3000],
        "reported_title": payload.get("name") or payload.get("title"),
        "raw_bytes_verified": False, "sensor_models_verified": False,
        "physical_grouping_verified": False, "reference_gas_ppm_verified": False,
        "g2_qualified": False, "g3_tank": False,
    }


def probe(url=API):
    req = urllib.request.Request(url, headers={
        "User-Agent": "HazRob-Tank-G2-source-metadata-audit/1.0",
        "Accept": "application/vnd.mendeley-public-dataset.1+json, application/json",
    })
    with urllib.request.urlopen(req, timeout=35) as response:
        content = response.read(MAX_METADATA_BYTES + 1)
    if len(content) > MAX_METADATA_BYTES:
        raise ValueError("SOURCE_METADATA_EXCEEDS_LIMIT")
    metadata = json.loads(content)
    result = inspect_metadata(metadata)
    result["api_url"] = url
    result["metadata_sha256"] = hashlib.sha256(content).hexdigest()
    result["metadata_fetch_status"] = "SOURCE_METADATA_RETRIEVED_NOT_RAW"
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", required=True)
    args = ap.parse_args()
    attempts = []
    result = None
    for url in (API, ALT_API):
        try:
            result = probe(url)
            break
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            attempts.append({"api_url": url, "error_type": type(exc).__name__,
                             "http_status": getattr(exc, "code", None)})
    if result is None:
        result = {"schema": "hazrob.g2.mq14.source-metadata-probe.v1",
                  "dataset_id": "MQ-EXT-14", "dataset_url": SOURCE,
                  "api_url": API, "metadata_fetch_status": "SOURCE_METADATA_ACCESS_BLOCKED",
                  "attempts": attempts,
                  "raw_bytes_verified": False, "sensor_models_verified": False,
                  "physical_grouping_verified": False,
                  "reference_gas_ppm_verified": False,
                  "g2_qualified": False, "g3_tank": False}
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\\n")
    print(json.dumps({"dataset_id": result["dataset_id"],
                      "status": result["metadata_fetch_status"],
                      "files": result.get("files_reported", 0),
                      "attempts": attempts,
                      "g2_qualified": False}))


if __name__ == "__main__":
    main()
