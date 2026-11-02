"""Exact day-cluster false-alert concentration from authenticated external MQ4 receipts.

Research diagnostic only. Calendar days are descriptive clusters, not independent
Tank exposures. Fractions are represented as integer numerator/denominator pairs.
"""
import argparse
from fractions import Fraction
import json
from pathlib import Path

from g2_mq4_forward_error_burden import run as run_burden

SCHEMA = "hazrob.g2.mq4-false-alert-day-concentration.v1"
SCOPE = "EXTERNAL_MQ4_DESCRIPTIVE_DAY_CLUSTERS_NOT_G2_G3"


def require(condition, label):
    if not condition:
        raise ValueError("MQ4_DAY_FP_" + label)


def fraction_pair(n, d):
    require(type(n) is int and type(d) is int and 0 <= n <= d and d > 0,
            "FRACTION_INVALID")
    return {"numerator": n, "denominator": d}


def make_report(burden):
    require(type(burden) is dict and
            burden.get("schema") == "hazrob.g2.mq4-forward-error-burden.v1" and
            burden.get("scope") == "EXTERNAL_MQ4_OBSERVATIONAL_FORWARD_ONLY_NOT_G2_G3" and
            burden.get("independent_tank_exposures") is False and
            burden.get("g2_qualified") is False and
            burden.get("g3_qualified") is False and
            burden.get("threshold_is_safety_limit") is False,
            "SOURCE_SCOPE")
    days = burden.get("test_days")
    models = burden.get("models")
    require(type(days) is list and len(days) == 7 and
            len(set(days)) == 7 and all(type(d) is str for d in days) and
            type(models) is dict and len(models) == 5 and
            burden.get("rows") == 9325 and
            burden.get("reference_positives") == 1425,
            "SOURCE_SHAPE")
    results = {}
    reference_day_shape = {}
    for name, model in sorted(models.items()):
        require(type(model) is dict and type(model.get("days")) is dict and
                set(model["days"]) == set(days) and
                type(model.get("aggregate")) is dict, "MODEL_SHAPE")
        aggregate = model["aggregate"]
        totals = {"tp": 0, "fn": 0, "fp": 0, "tn": 0}
        daily = {}
        for day in days:
            record = model["days"][day]
            require(type(record) is dict, "DAY_SHAPE")
            for key in totals:
                value = record.get(key)
                require(type(value) is int and value >= 0, "DAY_COUNT")
                totals[key] += value
            require(type(record.get("rows")) is int and
                    record["rows"] == sum(record[k] for k in totals) and
                    type(record.get("reference_positive")) is int and
                    record["reference_positive"] == record["tp"] + record["fn"],
                    "DAY_CONFUSION")
            # All models score the same physical source rows and labels on each day.
            # Aggregate reconciliation alone cannot detect a cross-day transfer.
            reference_shape = (record["rows"], record["reference_positive"])
            if day in reference_day_shape:
                require(reference_day_shape[day] == reference_shape,
                        "CROSS_MODEL_DAY_REFERENCE")
            else:
                reference_day_shape[day] = reference_shape
            daily[day] = {
                "false_positives": record["fp"],
                "reference_negatives": record["fp"] + record["tn"],
            }
        for key, value in totals.items():
            require(type(aggregate.get(key)) is int and
                    aggregate[key] == value, "AGGREGATE_REPLAY")
        negatives = totals["fp"] + totals["tn"]
        require(negatives == 7900 and totals["tp"] + totals["fn"] == 1425,
                "REFERENCE_TOTALS")
        fp_total = totals["fp"]
        largest = max(row["false_positives"] for row in daily.values())
        worst = [day for day in days if daily[day]["false_positives"] == largest] if fp_total else []
        loo = []
        for day in days:
            fp = fp_total - daily[day]["false_positives"]
            neg = negatives - daily[day]["reference_negatives"]
            require(neg > 0, "EMPTY_LEAVE_DAY_OUT_DENOMINATOR")
            loo.append((day, Fraction(fp, neg), fp, neg))
        min_value = min(row[1] for row in loo)
        max_value = max(row[1] for row in loo)
        results[name] = {
            "false_positives": fp_total,
            "reference_negatives": negatives,
            "false_positive_days": [day for day in days if daily[day]["false_positives"] > 0],
            "daily_false_positives": daily,
            "worst_false_positive_day_or_ties": worst,
            "worst_day_share_of_false_positives":
                fraction_pair(largest, fp_total) if fp_total else None,
            "leave_one_day_out_min_false_positive_rate": [
                {"excluded_day": day, **fraction_pair(fp, neg)}
                for day, value, fp, neg in loo if value == min_value
            ],
            "leave_one_day_out_max_false_positive_rate": [
                {"excluded_day": day, **fraction_pair(fp, neg)}
                for day, value, fp, neg in loo if value == max_value
            ],
        }
    return {
        "schema": SCHEMA,
        "scope": SCOPE,
        "source_zip_sha256": burden["source_zip_sha256"],
        "source_csv_sha256": burden["source_csv_sha256"],
        "calendar_days": days,
        "models": results,
        "independent_tank_exposures": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "threshold_is_safety_limit": False,
        "limitations": (
            "Exact arithmetic on seven externally measured calendar-day clusters. "
            "Deleting a day is a sensitivity diagnostic, not a new test, confidence "
            "interval, calibration uncertainty, prospective Tank validation or "
            "certified alarm rate. A zero-alert baseline can have zero false "
            "positives while missing every reference positive."
        ),
    }


def run():
    return make_report(run_burden())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    encoded = json.dumps(run(), ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
