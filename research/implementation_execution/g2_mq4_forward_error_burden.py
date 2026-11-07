"""Source-only forward-day error burden; NOT Tank G2/G3 qualification.

Authenticate the immutable external MQ4 forward receipt and its dependent
receipts before deriving confusion-matrix metrics. Calendar days are the
descriptive reporting unit; individual rows are not independent exposures.
"""
import argparse
import json
from pathlib import Path

from g2_mq4_receipt_lineage_ci import ROOT, FILENAMES, audit, strict_json

SCHEMA = "hazrob.g2.mq4-forward-error-burden.v1"


def require(ok, code):
    if not ok:
        raise ValueError("MQ4_ERROR_BURDEN_" + code)


def ratio(numerator, denominator):
    return round(numerator / denominator, 8) if denominator else None


def summary(n, positives, false_negative, false_positive):
    for v in (n, positives, false_negative, false_positive):
        require(type(v) is int and v >= 0, "COUNTS_NOT_NONNEGATIVE_INTEGERS")
    require(positives <= n and false_negative <= positives and
            false_positive <= n - positives, "CONFUSION_OUT_OF_RANGE")
    tp = positives - false_negative
    tn = n - positives - false_positive
    alerts = tp + false_positive
    return {
        "rows": n, "reference_positive": positives,
        "tp": tp, "fn": false_negative, "fp": false_positive, "tn": tn,
        "predicted_positive": alerts,
        "precision_ppv": ratio(tp, alerts),
        "sensitivity": ratio(tp, positives),
        "specificity": ratio(tn, n - positives),
        "balanced_accuracy": (
            round((tp / positives + tn / (n - positives)) / 2, 8)
            if positives and n > positives else None),
        "f1": ratio(2 * tp, 2 * tp + false_positive + false_negative),
        "false_positives_per_1000_reference_negatives":
            round(1000 * false_positive / (n - positives), 6)
            if n > positives else None,
    }


def evaluate(source):
    require(type(source) is dict and
            source.get("schema") == "hazrob.g2.mq4-forward-4ppm-source.v1" and
            source.get("source_only_not_tank") is True and
            source.get("g2_qualified") is False and
            source.get("g3_qualified") is False and
            source.get("threshold_4ppm_is_safety_limit") is False,
            "SOURCE_SCOPE")
    days = source.get("test_days")
    rows = source.get("test_rows_by_day")
    positives = source.get("positive_by_test_day")
    models = source.get("models")
    require(type(days) is list and len(days) == 7 and
            len(set(days)) == 7 and type(rows) is dict and
            type(positives) is dict and
            set(rows) == set(positives) == set(days) and
            type(models) is dict and len(models) == 5,
            "TEST_SCOPE")
    require(sum(rows.values()) == 9325 and
            sum(positives.values()) == 1425, "SOURCE_TOTALS")
    results = {}
    for name, model in sorted(models.items()):
        require(type(model) is dict and
                type(model.get("days")) is list and
                len(model["days"]) == len(days) and
                type(model.get("aggregate")) is dict, "MODEL_SHAPE")
        day_results = {}
        for expected_day, row in zip(days, model["days"]):
            require(type(row) is dict and row.get("day") == expected_day and
                    row.get("pos") == positives[expected_day], "DAY_ALIGNMENT")
            day_results[expected_day] = summary(
                rows[expected_day], positives[expected_day],
                row.get("fn"), row.get("fp"))
        a = model["aggregate"]
        expected = summary(9325, 1425, sum(x["fn"] for x in day_results.values()),
                           sum(x["fp"] for x in day_results.values()))
        for key, value in (("n", 9325), ("pos", 1425),
                           ("tp", expected["tp"]), ("fn", expected["fn"]),
                           ("fp", expected["fp"]), ("tn", expected["tn"])):
            require(type(a.get(key)) is int and a[key] == value,
                    "AGGREGATE_RECONCILIATION")
        # Undefined precision is not zero: a model that never alarms
        # cannot claim perfect positive predictive value.
        results[name] = {
            "aggregate": expected,
            "days": day_results,
            "zero_reference_positive_days": [
                d for d in days if positives[d] == 0],
            "false_positive_alerts_on_zero_positive_days": sum(
                day_results[d]["fp"] for d in days if positives[d] == 0),
            "days_with_no_alerts": [
                d for d in days if day_results[d]["predicted_positive"] == 0],
        }
    return {
        "schema": SCHEMA,
        "scope": "EXTERNAL_MQ4_OBSERVATIONAL_FORWARD_ONLY_NOT_G2_G3",
        "source_zip_sha256": source["zip_sha256"],
        "source_csv_sha256": source["csv_sha256"],
        "test_days": days,
        "rows": 9325,
        "reference_positives": 1425,
        "models": results,
        "independent_tank_exposures": False,
        "g2_qualified": False,
        "g3_qualified": False,
        "threshold_is_safety_limit": False,
        "limitations": (
            "Row-weighted descriptive metrics on seven external calendar days. "
            "Rows are autocorrelated and are not independent physical Tank "
            "exposures. PPV depends on this dataset prevalence, not field "
            "prevalence. Undefined denominators are null. No uncertainty "
            "interval, certified concentration, or safety performance."
        ),
    }


def run():
    audit()  # SHA-pinned source and complete four-receipt lineage
    source = strict_json((ROOT / FILENAMES["source"]).read_bytes())
    return evaluate(source)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = run()
    encoded = json.dumps(result, ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
