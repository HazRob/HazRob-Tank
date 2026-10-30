"""Synthetic regression fixtures; not original physical-gas validation."""
import csv
import hashlib
import io
import json
import sys
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path
from datetime import datetime, timedelta

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_temporal_continuity_audit import MEMBER, audit_mq4_temporal_continuity


class MQ4TemporalAuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.archive = Path(self.tmp.name) / "fixture.zip"
        self.rows = [
            ["2021-07-21 23:58:00", 2000, 10, 20, 30],
            ["2021-07-21 23:59:00", 2001, 11, 21, 31],
            ["2021-07-22 00:00:00", 2002, 12, 22, 32],
            ["2021-07-22 00:02:00", 2003, 13, 23, 33],
        ]

    def write(self, *, rows=None, header=None):
        out = io.StringIO(newline="")
        writer = csv.writer(out, lineterminator="\n")
        writer.writerow(header or ["datetime", "licor_ch4", "MQ4_1", "MQ4_2", "MQ4_3"])
        writer.writerows(self.rows if rows is None else rows)
        raw = out.getvalue().encode("utf-8")
        with zipfile.ZipFile(self.archive, "w") as z:
            z.writestr(MEMBER, raw)
        return hashlib.sha256(raw).hexdigest()

    def audit(self, **kwargs):
        return audit_mq4_temporal_continuity(self.archive, **kwargs)

    def test_midnight_continuity_and_gap_not_independent_day_groups(self):
        digest = self.write()
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["row_count"], 4)
        self.assertEqual(r["calendar_day_count"], 2)
        self.assertEqual(r["adjacent_one_minute_pairs"], 2)
        self.assertEqual(r["one_minute_pairs_crossing_midnight"], 1)
        self.assertEqual(r["cross_midnight_reference_abs_delta_ppb"], [1])
        self.assertEqual(r["non_one_minute_gaps"], 1)
        self.assertAlmostEqual(r["one_minute_lag_pearson"]["licor_ch4"], 1)
        self.assertFalse(r["calendar_day_independence_verified"])
        self.assertFalse(r["g2_qualified"])

    def test_byte_tampering_fails_sha_check(self):
        digest = self.write()
        altered = [row[:] for row in self.rows]
        altered[0][1] += 1
        self.write(rows=altered)
        with self.assertRaisesRegex(ValueError, "MQ4_RAW_SHA256_MISMATCH"):
            self.audit(expected_member_sha256=digest)

    def test_duplicate_and_unsorted_timestamp_rejected(self):
        for altered in (self.rows[:2] + [self.rows[1]],
                        [self.rows[1], self.rows[0]]):
            with self.subTest(altered=altered):
                digest = self.write(rows=altered)
                with self.assertRaisesRegex(ValueError, "MQ4_NONPOSITIVE_OR_NONMONOTONIC"):
                    self.audit(expected_member_sha256=digest)

    def test_invalid_numeric_and_nonfinite_rejected(self):
        for value in ("nan", "inf", "-1", "0", "n/a"):
            with self.subTest(value=value):
                altered = [row[:] for row in self.rows]
                altered[0][2] = value
                digest = self.write(rows=altered)
                with self.assertRaisesRegex(ValueError, "MQ4_(NONPOSITIVE_OR_NONMONOTONIC|TIMESTAMP_OR_VALUE_INVALID)"):
                    self.audit(expected_member_sha256=digest)

    def test_header_and_duplicate_member_rejected(self):
        digest = self.write(header=["datetime", "licor_ch4", "MQ4_1", "MQ4_2", "MQ4_2"])
        with self.assertRaisesRegex(ValueError, "MQ4_HEADER_INVALID"):
            self.audit(expected_member_sha256=digest)
        self.write()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(self.archive, "a") as z:
                z.writestr(MEMBER, b"duplicate")
        with self.assertRaisesRegex(ValueError, "MQ4_RAW_MEMBER_MISSING_OR_DUPLICATED"):
            self.audit(expected_member_sha256=digest)

    def test_gap_segmentation_and_midnight_crossing(self):
        digest = self.write()
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["contiguous_one_minute_segment_count"], 2)
        self.assertEqual(r["contiguous_one_minute_segment_lengths"], [3, 1])
        self.assertEqual(r["contiguous_segments_spanning_calendar_days"], 1)
        self.assertEqual(r["sixty_minute_contiguous_pairs"], 0)
        self.assertTrue(r["sixty_minute_lag_requires_60_consecutive_minute_steps"])

    def test_exact_sixty_one_minute_sequence_has_one_lag60_pair(self):
        start = datetime(2021, 7, 21, 23, 30)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(61)
        ]
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["contiguous_one_minute_segment_lengths"], [61])
        self.assertEqual(r["sixty_minute_contiguous_pairs"], 1)
        self.assertEqual(r["one_minute_pairs_crossing_midnight"], 1)
        self.assertEqual(r["contiguous_segments_spanning_calendar_days"], 1)

    def test_compensating_short_and_long_intervals_cannot_fake_lag60(self):
        start = datetime(2021, 7, 21, 12, 0)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(61)
        ]
        # 30 s then 90 s: endpoints are still exactly 60 minutes apart,
        # but the sixty intervening steps are not all one minute.
        rows[20][0] = (start + timedelta(minutes=19, seconds=30)).strftime(
            "%Y-%m-%d %H:%M:%S")
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["non_one_minute_gaps"], 2)
        self.assertEqual(r["contiguous_one_minute_segment_lengths"], [20, 1, 40])
        self.assertEqual(r["sixty_minute_contiguous_pairs"], 0)
        self.assertFalse(r["g2_qualified"])

    def test_deterministic_result_for_identical_bytes(self):
        digest = self.write()
        before = json.dumps(self.audit(expected_member_sha256=digest), sort_keys=True)
        after = json.dumps(self.audit(expected_member_sha256=digest), sort_keys=True)
        self.assertEqual(before, after)


    def test_day_split_strict_lag60_has_sixty_cross_midnight_windows(self):
        start = datetime(2021, 7, 21, 23, 0)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(121)
        ]
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["sixty_minute_contiguous_pairs"], 61)
        self.assertEqual(r["sixty_minute_contiguous_pairs_crossing_calendar_days"], 60)
        self.assertEqual(r["sixty_minute_lag_cross_day_pairs_by_later_date"],
                         {"2021-07-22": 60})
        self.assertEqual(
            r["sixty_minute_lag_cross_day_test_rows_requiring_prior_day_context"], 60)
        self.assertTrue(r["calendar_day_split_window_overlap_detected"])
        self.assertFalse(r["calendar_day_split_physical_independence_verified"])
        self.assertFalse(r["g2_qualified"])

    def test_midnight_61_row_window_has_exactly_one_cross_day_pair(self):
        start = datetime(2021, 7, 21, 23, 30)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(61)
        ]
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["sixty_minute_contiguous_pairs_crossing_calendar_days"], 1)
        self.assertEqual(r["sixty_minute_lag_cross_day_pairs_by_later_date"],
                         {"2021-07-22": 1})

    def test_nonminute_midnight_gap_breaks_cross_day_lag_context(self):
        start = datetime(2021, 7, 21, 23, 0)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(121)
        ]
        rows[60][0] = "2021-07-22 00:00:30"
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["sixty_minute_contiguous_pairs_crossing_calendar_days"], 0)
        self.assertEqual(r["sixty_minute_lag_cross_day_pairs_by_later_date"], {})
        self.assertFalse(r["calendar_day_split_window_overlap_detected"])

    def test_two_midnight_boundaries_accounted_independently(self):
        start = datetime(2021, 7, 21, 23, 0)
        rows = [
            [(start + timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S"),
             2000 + i, 10 + i, 20 + i, 30 + i]
            for i in range(1501)
        ]
        digest = self.write(rows=rows)
        r = self.audit(expected_member_sha256=digest)
        self.assertEqual(r["sixty_minute_contiguous_pairs_crossing_calendar_days"], 120)
        self.assertEqual(r["sixty_minute_lag_cross_day_pairs_by_later_date"],
                         {"2021-07-22": 60, "2021-07-23": 60})
        self.assertEqual(
            r["sixty_minute_lag_cross_day_test_rows_requiring_prior_day_context"], 120)


if __name__ == "__main__":
    unittest.main()
