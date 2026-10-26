"""Offline regression tests; synthetic ZIP fixtures never become G2 evidence."""
import hashlib
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from g2_zenodo_mq01_raw_inventory import inspect_archive, download_verified

class ZenodoMQ01Tests(unittest.TestCase):
    def test_zip_inventory_reads_and_verifies_crc(self):
        with tempfile.TemporaryDirectory() as d:
            archive = Path(d) / "test.zip"
            with zipfile.ZipFile(archive, "w") as zf:
                zf.writestr("samples/a.csv", "MQ2,MQ3\\n1,2\\n")
            members = inspect_archive(archive)
            self.assertEqual(len(members), 1)
            self.assertEqual(members[0]["path"], "samples/a.csv")

    def test_zip_traversal_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            archive = Path(d) / "test.zip"
            with zipfile.ZipFile(archive, "w") as zf:
                zf.writestr("../outside.csv", "bad")
            with self.assertRaisesRegex(ValueError, "ZIP_UNSAFE_MEMBER_PATH"):
                inspect_archive(archive)

    def test_original_checksum_mismatch_fails_closed(self):
        with tempfile.TemporaryDirectory() as d:
            source = Path(d) / "source.bin"
            source.write_bytes(b"synthetic-not-source")
            with self.assertRaisesRegex(RuntimeError, "MD5_MISMATCH"):
                download_verified(Path(d) / "copy.bin", source.as_uri())

    def test_published_md5_match_but_sha256_mismatch_is_rejected(self):
        # Synthetic bytes: proves both digests must match independently.
        with tempfile.TemporaryDirectory() as d:
            source = Path(d) / "source.bin"
            payload = b"synthetic-source-with-matching-md5-only"
            source.write_bytes(payload)
            with patch("g2_zenodo_mq01_raw_inventory.PUBLISHED_MD5", hashlib.md5(payload).hexdigest()):
                with self.assertRaisesRegex(RuntimeError, "ORIGINAL_ARCHIVE_SHA256_MISMATCH"):
                    download_verified(Path(d) / "copy.bin", source.as_uri())

    def test_checksum_match_is_byte_verified(self):
        with tempfile.TemporaryDirectory() as d:
            source = Path(d) / "source.bin"
            payload = b"synthetic-only"
            source.write_bytes(payload)
            with patch("g2_zenodo_mq01_raw_inventory.PUBLISHED_MD5", hashlib.md5(payload).hexdigest()), \
                 patch("g2_zenodo_mq01_raw_inventory.PINNED_SHA256", hashlib.sha256(payload).hexdigest()):
                record = download_verified(Path(d) / "copy.bin", source.as_uri())
            self.assertEqual(record["sha256"], hashlib.sha256(payload).hexdigest())

if __name__ == "__main__":
    unittest.main()
