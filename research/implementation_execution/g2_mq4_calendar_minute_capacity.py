"""Fail-closed MQ4 calendar-minute capacity check of pinned REPORTED metadata.

This checks three existing research summaries, NOT the original ZIP bytes.
A minute-bin bound is conditional on the intake's unique-timestamp and
minute-averaging assertions; it cannot independently verify either claim.
"""
import argparse
import json
from datetime import datetime, timedelta
from pathlib import Path

SOURCE = "MQ-ACQ-2026-06"
ARCHIVE = "7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9"
RAW = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"


def require(ok, code):
    if not ok:
        raise ValueError("MQ4_CALENDAR_CAPACITY_" + code)


def parse_minute(value):
    require(type(value) is str, "TIMESTAMP")
    try:
        dt = datetime.strptime(value, "%Y-%m-%d %H:%M:%S")
    except ValueError:
        raise ValueError("MQ4_CALENDAR_CAPACITY_TIMESTAMP") from None
    require(dt.strftime("%Y-%m-%d %H:%M:%S") == value and dt.second == 0,
            "MINUTE_ALIGNMENT")
    return dt


def audit(intake, lodo, segments):
    require(type(intake) is dict and type(lodo) is dict and
            type(segments) is dict, "OBJECTS")
    require(intake.get("schema") == "hazrob.g2.external-library-intake-candidate.v1" and
            lodo.get("schema") == "hazrob.g2.mq4-lodo-two-way-purge-raw-summary.v1" and
            segments.get("schema") == "hazrob.g2.mq4-strict-contiguous-segment-verification.v1",
            "SCHEMAS")
    entries = intake.get("source_entries")
    require(type(entries) is list and all(type(e) is dict for e in entries),
            "SOURCE_ENTRIES")
    matches = [e for e in entries if e.get("source_id") == SOURCE]
    require(len(matches) == 1, "SOURCE_UNIQUE")
    src = matches[0]
    require(lodo.get("source") == segments.get("source_id") == SOURCE and
            intake.get("archive_sha256") == lodo.get("source_archive_sha256") ==
            segments.get("source_archive_sha256") == ARCHIVE and
            src.get("sha256") == lodo.get("raw_member_sha256") ==
            segments.get("source_member_sha256") == RAW, "SOURCE_PIN")
    require(src.get("unique_timestamps") is True and
            "averaged to minute" in str(src.get("raw_preprocessing", "")).lower(),
            "REPORTED_MINUTE_UNIQUENESS")
    first = parse_minute(src.get("first_timestamp"))
    last = parse_minute(src.get("last_timestamp"))
    require(first < last, "TIMESTAMP_ORDER")
    folds = lodo.get("folds")
    require(type(folds) is dict and len(folds) >= 2 and
            type(lodo.get("calendar_dates")) is int and
            len(folds) == lodo["calendar_dates"], "FOLDS")
    days = sorted(folds)
    require(days[0] == first.date().isoformat() and
            days[-1] == last.date().isoformat() and
            all(type(d) is str and len(d) == 10 and
                datetime.strptime(d, "%Y-%m-%d").date().isoformat() == d
                for d in days), "DATE_BOUNDS")
    require(type(src.get("rows")) is int and type(lodo.get("raw_rows")) is int and
            src["rows"] == lodo["raw_rows"] == segments.get("rows") and
            src["rows"] > 0, "ROW_PIN")
    counts = []
    for d in days:
        f = folds[d]
        require(type(f) is dict and type(f.get("holdout_total")) is int and
                0 < f["holdout_total"] <= 1440, "DAY_MINUTE_CAPACITY")
        counts.append(f["holdout_total"])
    require(sum(counts) == src["rows"], "DAY_SUM")
    first_cap = 1440 - (first.hour * 60 + first.minute)
    last_cap = last.hour * 60 + last.minute + 1
    require(counts[0] <= first_cap, "FIRST_DAY_CAPACITY")
    require(counts[-1] <= last_cap, "LAST_DAY_CAPACITY")
    lengths = segments.get("contiguous_one_minute_segment_lengths")
    require(type(lengths) is list and lengths and
            all(type(x) is int and x > 0 for x in lengths) and
            type(lodo.get("strict_minute_segments")) is int and
            len(lengths) == lodo["strict_minute_segments"] ==
            segments.get("contiguous_one_minute_segment_count") and
            sum(lengths) == src["rows"], "SEGMENT_SUM")
    return {
        "schema": "hazrob.g2.mq4-reported-calendar-minute-capacity.v1",
        "source_id": SOURCE,
        "source_archive_sha256": ARCHIVE,
        "source_member_sha256": RAW,
        "reported_first_timestamp": src["first_timestamp"],
        "reported_last_timestamp": src["last_timestamp"],
        "reported_rows": src["rows"],
        "reported_calendar_days": len(days),
        "reported_max_daily_rows": max(counts),
        "first_day_reported_rows": counts[0],
        "first_day_max_minute_bins": first_cap,
        "last_day_reported_rows": counts[-1],
        "last_day_max_minute_bins": last_cap,
        "segment_lengths_reconciled": True,
        "conditional_on_reported_minute_uniqueness": True,
        "original_raw_zip_replayed_by_this_audit": False,
        "physical_independence_verified": False,
        "traceable_calibration_verified": False,
        "g2_qualified": False,
        "g3_tank_qualified": False,
        "limitations": "Cross-checks reported intake, calendar folds and segment metadata only. Minute uniqueness and endpoint timestamps are upstream assertions, not independently replayed raw bytes. No independent exposures, traceable calibration, blinded physical holdouts or scientific qualification."
    }


def load(path):
    def unique(pairs):
        out = {}
        for key, value in pairs:
            require(key not in out, "DUPLICATE_JSON_KEY")
            out[key] = value
        return out
    def invalid(value):
        raise ValueError("MQ4_CALENDAR_CAPACITY_NONFINITE_JSON")
    return json.loads(Path(path).read_text(encoding="utf-8"),
                      object_pairs_hook=unique, parse_constant=invalid)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("intake", "lodo", "segments", "output"):
        parser.add_argument("--" + key, type=Path, required=True)
    args = parser.parse_args()
    result = audit(load(args.intake), load(args.lodo), load(args.segments))
    args.output.write_text(json.dumps(result, sort_keys=True, ensure_ascii=False,
                                      indent=2) + "\n", encoding="utf-8")
    print("Reported MQ4 calendar-minute capacity reconciled; G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
