"""Recompute paired G2 model/baseline outcomes from the SAME blind exposures.

This is a research-only, post-unblinding internal-consistency diagnostic.
It verifies per-group count arithmetic and exposure-key uniqueness, not the
authenticity of physical experiments, independent groups, or blind release.
"""
import argparse
import csv
import hashlib
import io
import json
import unicodedata
from collections import Counter
from pathlib import Path

from g2_paired_delta_diagnostic import (
    _canonical_group_id, paired_cluster_accuracy_delta,
)

PREDICTION_FIELDS = {
    "exposure_id", "group_id", "true_reference",
    "predicted_reference", "baseline_predicted_reference",
}
METRIC_FIELDS = {
    "group_id", "sample_count", "correct_count", "baseline_correct_count",
}


def _valid_label(value):
    """Keep chemical-label Unicode semantics; reject ambiguous control text."""
    return (isinstance(value, str) and bool(value.strip())
            and value == value.strip()
            and unicodedata.normalize("NFC", value) == value
            and not any(
                unicodedata.category(c).startswith("C")
                or unicodedata.category(c) in ("Zl", "Zp")
                or (unicodedata.category(c) == "Zs" and c != " ")
                for c in value
            ))


def _read_csv(path, required):
    raw = Path(path).read_bytes()
    try:
        with io.StringIO(raw.decode("utf-8"), newline="") as stream:
            reader = csv.DictReader(stream, strict=True)
            header = reader.fieldnames
            if (not header or len(header) != len(set(header))
                    or not required.issubset(set(header))
                    or any(not _canonical_group_id(name) for name in header)):
                raise ValueError("PAIRED_REPLAY_CSV_HEADER_INVALID")
            rows = list(reader)
    except (UnicodeError, csv.Error) as exc:
        raise ValueError("PAIRED_REPLAY_CSV_FORMAT_INVALID") from exc
    if (not rows or any(
            None in row
            or any(value is None or not _valid_label(value)
                   for key, value in row.items() if key in required)
            or any(value is None for value in row.values())
            for row in rows)):
        raise ValueError("PAIRED_REPLAY_CSV_ROW_INVALID")
    return raw, rows


def _full_table_semantic_sha256(rows):
    """Bind EVERY CSV field, including optional provenance, independent of order.

    This detects edits to declared device/session/reference metadata that do
    not affect scored labels. It does NOT authenticate that metadata.
    """
    canonical_rows = [
        [[key, row[key]] for key in sorted(row)]
        for row in rows
    ]
    payload = json.dumps(sorted(canonical_rows), ensure_ascii=False,
                         separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _integer(value):
    try:
        number = int(value)
    except (ValueError, TypeError) as exc:
        raise ValueError("PAIRED_REPLAY_METRIC_COUNT_INVALID") from exc
    if number < 0 or str(number) != value:
        raise ValueError("PAIRED_REPLAY_METRIC_COUNT_INVALID")
    return number


def replay_paired_predictions(predictions_csv, metrics_per_group_csv,
                              *, seed=20261029, resamples=2000):
    """Independently reconcile post-blind prediction rows and group metrics.

    Model and baseline are scored on identical exposure IDs in each row.
    Group names and physical independence are only declarations: external
    source provenance and blinded protocol review remain mandatory.
    """
    predictions_raw, predictions = _read_csv(
        predictions_csv, PREDICTION_FIELDS)
    metrics_raw, metrics = _read_csv(metrics_per_group_csv, METRIC_FIELDS)

    seen_exposures = set()
    counts = Counter()
    model_correct = Counter()
    baseline_correct = Counter()
    both_correct = both_wrong = model_only = baseline_only = 0
    assignment = []
    outcome_rows = []
    per_group_outcomes = {}

    for row in predictions:
        exposure, group = row["exposure_id"], row["group_id"]
        if (not _canonical_group_id(exposure)
                or not _canonical_group_id(group)):
            raise ValueError("PAIRED_REPLAY_PHYSICAL_ID_INVALID")
        if exposure in seen_exposures:
            raise ValueError("PAIRED_REPLAY_DUPLICATE_EXPOSURE_ID")
        seen_exposures.add(exposure)
        assignment.append([exposure, group])
        outcome_rows.append([exposure, group, row["true_reference"],
                             row["predicted_reference"],
                             row["baseline_predicted_reference"]])
        truth = row["true_reference"]
        model = row["predicted_reference"] == truth
        baseline = row["baseline_predicted_reference"] == truth
        counts[group] += 1
        model_correct[group] += int(model)
        baseline_correct[group] += int(baseline)
        both_correct += int(model and baseline)
        both_wrong += int(not model and not baseline)
        model_only += int(model and not baseline)
        baseline_only += int(baseline and not model)
        if group not in per_group_outcomes:
            per_group_outcomes[group] = {
                "both_correct": 0, "both_wrong": 0,
                "model_only_correct": 0, "baseline_only_correct": 0,
            }
        outcome_key = (
            "both_correct" if model and baseline else
            "model_only_correct" if model else
            "baseline_only_correct" if baseline else "both_wrong"
        )
        per_group_outcomes[group][outcome_key] += 1

    declared = {}
    for row in metrics:
        group = row["group_id"]
        if not _canonical_group_id(group):
            raise ValueError("PAIRED_REPLAY_PHYSICAL_ID_INVALID")
        if group in declared:
            raise ValueError("PAIRED_REPLAY_DUPLICATE_METRIC_GROUP")
        declared[group] = tuple(
            _integer(row[field]) for field in
            ("sample_count", "correct_count", "baseline_correct_count")
        )
    if set(declared) != set(counts):
        raise ValueError("PAIRED_REPLAY_METRIC_GROUP_MISMATCH")
    for group in counts:
        expected = (counts[group], model_correct[group],
                    baseline_correct[group])
        if declared[group] != expected:
            raise ValueError("PAIRED_REPLAY_METRIC_COUNT_MISMATCH")

    # Per-group paired discordance exposes cancellation across physical groups.
    # It is descriptive, not a McNemar significance test or evidence of
    # independent physical exposures.
    group_pairing = []
    for group in sorted(counts):
        outcomes = per_group_outcomes[group]
        if (sum(outcomes.values()) != counts[group] or
                outcomes["both_correct"] + outcomes["model_only_correct"]
                != model_correct[group] or
                outcomes["both_correct"] + outcomes["baseline_only_correct"]
                != baseline_correct[group]):
            raise ValueError("PAIRED_REPLAY_GROUP_DISCORDANCE_INVALID")
        group_pairing.append({
            "group_id": group,
            "exposures": counts[group],
            **outcomes,
            "paired_net_model_advantage_count": (
                outcomes["model_only_correct"] -
                outcomes["baseline_only_correct"]
            ),
        })
    if (sum(g["model_only_correct"] for g in group_pairing) != model_only or
            sum(g["baseline_only_correct"] for g in group_pairing) != baseline_only):
        raise ValueError("PAIRED_REPLAY_GROUP_DISCORDANCE_INVALID")

    # This check uses the paired post-unblinding outcomes, rather than
    # inferring pair agreement from two marginal group accuracies.
    if (model_only - baseline_only
            != sum(model_correct.values()) - sum(baseline_correct.values())):
        raise ValueError("PAIRED_REPLAY_DISCORDANCE_ARITHMETIC_INVALID")

    result = paired_cluster_accuracy_delta(
        dict(counts), dict(model_correct), dict(baseline_correct),
        seed=seed, resamples=resamples,
    )
    result.update({
        "paired_outcomes_recomputed_from_same_exposure_rows": True,
        "per_group_metrics_recomputed_from_prediction_rows": True,
        "paired_both_correct": both_correct,
        "paired_both_wrong": both_wrong,
        "paired_model_only_correct": model_only,
        "paired_baseline_only_correct": baseline_only,
        "paired_outcomes_by_declared_physical_group": group_pairing,
        "prediction_rows_semantic_sha256": hashlib.sha256(
            json.dumps(sorted(outcome_rows), ensure_ascii=False,
                       separators=(",", ":")).encode("utf-8")
        ).hexdigest(),
        "prediction_full_rows_semantic_sha256": (
            _full_table_semantic_sha256(predictions)
        ),
        "metrics_full_rows_semantic_sha256": (
            _full_table_semantic_sha256(metrics)
        ),
        "full_row_digest_scope": (
            "ALL_DECLARED_CSV_FIELDS_INCLUDING_OPTIONAL_PROVENANCE;"
            "NOT_AUTHENTICATION_OF_PHYSICAL_SOURCE_OR_G2_G3_APPROVAL"
        ),
        "predictions_csv_sha256": hashlib.sha256(predictions_raw).hexdigest(),
        "metrics_per_group_csv_sha256": hashlib.sha256(metrics_raw).hexdigest(),
        "exposure_group_assignment_sha256": hashlib.sha256(
            json.dumps(sorted(assignment), ensure_ascii=False,
                       separators=(",", ":")).encode("utf-8")
        ).hexdigest(),
        "pairing_scope": (
            "POST_UNBLINDING_INTERNAL_ROW_ARITHMETIC_ONLY;"
            "NOT_PHYSICAL_GROUP_INDEPENDENCE_OR_G2_G3_APPROVAL"
        ),
    })
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--predictions-csv", type=Path, required=True)
    parser.add_argument("--per-group-csv", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=20261029)
    parser.add_argument("--resamples", type=int, default=2000)
    args = parser.parse_args()
    result = replay_paired_predictions(
        args.predictions_csv, args.per_group_csv,
        seed=args.seed, resamples=args.resamples,
    )
    args.output.write_text(
        json.dumps(result, sort_keys=True, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print("Paired post-blind arithmetic verified; NOT G2/G3 approval")


if __name__ == "__main__":
    main()
