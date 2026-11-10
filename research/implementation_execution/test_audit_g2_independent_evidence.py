"""Regression tests for the G2 independent-evidence provenance gate.

Run: python3 -m unittest discover -s research/implementation_execution -p 'test_audit_g2_independent_evidence.py'
"""
import unittest
from audit_g2_independent_evidence import audit

SCHEMA = "hazrob.g2.independent-evidence.v1"
def valid_record():
    return {"source_id":"independent-001","raw_data_path":"data/raw.csv",
            "raw_sha256":"a"*64,"measurement_date":"2026-11-10",
            "instrument_id":"reference-001","calibration_reference":"cal-001",
            "holdout_split_id":"holdout-001"}

class EvidenceGateTests(unittest.TestCase):
    def test_empty_is_blocked(self):
        result = audit({"schema":SCHEMA,"records":[]})
        self.assertEqual(result["status"],"BLOCKED")
        self.assertFalse(result["scientifically_validated"])
    def test_missing_provenance_is_blocked(self):
        record = valid_record()
        del record["calibration_reference"]
        self.assertEqual(audit({"schema":SCHEMA,"records":[record]})["status"],"BLOCKED")
    def test_bad_digest_is_blocked(self):
        record = valid_record()
        record["raw_sha256"] = "not-a-digest"
        self.assertEqual(audit({"schema":SCHEMA,"records":[record]})["status"],"BLOCKED")
    def test_duplicate_source_is_blocked(self):
        record = valid_record()
        self.assertEqual(audit({"schema":SCHEMA,"records":[record,record]})["status"],"BLOCKED")
    def test_complete_metadata_is_not_scientific_approval(self):
        result = audit({"schema":SCHEMA,"records":[valid_record()]})
        self.assertEqual(result["status"],"PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])
    def test_unhashable_source_id_fails_closed(self):
        record = valid_record()
        record["source_id"] = ["bad", "identifier"]
        result = audit({"schema": SCHEMA, "records": [record]})
        self.assertEqual(result["status"], "BLOCKED")
        self.assertFalse(result["scientifically_validated"])
    def test_invalid_schema_is_blocked(self):
        self.assertEqual(audit({"schema":"other","records":[]})["status"],"BLOCKED")
if __name__ == "__main__":
    unittest.main()
