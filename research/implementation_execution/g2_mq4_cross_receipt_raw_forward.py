"""Cross-bind two independently pinned MQ4 receipts (external data, NOT Tank G2).

This is a metadata consistency check. Neither source archive is replayed in CI.
"""
import argparse
import json
from pathlib import Path

from g2_mq4_raw_receipt_audit import audit_bytes as audit_raw_bytes
from g2_mq4_receipt_lineage_ci import audit as audit_forward_lineage
from g2_mq4_receipt_lineage_ci import strict_json, SOURCE_RECEIPT_SHA256

HERE = Path(__file__).resolve().parent
RAW = HERE / "G2_MQ4_RAW_DAY_DIAGNOSTICS_2026-11-02.json"
FORWARD = HERE / "G2_MQ4_FORWARD_4PPM_2026-11-01.json"


def require(condition, code):
    if not condition:
        raise ValueError("MQ4_CROSS_RECEIPT_" + code)


def compare(raw, forward):
    require(type(raw) is dict and type(forward) is dict, "OBJECTS")
    require(raw.get("archive_sha256") == forward.get("zip_sha256"), "ZIP_SHA")
    require(raw.get("member_sha256") == forward.get("csv_sha256"), "CSV_SHA")
    require(type(raw.get("rows")) is int and raw["rows"] == forward.get("source_rows"),
            "TOTAL_ROWS")
    require(raw.get("source") == "external_observational_minnesota_MQ4_not_tank" and
            forward.get("source_only_not_tank") is True, "EXTERNAL_SOURCE")
    require(raw.get("g2_qualified") is False and raw.get("g3_qualified") is False and
            forward.get("g2_qualified") is False and forward.get("g3_qualified") is False,
            "NO_APPROVAL")
    days = raw.get("daily_compact")
    counts = forward.get("test_rows_by_day")
    positives = forward.get("positive_by_test_day")
    test_days = forward.get("test_days")
    require(type(days) is dict and type(counts) is dict and
            type(positives) is dict and type(test_days) is list, "DAY_MAPS")
    require(len(test_days) == len(set(test_days)) and
            set(test_days) == set(counts) == set(positives) and
            set(test_days).issubset(days), "DAY_KEYS")
    require(sum(item["n"] for item in days.values()) == raw["rows"], "RAW_DAY_SUM")
    require(sum(counts.values()) == forward["models"]["RIDGE_MQ4_3"]["aggregate"]["n"],
            "FORWARD_DAY_SUM")
    support = {"no_positive_days": [], "mixed_days": [], "all_positive_days": []}
    for day in test_days:
        item = days[day]
        n, p = counts[day], positives[day]
        require(type(n) is int and type(p) is int and n > 0 and 0 <= p <= n,
                "COUNT_DOMAIN")
        require(item["n"] == n, "DAY_ROW_COUNT_" + day)
        lo, hi = item["ref_min_ppm"], item["ref_max_ppm"]
        if hi < 4:
            require(p == 0, "IMPOSSIBLE_POSITIVE_" + day)
            support["no_positive_days"].append(day)
        elif lo >= 4:
            require(p == n, "IMPOSSIBLE_NEGATIVE_" + day)
            support["all_positive_days"].append(day)
        else:
            # A range crossing the threshold has at least one sample on either side.
            require(0 < p < n, "MIXED_RANGE_CLASS_COUNT_" + day)
            support["mixed_days"].append(day)
    require(sum(positives.values()) == forward["models"]["RIDGE_MQ4_3"]["aggregate"]["pos"],
            "FORWARD_POSITIVE_SUM")
    return {"schema": "hazrob.g2.mq4-raw-forward-cross-receipt.v1",
            "scope": "EXTERNAL_OBSERVATIONAL_METADATA_ONLY_NOT_TANK",
            "source_receipt_sha256": SOURCE_RECEIPT_SHA256,
            "source_rows": raw["rows"], "matched_test_days": len(test_days),
            "matched_test_rows": sum(counts.values()),
            "reference_positives": sum(positives.values()),
            "threshold_support": support,
            "original_zip_replayed_in_ci": False,
            "independent_tank_exposures": False,
            "traceable_calibration": False,
            "g2_qualified": False, "g3_qualified": False}


def audit():
    # Each receipt is independently authenticated before cross-comparison.
    raw_bytes = RAW.read_bytes()
    audit_raw_bytes(raw_bytes)
    audit_forward_lineage()
    return compare(strict_json(raw_bytes), strict_json(FORWARD.read_bytes()))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = audit()
    encoded = json.dumps(result, ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
