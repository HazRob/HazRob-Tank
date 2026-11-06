"""Reproducible stdlib regression tests for G2 evidence inventory.

Run: python3 -m unittest discover -s research/implementation_execution -p 'test_g2_evidence_inventory.py'
"""
import contextlib
import io
import json
import pathlib
import tempfile
import unittest

from g2_evidence_inventory import inspect, SCHEMA

class InventoryTests(unittest.TestCase):
    def check(self, data, expected_code, expected_status):
        with tempfile.TemporaryDirectory() as d:
            p = pathlib.Path(d) / "evidence.json"
            p.write_text(json.dumps(data), encoding="utf-8")
            with contextlib.redirect_stdout(io.StringIO()) as out:
                code = inspect(p)
            self.assertEqual(code, expected_code)
            result = json.loads(out.getvalue())
            self.assertEqual(result["status"], expected_status)
            self.assertFalse(result.get("g2_approved", False))
            return result

    def test_empty_records_are_not_approval(self):
        result = self.check({"schema": SCHEMA, "records": []}, 0, "NO_INDEPENDENT_RECORDS")
        self.assertEqual(result["record_count"], 0)

    def test_unvalidated_record_is_not_approval(self):
        self.check({"schema": SCHEMA, "records": [{"id": "unverified"}]}, 0, "RECORDS_PRESENT_NOT_VALIDATED")

    def test_bad_schema(self):
        self.check({"schema": "other", "records": []}, 2, "INVALID_SCHEMA")

    def test_non_object_record(self):
        self.check({"schema": SCHEMA, "records": ["bad"]}, 2, "INVALID_RECORD")

    def test_missing_file(self):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            code = inspect(pathlib.Path("/definitely/missing/g2.json"))
        self.assertEqual(code, 3)
        self.assertEqual(json.loads(out.getvalue())["status"], "INPUT_ERROR")

if __name__ == "__main__":
    unittest.main()
