"""Synthetic-only deterministic regressions for the grouped MQ4 probe."""
import csv
import hashlib
import io
import json
import sys
import unittest
import zipfile
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_grouped_numeric_probe import MEMBER, probe_csv, probe_zip


def synthetic():
    buf = io.StringIO()
    writer = csv.writer(buf)
    writer.writerow(["datetime", "licor_ch4", "MQ4_1", "MQ4_2", "MQ4_3"])
    first = datetime(2021, 1, 1, 0, 0)
    for day in range(3):
        for minute in range(5):
            ts = first + timedelta(days=day, minutes=minute)
            writer.writerow([ts.strftime("%Y-%m-%d %H:%M:%S"), 2000+day*100+minute*10,
                             1000-day*20-minute*2, 3000+day*40+minute*3, 5000+day*50+minute*4])
    return buf.getvalue().encode("utf-8")


class MQ4GroupedNumericProbeTests(unittest.TestCase):
    def test_three_day_grouped_holdout(self):
        r = probe_csv(synthetic())
        self.assertEqual(r["rows"], 15)
        self.assertEqual(r["days"], 3)
        self.assertEqual([f["test_rows"] for f in r["channels"]["MQ4_1"]["folds"].values()], [5]*3)
        self.assertEqual([f["train_rows"] for f in r["channels"]["MQ4_1"]["folds"].values()], [10]*3)
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["blinded_holdout_verified"])

    def test_repeatability(self):
        self.assertEqual(json.dumps(probe_csv(synthetic()), sort_keys=True),
                         json.dumps(probe_csv(synthetic()), sort_keys=True))

    def test_member_hash_mismatch(self):
        with self.assertRaisesRegex(ValueError, "MEMBER_SHA256"):
            probe_csv(synthetic(), "0"*64)

    def test_timestamp_duplicate_rejected(self):
        raw = synthetic().replace(b"2021-01-01 00:01:00", b"2021-01-01 00:00:00")
        with self.assertRaisesRegex(ValueError, "TIME_ORDER"):
            probe_csv(raw)

    def test_nonfinite_rejected(self):
        raw = synthetic().replace(b",2000,", b",nan,")
        with self.assertRaisesRegex(ValueError, "FINITE"):
            probe_csv(raw)

    def test_pinned_zip_replay_and_tamper(self):
        raw = synthetic()
        out = io.BytesIO()
        with zipfile.ZipFile(out, "w") as z:
            z.writestr(MEMBER, raw)
        archive = out.getvalue()
        result = probe_zip(archive, hashlib.sha256(archive).hexdigest(), hashlib.sha256(raw).hexdigest())
        self.assertEqual(result["rows"], 15)
        with self.assertRaisesRegex(ValueError, "ARCHIVE_SHA256"):
            probe_zip(archive+b"x", hashlib.sha256(archive).hexdigest(), hashlib.sha256(raw).hexdigest())


if __name__ == "__main__":
    unittest.main()
