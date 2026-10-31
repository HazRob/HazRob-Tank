"""Source-specific negative G2 gate for the measured UCI1081 open-set artifact."""
import json
from pathlib import Path
import unittest
from g2_signal_lineage_gate import audit_signal_lineage

MANIFEST = Path(__file__).with_name("G2_UCI1081_SIGNAL_R0_PROVENANCE_2026-10-26.json")

class UCI1081SourceContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.record=json.loads(MANIFEST.read_text(encoding="utf-8"))

    def test_baseline_is_not_r0(self):
        p=self.record["signal_processing"]
        self.assertEqual(p["feature_formula"],"MEDIAN_RESPONSE_MINUS_MEDIAN_BASELINE")
        self.assertEqual(p["baseline_window_s"],[180,300])
        self.assertEqual(p["response_window_s"],[780,900])
        self.assertEqual(p["r0_mode"],"NOT_USED")
        self.assertFalse(p["r0_subtracted_from_reading"])
        self.assertIsNone(p["r0_reference"])

    def test_real_artifact_is_exploratory_not_g2(self):
        self.assertEqual(self.record["gate"],"G2_BLOCKED")
        self.assertFalse(self.record["source_row_grouping"]["suitable_for_g2_grouped_holdout"])
        self.assertIn("SOURCE_SIGNAL_UNITS_UNVERIFIED_FOR_G2",audit_signal_lineage(self.record))

    def test_measured_false_accept_count(self):
        m=self.record["measured_exploratory_metrics"]
        self.assertEqual(m["unknown_total"],90)
        self.assertEqual(m["unknown_false_accepts"],86)
        self.assertAlmostEqual(m["unknown_reject_rate"],4/90)

    def test_source_archive_and_artifact_pinned(self):
        self.assertEqual(self.record["original_archive_sha256"],
            "813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab")
        self.assertEqual(self.record["artifact_zip_sha256"],
            "9663f13673e635c0c0b0f49d4c87e0683dccfb74b338a858e04ee7c7a7461b04")

if __name__=="__main__": unittest.main()
