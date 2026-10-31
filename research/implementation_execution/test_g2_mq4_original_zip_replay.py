"""Synthetic controls for raw CSV reconstruction. CI does not possess Library ZIP."""
import json
import sys
import unittest
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from g2_mq4_original_zip_replay import derive


def fixture(times, tweak=None):
    cols = ["datetime", "licor_ch4", "licor_h2o", "t", "rh"] + [
        "sensor_" + str(i) for i in range(12)] + ["MQ4_1", "MQ4_2", "MQ4_3"]
    assert len(cols) == 20
    lines = [",".join(cols)]
    for i, t in enumerate(times):
        row = [t.strftime("%Y-%m-%d %H:%M:%S")] + ["2000"] * 19
        if tweak is not None:
            tweak(i, row)
        lines.append(",".join(row))
    return ("\n".join(lines) + "\n").encode("utf-8")


class MQ4OriginalZipReplayTests(unittest.TestCase):
    def test_midnight_one_minute_crossing(self):
        base = datetime(2021, 7, 21, 23, 59)
        result = derive(fixture([base + timedelta(minutes=i) for i in range(3)]))
        self.assertEqual(result["daily_rows"], {"2021-07-21": 1, "2021-07-22": 2})
        self.assertEqual(result["segment_lengths"], [3])
        self.assertEqual(result["lags"]["1"]["cross_day_by_later_date"], {"2021-07-22": 1})
        self.assertEqual(result["lags"]["1"]["contiguous_lag_pairs"], 2)
        self.assertEqual(result["lags"]["5"]["contiguous_lag_pairs"], 0)

    def test_strict_gap_does_not_bridge(self):
        base = datetime(2021, 7, 21, 10)
        result = derive(fixture([base, base + timedelta(minutes=1),
                                 base + timedelta(minutes=3)]))
        self.assertEqual(result["segment_lengths"], [2, 1])
        self.assertEqual(result["lags"]["1"]["contiguous_lag_pairs"], 1)

    def test_long_lag_cross_midnight(self):
        base = datetime(2021, 7, 21, 23)
        result = derive(fixture([base + timedelta(minutes=i) for i in range(125)]))
        self.assertEqual(result["lags"]["120"]["contiguous_lag_pairs"], 5)
        self.assertEqual(result["lags"]["120"]["cross_day_context_rows"], 5)
        self.assertEqual(result["max_lag_eligible_by_day"], {"2021-07-22": 5})

    def test_reject_duplicate_timestamps(self):
        t = datetime(2021, 7, 21, 10)
        with self.assertRaisesRegex(ValueError, "TIMESTAMP_ORDER_OR_DUPLICATE"):
            derive(fixture([t, t]))

    def test_reject_out_of_order_timestamps(self):
        t = datetime(2021, 7, 21, 10)
        with self.assertRaisesRegex(ValueError, "TIMESTAMP_ORDER_OR_DUPLICATE"):
            derive(fixture([t + timedelta(minutes=1), t]))

    def test_reject_nonminute_timestamps(self):
        t = datetime(2021, 7, 21, 10)
        with self.assertRaisesRegex(ValueError, "MINUTE_TIMESTAMP"):
            derive(fixture([t, t + timedelta(seconds=30)]))

    def test_reject_nan_sensor_value(self):
        t = datetime(2021, 7, 21, 10)
        with self.assertRaisesRegex(ValueError, "NONFINITE"):
            derive(fixture([t, t + timedelta(minutes=1)],
                           lambda i, row: row.__setitem__(18, "nan") if i == 1 else None))

    def test_reject_missing_sensor_value(self):
        t = datetime(2021, 7, 21, 10)
        with self.assertRaisesRegex(ValueError, "MISSING_CELLS"):
            derive(fixture([t, t + timedelta(minutes=1)],
                           lambda i, row: row.__setitem__(18, "") if i == 1 else None))

    def test_reject_wrong_column_count(self):
        t = datetime(2021, 7, 21, 10)
        bad = fixture([t, t + timedelta(minutes=1)]).replace(b",MQ4_3", b"")
        with self.assertRaisesRegex(ValueError, "COLUMNS"):
            derive(bad)

    def test_pinned_scientific_evidence_still_empty(self):
        evidence = json.loads((ROOT / "G2_INDEPENDENT_EVIDENCE.json").read_text())
        self.assertEqual(evidence["records"], [])

    def test_pinned_raw_replay_result_fail_closed(self):
        report = json.loads((ROOT / "G2_MQ4_ORIGINAL_ZIP_RAW_REPLAY_2026-10-31.json").read_text())
        self.assertEqual(report["schema"], "hazrob.g2.mq4-original-zip-raw-replay.v1")
        self.assertEqual(report["derived"]["rows"], 14186)
        self.assertEqual(report["derived"]["segment_lengths"],
                         [1406, 1565, 1388, 5, 1454, 2495, 675, 1367, 995, 1417, 1419])
        self.assertFalse(report["g2_qualified"])
        self.assertFalse(report["g3_tank_qualified"])
        self.assertTrue(report["raw_replayed_in_this_execution"])


if __name__ == "__main__":
    unittest.main()
