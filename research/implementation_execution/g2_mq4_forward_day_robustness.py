"""Day-level sensitivity audit of MQ4 forward-only observational folds.

Reuses the full per-fold report, not the compact summary. Descriptive stress
analysis only: six calendar days are not six certified independent exposures.
No G2/G3 promotion, reference calibration or Tank-transfer inference.
"""
import argparse
import json
import math
from datetime import date
from pathlib import Path

CHANNELS = ("MQ4_1", "MQ4_2", "MQ4_3")


def require(ok, code):
    if not ok:
        raise ValueError("MQ4_DAY_ROBUSTNESS_" + code)


def close(actual, reported):
    return math.isclose(actual, reported, rel_tol=1e-9, abs_tol=1e-7)


def analyze(full):
    require(full.get("schema") == "hazrob.g2.mq4-forward-calendar-day.v1", "SCHEMA")
    require(all(full.get("scientific_state", {}).get(k) is False for k in
                ("g2_qualified", "g3_tank_qualified", "physical_independence_verified",
                 "reference_calibration_verified", "blinded_holdout_verified")), "PROMOTION")
    days = full.get("common_test_days")
    source_days = full.get("days")
    gaps = full.get("gaps")
    require(isinstance(days, list) and len(days) >= 2 and len(set(days)) == len(days)
            and days == sorted(days), "DAYS")
    require(isinstance(source_days, list) and source_days == sorted(set(source_days))
            and set(days).issubset(source_days), "SOURCE_DAYS")
    require(isinstance(gaps, list) and gaps and len(gaps) == len(set(gaps))
            and all(type(g) is int and 0 <= g <= 30 for g in gaps), "GAPS")
    require(set(full.get("channels", {})) == set(CHANNELS), "CHANNELS")
    require(type(full.get("min_train_days")) is int and
            full["min_train_days"] >= 2, "TRAIN_MINIMUM")
    try:
        dates = [date.fromisoformat(d) for d in days]
    except (ValueError, TypeError) as exc:
        raise ValueError("MQ4_DAY_ROBUSTNESS_DATES") from exc
    require(len(set(dates)) == len(dates), "DATES")
    output = {
        "schema": "hazrob.g2.mq4-forward-day-robustness.v1",
        "raw_member_sha256": full.get("raw_member_sha256"),
        "common_test_days": days,
        "gaps": gaps,
        "calendar_day_count": len(days),
        "channels": {},
        "scientific_state": {
            "g2_qualified": False, "g3_tank_qualified": False,
            "physical_independence_verified": False,
            "reference_calibration_verified": False,
            "blinded_holdout_verified": False,
        },
        "limitations": "Day-level descriptive sensitivity of observational source data. Calendar days may be dependent; neither row counts nor jackknife resampling establish independent exposures, calibrated ppm, blind holdout or Tank validity."
    }
    expected_rows = None
    for channel in CHANNELS:
        channel_result = {}
        require(set(full["channels"][channel]) == set(map(str, gaps)), "GAP_CHANNELS")
        for gap in gaps:
            source = full["channels"][channel][str(gap)]
            folds = source.get("folds", {})
            require(set(folds) == set(days), "FOLD_DAYS")
            total_model = total_baseline = 0.0
            per_day = []
            n_total = 0
            negative = 0
            for d, parsed in zip(days, dates):
                fold = folds[d]
                n = fold.get("test_rows")
                require(type(n) is int and n > 0, "TEST_ROWS")
                train_days = fold.get("train_days")
                require(isinstance(train_days, list) and len(train_days) >= full["min_train_days"]
                        and len(set(train_days)) == len(train_days), "TRAIN_DAYS")
                try:
                    train_dates = [date.fromisoformat(x) for x in train_days]
                except (ValueError, TypeError) as exc:
                    raise ValueError("MQ4_DAY_ROBUSTNESS_TRAIN_DATE") from exc
                require(train_days == [t for t in source_days
                                       if (parsed - date.fromisoformat(t)).days > gap],
                        "TRAIN_MEMBERSHIP")
                m = fold.get("rmse_model_ppb")
                b = fold.get("rmse_train_mean_baseline_ppb")
                require(all(type(v) in (int, float) and math.isfinite(v) and v >= 0
                            for v in (m, b)), "FOLD_RMSE")
                ms, bs = n * m*m, n * b*b
                total_model += ms
                total_baseline += bs
                n_total += n
                negative += ms > bs
                per_day.append((d, n, ms, bs))
            require(n_total == source.get("total_test_rows") and
                    close(math.sqrt(total_model/n_total), source["pooled_rmse_model_ppb"]) and
                    close(math.sqrt(total_baseline/n_total), source["pooled_rmse_baseline_ppb"]) and
                    negative == source.get("days_worse_than_training_mean"), "POOLED_RECONCILIATION")
            if expected_rows is None:
                expected_rows = [(d, n) for d, n, _, _ in per_day]
            require(expected_rows == [(d, n) for d, n, _, _ in per_day], "TEST_POPULATION")
            skill = 1 - total_model / total_baseline if total_baseline > 0 else None
            require((skill is None and source.get("pooled_skill_vs_train_mean") is None)
                    or (skill is not None and close(skill, source.get("pooled_skill_vs_train_mean"))),
                    "SKILL_RECONCILIATION")
            loo = []
            for d, _, ms, bs in per_day:
                remaining_baseline = total_baseline - bs
                loo.append({"excluded_test_day": d,
                            "pooled_skill_remaining": 1 - (total_model-ms)/remaining_baseline
                            if remaining_baseline > 0 else None})
            defined_loo = [x["pooled_skill_remaining"] for x in loo
                           if x["pooled_skill_remaining"] is not None]
            day_skills = [1 - ms/bs for _, _, ms, bs in per_day if bs > 0]
            channel_result[str(gap)] = {
                "calendar_test_days": len(per_day), "test_rows": n_total,
                "pooled_skill": skill,
                "equal_day_weighted_rmse_model_ppb": math.sqrt(sum(ms/n for _, n, ms, _ in per_day)/len(per_day)),
                "equal_day_weighted_rmse_baseline_ppb": math.sqrt(sum(bs/n for _, n, _, bs in per_day)/len(per_day)),
                "negative_skill_days": negative,
                "positive_skill_days": sum(ms < bs for _, _, ms, bs in per_day),
                "zero_baseline_days": len(per_day) - len(day_skills),
                "mean_defined_day_skill": sum(day_skills)/len(day_skills) if day_skills else None,
                "leave_one_test_day_out_skill_min": min(defined_loo) if defined_loo else None,
                "leave_one_test_day_out_skill_max": max(defined_loo) if defined_loo else None,
                "leave_one_test_day_out": loo,
                "max_day_share_model_sse": max(ms for _, _, ms, _ in per_day)/total_model
                if total_model > 0 else None,
                "max_day_share_baseline_sse": max(bs for _, _, _, bs in per_day)/total_baseline
                if total_baseline > 0 else None,
            }
        output["channels"][channel] = channel_result
    return output


def compact(report):
    """Compact pinned metrics; full jackknife fold exclusions remain reproducible."""
    result = json.loads(json.dumps(report))
    for gaps in result["channels"].values():
        for metrics in gaps.values():
            metrics.pop("leave_one_test_day_out")
    result["artifact_kind"] = "source-derived day-level sensitivity summary; full exclusions reproducible from original ZIP"
    return result


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--full-forward-report", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--compact", action="store_true")
    a = p.parse_args()
    full = json.loads(a.full_forward_report.read_text(encoding="utf-8"))
    result = analyze(full)
    a.output.write_text(json.dumps(compact(result) if a.compact else result,
                                   sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("Day-level MQ4 observational sensitivity complete; G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
