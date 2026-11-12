"""Deterministic regression tests for G2 raw-byte integrity preflight."""
import hashlib
import json
import pathlib
import tempfile
import unittest
from g2_verify_raw_files import verify

class RawIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        self.raw = self.root / "sample.bin"
        self.raw.write_bytes(b"measured-raw-bytes\\x00")
        self.manifest = self.root / "manifest.json"
        self.record = {
            "source_id": "test-only-001",
            "raw_data_path": "sample.bin",
            "raw_sha256": hashlib.sha256(self.raw.read_bytes()).hexdigest(),
            "measurement_date": "2026-11-12",
            "instrument_id": "synthetic-test-instrument",
            "calibration_reference": "synthetic-test-calibration",
            "holdout_split_id": "test-only",
        }
    def check(self, records):
        self.manifest.write_text(json.dumps({"records": records}), encoding="utf-8")
        return verify(self.manifest, self.root)
    def test_empty_is_blocked(self):
        self.assertTrue(self.check([]))
    def test_matching_bytes_pass_integrity_only(self):
        self.assertEqual([], self.check([self.record]))
    def test_changed_bytes_blocked(self):
        self.raw.write_bytes(b"tampered")
        self.assertIn("mismatch", " ".join(self.check([self.record])))
    def test_missing_file_blocked(self):
        self.raw.unlink()
        self.assertTrue(self.check([self.record]))
    def test_path_escape_blocked(self):
        self.record["raw_data_path"] = "../outside.bin"
        self.assertIn("unsafe path", " ".join(self.check([self.record])))
    def test_duplicate_digest_blocked(self):
        second = dict(self.record, source_id="test-only-002")
        self.assertIn("duplicate raw SHA-256", " ".join(self.check([self.record, second])))
    def test_duplicate_id_blocked(self):
        self.assertIn("duplicate source_id", " ".join(self.check([self.record, self.record])))
    def test_bad_hash_blocked(self):
        self.record["raw_sha256"] = "x" * 64
        self.assertTrue(self.check([self.record]))
    def test_symlink_escape_blocked(self):
        outside = self.root.parent / (self.root.name + "-outside")
        outside.write_bytes(b"outside")
        self.addCleanup(lambda: outside.unlink(missing_ok=True))
        (self.root / "escape.bin").symlink_to(outside)
        self.record["raw_data_path"] = "escape.bin"
        self.assertTrue(self.check([self.record]))

if __name__ == "__main__":
    unittest.main()
