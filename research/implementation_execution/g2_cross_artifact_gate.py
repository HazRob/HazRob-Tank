"""Cross-artifact consistency checks for G2 evidence bundles.

Requires a byte-verified bundle first. This checks *internal consistency* only:
it does not authenticate the physical experiment, source or CI run.
"""
import csv
import json
import math
from collections import Counter
from pathlib import Path
from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy


def _read_csv(base, name):
    """Reject ambiguous CSV schemas even when the file hash is valid.

    DictReader silently overwrites duplicate column names, puts surplus cells
    under the None key, and pads short rows with None. Any of these can hide
    the true physical grouping, blind labels or per-group counts.
    """
    with (base / name).open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream, strict=True)
        header = reader.fieldnames
        if (not header or any(not col or col != col.strip() for col in header)
                or len(set(header)) != len(header)):
            raise ValueError("CSV_HEADER_AMBIGUOUS")
        rows = list(reader)
        if any(None in row or any(value is None for value in row.values())
               for row in rows):
            raise ValueError("CSV_ROW_WIDTH_MISMATCH")
        return rows


def _same_number(left, right):
    return (isinstance(left, (int, float)) and not isinstance(left, bool)
            and isinstance(right, (int, float)) and not isinstance(right, bool)
            and math.isfinite(left) and math.isfinite(right)
            and math.isclose(left, right, rel_tol=1e-12, abs_tol=1e-12))


def audit_cross_artifact(record, repo_root=Path(".")):
    """Require consistent original groups, splits, blind predictions and metrics.

    Caller must run audit_evidence_bundle first and only invoke this when
    all fourteen file hashes and container formats have passed.
    """
    base = (Path(repo_root).resolve() / "research" / "g2" / "evidence"
            / record["id"] / record["dataset_id"] / record["run_id"])
    issues = []
    try:
        raw = json.loads((base / "raw_manifest.json").read_text(encoding="utf-8"))
        source = json.loads((base / "source_qualification.json").read_text(encoding="utf-8"))
        signal = json.loads((base / "signal_lineage.json").read_text(encoding="utf-8"))
        split = json.loads((base / "split_manifest.json").read_text(encoding="utf-8"))
        calibration = json.loads((base / "calibration_manifest.json").read_text(encoding="utf-8"))
        metrics = json.loads((base / "metrics_summary.json").read_text(encoding="utf-8"))
        uncertainty = json.loads((base / "uncertainty_grouped.json").read_text(encoding="utf-8"))
        groups = _read_csv(base, "group_manifest.csv")
        predictions = _read_csv(base, "predictions_blind.csv")
        per_group = _read_csv(base, "metrics_per_group.csv")
    except (OSError, UnicodeError, ValueError, csv.Error, KeyError, TypeError):
        return ["CROSS_ARTIFACT_READ_FAILED"]

    if (raw.get("raw_sha256") != record.get("raw_sha256") or
            raw.get("dataset_uri") != record.get("dataset_uri")):
        issues.append("RAW_MANIFEST_RECORD_MISMATCH")
    # A matching SHA256 proves the bundle was not altered after recording;
    # it does not prove the source or sensor lineage agrees with the claim.
    # Bind source and transformation metadata to their byte-verified files.
    source_fields = ("source_kind", "dataset_uri", "raw_sha256",
                     "raw_sha256_verified_from_bytes", "source_raw_data_accessible")
    if (not isinstance(source, dict) or
            any(key not in source or source[key] != record.get(key)
                for key in source_fields)):
        issues.append("SOURCE_QUALIFICATION_RECORD_MISMATCH")
    signal_fields = ("signal_processing", "measurement_units", "sensor_metadata")
    if (not isinstance(signal, dict) or
            any(key not in signal or signal[key] != record.get(key)
                for key in signal_fields)):
        issues.append("SIGNAL_LINEAGE_RECORD_MISMATCH")
    parts = ("train_group_ids", "calibration_group_ids", "test_group_ids")
    if any(not isinstance(split.get(p), list) or
           split[p] != record.get(p) for p in parts):
        issues.append("SPLIT_MANIFEST_RECORD_MISMATCH")
    if calibration.get("calibration_group_ids") != record.get("calibration_group_ids"):
        issues.append("CALIBRATION_GROUPS_RECORD_MISMATCH")

    expected_groups = {}
    for part in parts:
        values = record.get(part)
        if isinstance(values, list):
            for value in values:
                if isinstance(value, str):
                    expected_groups.setdefault(value, part)
    required_group_columns = {"exposure_id", "group_id"}
    if not groups or any(not required_group_columns <= set(row) for row in groups):
        issues.append("GROUP_MANIFEST_SCHEMA_MISMATCH")
        return issues
    # A textual group ID alone cannot prove a physical holdout. Bind each
    # declared group to the source's physical columns and reject aliases:
    # two different group IDs for the same device/session would otherwise
    # allow one physical session to appear in train and blind test.
    source_field = record.get("grouping_source_field")
    physical_columns = ([name.strip() for name in source_field.split(",")]
                        if isinstance(source_field, str) else [])
    if (not physical_columns or any(not name or
            not name.replace("_", "").isalnum() for name in physical_columns)
            or len(set(physical_columns)) != len(physical_columns)):
        issues.append("GROUPING_SOURCE_COLUMNS_INVALID")
        physical_columns = []
    elif not set(physical_columns) <= set(groups[0]):
        issues.append("GROUPING_SOURCE_COLUMNS_MISSING")
        physical_columns = []
    # A declared physical grouping key must be backed by the actual source
    # identity columns, not arbitrary per-row identifiers (e.g. sample_id).
    # Additional source columns are allowed, but may not replace the minimum.
    required_by_grouping_key = {
        "session_id": {"session_id"},
        "device_session_id": {"device_id", "session_id"},
        "batch_id": {"batch_id"},
        "campaign_id": {"campaign_id"},
        "site_day_device": {"site_id", "day_id", "device_id"},
    }
    required_physical = required_by_grouping_key.get(record.get("grouping_key"))
    if required_physical is None or not required_physical.issubset(set(physical_columns)):
        issues.append("GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY")
    # Only the canonical physical identity for the declared grouping key
    # determines independence. Extra source fields (e.g. sample_id, timestamp)
    # may be retained as provenance, but must never make one physical session
    # look like two independent groups across train/calibration/test.
    identity_columns = (sorted(required_physical)
                        if required_physical is not None and
                        required_physical.issubset(set(physical_columns)) else [])
    if physical_columns and identity_columns:
        group_to_physical = {}
        physical_to_group = {}
        for row in groups:
            gid = row.get("group_id")
            physical = tuple((row.get(name) or "").strip()
                             for name in identity_columns)
            if (any(not value for value in physical) or
                    any(not (row.get(name) or "").strip()
                        for name in physical_columns)):
                issues.append("GROUPING_SOURCE_VALUE_MISSING")
                continue
            if gid in group_to_physical and group_to_physical[gid] != physical:
                issues.append("GROUP_ID_MAPS_TO_MULTIPLE_PHYSICAL_UNITS")
            if physical in physical_to_group and physical_to_group[physical] != gid:
                issues.append("PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS")
            group_to_physical[gid] = physical
            physical_to_group[physical] = gid

    exposures = {}
    counts = Counter()
    for row in groups:
        eid, gid = row.get("exposure_id"), row.get("group_id")
        if not eid or not gid or eid in exposures:
            issues.append("DUPLICATE_OR_EMPTY_EXPOSURE_ID")
        if gid not in expected_groups:
            issues.append("GROUP_MANIFEST_UNASSIGNED_GROUP")
        exposures[eid] = gid
        counts[gid] += 1
    if set(counts) != set(expected_groups):
        issues.append("GROUP_MANIFEST_MISSING_SPLIT_GROUPS")

    required_prediction_columns = {"exposure_id", "group_id"}
    if not predictions or any(not required_prediction_columns <= set(row) for row in predictions):
        issues.append("BLIND_PREDICTIONS_SCHEMA_MISMATCH")
    else:
        seen = set()
        for row in predictions:
            eid, gid = row.get("exposure_id"), row.get("group_id")
            if not eid or eid in seen or exposures.get(eid) != gid or expected_groups.get(gid) != "test_group_ids":
                issues.append("BLIND_PREDICTION_EXPOSURE_OR_GROUP_LEAKAGE")
            seen.add(eid)
        test_exposures = {eid for eid, gid in exposures.items()
                          if expected_groups.get(gid) == "test_group_ids"}
        if seen != test_exposures:
            issues.append("BLIND_PREDICTION_COVERAGE_MISMATCH")

    if not per_group or any(not {"group_id", "sample_count"} <= set(row) for row in per_group):
        issues.append("METRICS_PER_GROUP_SCHEMA_MISMATCH")
    else:
        seen = set()
        for row in per_group:
            gid = row.get("group_id")
            if not gid or gid in seen or expected_groups.get(gid) != "test_group_ids":
                issues.append("METRICS_PER_GROUP_SPLIT_MISMATCH")
            seen.add(gid)
            try:
                n = int(row["sample_count"])
                if n < 1 or str(n) != row["sample_count"] or n != counts.get(gid, 0):
                    issues.append("METRICS_PER_GROUP_COUNT_MISMATCH")
            except (ValueError, TypeError):
                issues.append("METRICS_PER_GROUP_COUNT_MISMATCH")
        if seen != set(record.get("test_group_ids", [])):
            issues.append("METRICS_PER_GROUP_COVERAGE_MISMATCH")

    # Classification accuracy must be computed from the actual blind rows,
    # not accepted solely because two JSON summaries agree with one another.
    # A matching digest proves integrity, not the arithmetic of the claim.
    interval = record.get("confidence_interval")
    if record.get("metric_name") == "classification_accuracy":
        required_truth = {"exposure_id", "group_id", "gas_reference_id"}
        # Baseline accuracy must be reproducible from the *same* blind rows.
        # A self-declared baseline_metric_value in two matching JSON files is
        # not independent arithmetic evidence and could inflate an improvement.
        required_pred = {"exposure_id", "group_id", "true_reference",
                         "predicted_reference", "baseline_predicted_reference"}
        required_group_metric = {"group_id", "sample_count", "correct_count"}
        if (any(not required_truth <= set(row) for row in groups) or
                any(not required_pred <= set(row) for row in predictions) or
                any(not required_group_metric <= set(row) for row in per_group)):
            issues.append("ACCURACY_RECOMPUTATION_FIELDS_MISSING")
        else:
            truths = {row["exposure_id"]: row["gas_reference_id"] for row in groups}
            correct = Counter()
            baseline_correct = 0
            total = 0
            for row in predictions:
                eid, gid = row["exposure_id"], row["group_id"]
                truth = row["true_reference"]
                prediction = row["predicted_reference"]
                baseline_prediction = row["baseline_predicted_reference"]
                if (not truth.strip() or not prediction.strip() or
                        not baseline_prediction.strip() or truth != truths.get(eid)):
                    issues.append("BLIND_PREDICTION_TRUTH_MISMATCH")
                if truth == prediction:
                    correct[gid] += 1
                if truth == baseline_prediction:
                    baseline_correct += 1
                total += 1
            if total == 0 or not _same_number(record.get("metric_value"),
                                                sum(correct.values()) / total):
                issues.append("BLIND_ACCURACY_RECOMPUTATION_MISMATCH")
            if total == 0 or not _same_number(record.get("baseline_metric_value"),
                                                baseline_correct / total):
                issues.append("BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH")
            for row in per_group:
                try:
                    value = int(row["correct_count"])
                    if (str(value) != row["correct_count"] or value < 0 or
                            value > counts.get(row["group_id"], 0) or
                            value != correct[row["group_id"]]):
                        issues.append("PER_GROUP_CORRECT_COUNT_MISMATCH")
                except (ValueError, TypeError):
                    issues.append("PER_GROUP_CORRECT_COUNT_MISMATCH")
            issues.extend(audit_group_bootstrap_accuracy(
                {gid: counts[gid] for gid in record.get("test_group_ids", []) if gid in counts},
                {gid: correct[gid] for gid in record.get("test_group_ids", []) if gid in counts},
                interval))

    # Only classification_accuracy has a verified arithmetic path from the
    # blind exposure-level predictions and its grouped uncertainty. Other
    # metric families (RMSE, F1, etc.) must not reach review-candidate status
    # merely because the summary JSON files and hashes agree. Add a separate
    # metric-specific recomputation and uncertainty audit before enabling them.
    if record.get("metric_name") != "classification_accuracy":
        issues.append("METRIC_FAMILY_NOT_RECOMPUTABLE")

    if (metrics.get("metric_name") != record.get("metric_name") or
            not _same_number(metrics.get("metric_value"), record.get("metric_value")) or
            not _same_number(metrics.get("baseline_metric_value"), record.get("baseline_metric_value"))):
        issues.append("METRICS_SUMMARY_RECORD_MISMATCH")
    if not isinstance(interval, dict) or any(
        not (_same_number(uncertainty.get(k), interval.get(k)) if k in ("low", "high")
             else uncertainty.get(k) == interval.get(k))
        for k in ("low", "high", "method", "grouping_key", "independent_test_groups",
                  "seed", "resamples", "confidence_level", "resampling_unit",
                  "statistic", "quantile_method")
    ):
        issues.append("UNCERTAINTY_RECORD_MISMATCH")
    return sorted(set(issues))
