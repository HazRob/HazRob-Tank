"""Synthetic CSV header identity controls at the earliest G2 bundle byte gate.

These tests are software integrity checks, not physical gas evidence.
"""
import hashlib
import tempfile
import unittest

from g2_evidence_bundle_gate import audit_evidence_bundle
from g2_evidence_audit import audit_record
from test_g2_evidence_bundle_gate import synthetic_bundle


class BundleCSVHeaderIdentityTests(unittest.TestCase):
    def _mutate_group_header(self, replacement):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            path = base / "group_manifest.csv"
            original = path.read_text(encoding="utf-8")
            self.assertTrue(original.startswith("exposure_id,"))
            path.write_text(original.replace("exposure_id,", replacement + ",", 1),
                            encoding="utf-8")
            # A matching SHA256 must not be mistaken for an unambiguous schema.
            record["evidence_files"]["group_manifest.csv"] = hashlib.sha256(
                path.read_bytes()).hexdigest()
            issues = audit_evidence_bundle(record, temp)
            return issues, audit_record(record, temp)

    def test_fullwidth_identity_header_fails_at_bundle_gate(self):
        issues, result = self._mutate_group_header("ｅxposure_id")
        self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:group_manifest.csv", issues)
        self.assertEqual(result["status"], "G2_BLOCKED")

    def test_decomposed_header_fails_at_bundle_gate(self):
        issues, result = self._mutate_group_header("cafe\u0301")
        self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:group_manifest.csv", issues)
        self.assertEqual(result["status"], "G2_BLOCKED")

    def test_canonical_multilingual_extra_header_remains_accepted(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            path = base / "group_manifest.csv"
            original = path.read_text(encoding="utf-8")
            lines = original.splitlines()
            lines[0] += ",موقع"
            for i in range(1, len(lines)):
                lines[i] += ",صنعاء"
            path.write_text("\n".join(lines) + "\n", encoding="utf-8")
            record["evidence_files"]["group_manifest.csv"] = hashlib.sha256(
                path.read_bytes()).hexdigest()
            self.assertEqual(audit_evidence_bundle(record, temp), [])


if __name__ == "__main__":
    unittest.main()
