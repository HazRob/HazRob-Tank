"""Deterministic fail-closed tests: MQ4 source-derived 4 ppm confusion matrices."""
import hashlib
import json
import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_retrospective_threshold_audit import SOURCE, SOURCE_SHA256, audit, POSITIVE, NEGATIVE


class ThresholdRiskAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = SOURCE.read_bytes()
        cls.result = audit(cls.raw)

    def test_source_integrity(self):
        self.assertEqual(hashlib.sha256(self.raw).hexdigest(), SOURCE_SHA256)

    def test_pinned_replay(self):
        expected = json.loads(SOURCE.with_name("G2_MQ4_RETROSPECTIVE_THRESHOLD_RISK_2026-11-01.json").read_text())
        self.assertEqual(self.result, expected)

    def test_total_reference_denominators(self):
        self.assertEqual((POSITIVE, NEGATIVE, POSITIVE + NEGATIVE), (2346, 11840, 14186))

    def test_five_predeclared_models(self):
        self.assertEqual(len(self.result["models"]), 5)

    def test_all_confusion_matrices_reconcile(self):
        for m in self.result["models"]:
            self.assertEqual(m["true_positive"] + m["false_negative"], POSITIVE)
            self.assertEqual(m["true_negative"] + m["false_positive"], NEGATIVE)

    def test_reference_only_baseline_cannot_detect_positives(self):
        m = self.result["models"][0]
        self.assertEqual(m["sensitivity"], 0.0)
        self.assertIsNone(m["precision"])
        self.assertEqual(m["false_positive_rate"], 0.0)

    def test_hgb_reproduces_reported_sensitivity(self):
        m = self.result["models"][-1]
        self.assertAlmostEqual(m["sensitivity"], 0.22719522591645352)

    def test_best_pooled_sensitivity_still_misses_cases(self):
        self.assertEqual(self.result["max_observed_sensitivity_model"], "RIDGE_MQ4_3_T_RH")
        self.assertLess(self.result["max_observed_sensitivity"], 0.56)
        self.assertGreater(self.result["models"][-2]["miss_rate"], 0.44)

    def test_all_interval_coverages_below_nominal(self):
        self.assertTrue(self.result["all_models_below_90pct_pooled_interval_coverage"])
        self.assertTrue(all(m["pooled_interval_coverage"] < .9 for m in self.result["models"]))

    def test_threshold_not_safety_limit(self):
        self.assertFalse(self.result["threshold_is_safety_limit"])

    def test_no_promotion_or_independent_records(self):
        self.assertFalse(self.result["g2_qualified"])
        self.assertFalse(self.result["g3_qualified"])
        self.assertEqual(self.result["independent_physical_reference_records_verified"], 0)

    def test_tampered_snapshot_rejected(self):
        for tampered in (self.raw + b" ", self.raw[:-1], self.raw.replace(b"2346", b"2347")):
            with self.subTest(length=len(tampered)), self.assertRaisesRegex(ValueError, "SHA_MISMATCH"):
                audit(tampered)

    def test_no_prospective_inference(self):
        self.assertIn("not prospective", self.result["evaluation_protocol"])
        self.assertTrue(self.result["pooled_rows_not_independent_exposure_groups"])


if __name__ == "__main__":
    unittest.main()
