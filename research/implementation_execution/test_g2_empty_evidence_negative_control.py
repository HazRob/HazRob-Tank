"""Research-only regression tests for the independent-evidence negative control.

These fixtures are synthetic and MUST NOT be interpreted as physical G2 evidence.
Run: python -m unittest discover -s research/implementation_execution -p 'test_g2_empty_evidence_negative_control.py' -v
"""
import unittest
from g2_empty_evidence_negative_control import has_independent_records


class EmptyEvidenceNegativeControlTests(unittest.TestCase):
    def test_empty_register_is_not_eligible(self):
        self.assertFalse(has_independent_records({
            "schema": "hazrob.g2.independent-evidence.v1", "records": []
        }))

    def test_nonempty_register_is_not_automatic_approval(self):
        # Presence means only that provenance review can begin.
        self.assertTrue(has_independent_records({
            "schema": "hazrob.g2.independent-evidence.v1",
            "records": [{"synthetic": True}]
        }))

    def test_wrong_schema_is_rejected(self):
        with self.assertRaises(ValueError):
            has_independent_records({"schema": "unknown", "records": []})

    def test_missing_or_nonlist_records_are_rejected(self):
        for value in (None, {}, "raw", 1, False):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    has_independent_records({
                        "schema": "hazrob.g2.independent-evidence.v1",
                        "records": value
                    })


if __name__ == "__main__":
    unittest.main()
