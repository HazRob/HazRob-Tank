"""Source-derived MQ4 prevalence-shift stress test; no field performance inference."""
import argparse
import hashlib
import json
from fractions import Fraction
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "G2_MQ4_EXTERNAL_SYSTEM_SUMMARY_LIBRARY_SNAPSHOT_2026-10-30.json"
EVIDENCE = ROOT / "G2_INDEPENDENT_EVIDENCE.json"
SOURCE_SHA256 = "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea"
PREVALENCES = ((1, 100), (1, 20), (1, 10), (1, 2))
MODELS = ("TRAIN_MEDIAN_REFERENCE", "RIDGE_MQ4_1", "RIDGE_MQ4_3",
          "RIDGE_MQ4_3_T_RH", "HGB_MQ4_3_T_RH")


def fraction_pair(x):
    return [x.numerator, x.denominator]


def project(raw: bytes, evidence_raw: bytes) -> dict:
    if hashlib.sha256(raw).hexdigest() != SOURCE_SHA256:
        raise ValueError("MQ4_PREVALENCE_SOURCE_SHA_MISMATCH")
    source = json.loads(raw)
    if (source.get("schema") != "hazrob-tank-v6-mq4-external-system-evaluation/1"
            or source.get("role") != "EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME"
            or source.get("source_rows") != 14186 or source.get("days") != 11
            or source.get("distinct_gases") != 1
            or source.get("four_ppm_diagnostic", {}).get("not_safety_threshold") is not True
            or source.get("evidence_policy", {}).get("tank_g3_validated") != 0):
        raise ValueError("MQ4_PREVALENCE_SCOPE_INVALID")
    ev = json.loads(evidence_raw)
    if ev.get("schema") != "hazrob.g2.independent-evidence.v1" or ev.get("records") != []:
        raise ValueError("MQ4_PREVALENCE_UNVERIFIED_INDEPENDENT_EVIDENCE")
    models = source.get("models")
    if not isinstance(models, list) or [m.get("model") for m in models] != list(MODELS):
        raise ValueError("MQ4_PREVALENCE_MODEL_SET_INVALID")
    rows = []
    for m in models:
        keys = ("total_4ppm_true_positive", "total_4ppm_false_negative",
                "total_4ppm_false_positive", "total_4ppm_true_negative")
        vals = [m.get(k) for k in keys]
        if any(type(x) is not int or x < 0 for x in vals):
            raise ValueError("MQ4_PREVALENCE_INVALID_COUNTS")
        tp, fn, fp, tn = vals
        if tp + fn != 2346 or fp + tn != 11840:
            raise ValueError("MQ4_PREVALENCE_DENOMINATOR_MISMATCH")
        se = Fraction(tp, tp + fn)
        fpr = Fraction(fp, fp + tn)
        sp = 1 - fpr
        cases = []
        for n, d in PREVALENCES:
            p = Fraction(n, d)
            true_alarm = se * p
            false_alarm = fpr * (1 - p)
            true_no_alarm = sp * (1 - p)
            false_no_alarm = (1 - se) * p
            alarms = true_alarm + false_alarm
            no_alarms = true_no_alarm + false_no_alarm
            cases.append({
                "prevalence": [n, d],
                "expected_alarms_per_1000": fraction_pair(1000 * alarms),
                "expected_false_alarms_per_1000": fraction_pair(1000 * false_alarm),
                "precision": fraction_pair(true_alarm / alarms) if alarms else None,
                "negative_predictive_value": fraction_pair(true_no_alarm / no_alarms) if no_alarms else None,
            })
        rows.append({"model": m["model"], "source_counts_tp_fn_fp_tn": vals,
                     "hypothetical_prevalence_cases": cases})
    return {
        "schema": "hazrob.g2.mq4-prevalence-transport-sensitivity.v1",
        "source_library_summary_sha256": SOURCE_SHA256,
        "reference_source_rows": 14186,
        "reference_source_days": 11,
        "reference_source_gases": 1,
        "source_threshold_ppm": 4,
        "threshold_is_safety_limit": False,
        "prevalence_scenarios_are_hypothetical": True,
        "conditional_on_fixed_sensitivity_specificity": True,
        "prospective_or_tank_validation": False,
        "independent_physical_evidence_records": 0,
        "g2_qualified": False,
        "g3_qualified": False,
        "models": rows,
        "limitations": "Exact rational prevalence projections from retrospective 11-day source confusion counts. Serial rows not independent; 10/11 folds train on future dates. Changing prevalence can also change covariates, sensitivity and specificity. Hypothetical values are NOT prospective predictive values, safety performance, gas identification or Tank calibration.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=SOURCE)
    parser.add_argument("--evidence", type=Path, default=EVIDENCE)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = project(args.source.read_bytes(), args.evidence.read_bytes())
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("MQ4 source-only hypothetical prevalence audit; no G2/G3 promotion")


if __name__ == "__main__":
    main()
