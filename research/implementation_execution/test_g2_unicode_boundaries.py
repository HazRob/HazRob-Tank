"""G2 Unicode identifier/path boundary regressions; provenance checks only."""
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("audit_g2_independent_evidence.py")
spec = importlib.util.spec_from_file_location("g2_audit_unicode_boundaries", SCRIPT)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

def record():
    return {
        "source_id": "sample-01",
        "raw_data_path": "research/raw/sample-01.csv",
        "raw_sha256": "a" * 64,
        "measurement_date": "2026-11-12",
        "instrument_id": "instrument-01",
        "calibration_reference": "calibration-01",
        "holdout_split_id": "holdout-01",
    }

def check(item):
    return audit.audit({"schema": "hazrob.g2.independent-evidence.v1", "records": [item]})

class UnicodeBoundaryRegressions(unittest.TestCase):
    def test_invisible_and_compatibility_identifier_characters_blocked(self):
        for field in ("source_id", "instrument_id", "calibration_reference", "holdout_split_id"):
            for value in ("sample\u200b01", "sample\u202e01", "sample\u00ad01", "sample\uff11"):
                with self.subTest(field=field, value=repr(value)):
                    item = record()
                    item[field] = value
                    result = check(item)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertFalse(result["scientifically_validated"])
                    self.assertTrue(any("unsafe " + field in e for e in result["errors"]))

    def test_invisible_and_compatibility_path_characters_blocked(self):
        for value in ("research/raw/sa\u200bmple.csv", "research/raw/sa\u202emple.csv",
                      "research/raw/sa\u00ad mple.csv", "research/raw/\uff11.csv"):
            with self.subTest(path=repr(value)):
                item = record()
                item["raw_data_path"] = value
                result = check(item)
                self.assertEqual(result["status"], "BLOCKED")
                self.assertFalse(result["scientifically_validated"])
                self.assertTrue(any("unsafe raw_data_path" in e for e in result["errors"]))

    def test_normal_unicode_does_not_grant_scientific_validation(self):
        item = record()
        item["source_id"] = "قياس-01"
        result = check(item)
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])

if __name__ == "__main__":
    unittest.main()
