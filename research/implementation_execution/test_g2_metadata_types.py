"""Deterministic G2 metadata type rejection tests (stdlib only)."""
import unittest
from audit_g2_independent_evidence import audit
SCHEMA = "hazrob.g2.independent-evidence.v1"
BASE = {"source_id":"independent-001","raw_data_path":"data/raw.csv","raw_sha256":"a"*64,"measurement_date":"2026-11-10","instrument_id":"reference-001","calibration_reference":"cal-001","holdout_split_id":"holdout-001"}
class MetadataTypeFailClosedTests(unittest.TestCase):
    def test_non_string_required_fields_blocked(self):
        for field in BASE:
            for value in (None, False, True, 0, 1.5, [], {}, ["fake"]):
                with self.subTest(field=field, value=repr(value)):
                    record = dict(BASE)
                    record[field] = value
                    result = audit({"schema":SCHEMA,"records":[record]})
                    self.assertEqual(result["status"],"BLOCKED")
                    self.assertFalse(result["scientifically_validated"])
    def test_mixed_batch_blocked(self):
        bad = dict(BASE, source_id="independent-002", raw_data_path={"path":"data/other.csv"}, raw_sha256="b"*64)
        result = audit({"schema":SCHEMA,"records":[dict(BASE),bad]})
        self.assertEqual(result["status"],"BLOCKED")
        self.assertEqual(result["record_count"],2)
        self.assertFalse(result["scientifically_validated"])
if __name__ == "__main__":
    unittest.main()
