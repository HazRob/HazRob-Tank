"""Synthetic research-only regression tests; no physical G2 evidence."""
import json
import tempfile
import unittest
from pathlib import Path
from g2_empty_evidence_negative_control import has_independent_records, load_register

SCHEMA = "hazrob.g2.independent-evidence.v1"


class EmptyEvidenceNegativeControlTests(unittest.TestCase):
    def test_empty_register_is_not_eligible(self):
        self.assertFalse(has_independent_records({"schema": SCHEMA, "records": []}))

    def test_nonempty_register_is_not_automatic_approval(self):
        # True means records exist, NOT that they are scientifically qualified.
        self.assertTrue(has_independent_records({"schema": SCHEMA, "records": [{"synthetic": True}]}))

    def test_wrong_schema_is_rejected(self):
        with self.assertRaises(ValueError):
            has_independent_records({"schema": "unknown", "records": []})

    def test_missing_or_nonlist_records_are_rejected(self):
        for value in (None, {}, "raw", 1, False):
            with self.subTest(value=value), self.assertRaises(ValueError):
                has_independent_records({"schema": SCHEMA, "records": value})

    def test_nonobject_payload_is_rejected(self):
        for value in (None, [], 3, "text"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                has_independent_records(value)

    def test_approval_fields_rejected_even_when_false(self):
        for field in ("g2_approved", "g3_approved"):
            for value in (None, False, True, "yes"):
                with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                    has_independent_records({"schema": SCHEMA, "records": [], field: value})

    def test_nested_approval_assertions_rejected(self):
        for field in ("g2_approved", "g3_approved"):
            for value in (None, False, True):
                with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                    has_independent_records({"schema": SCHEMA, "records": [{"raw": {"metadata": [{field: value}]}}]})

    def test_duplicate_keys_and_nonfinite_numbers_rejected(self):
        cases = (
            '{"schema":"%s","records":[],"records":[]}' % SCHEMA,
            '{"schema":"%s","records":[NaN]}' % SCHEMA,
            '{"schema":"%s","records":[Infinity]}' % SCHEMA,
        )
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "register.json"
            for raw in cases:
                with self.subTest(raw=raw), self.assertRaises(ValueError):
                    path.write_text(raw, encoding="utf-8")
                    load_register(path)

    def test_nested_duplicate_keys_and_malformed_json_rejected(self):
        # Nested duplicates can otherwise silently overwrite provenance fields.
        cases = (
            '{"schema":"%s","records":[{"sample_id":"A","sample_id":"B"}]}' % SCHEMA,
            '{"schema":"%s","records":[{"raw":{"device":"A","device":"B"}}]}' % SCHEMA,
            '{"schema":"%s","records":[' % SCHEMA,
            '{"schema":"%s","records":[]}, trailing' % SCHEMA,
        )
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "register.json"
            for raw in cases:
                with self.subTest(raw=raw), self.assertRaises(ValueError):
                    path.write_text(raw, encoding="utf-8")
                    load_register(path)

    def test_nonfinite_numbers_rejected_inside_nested_records(self):
        for constant in ("NaN", "Infinity", "-Infinity"):
            with tempfile.TemporaryDirectory() as tmp:
                path = Path(tmp) / "register.json"
                path.write_text(
                    '{"schema":"%s","records":[{"raw":{"adc":%s}}]}' % (SCHEMA, constant),
                    encoding="utf-8",
                )
                with self.subTest(constant=constant), self.assertRaises(ValueError):
                    load_register(path)

    def test_in_memory_nonfinite_numeric_values_rejected(self):
        for value in (float('nan'), float('inf'), float('-inf')):
            with self.subTest(value=str(value)), self.assertRaises(ValueError):
                has_independent_records({'schema': SCHEMA, 'records': [{'raw': {'adc': [value]}}]})

    def test_finite_numeric_values_are_not_approval(self):
        self.assertTrue(has_independent_records({'schema': SCHEMA, 'records': [{'raw': {'adc': [0.0, 1.0, -1.0]}}]}))

    def test_valid_register_round_trip(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "register.json"
            path.write_text(json.dumps({"schema": SCHEMA, "records": []}), encoding="utf-8")
            self.assertFalse(has_independent_records(load_register(path)))


if __name__ == "__main__":
    unittest.main()
