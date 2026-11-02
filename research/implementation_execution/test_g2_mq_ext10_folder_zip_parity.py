"""Synthetic, deterministic cross-intake tests; never physical MQ evidence."""
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from g2_mq_ext10_folder_zip_parity import run


class MQExt10FolderZipParityTests(unittest.TestCase):
    def test_no_raw_is_explicitly_unqualified_and_reproducible(self):
        a = run()
        b = run()
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertEqual(a["source_status"], "PAIRED_ORIGINAL_RAW_NOT_AVAILABLE")
        self.assertEqual(a["samples_compared"], 0)
        self.assertFalse(a["g2_qualified"])
        self.assertFalse(a["g3_qualified"])

    def test_requires_both_intake_inputs(self):
        with self.assertRaisesRegex(ValueError, "PARITY_BOTH_FOLDER_AND_ZIP_REQUIRED"):
            run(raw_root=Path("/missing"))
        with self.assertRaisesRegex(ValueError, "PARITY_BOTH_FOLDER_AND_ZIP_REQUIRED"):
            run(raw_zip=Path("/missing"))

    def _make_pair(self, root, zip_values=None, zip_names=None, include_wine=False):
        folder = root / "folder"
        folder.mkdir()
        samples = {
            "Ea-C1_R01.txt": ("50,25,1,2,3,4,5,6\n" * 3330).encode(),
            "Ea-C2_R01.txt": ("51,26,2,3,4,5,6,7\n" * 3330).encode(),
        }
        for name, raw in samples.items():
            (folder / name).write_bytes(raw)
        archive = root / "original.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as zf:
            for name, raw in samples.items():
                if zip_names is not None and name not in zip_names:
                    continue
                zf.writestr("Ethanol/" + name,
                            zip_values.get(name, raw) if zip_values else raw)
            if include_wine:
                zf.writestr("Wine/Wine-C1_R01.txt", b"not an ethanol file")
        return folder, archive

    def test_same_bytes_two_paths_match_but_do_not_qualify(self):
        with tempfile.TemporaryDirectory() as d:
            folder, archive = self._make_pair(Path(d), include_wine=True)
            r = run(folder, archive)
            self.assertEqual(r["source_status"], "PAIRED_SOURCE_BYTES_MATCH_UNQUALIFIED")
            self.assertEqual(r["samples_compared"], 2)
            self.assertEqual(r["folder_manifest_sha256"], r["zip_manifest_sha256"])
            self.assertEqual(len(r["folder_manifest_sha256"]), 64)
            self.assertFalse(r["reported_ethanol_count_complete"])
            self.assertFalse(r["all_six_published_liquid_levels_present"])
            self.assertIn("INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS", r["blockers"])
            self.assertIn("MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS", r["blockers"])
            self.assertFalse(r["headspace_ethanol_ppm_measured"])
            self.assertFalse(r["g2_qualified"])
            self.assertFalse(r["g3_qualified"])

    def test_mismatched_sample_membership_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            folder, archive = self._make_pair(Path(d), zip_names={"Ea-C1_R01.txt"})
            with self.assertRaisesRegex(ValueError, "PARITY_SAMPLE_MEMBERSHIP_MISMATCH"):
                run(folder, archive)

    def test_changed_sample_bytes_are_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            changed = ("51,26,2,3,4,5,6,8\n" * 3330).encode()
            folder, archive = self._make_pair(
                Path(d), zip_values={"Ea-C2_R01.txt": changed})
            with self.assertRaisesRegex(ValueError, "PARITY_SOURCE_BYTES_MISMATCH"):
                run(folder, archive)

    def test_numeric_equivalent_but_byte_different_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            changed = ("5e1;25.0;1.00;2;3;4;5;6\n" * 3330).encode()
            folder, archive = self._make_pair(
                Path(d), zip_values={"Ea-C1_R01.txt": changed})
            with self.assertRaisesRegex(ValueError, "PARITY_SOURCE_BYTES_MISMATCH"):
                run(folder, archive)

    def test_paired_manifest_digest_is_deterministic(self):
        with tempfile.TemporaryDirectory() as d:
            folder, archive = self._make_pair(Path(d))
            a = run(folder, archive)
            b = run(folder, archive)
            self.assertEqual(a, b)


if __name__ == "__main__":
    unittest.main()
