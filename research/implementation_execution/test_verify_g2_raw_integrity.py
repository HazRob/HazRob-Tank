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

if __name__ == "__main__":
    unittest.main()
