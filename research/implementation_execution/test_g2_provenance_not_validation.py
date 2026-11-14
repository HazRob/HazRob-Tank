"""G2 provenance-only positive-path regression; never interprets metadata as science."""
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("audit_g2_independent_evidence.py")
spec = importlib.util.spec_from_file_location("g2_independent_audit", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class ProvenanceNotValidation(unittest.TestCase):
    def test_complete_metadata_cannot_grant_scientific_validation(self):
        record = {
            "source_id": "independent-source-001",
            "raw_data_path": "research/raw/independent-001.csv",
            "raw_sha256": "a" * 64,
            "measurement_date": "2026-11-12",
            "instrument_id": "instrument-001",
            "calibration_reference": "calibration-001",
            "holdout_split_id": "holdout-001",
        }
        result = module.audit({
            "schema": "hazrob.g2.independent-evidence.v1",
            "records": [record],
        })
        self.assertEqual(result["record_count"], 1)
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])
        self.assertEqual(result["errors"], [])

    def test_metadata_with_duplicate_raw_hash_stays_blocked(self):
        record = {
            "source_id": "independent-source-001",
            "raw_data_path": "research/raw/independent-001.csv",
            "raw_sha256": "a" * 64,
            "measurement_date": "2026-11-12",
            "instrument_id": "instrument-001",
            "calibration_reference": "calibration-001",
            "holdout_split_id": "holdout-001",
        }
        second = dict(record, source_id="independent-source-002",
                      raw_data_path="research/raw/independent-002.csv")
        result = module.audit({
            "schema": "hazrob.g2.independent-evidence.v1",
            "records": [record, second],
        })
        self.assertEqual(result["status"], "BLOCKED")
        self.assertFalse(result["scientifically_validated"])
        self.assertTrue(any("duplicate raw_sha256" in e for e in result["errors"]))

if __name__ == "__main__":
    unittest.main()
