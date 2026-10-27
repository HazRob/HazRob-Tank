"""Cross-artifact consistency checks for G2 evidence bundles.

Requires a byte-verified bundle first. This checks *internal consistency* only:
it does not authenticate the physical experiment, source or CI run.
"""
import csv
import hashlib
import json
import math
import unicodedata
from collections import Counter
from pathlib import Path
from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy


def _csv_has_control(value):
    """Reject invisible Unicode format/control code points in evidence CSV.

    SHA256 confirms bytes, not that a reviewer sees the same physical group
    identifier or gas label. Reject C* categories (including C1, bidi and
    zero-width formatting) and Unicode line/paragraph separators. Printable
    multilingual letters, marks and ordinary spaces remain valid.
    """
    return any(unicodedata.category(ch).startswith("C") or
               unicodedata.category(ch) in ("Zl", "Zp") for ch in value)


def _read_csv(base, name):
    """Reject ambiguous CSV schemas even when the file hash is valid.

    DictReader silently overwrites duplicate column names, puts surplus cells
    under the None key, and pads short rows with None. Any of these can hide
    the true physical grouping, blind labels or per-group counts.
    """
    with (base / name).open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream, strict=True)
        header = reader.fieldnames
        if (not header or any(not col or col != col.strip() or
                              _csv_has_control(col) for col in header)
                or len(set(header)) != len(header)):
            raise ValueError("CSV_HEADER_AMBIGUOUS")
        rows = list(reader)
        if any(None in row or any(value is None or _csv_has_control(value)
                                  for value in row.values()) for row in rows):
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
        protocol = json.loads((base / "protocol_locked.json").read_text(encoding="utf-8"))
        calibration = json.loads((base / "calibration_manifest.json").read_text(encoding="utf-8"))
        metrics = json.loads((base / "metrics_summary.json").read_text(encoding="utf-8"))
        uncertainty = json.loads((base / "uncertainty_grouped.json").read_text(encoding="utf-8"))
        groups = _read_csv(base, "group_manifest.csv")
        predictions = _read_csv(base, "predictions_blind.csv")
        per_group = _read_csv(base, "metrics_per_group.csv")
    except (OSError, UnicodeError, ValueError, csv.Error, KeyError, TypeError):
        return ["CROSS_ARTIFACT_READ_FAILED"]

    # The byte gate rejects non-object JSON, but this replay function is also
    # invoked independently by regression tests and external research tools.
    # Never let an array/scalar at any verified JSON path crash the audit.
    documents = (raw, source, signal, split, protocol, calibration,
                 metrics, uncertainty)
    if any(not isinstance(document, dict) for document in documents):
        return ["CROSS_ARTIFACT_JSON_SCHEMA_INVALID"]

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

    # A blind baseline must be defined in the byte-verified locked protocol,
    # not invented after seeing the blind labels to inflate the comparison.
    # This checks consistency and documented group isolation; it cannot prove
    # that the protocol was actually registered before unblinding.
    protocol_fields = ("target_definition", "metric_name", "grouping_key",
                       "train_group_ids", "calibration_group_ids", "test_group_ids")
    if (not isinstance(protocol, dict) or any(
            protocol.get(field) != record.get(field) for field in protocol_fields)):
        issues.append("LOCKED_PROTOCOL_RECORD_MISMATCH")
    baseline = protocol.get("baseline_model") if isinstance(protocol, dict) else None
    if (not isinstance(baseline, dict) or
            not isinstance(baseline.get("algorithm_id"), str) or
            not baseline["algorithm_id"].strip() or
            baseline.get("prediction_column") != "baseline_predicted_reference" or
            not isinstance(baseline.get("fitted_group_ids"), list) or
            baseline.get("fit_policy") not in ("TRAIN_ONLY", "NO_FIT")):
        issues.append("BASELINE_PROTOCOL_NOT_LOCKED")
    else:
        fitted = baseline["fitted_group_ids"]
        train_ids = record.get("train_group_ids")
        if (any(not isinstance(g, str) or not g.strip() for g in fitted) or
                len(fitted) != len(set(fitted)) or
                not isinstance(train_ids, list) or
                any(not isinstance(g, str) for g in train_ids) or
                not set(fitted).issubset(set(train_ids)) or
                (baseline["fit_policy"] == "NO_FIT" and fitted) or
                (baseline["fit_policy"] == "TRAIN_ONLY" and not fitted)):
            issues.append("BASELINE_TRAINING_GROUP_LEAKAGE_OR_INVALID_FIT")
        if baseline.get("selected_on_test") is not False:
            issues.append("BASELINE_TEST_SELECTION_NOT_FROZEN")

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
    required_physical = (required_by_grouping_key.get(record["grouping_key"])
                         if isinstance(record.get("grouping_key"), str) else None)
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
        if seen != {gid for gid, part in expected_groups.items() if part == "test_group_ids"}:
            issues.append("METRICS_PER_GROUP_COVERAGE_MISMATCH")

    # Classification accuracy must be computed from the actual blind rows,
    # not accepted solely because two JSON summaries agree with one another.
    # A matching digest proves integrity, not the arithmetic of the claim.
    interval = record.get("confidence_interval")
    if record.get("metric_name") == "classification_accuracy":
        # Replay the locked majority-class baseline from training labels, not
        # only from blind predictions. A self-declared algorithm name and a
        # matching blind accuracy cannot establish baseline reproducibility.
        # Other algorithms require explicit replay adapters before review.
        if isinstance(baseline, dict):
            if baseline.get("algorithm_id") != "majority_train":
                issues.append("BASELINE_ALGORITHM_REPLAY_UNAVAILABLE")
            elif baseline.get("fit_policy") != "TRAIN_ONLY":
                issues.append("BASELINE_MAJORITY_TRAIN_FIT_POLICY_INVALID")
            else:
                fitted_ids = baseline.get("fitted_group_ids")
                if (not isinstance(fitted_ids, list) or not fitted_ids or
                        any(not isinstance(g, str) for g in fitted_ids)):
                    issues.append("BASELINE_TRAINING_LABELS_UNAVAILABLE")
                elif (not isinstance(record.get("train_group_ids"), list) or
                      any(not isinstance(g, str) for g in record["train_group_ids"]) or
                      set(fitted_ids) != set(record["train_group_ids"])):
                    # A majority_train baseline must use all training groups.
                    # Selecting a subset can artificially weaken its accuracy.
                    issues.append("BASELINE_MAJORITY_TRAIN_PARTIAL_FIT")
                else:
                    labels = [row.get("gas_reference_id", "").strip()
                              for row in groups
                              if row.get("group_id") in fitted_ids]
                    if not labels or any(not label for label in labels):
                        issues.append("BASELINE_TRAINING_LABELS_UNAVAILABLE")
                    else:
                        # Bind exact training labels to the locked protocol.
                        # A changed label can preserve the majority class and
                        # escape prediction-only replay.
                        label_rows = sorted(
                            [[row["exposure_id"], row["group_id"], row["gas_reference_id"]]
                             for row in groups if row.get("group_id") in fitted_ids],
                            key=lambda values: (values[0], values[1]))
                        label_bytes = json.dumps(
                            label_rows, ensure_ascii=False,
                            separators=(",", ":")).encode("utf-8")
                        actual_digest = hashlib.sha256(label_bytes).hexdigest()
                        locked_digest = baseline.get("training_labels_sha256")
                        if (not isinstance(locked_digest, str) or
                                len(locked_digest) != 64 or
                                any(c not in "0123456789abcdef" for c in locked_digest)):
                            issues.append("BASELINE_TRAINING_LABEL_DIGEST_NOT_LOCKED")
                        elif actual_digest != locked_digest:
                            issues.append("BASELINE_TRAINING_LABEL_DIGEST_MISMATCH")
                        label_counts = Counter(labels)
                        max_count = max(label_counts.values())
                        winners = [label for label, n in label_counts.items()
                                   if n == max_count]
                        if len(winners) != 1:
                            if baseline.get("tie_policy") == "LEXICOGRAPHIC_ASC":
                                expected_label = min(winners)
                            else:
                                expected_label = None
                                issues.append("BASELINE_MAJORITY_TIE_POLICY_UNSPECIFIED")
                        else:
                            expected_label = winners[0]
                        if (expected_label is not None and
                                any(row.get("baseline_predicted_reference") != expected_label
                                    for row in predictions)):
                            issues.append("BASELINE_MAJORITY_TRAIN_REPLAY_MISMATCH")
        required_truth = {"exposure_id", "group_id", "gas_reference_id"}
        # Baseline accuracy must be reproducible from the *same* blind rows.
        # A self-declared baseline_metric_value in two matching JSON files is
        # not independent arithmetic evidence and could inflate an improvement.
        required_pred = {"exposure_id", "group_id", "true_reference",
                         "predicted_reference", "baseline_predicted_reference"}
        required_group_metric = {"group_id", "sample_count", "correct_count",
                                 "baseline_correct_count"}
        if (any(not required_truth <= set(row) for row in groups) or
                any(not required_pred <= set(row) for row in predictions) or
                any(not required_group_metric <= set(row) for row in per_group)):
            issues.append("ACCURACY_RECOMPUTATION_FIELDS_MISSING")
        else:
            truths = {row["exposure_id"]: row["gas_reference_id"] for row in groups}
            correct = Counter()
            baseline_correct = 0
            baseline_correct_by_group = Counter()
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
                    baseline_correct_by_group[gid] += 1
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
                # A global baseline accuracy can conceal group-specific errors:
                # recompute the baseline numerator for every blind group too.
                try:
                    baseline_n = int(row["baseline_correct_count"])
                    if (str(baseline_n) != row["baseline_correct_count"] or
                            baseline_n < 0 or baseline_n > counts.get(row["group_id"], 0) or
                            baseline_n != baseline_correct_by_group[row["group_id"]]):
                        issues.append("PER_GROUP_BASELINE_CORRECT_COUNT_MISMATCH")
                except (ValueError, TypeError):
                    issues.append("PER_GROUP_BASELINE_CORRECT_COUNT_MISMATCH")
            issues.extend(audit_group_bootstrap_accuracy(
                {gid: counts[gid] for gid, part in expected_groups.items()
                 if part == "test_group_ids" and gid in counts},
                {gid: correct[gid] for gid, part in expected_groups.items()
                 if part == "test_group_ids" and gid in counts},
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
