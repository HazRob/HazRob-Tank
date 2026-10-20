"""Research-only source-locked importer for HazRob Tank exposure bundles.

A manifest is a sidecar; each source_artifact points to a separate raw JSON
record WITHOUT source_sha256 (avoids an impossible self-hash). The sidecar must
match the raw record exactly and its SHA256 must match the actual bytes.
Both layers are required. This does NOT authenticate gas standards, prove sensor
accuracy, or promote any technique to G3 scientific validation.

All inputs remain on disk and are only read; no robot I/O or network access.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from tank_measurement_adapter_v1 import audit_cohort, validate_exposure

BUNDLE_SCHEMA = "tank-source-bundle-1"
MAX_MANIFEST_BYTES = 5_000_000
MAX_RAW_BYTES = 20_000_000
ELIGIBILITY_MODES = ("integrity", "candidate_g3_data")


def _no_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON key: " + key)
        result[key] = value
    return result


def _read_json(path: Path, limit: int):
    if not path.is_file() or path.is_symlink():
        raise ValueError("regular non-symlink input file required")
    with path.open("rb") as handle:
        payload = handle.read(limit + 1)
    if not payload or len(payload) > limit:
        raise ValueError("empty or oversized input file")
    try:
        data = json.loads(payload.decode("utf-8"), object_pairs_hook=_no_duplicate_keys,
                          parse_constant=lambda _x: (_ for _ in ()).throw(
                              ValueError("nonfinite JSON value")))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError("invalid UTF-8 JSON") from exc
    return data, payload


def _safe_raw_path(root: Path, name: str):
    if not isinstance(name, str) or not name:
        raise ValueError("raw path required")
    parts = name.split("/")
    if (len(parts) < 2 or parts[0] != "raw"
            or any(p in ("", ".", "..") for p in parts)
            or "\\" in name or ":" in name):
        raise ValueError("raw artifact must be a clean relative raw/ path")
    candidate = root
    for p in parts:
        candidate = candidate / p
        if candidate.is_symlink():
            raise ValueError("symlinks are not permitted for source artifacts")
    resolved = candidate.resolve()
    if not resolved.is_relative_to(root.resolve()):
        raise ValueError("raw source escapes bundle root")
    return candidate


def ingest_bundle(manifest_file, *, mode="integrity"):
    """Return factual ingest/QC counts; never certify calibration or G3.

    'integrity' accepts partial sensor arrays as provenance-only experiments.
    'candidate_g3_data' additionally requires 8 explicitly mapped physical
    instances, BME280, defined heater phase and a declared reference/blank.
    Neither mode independently verifies laboratory certificate authenticity.
    """
    if mode not in ELIGIBILITY_MODES:
        raise ValueError("unsupported eligibility mode")
    manifest_path = Path(manifest_file).resolve()
    manifest, manifest_bytes = _read_json(manifest_path, MAX_MANIFEST_BYTES)
    if not isinstance(manifest, dict) or manifest.get("schema_version") != BUNDLE_SCHEMA:
        raise ValueError("wrong bundle schema")
    rows = manifest.get("exposures")
    if not isinstance(rows, list) or not rows:
        raise ValueError("exposures must be nonempty list")
    instance_map = manifest.get("expected_sensor_instances")
    if instance_map is not None and (
            not isinstance(instance_map, dict) or
            any(not isinstance(k, str) or not k.strip() or
                not isinstance(v, str) or not v.strip()
                for k, v in instance_map.items())):
        raise ValueError("invalid physical sensor instance map")
    if mode == "candidate_g3_data" and (
            not isinstance(instance_map, dict) or len(instance_map) != 8):
        raise ValueError("strict candidate mode requires exactly 8 declared instances")

    observations = []
    seen_source_paths = set()
    summaries = []
    for entry in rows:
        if not isinstance(entry, dict):
            raise ValueError("exposure sidecar object required")
        record = dict(entry)
        name = record.pop("source_artifact", None)
        source_sha = record.get("source_sha256")
        raw_record_expected = dict(record)
        raw_record_expected.pop("source_sha256", None)
        raw_path = _safe_raw_path(manifest_path.parent, name)
        if name in seen_source_paths:
            raise ValueError("one raw file cannot masquerade as two exposures")
        seen_source_paths.add(name)
        raw_data, raw_bytes = _read_json(raw_path, MAX_RAW_BYTES)
        actual_sha = hashlib.sha256(raw_bytes).hexdigest()
        if actual_sha != source_sha:
            raise ValueError("source SHA256 mismatch for " + name)
        if raw_data != raw_record_expected:
            raise ValueError("sidecar fields differ from raw acquisition for " + name)

        check = validate_exposure(record)
        observed_map = {s["sensor_instance_id"]: s["sensor_id"]
                        for s in record["sensors"]}
        if instance_map is not None and observed_map != instance_map:
            raise ValueError("actual sensors differ from declared physical mapping")
        if mode == "candidate_g3_data":
            if len(record["sensors"]) != 8 or not check["environment_present"]:
                raise ValueError("candidate record requires eight sensors and BME280")
            if not check["heater_phases_all_known"]:
                raise ValueError("unknown heater phase blocks candidate readiness")
            if not check["independent_gas_reference_present"]:
                raise ValueError("UNKNOWN record is not a referenced gas candidate")
        observations.append(record)
        summaries.append({"record_id": record["record_id"],
                          "raw_path": name, "source_sha256_verified": actual_sha,
                          "sensor_count": check["n_sensor_instances"],
                          "reference_type": record["reference_type"]})

    cohort = audit_cohort(observations)
    return {
        "bundle_schema": BUNDLE_SCHEMA,
        "mode": mode,
        "manifest_sha256": hashlib.sha256(manifest_bytes).hexdigest(),
        "raw_artifact_count_verified": len(summaries),
        "raw_sources": summaries,
        "cohort": cohort,
        "candidate_data_structure_ready": mode == "candidate_g3_data",
        "gas_standard_authenticity_verified": False,
        "measurement_physics_verified": False,
        "tank_g3_scientifically_validated": False,
        "warnings": [
            "Only bundle byte integrity, field parity and declared structure are checked.",
            "A reference_id string is not evidence of an authentic calibrated standard.",
            "No real Tank MQ8 gas exposure or operational gas alarm was validated."
        ]
    }
