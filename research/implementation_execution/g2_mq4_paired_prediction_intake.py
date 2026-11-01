"""Fail-closed intake of *submitted* MQ4 same-row predictions (research only).

This checks internal alignment and source-summary consistency. It cannot authenticate
a submitted row against the original raw archive, nor establish physical G2/G3.
No original paired prediction rows are bundled with this repository.
"""
import argparse
import hashlib
import itertools
import json
import re
from datetime import date
from pathlib import Path

from g2_mq4_pairwise_frechet_bounds import EVIDENCE, NAMES, SOURCE, project, strict_json

SCHEMA = "hazrob.g2.mq4-paired-predictions.v1"
MODEL_FIELDS = ("total_4ppm_true_positive", "total_4ppm_false_negative",
                "total_4ppm_false_positive", "total_4ppm_true_negative")


def _day(value):
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
        raise ValueError("INVALID_ISO_DAY")
    try:
        date.fromisoformat(value)
    except ValueError as exc:
        raise ValueError("INVALID_ISO_DAY") from exc
    return value


def _row_id(value):
    if not isinstance(value, str) or not value or len(value) > 128 or value.strip() != value:
        raise ValueError("INVALID_ROW_ID")
    return value



def validate_full_fold_partition(folds):
    """Check a declared 11-day, 9-train/1-calibration/1-test partition.

    This verifies internal arithmetic only, not the source's actual fold plan.
    """
    if not isinstance(folds, dict) or len(folds) != 11:
        raise ValueError("INVALID_FOLD_DAY_COUNT")
    days = set(folds)
    calibration = {}
    for test_day, train_days in folds.items():
        _day(test_day)
        if not isinstance(train_days, (tuple, list)) or len(train_days) != 9:
            raise ValueError("INVALID_FOLD_TRAINING_PARTITION")
        if any(not isinstance(d, str) or d not in days for d in train_days):
            raise ValueError("TRAIN_DAY_NOT_IN_SOURCE_DAYS")
        if len(set(train_days)) != 9 or test_day in train_days:
            raise ValueError("INVALID_FOLD_TRAINING_PARTITION")
        remaining = days - set(train_days) - {test_day}
        if len(remaining) != 1:
            raise ValueError("INVALID_FOLD_CALIBRATION_PARTITION")
        calibration[test_day] = next(iter(remaining))
    return dict(sorted(calibration.items()))


def audit(raw, source_raw, evidence_raw, *, require_full=True):
    # project() checks the pinned source SHA, scope, five model identities and
    # strict independent-evidence records=[]. Do not bypass this check.
    project(source_raw, evidence_raw)
    source = strict_json(source_raw)
    payload = strict_json(raw)
    if not isinstance(payload, dict) or payload.get("schema") != SCHEMA:
        raise ValueError("INVALID_INTAKE_SCHEMA")
    if payload.get("origin") != "EXTERNAL_SOURCE_RESEARCH_ONLY":
        raise ValueError("INVALID_ORIGIN")
    if payload.get("source_summary_sha256") != hashlib.sha256(source_raw).hexdigest():
        raise ValueError("SOURCE_LINEAGE_MISMATCH")
    rows = payload.get("rows")
    if not isinstance(rows, list) or not rows:
        raise ValueError("MISSING_PAIRED_ROWS")
    seen = set()
    folds = {}
    counts = {name: [0, 0, 0, 0] for name in NAMES}
    paired = {a + "|" + b: [0, 0, 0, 0]
              for a, b in itertools.combinations(NAMES, 2)}
    positive = 0
    future_days = set()
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("INVALID_ROW")
        rid = _row_id(row.get("row_id"))
        if rid in seen:
            raise ValueError("DUPLICATE_ROW_ID")
        seen.add(rid)
        day = _day(row.get("test_day"))
        if row.get("fold_test_day") != day:
            raise ValueError("TEST_FOLD_MISMATCH")
        sha = row.get("raw_row_sha256")
        if not isinstance(sha, str) or not re.fullmatch(r"[0-9a-f]{64}", sha):
            raise ValueError("INVALID_RAW_ROW_HASH")
        train = row.get("train_days")
        if not isinstance(train, list) or not train or any(not isinstance(v, str) for v in train):
            raise ValueError("INVALID_TRAIN_DAYS")
        train = tuple(_day(v) for v in train)
        if len(set(train)) != len(train) or day in train:
            raise ValueError("TRAIN_TEST_DAY_OVERLAP")
        if day in folds and folds[day] != train:
            raise ValueError("INCONSISTENT_FOLD_PROVENANCE")
        folds[day] = train
        if any(v > day for v in train):
            future_days.add(day)
        truth = row.get("reference_positive_4ppm")
        if type(truth) is not bool:
            raise ValueError("INVALID_REFERENCE_LABEL")
        positive += int(truth)
        predictions = row.get("predicted_positive_4ppm")
        if not isinstance(predictions, dict) or set(predictions) != set(NAMES):
            raise ValueError("INVALID_MODEL_PREDICTIONS")
        if any(type(v) is not bool for v in predictions.values()):
            raise ValueError("INVALID_MODEL_PREDICTIONS")
        for name in NAMES:
            pred = predictions[name]
            # TP, FN, FP, TN
            idx = 0 if truth and pred else 1 if truth else 2 if pred else 3
            counts[name][idx] += 1
        for a, b in itertools.combinations(NAMES, 2):
            ea = predictions[a] != truth
            eb = predictions[b] != truth
            paired[a + "|" + b][(int(ea) << 1) | int(eb)] += 1
    calibration_days = validate_full_fold_partition(folds) if require_full else {}
    if require_full:
        if len(rows) != source["source_rows"] or len(folds) != source["days"]:
            raise ValueError("INCOMPLETE_SOURCE_ROWS")
        if positive != 2346 or len(rows) - positive != 11840:
            raise ValueError("REFERENCE_MARGINAL_MISMATCH")
        for model in source["models"]:
            if counts[model["model"]] != [model[k] for k in MODEL_FIELDS]:
                raise ValueError("MODEL_MARGINAL_MISMATCH")
    return {
        "schema": "hazrob.g2.mq4-paired-intake-audit.v1",
        "submitted_rows": len(rows),
        "submitted_days": len(folds),
        "declared_calibration_day_by_test_day": calibration_days,
        "declared_fold_partition_structurally_checked": bool(require_full),
        "original_source_fold_partition_independently_verified": False,
        "source_summary_sha256": hashlib.sha256(source_raw).hexdigest(),
        "submitted_positive_rows": positive,
        "model_confusion_counts_tp_fn_fp_tn": counts,
        "pairwise_submitted_rows_both_correct_a_correct_b_wrong_a_wrong_b_correct_both_wrong": paired,
        "test_days_with_future_training": sorted(future_days),
        "internal_submitted_row_ids_unique": True,
        "internal_submitted_fold_alignment": True,
        "full_source_marginals_match": bool(require_full),
        "original_raw_archive_row_hashes_independently_verified": False,
        "physical_independent_exposure_evidence": False,
        "paired_inferential_test_authorized": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "limitations": "Submitted-row arithmetic and declared 11-day fold partition only. Hash syntax and fold claims are not authenticated against the original ZIP. Source retrospective folds can train on future dates. No McNemar inference, Tank calibration, gas identity or safety claims."
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", type=Path, required=True,
                    help="External paired predictions; no real rows are bundled")
    ap.add_argument("--source", type=Path, default=SOURCE)
    ap.add_argument("--evidence", type=Path, default=EVIDENCE)
    ap.add_argument("--output", type=Path, required=True)
    args = ap.parse_args()
    result = audit(args.input.read_bytes(), args.source.read_bytes(), args.evidence.read_bytes())
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Submitted-row arithmetic only; independent source/physical G2 evidence absent")


if __name__ == "__main__":
    main()
