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
    def test_whitespace_variant_duplicate_source_is_blocked(self):
        first = valid_record()
        second = valid_record()
        second["source_id"] = " independent-001 "
        result = audit({"schema": SCHEMA, "records": [first, second]})
        self.assertEqual(result["status"], "BLOCKED")
        self.assertFalse(result["scientifically_validated"])
    def test_unsafe_raw_paths_fail_closed(self):
        for path in ("../private.csv", "/tmp/data.csv", "C:/secret.csv", "data/../raw.csv", "data\\\\raw.csv", " raw.csv", "raw.csv" + chr(10)):
            with self.subTest(path=path):
                record = valid_record()
                record["raw_data_path"] = path
                result = audit({"schema": SCHEMA, "records": [record]})
                self.assertEqual(result["status"], "BLOCKED")
                self.assertFalse(result["scientifically_validated"])
    def test_safe_relative_raw_path_remains_metadata_only(self):
        record = valid_record()
        record["raw_data_path"] = "research/raw/source-001.csv"
        result = audit({"schema": SCHEMA, "records": [record]})
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])
    def test_invalid_calendar_dates_fail_closed(self):
        for value in ("2026-02-30", "2026-13-01", "yesterday", "2026-1-1", "2026-11-10T00:00:00", 20261110):
            with self.subTest(value=value):
                record = valid_record()
                record["measurement_date"] = value
                result = audit({"schema": SCHEMA, "records": [record]})
                self.assertEqual(result["status"], "BLOCKED")
                self.assertFalse(result["scientifically_validated"])
    def test_valid_leap_day_remains_metadata_only(self):
        record = valid_record()
        record["measurement_date"] = "2024-02-29"
        result = audit({"schema": SCHEMA, "records": [record]})
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])
    def test_duplicate_raw_path_is_blocked_even_with_distinct_source_ids(self):
        first = valid_record()
        second = valid_record()
        second["source_id"] = "independent-002"
        second["raw_sha256"] = "b" * 64
        result = audit({"schema": SCHEMA, "records": [first, second]})
        self.assertEqual(result["status"], "BLOCKED")
        self.assertTrue(any("duplicate raw_data_path" in error for error in result["errors"]))
        self.assertFalse(result["scientifically_validated"])
    def test_distinct_raw_paths_remain_metadata_only(self):
        first = valid_record()
        second = valid_record()
        second["source_id"] = "independent-002"
        second["raw_data_path"] = "data/raw2.csv"
        result = audit({"schema": SCHEMA, "records": [first, second]})
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])
    def test_ambiguous_relative_paths_fail_closed(self):
        for path in ("data//raw.csv", "data/raw.csv/", "./data/raw.csv"):
            with self.subTest(path=path):
                record = valid_record()
                record["raw_data_path"] = path
                result = audit({"schema": SCHEMA, "records": [record]})
                self.assertEqual(result["status"], "BLOCKED")
                self.assertFalse(result["scientifically_validated"])
    def test_provenance_control_characters_and_outer_whitespace_fail_closed(self):
        for field in ("source_id", "instrument_id", "calibration_reference", "holdout_split_id"):
            for value in (" value ", "value" + chr(10), "value" + chr(0)):
                with self.subTest(field=field, value=repr(value)):
                    record = valid_record()
                    record[field] = value
                    result = audit({"schema": SCHEMA, "records": [record]})
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertFalse(result["scientifically_validated"])
    def test_invalid_schema_is_blocked(self):
        self.assertEqual(audit({"schema":"other","records":[]})["status"],"BLOCKED")
if __name__ == "__main__":
    unittest.main()
