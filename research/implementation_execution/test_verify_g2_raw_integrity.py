"""Raw byte integrity regression tests (stdlib only)."""
import hashlib
import tempfile
import unittest
from pathlib import Path
from verify_g2_raw_integrity import verify

class RawIntegrityTests(unittest.TestCase):
    def test_empty_manifest_is_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(verify({"records": []}, directory)["status"], "BLOCKED")

    def test_real_bytes_verified_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"raw,adc\\n1,200\\n"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest()}]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertFalse(result["scientifically_validated"])

    def test_modified_bytes_are_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "sample.csv").write_bytes(b"modified")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(b"original").hexdigest()}]}, directory)
            self.assertEqual(result["status"], "BLOCKED")

    def test_traversal_and_symlink_escape_are_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory, "root")
            root.mkdir()
            external = Path(directory, "outside.csv")
            external.write_bytes(b"raw")
            (root / "link.csv").symlink_to(external)
            for path in ("../outside.csv", "link.csv"):
                with self.subTest(path=path):
                    result = verify({"records": [{"raw_data_path": path,
                        "raw_sha256": hashlib.sha256(b"raw").hexdigest()}]}, root)
                    self.assertEqual(result["status"], "BLOCKED")


    def test_missing_file_blocks_batch_even_when_another_file_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"independent-raw-sample"
            Path(directory, "present.csv").write_bytes(data)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "present.csv", "raw_sha256": digest},
                {"raw_data_path": "missing.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertEqual(len(result["errors"]), 1)
            self.assertFalse(result["scientifically_validated"])

    def test_uppercase_sha256_matches_real_bytes_without_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"raw-data-case-check"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [{
                "raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest().upper()
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])

    def test_missing_root_is_blocked_without_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = Path(directory, "missing")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": "0" * 64}]}, missing)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])
            self.assertIn("invalid raw-data root", result["errors"][0])

    def test_file_root_is_blocked_without_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            root_file = Path(directory, "root.txt")
            root_file.write_text("not a directory")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": "0" * 64}]}, root_file)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertFalse(result["scientifically_validated"])

    def test_duplicate_raw_path_blocks_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"independent-sample"
            Path(directory, "sample.csv").write_bytes(data)
            record = {"raw_data_path": "sample.csv",
                      "raw_sha256": hashlib.sha256(data).hexdigest()}
            result = verify({"records": [record, dict(record)]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("duplicate raw file path", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_distinct_raw_paths_are_integrity_checked_only(self):
        with tempfile.TemporaryDirectory() as directory:
            records = []
            for name, data in (("a.csv", b"a"), ("b.csv", b"b")):
                Path(directory, name).write_bytes(data)
                records.append({"raw_data_path": name,
                                "raw_sha256": hashlib.sha256(data).hexdigest()})
            result = verify({"records": records}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 2)
            self.assertFalse(result["scientifically_validated"])

    def test_hardlinked_files_cannot_count_as_independent_files(self):
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory, "first.csv")
            second = Path(directory, "second.csv")
            data = b"same-inode"
            first.write_bytes(data)
            import os
            os.link(first, second)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "first.csv", "raw_sha256": digest},
                {"raw_data_path": "second.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("hard link", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_internal_symlink_alias_cannot_inflate_raw_file_count(self):
        with tempfile.TemporaryDirectory() as directory:
            original = Path(directory, "original.csv")
            alias = Path(directory, "alias.csv")
            data = b"single-source"
            original.write_bytes(data)
            alias.symlink_to(original.name)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "original.csv", "raw_sha256": digest},
                {"raw_data_path": "alias.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("duplicate raw file path", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

if __name__ == "__main__":
    unittest.main()
