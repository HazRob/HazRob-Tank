"""G2 evidence audit: fail closed without independent raw measured holdout evidence."""
import argparse
import json
from pathlib import Path

REQUIRED = ("raw_sha256", "dataset_uri", "sensor_metadata", "grouping_key",
            "train_group_ids", "test_group_ids", "target_definition",
            "metric_name", "metric_value", "baseline_metric_value",
            "protocol_sha256", "evaluation_artifact_sha256")

def audit_record(item):
    issues = []
    for key in REQUIRED:
        if key not in item or item[key] is None or item[key] == "":
            issues.append("MISSING_" + key.upper())
    if item.get("source_kind") != "INDEPENDENT_MEASURED":
        issues.append("NOT_INDEPENDENT_MEASURED_SOURCE")
    train, test = item.get("train_group_ids"), item.get("test_group_ids")
    if not isinstance(train, list) or not isinstance(test, list) or not train or not test:
        issues.append("GROUP_HOLDOUT_NOT_DOCUMENTED")
    elif set(map(str, train)) & set(map(str, test)):
        issues.append("GROUP_LEAKAGE")
    if item.get("hyperparameters_selected_on_test", True) is not False:
        issues.append("TEST_SET_SELECTION_LEAKAGE_OR_UNKNOWN")
    if item.get("blind_holdout", False) is not True:
        issues.append("NO_BLIND_HOLDOUT")
    if item.get("measurement_units") in (None, ""):
        issues.append("MISSING_MEASUREMENT_UNITS")
    if item.get("confidence_interval") is None:
        issues.append("MISSING_UNCERTAINTY_INTERVAL")
    if item.get("source_raw_data_accessible", False) is not True:
        issues.append("RAW_DATA_UNAVAILABLE")
    return {"id": item.get("id"), "status": "G2_PASS_CANDIDATE_REQUIRES_REVIEW" if not issues else "G2_BLOCKED", "issues": issues}

def audit_all(ids, records):
    index = {r.get("id"): r for r in records}
    return [audit_record(index.get(tid, {"id": tid})) for tid in ids]

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
