"""Recompute MQ4 research summaries from original pinned ZIP bytes (stdlib only).

No model fitting, calibration certification, exposure independence or G2/G3 promotion.
The original ZIP is a user Library input; CI only tests synthetic byte fixtures.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

SOURCE = "MQ-ACQ-2026-06"
LAGS = (1, 5, 15, 30, 60, 120)
MEMBER = "raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv"


def need(ok, code):
    if not ok:
        raise ValueError("MQ4_ORIGINAL_REPLAY_" + code)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def derive(raw):
    """Independently parse each CSV record and calculate contiguous lag contexts."""
    try:
        decoded = raw.decode("utf-8-sig")
        reader = csv.DictReader(io.StringIO(decoded, newline=""))
        columns = reader.fieldnames
        need(columns is not None and len(columns) == 20 and len(set(columns)) == 20
             and columns[0] == "datetime" and "licor_ch4" in columns
             and all(x in columns for x in ("MQ4_1", "MQ4_2", "MQ4_3")),
             "COLUMNS")
        rows = []
        for record in reader:
            need(None not in record and all(v is not None and v.strip() for v in record.values()),
                 "MISSING_CELLS")
            timestamp = datetime.strptime(record["datetime"], "%Y-%m-%d %H:%M:%S")
            need(timestamp.strftime("%Y-%m-%d %H:%M:%S") == record["datetime"]
                 and timestamp.second == 0, "MINUTE_TIMESTAMP")
            for col in columns[1:]:
                need(math.isfinite(float(record[col])), "NONFINITE")
            rows.append((timestamp, float(record["licor_ch4"])))
    except (UnicodeError, csv.Error, TypeError, OverflowError, ValueError) as ex:
        if isinstance(ex, ValueError) and str(ex).startswith("MQ4_ORIGINAL_REPLAY_"):
            raise
        raise ValueError("MQ4_ORIGINAL_REPLAY_INVALID_CSV") from ex
    need(len(rows) > 1, "EMPTY")
    times = [r[0] for r in rows]
    need(all(b > a for a, b in zip(times, times[1:])), "TIMESTAMP_ORDER_OR_DUPLICATE")
    counts = Counter(t.date().isoformat() for t in times)
    segments = []
    start = 0
    for i in range(1, len(times)):
        if times[i] - times[i-1] != timedelta(minutes=1):
            segments.append((start, i))
            start = i
    segments.append((start, len(times)))
    eligible = Counter()
    lag_result = {}
    for lag in LAGS:
        crossing = Counter()
        same = 0
        for lo, hi in segments:
            for i in range(lo + lag, hi):
                day = times[i].date().isoformat()
                if lag == max(LAGS):
                    eligible[day] += 1
                if day != times[i-lag].date().isoformat():
                    crossing[day] += 1
                else:
                    same += 1
        cross = sum(crossing.values())
        lag_result[str(lag)] = {
            "contiguous_lag_pairs": same + cross,
            "same_day_context_rows": same,
            "cross_day_context_rows": cross,
            "cross_day_by_later_date": dict(sorted(crossing.items())),
            "unavailable_rows": len(times) - same - cross,
        }
    return {
        "rows": len(rows), "columns": len(columns),
        "first_timestamp": times[0].strftime("%Y-%m-%d %H:%M:%S"),
        "last_timestamp": times[-1].strftime("%Y-%m-%d %H:%M:%S"),
        "daily_rows": dict(sorted(counts.items())),
        "segment_lengths": [hi-lo for lo, hi in segments],
        "segments_spanning_calendar_days": sum(times[lo].date() != times[hi-1].date()
                                                for lo, hi in segments),
        "max_lag_eligible_by_day": dict(sorted(eligible.items())),
        "reference_methane_min_ppb": min(v for _, v in rows),
        "reference_methane_max_ppb": max(v for _, v in rows),
        "lags": lag_result,
    }


def replay(archive, intake, lodo, segments, multi):
    need(sha(archive) == intake["archive_sha256"], "ARCHIVE_SHA256")
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        names = z.namelist()
        need(len(names) == len(set(names)) and names.count(MEMBER) == 1, "ZIP_MEMBERS")
        raw = z.read(MEMBER)
    entries = [x for x in intake["source_entries"] if x.get("source_id") == SOURCE]
    need(len(entries) == 1, "SOURCE_UNIQUE")
    src = entries[0]
    need(src["raw_zip_member"] == MEMBER and sha(raw) == src["sha256"] and
         len(raw) == src["bytes"] and
         sha(raw) == lodo["raw_member_sha256"] == segments["source_member_sha256"]
         == multi["source_member_sha256"], "MEMBER_PIN")
    need(all(x == intake["archive_sha256"] for x in
             (lodo["source_archive_sha256"], segments["source_archive_sha256"],
              multi["source_archive_sha256"])), "ARCHIVE_PIN")
    actual = derive(raw)
    need(actual["rows"] == src["rows"] == lodo["raw_rows"] == segments["rows"]
         == multi["row_count"] and actual["columns"] == src["columns"], "ROW_COLUMN_COUNT")
    need(actual["first_timestamp"] == src["first_timestamp"] and
         actual["last_timestamp"] == src["last_timestamp"] and
         len(actual["daily_rows"]) == src["distinct_calendar_dates"] == lodo["calendar_dates"],
         "DATE_BOUNDS")
    need(abs(actual["reference_methane_min_ppb"] - src["reference_methane_min_ppb"]) < 1e-7
         and abs(actual["reference_methane_max_ppb"] - src["reference_methane_max_ppb"]) < 1e-7,
         "REFERENCE_RANGE")
    need(actual["segment_lengths"] == segments["contiguous_one_minute_segment_lengths"]
         and len(actual["segment_lengths"]) == lodo["strict_minute_segments"]
         == segments["contiguous_one_minute_segment_count"]
         == multi["contiguous_one_minute_segment_count"], "SEGMENT_LENGTHS")
    need(actual["segments_spanning_calendar_days"] ==
         segments["contiguous_segments_spanning_calendar_days"] and
         actual["lags"]["1"]["cross_day_context_rows"] ==
         segments["one_minute_pairs_crossing_midnight"] and
         actual["lags"]["60"]["contiguous_lag_pairs"] ==
         segments["sixty_minute_contiguous_pairs_strict"], "SEGMENT_CROSSINGS")
    folds = lodo["folds"]
    need(set(folds) == set(actual["daily_rows"]), "FOLD_DATES")
    eligible_total = actual["lags"][str(max(LAGS))]["contiguous_lag_pairs"]
    cross120 = actual["lags"][str(max(LAGS))]["cross_day_by_later_date"]
    days = sorted(folds)
    for i, day in enumerate(days):
        f = folds[day]
        n = actual["daily_rows"][day]
        eligible = actual["max_lag_eligible_by_day"].get(day, 0)
        exclude = cross120.get(day, 0)
        purge = cross120.get(days[i+1], 0) if i+1 < len(days) else 0
        expected = {
            "holdout_total": n, "holdout_timestamp_eligible": eligible,
            "holdout_history_ineligible": n-eligible,
            "holdout_excluded_training_context": exclude,
            "holdout_remaining": eligible-exclude,
            "train_timestamp_eligible": eligible_total-eligible,
            "train_purged_holdout_context": purge,
            "train_remaining": eligible_total-eligible-purge,
        }
        need(all(f.get(k) == v for k, v in expected.items()), "FOLD_REPLAY_" + day)
    need(eligible_total == lodo["eligible_rows_all_lags"] and
         sum(cross120.values()) == lodo["sum_holdout_excluded_across_folds"]
         == lodo["sum_train_purged_across_folds"], "FOLD_TOTALS")
    for lag in LAGS:
        a = actual["lags"][str(lag)]
        m = multi["lag_results"][str(lag)]
        need(a["contiguous_lag_pairs"] == m["contiguous_lag_pairs"] and
             a["same_day_context_rows"] == m["same_day_context_rows"] and
             a["cross_day_context_rows"] ==
             m["cross_day_context_rows_requiring_embargo_if_day_holdout"] and
             a["cross_day_by_later_date"] == m["cross_day_context_rows_by_later_date"] and
             a["unavailable_rows"] == m["lag_context_unavailable_rows"], "LAG_REPLAY_" + str(lag))
    need(all(x.get("g2_qualified") is False for x in (lodo, multi, segments))
         and all(x.get("g3_tank_qualified", x.get("g3_qualified")) is False
                 for x in (lodo, multi, segments)), "NO_PROMOTION")
    return {
        "schema": "hazrob.g2.mq4-original-zip-raw-replay.v1",
        "source_id": SOURCE, "archive_sha256": sha(archive),
        "raw_member_sha256": sha(raw), "raw_member_bytes": len(raw),
        "raw_replayed_in_this_execution": True, "summary_fields_verified": True,
        "derived": actual, "g2_qualified": False, "g3_tank_qualified": False,
        "physical_exposure_independence_verified": False,
        "reference_calibration_certificate_verified": False,
        "blinded_holdout_verified": False,
        "limitations": "Raw CSV time/values and published summaries reconciled. The original authors' measurements and instrument calibration were not independently replicated; timestamps do not establish physically independent exposures or qualified gas identification."
    }


def main():
    p = argparse.ArgumentParser()
    for name in ("archive", "intake", "lodo", "segments", "multiscale", "output"):
        p.add_argument("--" + name, type=Path, required=True)
    a = p.parse_args()
    load = lambda path: json.loads(path.read_text(encoding="utf-8"))
    report = replay(a.archive.read_bytes(), load(a.intake), load(a.lodo),
                    load(a.segments), load(a.multiscale))
    a.output.write_text(json.dumps(report, ensure_ascii=False, sort_keys=True,
                                   indent=2) + "\n", encoding="utf-8")
    print("Original ZIP raw replay PASS; scientific G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
