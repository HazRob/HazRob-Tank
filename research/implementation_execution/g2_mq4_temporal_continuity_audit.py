"""Research-only temporal dependence audit of SHA-verified raw source bytes.

Calendar days of minute-averaged methane observations are not automatically
independent physical exposure groups. No scientific promotion is implied.
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
CHANNELS = ("licor_ch4", "MQ4_1", "MQ4_2", "MQ4_3")


def _pearson(pairs):
    if len(pairs) < 2:
        return None
    mx = math.fsum(x for x, _ in pairs) / len(pairs)
    my = math.fsum(y for _, y in pairs) / len(pairs)
    cov = math.fsum((x - mx) * (y - my) for x, y in pairs)
    sx = math.fsum((x - mx) ** 2 for x, _ in pairs)
    sy = math.fsum((y - my) ** 2 for _, y in pairs)
    if sx == 0 or sy == 0:
        return None
    return cov / math.sqrt(sx * sy)


def audit_mq4_temporal_continuity(archive, *, expected_member_sha256=PINNED_SHA256):
    if (len(expected_member_sha256) != 64 or
            any(c not in "0123456789abcdef" for c in expected_member_sha256)):
        raise ValueError("MQ4_EXPECTED_SHA256_INVALID")
    archive_bytes = Path(archive).read_bytes()
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
    values = {name: [] for name in CHANNELS}
    for row in rows:
        if None in row or any(v is None for v in row.values()):
            raise ValueError("MQ4_CSV_ROW_INVALID")
        try:
            stamp = datetime.strptime(row["datetime"], "%Y-%m-%d %H:%M:%S")
            if stamp.strftime("%Y-%m-%d %H:%M:%S") != row["datetime"]:
                raise ValueError("noncanonical timestamp")
            readings = {name: float(row[name]) for name in CHANNELS}
        except (ValueError, TypeError, KeyError) as exc:
            raise ValueError("MQ4_TIMESTAMP_OR_VALUE_INVALID") from exc
        if (any(not math.isfinite(v) or v <= 0 for v in readings.values()) or
                (timestamps and stamp <= timestamps[-1])):
            raise ValueError("MQ4_NONPOSITIVE_OR_NONMONOTONIC")
        timestamps.append(stamp)
        for name, value in readings.items():
            values[name].append(value)
    minute_pairs, midnight_pairs = [], []
    # A 60-minute lag is valid only when ALL sixty intervening intervals
    # are exactly one minute. Endpoint elapsed time alone is insufficient:
    # a 30-second interval followed by a 90-second interval cancels out.
    segment_ids = [0]
    segment_starts = [0]
    gap_count = 0
    for i in range(1, len(timestamps)):
        seconds = (timestamps[i] - timestamps[i - 1]).total_seconds()
        if seconds == 60:
            minute_pairs.append((i - 1, i))
            if timestamps[i].date() != timestamps[i - 1].date():
                midnight_pairs.append((i - 1, i))
            segment_ids.append(segment_ids[-1])
        else:
            gap_count += 1
            segment_ids.append(segment_ids[-1] + 1)
            segment_starts.append(i)
    segment_ends = segment_starts[1:] + [len(timestamps)]
    segment_lengths = [
        end - start for start, end in zip(segment_starts, segment_ends)
    ]
    lag60_pairs = [
        (i - 60, i) for i in range(60, len(timestamps))
        if segment_ids[i] == segment_ids[i - 60]
    ]
    dates = Counter(t.date().isoformat() for t in timestamps)
    if len(minute_pairs) + gap_count != len(rows) - 1:
        raise ValueError("MQ4_PAIR_RECONCILIATION_INVALID")
    return {
        "schema": "hazrob.g2.mq4-temporal-continuity-audit.v1",
        "source_id": "MQ-ACQ-2026-06",
        "source_raw_member": MEMBER,
        "source_member_sha256_verified": True,
        "source_member_sha256": digest,
        "source_archive_sha256": hashlib.sha256(archive_bytes).hexdigest(),
        "row_count": len(rows),
        "calendar_day_count": len(dates),
        "rows_by_calendar_day": dict(sorted(dates.items())),
        "first_timestamp": timestamps[0].isoformat(sep=" "),
        "last_timestamp": timestamps[-1].isoformat(sep=" "),
        "adjacent_one_minute_pairs": len(minute_pairs),
        "non_one_minute_gaps": gap_count,
        "contiguous_one_minute_segment_count": len(segment_lengths),
        "contiguous_one_minute_segment_lengths": segment_lengths,
        "contiguous_segments_spanning_calendar_days": sum(
            timestamps[start].date() != timestamps[end - 1].date()
            for start, end in zip(segment_starts, segment_ends)
        ),
        "one_minute_pairs_crossing_midnight": len(midnight_pairs),
        "cross_midnight_reference_abs_delta_ppb": [
            abs(values["licor_ch4"][j] - values["licor_ch4"][i])
            for i, j in midnight_pairs
        ],
        "one_minute_lag_pearson": {
            name: _pearson([(values[name][i], values[name][j])
                            for i, j in minute_pairs]) for name in CHANNELS
        },
        "sixty_minute_lag_reference_pearson": _pearson([
            (values["licor_ch4"][i], values["licor_ch4"][j])
            for i, j in lag60_pairs
        ]),
        "sixty_minute_contiguous_pairs": len(lag60_pairs),
        "sixty_minute_lag_requires_60_consecutive_minute_steps": True,
        "reference_concentration_unit": "ppb",
        "mq4_channel_unit": "ohm",
        "calendar_day_independence_verified": False,
        "pulse_or_exposure_group_ids_verified": False,
        "traceable_reference_calibration_certificate_verified": False,
        "g2_qualified": False,
        "g3_tank_qualified": False,
        "scope": (
            "SOURCE_RAW_BYTES_AND_TEMPORAL_DIAGNOSTIC_ONLY;"
            "CROSS_MIDNIGHT_CONTINUITY_IS_NOT_PROOF_OF_EXPOSURE_IDENTITY;"
            "CORRELATION_NOT_INDEPENDENT_SAMPLE_COUNT;"
            "NO_TANK_PPM_OR_SAFETY_CLAIM"
        ),
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--archive", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--expected-member-sha256", default=PINNED_SHA256)
    args = p.parse_args()
    result = audit_mq4_temporal_continuity(
        args.archive, expected_member_sha256=args.expected_member_sha256)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True,
                                      ensure_ascii=False) + "\n", encoding="utf-8")
    print("MQ4 raw temporal continuity audited; G2/G3 NOT qualified")


if __name__ == "__main__":
    main()
