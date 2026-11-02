"""Synthetic ZIP adversarial tests; not external source measurements."""
import hashlib
import stat
import tempfile
import unittest
import zipfile
from pathlib import Path

from g2_mq_ext10_archive_intake import inspect_zip, run

ROW = "50,25,1,2,3,4,5,6\n"


def archive(path, members):
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for name, content in members:
            zf.writestr(name, content)


class MQExt10ArchiveIntakeTests(unittest.TestCase):
    def test_absent_source_never_qualifies(self):
        r = run()
        self.assertEqual(r["files_scanned"], 0)
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["publisher_archive_sha256_verified"])

    def test_valid_synthetic_ethanol_and_wine(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ethanol/Ea-C1_R01.txt", ROW * 2),
                        ("Data/AQ_Wines/AQ_Wine01-B01_R01.txt", ROW * 2)])
            r = inspect_zip(p, expected_rows=2)
            self.assertEqual(r["files_scanned"], 1)
            self.assertEqual(r["rows_scanned"], 2)
            self.assertEqual(r["archive_sha256_computed"], hashlib.sha256(p.read_bytes()).hexdigest())
            self.assertFalse(r["g2_qualified"])
            self.assertIn("INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS", r["blockers"])

    def test_no_ethanol_folder_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("AQ_Wines/file.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_NO_ETHANOL_FILES"):
                inspect_zip(p, expected_rows=1)

    def test_zip_slip_rejected_even_in_wine_folder(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/../evil.txt", ROW), ("Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_UNSAFE_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_windows_path_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [(r"Ethanol\Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_UNSAFE_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_nested_ethanol_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/sub/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_NESTED_ETHANOL_SAMPLE"):
                inspect_zip(p, expected_rows=1)

    def test_duplicate_member_path_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/Ea-C1_R01.txt", ROW), ("Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_DUPLICATE_MEMBER_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_numeric_duplicate_under_different_text(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/Ea-C1_R01.txt", ROW),
                        ("Ethanol/Ea-C2_R01.txt", "5e1;25;1.0;2;3;4;5;6\n")])
            with self.assertRaisesRegex(ValueError, "DUPLICATE_MEASUREMENT_NUMERIC_TRACE"):
                inspect_zip(p, expected_rows=1)

    def test_duplicate_sample_id_with_different_bytes(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/Ea-C1_R01.txt", ROW),
                        ("Ethanol/EaC1R01.csv", "51,25,1,2,3,4,5,6\n")])
            with self.assertRaisesRegex(ValueError, "DUPLICATE_CONCENTRATION_REPETITION"):
                inspect_zip(p, expected_rows=1)

    def test_symlink_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            with zipfile.ZipFile(p, "w") as zf:
                link = zipfile.ZipInfo("Ethanol/Ea-C1_R01.txt")
                link.create_system = 3
                link.external_attr = (stat.S_IFLNK | 0o777) << 16
                zf.writestr(link, "../outside")
            with self.assertRaisesRegex(ValueError, "ARCHIVE_SYMLINK_REJECTED"):
                inspect_zip(p, expected_rows=1)

    def test_corrupt_archive_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            p.write_bytes(b"not a zip")
            with self.assertRaisesRegex(ValueError, "ARCHIVE_CORRUPT_OR_UNREADABLE"):
                inspect_zip(p, expected_rows=1)

    def test_bad_source_row_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/Ea-C1_R01.txt", "50,25,1,2,3,4,5,nan\n")])
            with self.assertRaisesRegex(ValueError, "NONFINITE_ROW"):
                inspect_zip(p, expected_rows=1)

    def test_full_source_not_implied_by_partial_archive(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Ethanol/Ea-C1_R01.txt", ROW)])
            r = inspect_zip(p, expected_rows=1)
            self.assertFalse(r["publisher_archive_sha256_verified"])
            self.assertFalse(r["independent_tank_exposures"])
            self.assertFalse(r["headspace_ethanol_ppm_measured"])
            self.assertFalse(r["g3_qualified"])


if __name__ == "__main__":
    unittest.main()
