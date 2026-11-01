"""Cross-file MQ4 provenance reconciliation; research only, not physical validation.

Reads historical, previously recorded raw-ZIP replay, inspected Library protocol,
and SHA-pinned source summary. It does NOT replay the original ZIP in CI.
"""
import argparse
import json
from pathlib import Path

from g2_mq4_pairwise_frechet_bounds import EVIDENCE, SOURCE, project, strict_json
from g2_mq4_paired_prediction_intake import (
    SOURCE_DAILY_ROWS, validate_full_fold_partition, validate_pinned_source_folds,
)

ROOT = Path(__file__).resolve().parent
REPLAY = ROOT / "G2_MQ4_ORIGINAL_ZIP_RAW_REPLAY_2026-10-31.json"
PROTOCOL = ROOT / "G2_MQ4_RETROSPECTIVE_PROTOCOL_AUDIT_2026-11-01.json"
ARCHIVE_SHA = "7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9"
MEMBER_SHA = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"
SCRIPT_SHA = "367658f9884ff8355c89d803f67eb7ced78ec7299759d58ddd1742ba9a1b368b"
SOURCE_SHA = "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea"

# Historical replay observations pinned independently of this gate's arithmetic.
# These are NOT independently authenticated raw-source row measurements.
PINNED_SEGMENTS = (1406, 1565, 1388, 5, 1454, 2495, 675, 1367, 995, 1417, 1419)
PINNED_MAX_LAG_ELIGIBLE_BY_DAY = {
    "2021-07-21": 654, "2021-07-22": 1315, "2021-07-23": 1111,
    "2021-07-24": 1296, "2021-07-25": 1316, "2021-07-26": 1440,
    "2021-07-27": 1197, "2021-07-28": 1317, "2021-07-29": 1298,
    "2021-07-30": 1314, "2021-07-31": 723,
}
REPORTED_LAGS = (1, 5, 15, 30, 60, 120)


def _check(condition, code):
    if not condition:
        raise ValueError(code)


def reconcile(replay_raw, protocol_raw, source_raw, evidence_raw):
    """Validate pinned reports against one another; no independent evidence inferred."""
    project(source_raw, evidence_raw)  # SHA and independent-evidence fail-closed gate
    replay, protocol = strict_json(replay_raw), strict_json(protocol_raw)
    _check(isinstance(replay, dict) and isinstance(protocol, dict), "INVALID_REPORT_ROOT")
    _check(replay.get("schema") == "hazrob.g2.mq4-original-zip-raw-replay.v1"
           and replay.get("source_id") == "MQ-ACQ-2026-06"
           and replay.get("archive_sha256") == ARCHIVE_SHA
           and replay.get("raw_member_sha256") == MEMBER_SHA,
           "RAW_REPLAY_LINEAGE_MISMATCH")
    _check(replay.get("raw_replayed_in_this_execution") is True
           and replay.get("summary_fields_verified") is True
           and replay.get("blinded_holdout_verified") is False
           and replay.get("reference_calibration_certificate_verified") is False
           and replay.get("physical_exposure_independence_verified") is False
           and replay.get("g2_qualified") is False
           and replay.get("g3_tank_qualified") is False,
           "RAW_REPLAY_UNSUPPORTED_QUALIFICATION")
    derived = replay.get("derived")
    _check(isinstance(derived, dict)
           and type(derived.get("rows")) is int and derived["rows"] == 14186
           and type(derived.get("columns")) is int and derived["columns"] == 20
           and derived.get("daily_rows") == SOURCE_DAILY_ROWS
           and sum(SOURCE_DAILY_ROWS.values()) == 14186,
           "RAW_REPLAY_DAY_COUNT_MISMATCH")
    segments = derived.get("segment_lengths")
    _check(isinstance(segments, list)
           and all(type(v) is int and v > 0 for v in segments)
           and segments == list(PINNED_SEGMENTS)
           and sum(segments) == 14186
           and derived.get("segments_spanning_calendar_days") == 9,
           "RAW_REPLAY_SEGMENT_MISMATCH")
    _check(derived.get("first_timestamp") == "2021-07-21 11:06:00"
           and derived.get("last_timestamp") == "2021-07-31 12:02:00",
           "RAW_REPLAY_TIMELINE_MISMATCH")
    _check(type(replay.get("raw_member_bytes")) is int
           and replay["raw_member_bytes"] == 4787918,
           "RAW_REPLAY_BYTES_MISMATCH")
    _check(derived.get("max_lag_eligible_by_day") ==
           PINNED_MAX_LAG_ELIGIBLE_BY_DAY,
           "RAW_REPLAY_MAX_LAG_ELIGIBILITY_MISMATCH")
    # Recalculate all six reported lag scales from strict-minute segments,
    # rather than trusting only the 120-minute summary. Cross-day context
    # remains a historical report claim, not original-row authentication.
    lags = derived.get("lags")
    _check(isinstance(lags, dict)
           and set(lags) == {str(lag) for lag in REPORTED_LAGS},
           "RAW_REPLAY_LAG_CONTEXT_MISMATCH")
    for lag in REPORTED_LAGS:
        actual = lags[str(lag)]
        expected_pairs = sum(max(0, length - lag) for length in PINNED_SEGMENTS)
        expected_unavailable = sum(min(length, lag) for length in PINNED_SEGMENTS)
        expected_cross = lag * (len(SOURCE_DAILY_ROWS) - 1)
        expected_by_day = {day: lag for day in sorted(SOURCE_DAILY_ROWS)[1:]}
        _check(isinstance(actual, dict)
               and all(type(actual.get(key)) is int for key in (
                   "contiguous_lag_pairs", "same_day_context_rows",
                   "cross_day_context_rows", "unavailable_rows"))
               and actual["contiguous_lag_pairs"] == expected_pairs
               and actual["unavailable_rows"] == expected_unavailable
               and actual["cross_day_context_rows"] == expected_cross
               and actual["same_day_context_rows"] == expected_pairs - expected_cross
               and actual.get("cross_day_by_later_date") == expected_by_day
               and expected_pairs + expected_unavailable == 14186,
               "RAW_REPLAY_LAG_CONTEXT_MISMATCH")
    _check(protocol.get("schema") == "hazrob.g2.mq4-retrospective-protocol-audit.v1"
           and protocol.get("source_library_summary_sha256") == SOURCE_SHA
           and protocol.get("source_library_benchmark_script_sha256") == SCRIPT_SHA
           and protocol.get("source_library_snapshot_not_raw_zip") is True
           and protocol.get("source_rows") == 14186
           and protocol.get("source_days") == 11
           and protocol.get("physical_independent_reference_records_verified") == 0
           and protocol.get("g2_qualified") is False
           and protocol.get("g3_qualified") is False,
           "LIBRARY_PROTOCOL_LINEAGE_MISMATCH")
    folds_raw = protocol.get("folds")
    _check(isinstance(folds_raw, list) and len(folds_raw) == 11
           and all(isinstance(f, dict) for f in folds_raw),
           "LIBRARY_PROTOCOL_FOLD_MISMATCH")
    days = [f.get("test_day") for f in folds_raw]
    _check(len(set(days)) == 11 and set(days) == set(SOURCE_DAILY_ROWS),
           "LIBRARY_PROTOCOL_FOLD_MISMATCH")
    folds = {}
    for f in folds_raw:
        day = f["test_day"]
        training = f.get("training_days")
        _check(isinstance(training, list) and all(isinstance(d, str) for d in training),
               "LIBRARY_PROTOCOL_FOLD_MISMATCH")
        folds[day] = training
    try:
        inferred = validate_full_fold_partition(folds)
        pinned = validate_pinned_source_folds(folds, derived["daily_rows"])
    except (TypeError, ValueError) as exc:
        raise ValueError("LIBRARY_PROTOCOL_FOLD_MISMATCH") from exc
    _check(inferred == pinned, "LIBRARY_PROTOCOL_FOLD_MISMATCH")
    future_train, future_cal, prospective = [], [], []
    for f in folds_raw:
        day = f["test_day"]
        train_future = sorted(d for d in folds[day] if d > day)
        cal_future = inferred[day] > day
        _check(f.get("calibration_day") == inferred[day]
               and f.get("future_training_days") == train_future
               and f.get("future_calibration_day") is cal_future
               and f.get("prospective_valid") is (not train_future and not cal_future),
               "LIBRARY_PROTOCOL_TEMPORAL_FLAG_MISMATCH")
        if train_future:
            future_train.append(day)
        if cal_future:
            future_cal.append(day)
        if not train_future and not cal_future:
            prospective.append(day)
    _check(protocol.get("folds_with_future_training_days") == len(future_train)
           and protocol.get("folds_with_future_calibration_day") == len(future_cal)
           and protocol.get("folds_without_future_training_or_calibration") == len(prospective),
           "LIBRARY_PROTOCOL_TEMPORAL_TOTAL_MISMATCH")
    return {
        "schema": "hazrob.g2.mq4-three-way-source-reconciliation.v1",
        "historical_raw_archive_sha256": ARCHIVE_SHA,
        "historical_raw_member_sha256": MEMBER_SHA,
        "pinned_library_script_sha256": SCRIPT_SHA,
        "pinned_source_summary_sha256": SOURCE_SHA,
        "source_rows": 14186,
        "source_days": 11,
        "strict_minute_segments": 11,
        "lag120_cross_day_context_rows": 1200,
        "folds_with_future_training": len(future_train),
        "folds_with_future_calibration": len(future_cal),
        "folds_without_future_fit_data": len(prospective),
        "only_nonfuture_fold_test_days": prospective,
        "raw_zip_replayed_in_current_ci": False,
        "source_row_hashes_independently_authenticated": False,
        "physical_independent_exposures_verified": 0,
        "paired_inferential_test_authorized": False,
        "g2_qualified": False,
        "g3_qualified": False,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--replay", type=Path, default=REPLAY)
    ap.add_argument("--protocol", type=Path, default=PROTOCOL)
    ap.add_argument("--source", type=Path, default=SOURCE)
    ap.add_argument("--evidence", type=Path, default=EVIDENCE)
    ap.add_argument("--output", type=Path, required=True)
    a = ap.parse_args()
    result = reconcile(a.replay.read_bytes(), a.protocol.read_bytes(),
                       a.source.read_bytes(), a.evidence.read_bytes())
    a.output.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("Historical source reports reconciled; original ZIP not replayed; G2/G3 blocked")


if __name__ == "__main__":
    main()
