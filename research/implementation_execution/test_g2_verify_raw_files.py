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
    def test_duplicate_manifest_records_key_is_blocked(self):
        self.manifest.write_text('{"records": [], "records": [' + json.dumps(self.record) + ']}', encoding="utf-8")
        self.assertIn("duplicate JSON key", " ".join(verify(self.manifest, self.root)))

    def test_duplicate_record_field_is_blocked(self):
        record = json.dumps(self.record)
        record = record.replace('"source_id": "test-only-001"', '"source_id": "shadow", "source_id": "test-only-001"')
        self.manifest.write_text('{"records": [' + record + ']}', encoding="utf-8")
        self.assertIn("duplicate JSON key", " ".join(verify(self.manifest, self.root)))

    def test_nested_duplicate_metadata_key_is_blocked(self):
        self.manifest.write_text('{"records": [' + json.dumps(self.record) +
                                 '], "metadata": {"review": "pending", "review": "approved"}}',
                                 encoding="utf-8")
        self.assertIn("duplicate JSON key", " ".join(verify(self.manifest, self.root)))

    def test_malformed_manifest_is_blocked(self):
        self.manifest.write_text('{"records": [', encoding="utf-8")
        self.assertIn("manifest unreadable or invalid", " ".join(verify(self.manifest, self.root)))

    def test_non_object_record_is_blocked(self):
        self.assertIn("not an object", " ".join(self.check([None])))

    def test_missing_calibration_reference_is_blocked(self):
        self.record["calibration_reference"] = ""
        self.assertIn("missing/invalid required metadata", " ".join(self.check([self.record])))

    def test_uppercase_sha256_is_accepted_for_integrity_only(self):
        self.record["raw_sha256"] = self.record["raw_sha256"].upper()
        self.assertEqual([], self.check([self.record]))

    def test_empty_is_blocked(self):
        self.assertTrue(self.check([]))
    def test_matching_bytes_pass_integrity_only(self):
        self.assertEqual([], self.check([self.record]))
    def test_changed_bytes_blocked(self):
        self.raw.write_bytes(b"tampered")
        self.assertIn("mismatch", " ".join(self.check([self.record])))
    def test_zero_byte_raw_file_blocked_even_with_matching_digest(self):
        self.raw.write_bytes(b"")
        self.record["raw_sha256"] = hashlib.sha256(b"").hexdigest()
        self.assertIn("empty raw file", " ".join(self.check([self.record])))

    def test_missing_file_blocked(self):
        self.raw.unlink()
        self.assertTrue(self.check([self.record]))
    def test_path_escape_blocked(self):
        self.record["raw_data_path"] = "../outside.bin"
        self.assertIn("unsafe path", " ".join(self.check([self.record])))
    def test_absolute_raw_path_is_blocked(self):
        self.record["raw_data_path"] = str(self.raw.resolve())
        self.assertIn("unsafe path", " ".join(self.check([self.record])))

    def test_backslash_raw_path_is_blocked(self):
        self.record["raw_data_path"] = "subdir\\sample.bin"
        self.assertIn("unsafe path", " ".join(self.check([self.record])))

    def test_duplicate_resolved_path_is_blocked(self):
        alias = self.root / "alias.bin"
        alias.symlink_to(self.raw)
        second = dict(self.record, source_id="test-only-002",
                      raw_data_path="alias.bin",
                      raw_sha256="0" * 64)
        self.assertIn("duplicate raw file path", " ".join(self.check([self.record, second])))

    def test_whitespace_only_instrument_id_is_blocked(self):
        self.record["instrument_id"] = "   "
        self.assertIn("missing/invalid required metadata", " ".join(self.check([self.record])))

    def test_duplicate_digest_case_variant_is_blocked(self):
        second = dict(self.record, source_id="test-only-002",
                      raw_sha256=self.record["raw_sha256"].upper())
        self.assertIn("duplicate raw SHA-256", " ".join(self.check([self.record, second])))

    def test_valid_nested_relative_path_passes_integrity_only(self):
        subdir = self.root / "raw" / "device-01"
        subdir.mkdir(parents=True)
        target = subdir / "sample.bin"
        target.write_bytes(self.raw.read_bytes())
        self.record["raw_data_path"] = "raw/device-01/sample.bin"
        self.assertEqual([], self.check([self.record]))

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

    def test_null_byte_path_blocked_without_exception(self):
        self.record["raw_data_path"] = "sample\\u0000.bin"
        self.assertTrue(self.check([self.record]))
    def test_cyclic_symlink_blocked_without_exception(self):
        (self.root / "loop-a").symlink_to("loop-b")
        (self.root / "loop-b").symlink_to("loop-a")
        self.record["raw_data_path"] = "loop-a"
        self.assertTrue(self.check([self.record]))

if __name__ == "__main__":
    unittest.main()
