"""Synthetic negative controls and pinned-source byte-identical metadata replay."""
import copy
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from g2_mq4_calendar_minute_capacity import audit, load


def pinned():
    return (
        load(ROOT / "G2_MQ_LIBRARY_RAW_INTAKE_2026-10-30.json"),
        load(ROOT / "G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json"),
        load(ROOT / "G2_MQ4_STRICT_CONTIGUOUS_SEGMENTS_2026-10-30.json")
    )


class MQ4CalendarCapacityTests(unittest.TestCase):
    def test_pinned_replay_and_no_promotion(self):
        result = audit(*pinned())
        self.assertEqual((result["reported_rows"], result["reported_calendar_days"],
                          result["reported_max_daily_rows"]), (14186, 11, 1440))
        self.assertFalse(result["original_raw_zip_replayed_by_this_audit"])
        self.assertFalse(result["g2_qualified"])
        self.assertFalse(result["g3_tank_qualified"])

    def test_pinned_result_byte_identical(self):
        result = audit(*pinned())
        saved = ROOT / "G2_MQ4_REPORTED_CALENDAR_MINUTE_CAPACITY_2026-10-31.json"
        self.assertEqual(saved.read_text(encoding="utf-8"),
                         json.dumps(result, sort_keys=True, ensure_ascii=False,
                                    indent=2) + "\n")

    def test_reject_duplicate_source_identity(self):
        a, b, c = pinned()
        a["source_entries"].append(copy.deepcopy(a["source_entries"][0]))
        with self.assertRaisesRegex(ValueError, "SOURCE_UNIQUE"):
            audit(a, b, c)

    def test_reject_unverified_unique_minute_claim(self):
        a, b, c = pinned()
        a["source_entries"][0]["unique_timestamps"] = False
        with self.assertRaisesRegex(ValueError, "REPORTED_MINUTE_UNIQUENESS"):
            audit(a, b, c)

    def test_reject_source_member_digest_drift(self):
        a, b, c = pinned()
        a["source_entries"][0]["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SOURCE_PIN"):
            audit(a, b, c)

    def test_reject_nonminute_timestamp(self):
        a, b, c = pinned()
        a["source_entries"][0]["first_timestamp"] = "2021-07-21 11:06:01"
        with self.assertRaisesRegex(ValueError, "MINUTE_ALIGNMENT"):
            audit(a, b, c)

    def test_reject_daily_count_over_1440(self):
        a, b, c = pinned()
        b["folds"]["2021-07-26"]["holdout_total"] = 1441
        with self.assertRaisesRegex(ValueError, "DAY_MINUTE_CAPACITY"):
            audit(a, b, c)

    def test_reject_first_day_capacity_excess(self):
        a, b, c = pinned()
        b["folds"]["2021-07-21"]["holdout_total"] = 775
        with self.assertRaisesRegex(ValueError, "FIRST_DAY_CAPACITY"):
            audit(a, b, c)

    def test_reject_last_day_capacity_excess(self):
        a, b, c = pinned()
        b["folds"]["2021-07-31"]["holdout_total"] = 724
        with self.assertRaisesRegex(ValueError, "LAST_DAY_CAPACITY"):
            audit(a, b, c)

    def test_reject_segment_sum_drift(self):
        a, b, c = pinned()
        c["contiguous_one_minute_segment_lengths"][0] += 1
        with self.assertRaisesRegex(ValueError, "SEGMENT_SUM"):
            audit(a, b, c)


if __name__ == "__main__":
    unittest.main()
