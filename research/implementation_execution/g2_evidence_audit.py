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
from g2_evidence_bundle_gate import audit_evidence_bundle
from g2_cross_artifact_gate import audit_cross_artifact
from g2_raw_source_gate import audit_raw_source_bytes
from g2_r0_source_gate import audit_r0_source_bytes

def _read_strict_input_json(path):
    """Reject ambiguous JSON in the top-level G2 ledger and evidence register.

    Evidence bundles already use strict JSON, but the controlling register and
    execution ledger must not silently accept duplicate keys or nonfinite
    numbers. SHA256 integrity alone does not resolve ambiguous JSON semantics.
    """
    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("G2_INPUT_DUPLICATE_JSON_KEY:" + key)
            result[key] = value
        return result

    def reject_constant(token):
        raise ValueError("G2_INPUT_NONFINITE_NUMBER:" + token)

    def finite_float(token):
        value = float(token)
        if not math.isfinite(value):
            raise ValueError("G2_INPUT_NONFINITE_NUMBER:" + token)
        return value

    def finite_int(token):
        # Python accepts arbitrarily large JSON integers, but later
        # math.isfinite(int_value) can raise OverflowError. Reject these
        # before any candidate can crash the 80-technique audit.
        try:
            value = int(token)
            if not math.isfinite(float(value)):
                raise OverflowError("integer outside finite numeric domain")
        except (ValueError, OverflowError) as exc:
            raise ValueError("G2_INPUT_INTEGER_OUT_OF_RANGE") from exc
        return value

    return json.loads(
        Path(path).read_text(encoding="utf-8"),
        object_pairs_hook=unique_object,
        parse_constant=reject_constant,
        parse_float=finite_float,
        parse_int=finite_int,
    )


def load_g2_inputs(ledger_path, evidence_path):
    """Load controlling manifests without silently treating invalid evidence as empty."""
    ledger = _read_strict_input_json(ledger_path)
    evidence = _read_strict_input_json(evidence_path)
    if (not isinstance(ledger, dict) or
            ledger.get("schema_version") != "hazrob.tank.chemcore80.execution-ledger-v4" or
            not isinstance(ledger.get("work_items"), list) or
            not all(isinstance(item, dict) and isinstance(item.get("id"), str)
                    for item in ledger["work_items"])):
        raise ValueError("G2_EXECUTION_LEDGER_SCHEMA_INVALID")
    if (not isinstance(evidence, dict) or
            evidence.get("schema") != "hazrob.g2.independent-evidence.v1" or
            not isinstance(evidence.get("records"), list)):
        raise ValueError("G2_EVIDENCE_REGISTER_SCHEMA_INVALID")
    return ledger, evidence


def load_canonical_technique_ids(path):
    """Load the authoritative 80-ID registry; a count of 80 alone is insufficient.

    An execution ledger containing 80 substituted IDs must not silently
    certify that the original 80 techniques were all audited.
    """
    try:
        registry = _read_strict_input_json(path)
    except (OSError, UnicodeError, ValueError, TypeError) as exc:
        raise ValueError("G2_CANONICAL_TECHNIQUE_REGISTRY_INVALID") from exc
    if (not isinstance(registry, dict) or
            registry.get("total_techniques") != 80 or
            isinstance(registry.get("total_techniques"), bool) or
            not isinstance(registry.get("techniques"), list)):
        raise ValueError("G2_CANONICAL_TECHNIQUE_REGISTRY_INVALID")
    entries = registry["techniques"]
    ids = [entry.get("id") if isinstance(entry, dict) else None
           for entry in entries]
    if (len(ids) != 80 or
            any(not isinstance(tid, str) or
                re.fullmatch(r"[0-9]{2}\.[0-9]{1,2}", tid) is None
                for tid in ids) or
            len(set(ids)) != 80):
        raise ValueError("G2_CANONICAL_TECHNIQUE_REGISTRY_INVALID")
    return ids


REQUIRED = ("raw_sha256", "dataset_uri", "sensor_metadata", "grouping_key",
            "train_group_ids", "calibration_group_ids", "test_group_ids", "target_definition",
            "metric_name", "metric_value", "baseline_metric_value",
            "protocol_sha256", "evaluation_artifact_sha256")
HEX64 = re.compile(r"^[0-9a-f]{64}$")
PHYSICAL_GROUP_KEYS = {"session_id", "device_session_id", "batch_id", "campaign_id", "site_day_device"}

def _nonempty_str(value):
    return isinstance(value, str) and bool(value.strip())

def audit_record(item, repo_root=Path(".")):
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
    # Accuracy and its interval are proportions. A signed or >100% value
    # must not become a review candidate simply because all self-declared
    # JSON artifacts repeat the same number and their hashes are correct.
    # Other metric families (e.g. RMSE) intentionally have different domains.
    if item.get("metric_name") == "classification_accuracy":
        for field in ("metric_value", "baseline_metric_value"):
            v = item.get(field)
            if (isinstance(v, (int, float)) and not isinstance(v, bool)
                    and math.isfinite(v) and not 0.0 <= v <= 1.0):
                issues.append("CLASSIFICATION_ACCURACY_OUT_OF_RANGE:" + field)
        if isinstance(interval, dict):
            for field in ("low", "high"):
                v = interval.get(field)
                if (isinstance(v, (int, float)) and not isinstance(v, bool)
                        and math.isfinite(v) and not 0.0 <= v <= 1.0):
                    issues.append("CLASSIFICATION_INTERVAL_OUT_OF_RANGE:" + field)
    issues.extend(audit_signal_lineage(item))
    bundle_issues = audit_evidence_bundle(item, repo_root=repo_root)
    issues.extend(bundle_issues)
    if not bundle_issues:
        # A manifest checksum is a claim, not evidence that original raw
        # bytes were ever acquired or can be replayed by an independent CI.
        issues.extend(audit_raw_source_bytes(item, repo_root=repo_root))
        issues.extend(audit_r0_source_bytes(item, repo_root=repo_root))
        issues.extend(audit_cross_artifact(item, repo_root=repo_root))
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

def validate_ledger_scope(ids, records, expected_count=80, canonical_ids=None):
    """Reject incomplete, substituted, or malformed technique/evidence IDs."""
    if (not isinstance(ids, list) or len(ids) != expected_count or
            any(not _nonempty_str(tid) or tid != tid.strip() for tid in ids) or
            len(set(ids)) != expected_count):
        raise ValueError("G2_LEDGER_SCOPE_NOT_80_UNIQUE_TECHNIQUES")
    if canonical_ids is not None and set(ids) != set(canonical_ids):
        raise ValueError("G2_LEDGER_CANONICAL_TECHNIQUE_IDS_MISMATCH")
    if not isinstance(records, list) or any(not isinstance(r, dict) for r in records):
        raise ValueError("INVALID_G2_EVIDENCE_RECORDS")
    if any(not _nonempty_str(r.get("id")) or
           r["id"] != r["id"].strip() for r in records):
        raise ValueError("INVALID_G2_EVIDENCE_RECORD_ID")
    known = set(ids)
    unknown = [r["id"] for r in records if r["id"] not in known]
    if unknown:
        raise ValueError("UNKNOWN_G2_EVIDENCE_TECHNIQUE_IDS: " + repr(unknown))
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ledger", default="research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json")
    ap.add_argument("--evidence", default="research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json")
    ap.add_argument("--output", default="research/implementation_execution/G2_AUDIT_RESULTS.json")
    args = ap.parse_args()
    ledger, evidence = load_g2_inputs(args.ledger, args.evidence)
    ids = [x["id"] for x in ledger["work_items"]]
    records = evidence.get("records", [])
    canonical_ids = load_canonical_technique_ids("research/PROGRESS_80.json")
    validate_ledger_scope(ids, records, canonical_ids=canonical_ids)
    results = audit_all(ids, records)
    output = {"schema": "hazrob.g2.audit.v1", "scope": "new V4 independent source qualification",
              "g2_pass_candidates": sum(x["status"].startswith("G2_PASS") for x in results),
              "g2_blocked": sum(x["status"] == "G2_BLOCKED" for x in results),
              "records": results}
    Path(args.output).write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"g2_pass_candidates": output["g2_pass_candidates"], "g2_blocked": output["g2_blocked"]}))
if __name__ == "__main__":
    main()
