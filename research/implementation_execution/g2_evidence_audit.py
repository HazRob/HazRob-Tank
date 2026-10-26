"""G2 evidence audit: fail closed without independent raw measured holdout evidence.

No status here is automatic scientific certification. The highest outcome
is a review candidate. Synthetic software tests cannot satisfy G2.
"""
import argparse
import json
import math
import re
from pathlib import Path
from g2_signal_lineage_gate import audit_signal_lineage

REQUIRED = ("raw_sha256", "dataset_uri", "sensor_metadata", "grouping_key",
            "train_group_ids", "calibration_group_ids", "test_group_ids", "target_definition",
            "metric_name", "metric_value", "baseline_metric_value",
            "protocol_sha256", "evaluation_artifact_sha256")
HEX64 = re.compile(r"^[0-9a-f]{64}$")
PHYSICAL_GROUP_KEYS = {"session_id", "device_session_id", "batch_id", "campaign_id", "site_day_device"}

def _nonempty_str(value):
    return isinstance(value, str) and bool(value.strip())

def audit_record(item):
    issues = []
    for key in REQUIRED:
        if key not in item or item[key] is None or item[key] == "":
            issues.append("MISSING_" + key.upper())
    if item.get("source_kind") != "INDEPENDENT_MEASURED":
        issues.append("NOT_INDEPENDENT_MEASURED_SOURCE")
    for key in ("raw_sha256", "protocol_sha256", "evaluation_artifact_sha256"):
        digest = item.get(key)
        if digest is not None and digest != "" and (not isinstance(digest, str) or not HEX64.fullmatch(digest)):
            issues.append("INVALID_" + key.upper())
    if item.get("raw_sha256_verified_from_bytes") is not True:
        issues.append("RAW_SOURCE_BYTES_NOT_SHA_VERIFIED")
    if not isinstance(item.get("grouping_key"), str) or item.get("grouping_key") not in PHYSICAL_GROUP_KEYS:
        issues.append("GROUPING_NOT_VERIFIED_PHYSICAL_UNIT")
    if item.get("grouping_source_verified") is not True or not _nonempty_str(item.get("grouping_source_field")):
        issues.append("GROUPING_SOURCE_PROVENANCE_UNVERIFIED")
    # Calibration/threshold selection is a separate physical-group partition.
    # Checking train vs test alone would miss leakage through the calibrator.
    train = item.get("train_group_ids")
    calibration = item.get("calibration_group_ids")
    test = item.get("test_group_ids")
    partitions = (train, calibration, test)
    if any(not isinstance(p, list) or not p for p in partitions):
        issues.append("GROUP_TRAIN_CALIBRATION_TEST_NOT_DOCUMENTED")
    if any(isinstance(p, list) and
           (any(not _nonempty_str(g) for g in p) or
            any(p.count(g) > 1 for g in p)) for p in partitions):
        issues.append("INVALID_OR_DUPLICATE_GROUP_IDS")
    # Report known train/test leakage even when calibration provenance is absent.
    # The missing calibration group is a separate failure, not a reason to hide overlap.
    valid_sets = [set(p) if isinstance(p, list) and
                  all(_nonempty_str(g) for g in p) else set() for p in partitions]
    if (valid_sets[0] & valid_sets[1] or valid_sets[0] & valid_sets[2] or
        valid_sets[1] & valid_sets[2]):
        issues.append("GROUP_LEAKAGE")
    if item.get("hyperparameters_selected_on_test", True) is not False:
        issues.append("TEST_SET_SELECTION_LEAKAGE_OR_UNKNOWN")
    if item.get("blind_holdout", False) is not True:
        issues.append("NO_BLIND_HOLDOUT")
    if item.get("measurement_units") in (None, ""):
        issues.append("MISSING_MEASUREMENT_UNITS")
    interval = item.get("confidence_interval")
    if interval is None:
        issues.append("MISSING_UNCERTAINTY_INTERVAL")
    elif (not isinstance(interval, dict) or
          any(isinstance(interval.get(k), bool) or not isinstance(interval.get(k), (int, float)) or
              not math.isfinite(interval[k]) for k in ("low", "high")) or
          interval["low"] > interval["high"]):
        issues.append("INVALID_UNCERTAINTY_INTERVAL")
    # A row-level interval cannot establish uncertainty over independent
    # physical sessions/devices. Require explicit cluster provenance.
    if isinstance(interval, dict):
        if (interval.get("method") not in ("GROUP_BOOTSTRAP", "CLUSTER_ROBUST", "EXACT_GROUP") or
                interval.get("grouping_key") != item.get("grouping_key") or
                interval.get("independent_test_groups") is None or
                isinstance(interval.get("independent_test_groups"), bool) or
                not isinstance(interval.get("independent_test_groups"), int) or
                interval["independent_test_groups"] < 2 or
                (isinstance(test, list) and
                 interval["independent_test_groups"] != len(set(
                     g for g in test if _nonempty_str(g))))):
            issues.append("UNCERTAINTY_GROUPING_NOT_VERIFIED")
        if (isinstance(item.get("metric_value"), (int, float)) and
                not isinstance(item.get("metric_value"), bool) and
                math.isfinite(item["metric_value"]) and
                isinstance(interval.get("low"), (int, float)) and
                isinstance(interval.get("high"), (int, float)) and
                not isinstance(interval.get("low"), bool) and
                not isinstance(interval.get("high"), bool) and
                math.isfinite(interval["low"]) and math.isfinite(interval["high"]) and
                not (interval["low"] <= item["metric_value"] <= interval["high"])):
            issues.append("METRIC_OUTSIDE_REPORTED_CONFIDENCE_INTERVAL")
    if item.get("source_raw_data_accessible", False) is not True:
        issues.append("RAW_DATA_UNAVAILABLE")
    for metric in ("metric_value", "baseline_metric_value"):
        v = item.get(metric)
        if v is not None and (isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v)):
            issues.append("INVALID_" + metric.upper())
    issues.extend(audit_signal_lineage(item))
    return {"id": item.get("id"),
            "status": "G2_PASS_CANDIDATE_REQUIRES_REVIEW" if not issues else "G2_BLOCKED",
            "issues": issues}

def audit_all(ids, records):
    # Never silently pick the last of two conflicting evidence records.
    index = {}
    duplicate_ids = set()
    for record in records:
        if not isinstance(record, dict):
            continue
        rid = record.get("id")
        if rid in index:
            duplicate_ids.add(rid)
        index[rid] = record
    results = []
    for tid in ids:
        result = audit_record(index.get(tid, {"id": tid}))
        if tid in duplicate_ids:
            result["issues"].append("DUPLICATE_EVIDENCE_RECORD_ID")
            result["status"] = "G2_BLOCKED"
        results.append(result)
    return results

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ledger", default="research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json")
    ap.add_argument("--evidence", default="research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json")
    ap.add_argument("--output", default="research/implementation_execution/G2_AUDIT_RESULTS.json")
    args = ap.parse_args()
    ledger = json.loads(Path(args.ledger).read_text())
    evidence = json.loads(Path(args.evidence).read_text())
    results = audit_all([x["id"] for x in ledger["work_items"]], evidence.get("records", []))
    output = {"schema": "hazrob.g2.audit.v1", "scope": "new V4 independent source qualification",
              "g2_pass_candidates": sum(x["status"].startswith("G2_PASS") for x in results),
              "g2_blocked": sum(x["status"] == "G2_BLOCKED" for x in results),
              "records": results}
    Path(args.output).write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"g2_pass_candidates": output["g2_pass_candidates"], "g2_blocked": output["g2_blocked"]}))
if __name__ == "__main__":
    main()
