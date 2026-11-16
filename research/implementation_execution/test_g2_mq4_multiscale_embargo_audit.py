"""Synthetic-only tests: do not interpret as physical gas validation."""
import hashlib
import json
import sys
import tempfile
import unittest
import zipfile
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_multiscale_embargo_audit import compute_lag_embargo, audit_pinned_archive, MEMBER


class MQ4MultiscaleEmbargoTests(unittest.TestCase):
    def test_full_midnight_crossing_multilag_counts(self):
        start = datetime(2021, 7, 21, 23, 0)
        times = [start + timedelta(minutes=i) for i in range(181)]
        r = compute_lag_embargo(times)
        for lag in (1, 5, 15, 30, 60, 120):
            with self.subTest(lag=lag):
                d = r["lag_results"][str(lag)]
                self.assertEqual(d["contiguous_lag_pairs"], 181 - lag)
                self.assertEqual(d["cross_day_context_rows_requiring_embargo_if_day_holdout"], min(lag, 60))
                self.assertEqual(d["same_day_context_rows"] + d["cross_day_context_rows_requiring_embargo_if_day_holdout"] + d["lag_context_unavailable_rows"], 181)
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["model_use_of_lag_features_verified"])

    def test_nonminute_gap_blocks_all_crossday_windows(self):
        start = datetime(2021, 7, 21, 23, 0)
        times = [start + timedelta(minutes=i) for i in range(181)]
        times[60] += timedelta(seconds=30)
        r = compute_lag_embargo(times)
        for lag in (1, 5, 15, 30, 60, 120):
            self.assertEqual(r["lag_results"][str(lag)]["cross_day_context_rows_requiring_embargo_if_day_holdout"], 0)
        self.assertEqual(r["contiguous_one_minute_segment_count"], 3)

    def test_two_midnights_and_replay_stability(self):
        start = datetime(2021, 7, 21, 22, 0)
        times = [start + timedelta(minutes=i) for i in range(1681)]
        r = compute_lag_embargo(times)
        self.assertEqual(r["lag_results"]["60"]["cross_day_context_rows_by_later_date"], {"2021-07-22": 60, "2021-07-23": 60})
        self.assertEqual(r["lag_results"]["120"]["cross_day_context_rows_by_later_date"], {"2021-07-22": 120, "2021-07-23": 120})
        self.assertEqual(json.dumps(r, sort_keys=True), json.dumps(compute_lag_embargo(times), sort_keys=True))

    def test_reject_duplicate_unsorted_timestamps_and_bad_lags(self):
        times = [datetime(2021, 7, 21), datetime(2021, 7, 21)]
        with self.assertRaisesRegex(ValueError, "NONMONOTONIC"):
            compute_lag_embargo(times)
        for lags in ((0,), (1, 1), (5, 1), (True,), (), (1, 1.5)):
            with self.subTest(lags=lags):
                with self.assertRaisesRegex(ValueError, "LAGS_INVALID"):
                    compute_lag_embargo([datetime(2021, 7, 21)], lags=lags)

    def test_pinned_archive_path_and_tamper_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "synthetic.zip"
            raw = ("datetime,licor_ch4,MQ4_1,MQ4_2,MQ4_3\n"
                   "2021-07-21 23:59:00,2000,10,20,30\n"
                   "2021-07-22 00:00:00,2001,11,21,31\n").encode("utf-8")
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr(MEMBER, raw)
            digest = hashlib.sha256(raw).hexdigest()
            r = audit_pinned_archive(archive, expected_member_sha256=digest,
                                     expected_archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest())
            self.assertEqual(r["lag_results"]["1"]["cross_day_context_rows_requiring_embargo_if_day_holdout"], 1)
            with self.assertRaisesRegex(ValueError, "SHA256_MISMATCH"):
                audit_pinned_archive(archive)

    def test_archive_repackaging_with_same_raw_fails_pin(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "fixture.zip"
            raw = ("datetime,licor_ch4,MQ4_1,MQ4_2,MQ4_3\n"
                   "2021-07-21 23:59:00,2000,10,20,30\n").encode("utf-8")
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr(MEMBER, raw)
            pinned_archive = hashlib.sha256(archive.read_bytes()).hexdigest()
            pinned_member = hashlib.sha256(raw).hexdigest()
            with zipfile.ZipFile(archive, "a") as z:
                z.writestr("unrelated.txt", "tamper")
            with self.assertRaisesRegex(ValueError, "ARCHIVE_SHA256_MISMATCH"):
                audit_pinned_archive(archive, expected_member_sha256=pinned_member,
                                     expected_archive_sha256=pinned_archive)


if __name__ == "__main__":
    unittest.main()
