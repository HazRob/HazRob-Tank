"""Research-only MQ4 retrospective 4-ppm diagnostic: counts and rates, NOT safety validation."""
import argparse
import hashlib
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "G2_MQ4_EXTERNAL_SYSTEM_SUMMARY_LIBRARY_SNAPSHOT_2026-10-30.json"
SOURCE_SHA256 = "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea"
MODELS = ("TRAIN_MEDIAN_REFERENCE", "RIDGE_MQ4_1", "RIDGE_MQ4_3",
          "RIDGE_MQ4_3_T_RH", "HGB_MQ4_3_T_RH")
POSITIVE = 2346
NEGATIVE = 11840


def audit(raw: bytes) -> dict:
    if hashlib.sha256(raw).hexdigest() != SOURCE_SHA256:
        raise ValueError("G2_MQ4_THRESHOLD_SOURCE_SHA_MISMATCH")
    s = json.loads(raw)
    if (s.get("schema") != "hazrob-tank-v6-mq4-external-system-evaluation/1"
            or s.get("role") != "EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME"):
        raise ValueError("G2_MQ4_THRESHOLD_SOURCE_CONTRACT")
    if (s.get("source_rows"), s.get("days"), s.get("distinct_gases"),
            s.get("interval90_target_not_met"),
            s.get("four_ppm_diagnostic", {}).get("not_safety_threshold"),
            s.get("evidence_policy", {}).get("tank_g3_validated")) != (
                14186, 11, 1, True, True, 0):
        raise ValueError("G2_MQ4_THRESHOLD_SCOPE")
    models = s.get("models")
    if not isinstance(models, list) or [m.get("model") for m in models] != list(MODELS):
        raise ValueError("G2_MQ4_THRESHOLD_MODEL_SET")
    out = []
    for m in models:
        keys = ("total_4ppm_true_positive", "total_4ppm_false_negative",
                "total_4ppm_false_positive", "total_4ppm_true_negative")
        values = [m.get(k) for k in keys]
        if any(type(v) is not int or v < 0 for v in values):
            raise ValueError("G2_MQ4_THRESHOLD_INVALID_COUNTS")
        tp, fn, fp, tn = values
        if tp + fn != POSITIVE or fp + tn != NEGATIVE or sum(values) != s["source_rows"]:
            raise ValueError("G2_MQ4_THRESHOLD_DENOMINATOR_MISMATCH")
        cov = m.get("pooled_interval_coverage")
        if type(cov) not in (float, int) or not math.isfinite(cov) or not 0 <= cov < .9:
            raise ValueError("G2_MQ4_THRESHOLD_INTERVAL_SCOPE")
        out.append({
            "model": m["model"], "true_positive": tp, "false_negative": fn,
            "false_positive": fp, "true_negative": tn,
            "sensitivity": tp / POSITIVE, "miss_rate": fn / POSITIVE,
            "specificity": tn / NEGATIVE, "false_positive_rate": fp / NEGATIVE,
            "precision": tp / (tp + fp) if tp + fp else None,
            "balanced_accuracy": (tp / POSITIVE + tn / NEGATIVE) / 2,
            "pooled_interval_coverage": cov,
        })
    diag = s["four_ppm_diagnostic"]
    if any(diag.get(k) != v for k, v in {
            "source_reference_positive": POSITIVE,
            "hgb_true_positive": out[-1]["true_positive"],
            "hgb_false_negative": out[-1]["false_negative"],
            "hgb_false_positive": out[-1]["false_positive"],
            "hgb_true_negative": out[-1]["true_negative"],
            "ridge_three_with_environment_TP": out[-2]["true_positive"],
            "ridge_three_with_environment_FN": out[-2]["false_negative"],
    }.items()):
        raise ValueError("G2_MQ4_THRESHOLD_CROSS_REPORT_MISMATCH")
    best = max(out, key=lambda x: x["sensitivity"])
    return {
        "schema": "hazrob.g2.mq4-retrospective-threshold-risk-audit.v1",
        "source_library_summary_sha256": SOURCE_SHA256,
        "source_library_snapshot_not_raw_zip": True,
        "source_rows": POSITIVE + NEGATIVE, "source_days": 11, "source_gases": 1,
        "threshold_ppm": 4, "threshold_is_safety_limit": False,
        "reference_positive_rows": POSITIVE, "reference_negative_rows": NEGATIVE,
        "reference_positive_fraction": POSITIVE / (POSITIVE + NEGATIVE),
        "evaluation_protocol": "retrospective 11 day holdouts; 10 of 11 train on future dates; not prospective",
        "models": out,
        "max_observed_sensitivity_model": best["model"],
        "max_observed_sensitivity": best["sensitivity"],
        "all_models_below_90pct_pooled_interval_coverage": True,
        "pooled_rows_not_independent_exposure_groups": True,
        "independent_physical_reference_records_verified": 0,
        "g2_qualified": False, "g3_qualified": False,
        "limitations": "Source-derived pooled 4 ppm diagnostic only; no raw ZIP replay or independent Tank measurements. The 4 ppm cut is not a safety threshold. Serially correlated rows and retrospective future-trained folds preclude prospective safety or confidence claims. No multi-gas identity or device-specific ppm validation.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=SOURCE)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.source.read_bytes())
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Research-only retrospective 4 ppm audit; no G2/G3 qualification")


if __name__ == "__main__":
    main()
