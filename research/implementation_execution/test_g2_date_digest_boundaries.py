"""Deterministic G2 provenance boundary regressions (no physical validation)."""
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("audit_g2_independent_evidence.py")
spec = importlib.util.spec_from_file_location("g2_audit_date_digest", SCRIPT)
audit_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit_module)

def record(source, date_value, digest):
    return {
        "source_id": source,
        "raw_data_path": "research/raw/" + source + ".csv",
        "raw_sha256": digest,
        "measurement_date": date_value,
        "instrument_id": "instrument-01",
        "calibration_reference": "calibration-01",
        "holdout_split_id": "holdout-01",
    }

def manifest(*records):
    return {"schema": "hazrob.g2.independent-evidence.v1",
            "records": list(records)}

class DateAndDigestBoundaries(unittest.TestCase):
    def test_calendar_invalid_dates_are_blocked(self):
        for value in ("2026-02-29", "2025-02-29", "2026-04-31",
                      "2026-13-01", "2026-00-01", "2026-01-00",
                      "2026-1-01", "2026-01-1", "2026-01-01T00:00:00"):
            with self.subTest(value=value):
                result = audit_module.audit(manifest(record("sample", value, "a" * 64)))
                self.assertEqual(result["status"], "BLOCKED")
                self.assertFalse(result["scientifically_validated"])
                self.assertTrue(any("invalid measurement_date" in e for e in result["errors"]))

    def test_valid_leap_day_remains_provenance_only(self):
        result = audit_module.audit(manifest(record("sample", "2024-02-29", "a" * 64)))
        self.assertEqual(result["status"], "PROVENANCE_REVIEW_REQUIRED")
        self.assertFalse(result["scientifically_validated"])

    def test_duplicate_digest_is_case_insensitive(self):
        result = audit_module.audit(manifest(
            record("sample-1", "2026-11-11", "abcdef" * 10 + "abcd"),
            record("sample-2", "2026-11-12", "ABCDEF" * 10 + "ABCD"),
        ))
        self.assertEqual(result["status"], "BLOCKED")
        self.assertFalse(result["scientifically_validated"])
        self.assertTrue(any("duplicate raw_sha256" in e for e in result["errors"]))

if __name__ == "__main__":
    unittest.main()
