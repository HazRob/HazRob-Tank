"""Fail-closed regression tests of source-specific retrospective MQ4 split audit."""
import hashlib
import json
import sys
import unittest
from datetime import date, timedelta
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_retrospective_split_audit import SOURCE, SOURCE_SHA256, audit


class RetrospectiveProtocolTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = SOURCE.read_bytes()

    def test_source_hash(self):
        self.assertEqual(hashlib.sha256(self.raw).hexdigest(), SOURCE_SHA256)

    def test_pinned_replay(self):
        pinned = json.loads(SOURCE.with_name("G2_MQ4_RETROSPECTIVE_PROTOCOL_AUDIT_2026-11-01.json").read_text())
        self.assertEqual(audit(self.raw), pinned)

    def test_future_training(self):
        self.assertEqual(audit(self.raw)["folds_with_future_training_days"], 10)

    def test_future_calibration(self):
        self.assertEqual(audit(self.raw)["folds_with_future_calibration_day"], 1)

    def test_first_day_wraparound(self):
        f = audit(self.raw)["folds"][0]
        self.assertEqual(f["calibration_day"], "2021-07-31")
        self.assertTrue(f["future_calibration_day"])

    def test_last_day_is_prospective(self):
        f = audit(self.raw)["folds"][-1]
        self.assertEqual(f["test_day"], "2021-07-31")
        self.assertTrue(f["prospective_valid"])

    def test_disjoint_folds(self):
        for f in audit(self.raw)["folds"]:
            self.assertEqual(len(f["training_days"]), 9)
            self.assertEqual(len(set(f["training_days"] + [f["calibration_day"], f["test_day"]])), 11)

    def test_no_cross_protocol_comparison(self):
        p = audit(self.raw)["protocol_comparison"]
        self.assertFalse(p["pooled_metrics_directly_comparable"])
        self.assertTrue(p["future_day_training_invalidates_prospective_claim"])

    def test_all_five_interval_coverage_below_target(self):
        self.assertEqual(len(audit(self.raw)["models_below_nominal_90pct_pooled_interval_coverage"]), 5)

    def test_no_promotion(self):
        x = audit(self.raw)
        self.assertFalse(x["g2_qualified"])
        self.assertFalse(x["g3_qualified"])
        self.assertEqual(x["physical_independent_reference_records_verified"], 0)

    def test_tamper_rejected(self):
        for altered in (self.raw + b" ", self.raw.replace(b"14186", b"14185"), self.raw[:100]):
            with self.subTest(size=len(altered)), self.assertRaisesRegex(ValueError, "SHA_MISMATCH"):
                audit(altered)

    def test_invalid_days_rejected(self):
        days = [(date(2021, 7, 21) + timedelta(days=i)).isoformat() for i in range(11)]
        for altered in (days[:-1], days[::-1], days[:10] + [days[0]], days[:10] + ["bad"], True):
            with self.subTest(value=str(altered)[:40]), self.assertRaises(ValueError):
                audit(self.raw, altered)


if __name__ == "__main__":
    unittest.main()
