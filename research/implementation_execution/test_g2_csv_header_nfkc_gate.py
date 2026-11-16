"""Synthetic fail-closed tests for G2 evidence CSV header identity.

These fixtures are not experimental gas data and cannot establish G2/G3.
"""
import csv
import tempfile
import unittest
from pathlib import Path

from g2_cross_artifact_gate import _read_csv


class CSVHeaderCompatibilityIdentityTests(unittest.TestCase):
    def _parse(self, header, rows=(("e1", "train"),)):
        with tempfile.TemporaryDirectory() as temp:
            base = Path(temp)
            with (base / "group_manifest.csv").open("w", encoding="utf-8", newline="") as stream:
                writer = csv.writer(stream)
                writer.writerow(header)
                writer.writerows(rows)
            return _read_csv(base, "group_manifest.csv")

    def test_fullwidth_exposure_header_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "CSV_HEADER_AMBIGUOUS"):
            self._parse(["ｅxposure_id", "group_id"])

    def test_compatibility_alias_next_to_canonical_header_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "CSV_HEADER_AMBIGUOUS"):
            self._parse(["exposure_id", "ｅxposure_id", "group_id"],
                        rows=(("e1", "e2", "train"),))

    def test_compatibility_alias_of_physical_device_header_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "CSV_HEADER_AMBIGUOUS"):
            self._parse(["device_id", "ｄevice_id", "group_id"],
                        rows=(("d1", "d2", "train"),))

    def test_canonical_ascii_schema_is_preserved(self):
        rows = self._parse(["exposure_id", "group_id"])
        self.assertEqual(rows, [{"exposure_id": "e1", "group_id": "train"}])

    def test_canonical_multilingual_extra_column_is_preserved(self):
        rows = self._parse(["exposure_id", "موقع"],
                           rows=(("e1", "صنعاء"),))
        self.assertEqual(rows[0]["موقع"], "صنعاء")

    def test_decomposed_header_still_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "CSV_HEADER_AMBIGUOUS"):
            self._parse(["exposure_id", "cafe\u0301"])


if __name__ == "__main__":
    unittest.main()
