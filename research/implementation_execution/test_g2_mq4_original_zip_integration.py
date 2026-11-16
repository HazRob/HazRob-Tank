"""End-to-end synthetic ZIP + provenance controls for the MQ4 G2 replay.

No external archive is bundled. Passing these tests does NOT qualify G2/G3.
"""
import copy
import hashlib
import io
import json
import sys
import unittest
import warnings
import zipfile
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from g2_mq4_original_zip_replay import MEMBER, SOURCE, LAGS, derive, replay


def digest(data):
    return hashlib.sha256(data).hexdigest()


def synthetic_bundle(duplicate_member=False):
    columns = ["datetime", "licor_ch4", "licor_h2o", "t", "rh"] + [
        "sensor_" + str(i) for i in range(12)
    ] + ["MQ4_1", "MQ4_2", "MQ4_3"]
    assert len(columns) == 20
    first = datetime(2021, 7, 21, 23, 0)
    lines = [",".join(columns)]
    for i in range(130):
        timestamp = first + timedelta(minutes=i)
        values = [str(2000 + i)] + ["1"] * 18
        lines.append(timestamp.strftime("%Y-%m-%d %H:%M:%S") + "," + ",".join(values))
    raw = ("\n".join(lines) + "\n").encode("utf-8")
    derived = derive(raw)
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as z:
        z.writestr(MEMBER, raw)
        if duplicate_member:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                z.writestr(MEMBER, raw)
    archive = buf.getvalue()
    ah, mh = digest(archive), digest(raw)
    intake = {
        "archive_sha256": ah,
        "source_entries": [{
            "source_id": SOURCE, "raw_zip_member": MEMBER, "sha256": mh,
            "bytes": len(raw), "rows": derived["rows"], "columns": derived["columns"],
            "first_timestamp": derived["first_timestamp"],
            "last_timestamp": derived["last_timestamp"],
            "distinct_calendar_dates": len(derived["daily_rows"]),
            "reference_methane_min_ppb": derived["reference_methane_min_ppb"],
            "reference_methane_max_ppb": derived["reference_methane_max_ppb"],
        }]
    }
    lodo = {
        "raw_member_sha256": mh, "source_archive_sha256": ah,
        "raw_rows": derived["rows"], "calendar_dates": len(derived["daily_rows"]),
        "strict_minute_segments": len(derived["segment_lengths"]),
        "eligible_rows_all_lags": derived["lags"]["120"]["contiguous_lag_pairs"],
        "sum_holdout_excluded_across_folds": derived["lags"]["120"]["cross_day_context_rows"],
        "sum_train_purged_across_folds": derived["lags"]["120"]["cross_day_context_rows"],
        "g2_qualified": False, "g3_tank_qualified": False, "folds": {},
    }
    days = sorted(derived["daily_rows"])
    eligible_total = lodo["eligible_rows_all_lags"]
    cross = derived["lags"]["120"]["cross_day_by_later_date"]
    for i, day in enumerate(days):
        n = derived["daily_rows"][day]
        eligible = derived["max_lag_eligible_by_day"].get(day, 0)
        excluded = cross.get(day, 0)
        purged = cross.get(days[i+1], 0) if i+1 < len(days) else 0
        lodo["folds"][day] = {
            "holdout_total": n, "holdout_timestamp_eligible": eligible,
            "holdout_history_ineligible": n - eligible,
            "holdout_excluded_training_context": excluded,
            "holdout_remaining": eligible - excluded,
            "train_timestamp_eligible": eligible_total - eligible,
            "train_purged_holdout_context": purged,
            "train_remaining": eligible_total - eligible - purged,
        }
    segments = {
        "source_member_sha256": mh, "source_archive_sha256": ah,
        "rows": derived["rows"],
        "contiguous_one_minute_segment_lengths": derived["segment_lengths"],
        "contiguous_one_minute_segment_count": len(derived["segment_lengths"]),
        "contiguous_segments_spanning_calendar_days": derived["segments_spanning_calendar_days"],
        "one_minute_pairs_crossing_midnight": derived["lags"]["1"]["cross_day_context_rows"],
        "sixty_minute_contiguous_pairs_strict": derived["lags"]["60"]["contiguous_lag_pairs"],
        "g2_qualified": False, "g3_tank_qualified": False,
    }
    multi = {
        "source_member_sha256": mh, "source_archive_sha256": ah,
        "row_count": derived["rows"],
        "contiguous_one_minute_segment_count": len(derived["segment_lengths"]),
        "lag_results": {},
        "g2_qualified": False, "g3_tank_qualified": False,
    }
    for lag in LAGS:
        r = derived["lags"][str(lag)]
        multi["lag_results"][str(lag)] = {
            "contiguous_lag_pairs": r["contiguous_lag_pairs"],
            "same_day_context_rows": r["same_day_context_rows"],
            "cross_day_context_rows_requiring_embargo_if_day_holdout": r["cross_day_context_rows"],
            "cross_day_context_rows_by_later_date": r["cross_day_by_later_date"],
            "lag_context_unavailable_rows": r["unavailable_rows"],
        }
    return archive, intake, lodo, segments, multi


class MQ4OriginalZipIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.args = synthetic_bundle()

    def check(self, args=None):
        return replay(*(self.args if args is None else args))

    def mutate(self, index, change):
        args = copy.deepcopy(self.args)
        change(args[index])
        return args

    def test_full_zip_replay_cross_midnight(self):
        r = self.check()
        self.assertEqual(r["derived"]["rows"], 130)
        self.assertEqual(r["derived"]["daily_rows"],
                         {"2021-07-21": 60, "2021-07-22": 70})
        self.assertEqual(r["derived"]["lags"]["120"]["contiguous_lag_pairs"], 10)
        self.assertEqual(r["derived"]["lags"]["120"]["cross_day_context_rows"], 10)
        self.assertTrue(r["raw_replayed_in_this_execution"])
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["g3_tank_qualified"])

    def test_replay_deterministic_serialization(self):
        a = json.dumps(self.check(), ensure_ascii=False, sort_keys=True)
        b = json.dumps(self.check(), ensure_ascii=False, sort_keys=True)
        self.assertEqual(a.encode("utf-8"), b.encode("utf-8"))

    def test_archive_bytes_pin_rejects_tamper(self):
        args = list(self.args)
        args[0] += b"modified"
        with self.assertRaisesRegex(ValueError, "ARCHIVE_SHA256"):
            self.check(args)

    def test_raw_member_hash_pin_rejects_tamper(self):
        args = self.mutate(1, lambda x: x["source_entries"][0].update(sha256="0" * 64))
        with self.assertRaisesRegex(ValueError, "MEMBER_PIN"):
            self.check(args)

    def test_fold_context_rejects_tamper(self):
        args = self.mutate(2, lambda x: x["folds"]["2021-07-22"].update(train_remaining=1))
        with self.assertRaisesRegex(ValueError, "FOLD_REPLAY_2021-07-22"):
            self.check(args)

    def test_multiscale_lag_rejects_tamper(self):
        args = self.mutate(4, lambda x: x["lag_results"]["5"].update(contiguous_lag_pairs=999))
        with self.assertRaisesRegex(ValueError, "LAG_REPLAY_5"):
            self.check(args)

    def test_qualification_flag_rejected(self):
        args = self.mutate(4, lambda x: x.update(g2_qualified=True))
        with self.assertRaisesRegex(ValueError, "NO_PROMOTION"):
            self.check(args)

    def test_duplicate_zip_member_rejected(self):
        args = list(synthetic_bundle(duplicate_member=True))
        with self.assertRaisesRegex(ValueError, "ZIP_MEMBERS"):
            self.check(args)

    def test_intake_duplicate_source_rejected(self):
        args = self.mutate(1, lambda x: x["source_entries"].append(
            copy.deepcopy(x["source_entries"][0])))
        with self.assertRaisesRegex(ValueError, "SOURCE_UNIQUE"):
            self.check(args)


if __name__ == "__main__":
    unittest.main()
