"""Research-only multi-lag calendar-day feature-context diagnostic for pinned MQ4 raw bytes.

A calendar day is not a physical exposure group. This audit cannot qualify G2/G3.
The lag counts are conditional on use of the corresponding lagged features.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from collections import Counter
from datetime import datetime
from pathlib import Path

MEMBER = "raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv"
PINNED_SHA256 = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"
PINNED_ARCHIVE_SHA256 = "7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9"
CHANNELS = ("licor_ch4", "MQ4_1", "MQ4_2", "MQ4_3")
LAGS = (1, 5, 15, 30, 60, 120)


def compute_lag_embargo(timestamps, *, lags=LAGS):
    """Partition every row for each lag into unavailable, same-day, cross-day.

    Only fully contiguous 60-second intervals qualify as lagged context.
    Counts are not independent samples, physical exposure groups or model errors.
    """
    if not lags or any(type(n) is not int or n <= 0 for n in lags) or tuple(sorted(set(lags))) != tuple(lags):
        raise ValueError("MQ4_LAGS_INVALID")
    if not timestamps or any(type(t) is not datetime or t.tzinfo is not None for t in timestamps):
        raise ValueError("MQ4_TIMESTAMPS_INVALID")
    segment = [0]
    for i in range(1, len(timestamps)):
        delta = (timestamps[i] - timestamps[i - 1]).total_seconds()
        if delta <= 0:
            raise ValueError("MQ4_NONMONOTONIC_TIMESTAMPS")
        segment.append(segment[-1] + (delta != 60))
    counts = {}
    for lag in lags:
        within = 0
        cross = Counter()
        unavailable = 0
        for i, timestamp in enumerate(timestamps):
            if i < lag or segment[i] != segment[i - lag]:
                unavailable += 1
            elif timestamp.date() == timestamps[i - lag].date():
                within += 1
            else:
                cross[timestamp.date().isoformat()] += 1
        crossed = sum(cross.values())
        if within + crossed + unavailable != len(timestamps):
            raise AssertionError("MQ4_LAG_RECONCILIATION_INVALID")
        counts[str(lag)] = {
            "lag_minutes": lag,
            "contiguous_lag_pairs": within + crossed,
            "same_day_context_rows": within,
            "cross_day_context_rows_requiring_embargo_if_day_holdout": crossed,
            "cross_day_context_rows_by_later_date": dict(sorted(cross.items())),
            "lag_context_unavailable_rows": unavailable,
            "cross_day_fraction_of_available_pairs": (
                crossed / (within + crossed) if within + crossed else None
            ),
        }
    return {
        "row_count": len(timestamps),
        "contiguous_one_minute_segment_count": segment[-1] + 1,
        "lag_results": counts,
        "calendar_day_split_physical_independence_verified": False,
        "model_use_of_lag_features_verified": False,
        "g2_qualified": False,
        "g3_tank_qualified": False,
    }


def audit_pinned_archive(archive, *, expected_member_sha256=PINNED_SHA256,
                         expected_archive_sha256=PINNED_ARCHIVE_SHA256):
    if any(len(s) != 64 or any(c not in "0123456789abcdef" for c in s)
           for s in (expected_member_sha256, expected_archive_sha256)):
        raise ValueError("MQ4_EXPECTED_SHA256_INVALID")
    archive_bytes = Path(archive).read_bytes()
    archive_digest = hashlib.sha256(archive_bytes).hexdigest()
    if archive_digest != expected_archive_sha256:
        raise ValueError("MQ4_ARCHIVE_SHA256_MISMATCH")
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as zf:
        if zf.namelist().count(MEMBER) != 1:
            raise ValueError("MQ4_RAW_MEMBER_MISSING_OR_DUPLICATED")
        raw = zf.read(MEMBER)
    digest = hashlib.sha256(raw).hexdigest()
    if digest != expected_member_sha256:
        raise ValueError("MQ4_RAW_SHA256_MISMATCH")
    try:
        reader = csv.DictReader(io.StringIO(raw.decode("utf-8"), newline=""), strict=True)
        header = reader.fieldnames
        if (not header or len(set(header)) != len(header) or
                not set(("datetime", *CHANNELS)).issubset(header)):
            raise ValueError("MQ4_HEADER_INVALID")
        rows = list(reader)
    except (UnicodeError, csv.Error) as exc:
        raise ValueError("MQ4_CSV_INVALID") from exc
    if not rows:
        raise ValueError("MQ4_NO_ROWS")
    timestamps = []
    for row in rows:
        if None in row or any(v is None for v in row.values()):
            raise ValueError("MQ4_CSV_ROW_INVALID")
        try:
            stamp = datetime.strptime(row["datetime"], "%Y-%m-%d %H:%M:%S")
            if stamp.strftime("%Y-%m-%d %H:%M:%S") != row["datetime"]:
                raise ValueError("noncanonical timestamp")
            readings = [float(row[name]) for name in CHANNELS]
        except (ValueError, TypeError, KeyError) as exc:
            raise ValueError("MQ4_TIMESTAMP_OR_VALUE_INVALID") from exc
        if any(not math.isfinite(v) or v <= 0 for v in readings):
            raise ValueError("MQ4_NONPOSITIVE_OR_NONFINITE_VALUE")
        timestamps.append(stamp)
    result = compute_lag_embargo(timestamps)
    if expected_member_sha256 == PINNED_SHA256:
        if (result["row_count"] != 14186 or
                result["contiguous_one_minute_segment_count"] != 11 or
                result["lag_results"]["60"]["contiguous_lag_pairs"] != 13581 or
                result["lag_results"]["60"]["cross_day_context_rows_requiring_embargo_if_day_holdout"] != 600):
            raise ValueError("MQ4_PINNED_SOURCE_RECONCILIATION_FAILED")
    return {
        "schema": "hazrob.g2.mq4-multiscale-day-context-embargo.v1",
        "source_id": "MQ-ACQ-2026-06",
        "source_archive_sha256": archive_digest,
        "source_raw_member": MEMBER,
        "source_member_sha256": digest,
        "source_member_sha256_verified": True,
        **result,
        "reference_concentration_unit": "ppb",
        "mq4_channel_unit": "ohm",
        "method": "Each lag requires every intervening interval to equal 60 seconds. Rows with prior-day lag context would require embargo or same-day-only feature construction if calendar-day holdouts and that lag feature were used.",
        "limitations": "No claim of actual model lag usage, physical independence, calibrated Tank ppm, blind holdout, or scientific G2/G3 promotion. Raw timestamps lack independently verified timezone and exposure IDs.",
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--archive", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    args = p.parse_args()
    result = audit_pinned_archive(args.archive)
    args.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("MQ4 multiscale day-context audit complete; NO G2/G3 qualification")


if __name__ == "__main__":
    main()
