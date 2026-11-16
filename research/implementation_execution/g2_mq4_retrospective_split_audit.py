"""Research-only audit: retrospective MQ4 day holdout is not prospective validation.
The original Library script was manually inspected and SHA-pinned; CI only
replays its pinned summary and the transcribed split schedule, not raw ZIP.
"""
import argparse
import hashlib
import json
import math
from datetime import date, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "G2_MQ4_EXTERNAL_SYSTEM_SUMMARY_LIBRARY_SNAPSHOT_2026-10-30.json"
SOURCE_SHA256 = "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea"
SCRIPT_SHA256 = "367658f9884ff8355c89d803f67eb7ced78ec7299759d58ddd1742ba9a1b368b"
MODELS = ("TRAIN_MEDIAN_REFERENCE", "RIDGE_MQ4_1", "RIDGE_MQ4_3",
          "RIDGE_MQ4_3_T_RH", "HGB_MQ4_3_T_RH")


def audit(raw, days=None):
    if hashlib.sha256(raw).hexdigest() != SOURCE_SHA256:
        raise ValueError("G2_RETROSPECTIVE_SOURCE_SHA_MISMATCH")
    s = json.loads(raw)
    if s.get("schema") != "hazrob-tank-v6-mq4-external-system-evaluation/1" or s.get("role") != "EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME":
        raise ValueError("G2_RETROSPECTIVE_SOURCE_CONTRACT")
    if (s.get("source_rows"), s.get("days"), s.get("sensor_instances"), s.get("distinct_gases"), s.get("interval90_target_not_met"), s.get("evidence_policy", {}).get("tank_g3_validated")) != (14186, 11, 3, 1, True, 0):
        raise ValueError("G2_RETROSPECTIVE_SOURCE_SCOPE")
    models = s.get("models")
    if not isinstance(models, list) or [m.get("model") for m in models] != list(MODELS):
        raise ValueError("G2_RETROSPECTIVE_MODEL_SCOPE")
    if any(type(m.get("pooled_interval_coverage")) not in (float, int) or not math.isfinite(m["pooled_interval_coverage"]) or not 0 <= m["pooled_interval_coverage"] < .9 for m in models):
        raise ValueError("G2_RETROSPECTIVE_COVERAGE_SCOPE")
    expected = [date(2021, 7, 21) + timedelta(days=i) for i in range(11)]
    if days is None:
        days = [d.isoformat() for d in expected]
    if not isinstance(days, list) or len(days) != 11 or any(type(d) is not str for d in days) or len(set(days)) != 11:
        raise ValueError("G2_RETROSPECTIVE_DAY_SCOPE")
    try:
        parsed = [date.fromisoformat(d) for d in days]
    except ValueError as exc:
        raise ValueError("G2_RETROSPECTIVE_DAY_FORMAT") from exc
    if parsed != expected:
        raise ValueError("G2_RETROSPECTIVE_DAY_SCOPE")
    # Pinned original Library script: calday=days[(fold-1)%len(days)]
    # train=(d.day!=day)&(d.day!=calday); 9 train, 1 cal, 1 test.
    folds = []
    for i, test in enumerate(days):
        cal = days[(i - 1) % 11]
        train = [d for d in days if d not in (test, cal)]
        if len(train) != 9 or len(set(train + [test, cal])) != 11:
            raise ValueError("G2_RETROSPECTIVE_PARTITION")
        future = [d for d in train if d > test]
        folds.append({"test_day": test, "calibration_day": cal, "training_days": train,
                      "future_training_days": future, "future_calibration_day": cal > test,
                      "prospective_valid": not future and cal <= test})
    n_future = sum(bool(f["future_training_days"]) for f in folds)
    n_future_cal = sum(f["future_calibration_day"] for f in folds)
    n_prospective = sum(f["prospective_valid"] for f in folds)
    if (n_future, n_future_cal, n_prospective) != (10, 1, 1):
        raise ValueError("G2_RETROSPECTIVE_RECONCILIATION")
    return {
        "schema": "hazrob.g2.mq4-retrospective-protocol-audit.v1",
        "source_library_summary_sha256": SOURCE_SHA256,
        "source_library_benchmark_script_sha256": SCRIPT_SHA256,
        "source_library_snapshot_not_raw_zip": True,
        "source_rows": 14186, "source_days": 11, "folds": folds,
        "folds_with_future_training_days": n_future,
        "folds_with_future_calibration_day": n_future_cal,
        "folds_without_future_training_or_calibration": n_prospective,
        "models_below_nominal_90pct_pooled_interval_coverage": list(MODELS),
        "protocol_comparison": {
            "library_study": "retrospective leave-one-day-out, 9 train, cyclic previous-day calibration",
            "research_forward_study": "forward-only prior-day training; six common test days; embargo 0/1/2 days",
            "pooled_metrics_directly_comparable": False,
            "future_day_training_is_label_leakage_within_group_holdout": False,
            "future_day_training_invalidates_prospective_claim": True
        },
        "physical_independent_reference_records_verified": 0,
        "g2_qualified": False, "g3_qualified": False,
        "limitations": "This is a protocol audit, not a new raw-data benchmark. Grouped retrospective holdout is not same-day leakage, but 10/11 folds train on future dates and the first fold calibrates on a future date. The original script is hash-pinned and manually inspected; CI does not re-read Library script or raw ZIP. No independent Tank exposures, traceable calibration, multi-gas discrimination, prospective field performance or G2/G3 qualification."
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--source", type=Path, default=SOURCE)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    result = audit(a.source.read_bytes())
    a.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("MQ4 retrospective protocol audit only; G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
