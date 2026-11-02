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

    def test_nfkc_equivalent_wine_member_paths_are_not_distinct(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/AQ_Wines/AQ_Wine01-B01_R01.txt", ROW),
                        ("Data/AQ_Wines/ＡＱ_Wine01-B01_R01.txt", ROW),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_DUPLICATE_MEMBER_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_fullwidth_ethanol_folder_cannot_hide_measurements(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ｅｔｈａｎｏｌ/Ea-C1_R01.txt", ROW),
                        ("Data/Ethanol/Ea-C2_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_ETHANOL_FOLDER_ALIAS"):
                inspect_zip(p, expected_rows=1)

    def test_case_variant_ethanol_folder_cannot_hide_measurements(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/ethanol/Ea-C1_R01.txt", ROW),
                        ("Data/Ethanol/Ea-C2_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_ETHANOL_FOLDER_ALIAS"):
                inspect_zip(p, expected_rows=1)

    def test_alias_directory_entry_rejected_before_data_count(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ｅｔｈａｎｏｌ/", b""),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_ETHANOL_FOLDER_ALIAS"):
                inspect_zip(p, expected_rows=1)

    def test_invisible_format_control_in_archive_path_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/AQ_Wines/" + chr(0x202e) + "Wine.txt", ROW),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_UNSAFE_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_valid_nfc_multilingual_wine_folder_is_preserved(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Café_Wines/wine.txt", ROW),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            result = inspect_zip(p, expected_rows=1)
            self.assertEqual(result["files_scanned"], 1)
            self.assertFalse(result["g2_qualified"])

    def test_corrupt_wine_member_crc_rejected_even_when_ethanol_valid(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            marker = b"wine-payload-for-crc-integrity-check"
            with zipfile.ZipFile(p, "w", compression=zipfile.ZIP_STORED) as zf:
                zf.writestr("Data/AQ_Wines/wine.txt", marker)
                zf.writestr("Data/Ethanol/Ea-C1_R01.txt", ROW)
            original = p.read_bytes()
            self.assertEqual(original.count(marker), 1)
            p.write_bytes(original.replace(marker, b"X" + marker[1:], 1))
            with self.assertRaisesRegex(ValueError, "MQ_EXT10_ARCHIVE_CORRUPT_OR_UNREADABLE"):
                inspect_zip(p, expected_rows=1)

    def test_directory_entry_with_payload_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/AQ_Wines/", b"unexpected-directory-payload"),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_DIRECTORY_NONEMPTY"):
                inspect_zip(p, expected_rows=1)

    def test_valid_non_ethanol_binary_member_does_not_change_ethanol_count(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/AQ_Wines/binary.bin", bytes(range(256))),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            result = inspect_zip(p, expected_rows=1)
            self.assertEqual(result["files_scanned"], 1)
            self.assertEqual(result["rows_scanned"], 1)
            self.assertFalse(result["g2_qualified"])

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


    def test_sixty_five_files_all_c1_is_not_complete_six_level_source(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            members = [(f"Ethanol/Ea-C1_R{i:02d}.txt",
                        f"50,25,1,2,3,4,5,{6+i}\n")
                       for i in range(1, 66)]
            archive(p, members)
            r = inspect_zip(p, expected_rows=1)
            self.assertEqual(r["files_scanned"], 65)
            self.assertTrue(r["reported_ethanol_count_complete"])
            self.assertFalse(r["all_six_published_liquid_levels_present"])
            self.assertEqual(r["concentration_code_counts"]["C1"], 65)
            self.assertEqual(r["concentration_code_counts"]["C6"], 0)
            self.assertEqual(r["source_status"], "REPORTED_COUNT_MISSING_CONCENTRATION_LEVELS")
            self.assertIn("MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS", r["blockers"])
            self.assertFalse(r["g2_qualified"])

    def test_six_levels_with_only_six_files_remains_partial(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [(f"Ethanol/Ea-C{i}_R01.txt", f"50,25,1,2,3,4,5,{6+i}\n")
                        for i in range(1, 7)])
            r = inspect_zip(p, expected_rows=1)
            self.assertTrue(r["all_six_published_liquid_levels_present"])
            self.assertFalse(r["reported_ethanol_count_complete"])
            self.assertIn("INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS", r["blockers"])
            self.assertNotIn("MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS", r["blockers"])
            self.assertEqual(r["source_status"], "PARTIAL_ARCHIVE_BYTES_SCANNED_UNQUALIFIED")

    def test_sixty_five_across_six_levels_still_not_scientifically_qualified(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [(f"Ethanol/Ea-C{(i % 6)+1}_R{(i // 6)+1:02d}.txt",
                         f"50,25,1,2,3,4,5,{6+i}\n")
                        for i in range(65)])
            r = inspect_zip(p, expected_rows=1)
            self.assertTrue(r["all_six_published_liquid_levels_present"])
            self.assertTrue(r["reported_ethanol_count_complete"])
            self.assertEqual(sum(r["concentration_code_counts"].values()), 65)
            self.assertEqual(r["source_status"], "ARCHIVE_BYTES_SCANNED_UNQUALIFIED")
            self.assertNotIn("INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS", r["blockers"])
            self.assertNotIn("MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS", r["blockers"])
            self.assertFalse(r["publisher_archive_sha256_verified"])
            self.assertFalse(r["g2_qualified"])
            self.assertFalse(r["g3_qualified"])

    def test_regular_file_cannot_be_parent_of_later_member(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ethanol", b"not-a-directory"),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_FILE_DIRECTORY_CONFLICT"):
                inspect_zip(p, expected_rows=1)

    def test_regular_file_cannot_replace_implicit_parent(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ethanol/Ea-C1_R01.txt", ROW),
                        ("Data/Ethanol", b"not-a-directory")])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_FILE_DIRECTORY_CONFLICT"):
                inspect_zip(p, expected_rows=1)

    def test_nfkc_fullwidth_slash_cannot_hide_ethanol_folder(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ｅｔｈａｎｏｌ／Ea-C1_R01.txt", ROW),
                        ("Data/Ethanol/Ea-C2_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_UNSAFE_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_nfkc_fullwidth_dotdot_cannot_hide_traversal(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/．．/wine.txt", ROW),
                        ("Data/Ethanol/Ea-C1_R01.txt", ROW)])
            with self.assertRaisesRegex(ValueError, "ARCHIVE_UNSAFE_PATH"):
                inspect_zip(p, expected_rows=1)

    def test_explicit_directory_and_implicit_parents_remain_valid(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.zip"
            archive(p, [("Data/Ethanol/Ea-C1_R01.txt", ROW),
                        ("Data/Ethanol/", b""),
                        ("Data/AQ_Wines/", b""),
                        ("Data/AQ_Wines/wine.txt", ROW)])
            r = inspect_zip(p, expected_rows=1)
            self.assertEqual(r["files_scanned"], 1)
            self.assertFalse(r["g2_qualified"])


if __name__ == "__main__":
    unittest.main()
