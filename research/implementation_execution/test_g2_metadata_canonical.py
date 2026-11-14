"""Deterministic G2 metadata rejection tests; no physical qualification implied."""
import importlib.util
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("g2_verify_raw_files.py")
spec = importlib.util.spec_from_file_location("g2_raw_preflight", MODULE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class MetadataCanonicalTests(unittest.TestCase):
    def test_control_characters_and_whitespace_rejected_for_every_field(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            raw = root / "raw.csv"
            raw.write_bytes(b"timestamp,mq\n1,2\n")
            baseline = dict(source_id="sample", raw_data_path="raw.csv",
                raw_sha256=hashlib.sha256(raw.read_bytes()).hexdigest(),
                measurement_date="2026-11-14", instrument_id="device-01",
                calibration_reference="cal-01", holdout_split_id="holdout-01")
            manifest = root / "manifest.json"
            for field in mod.FIELDS:
                for suffix in ("\n", "\t", " ", "\x7f"):
                    with self.subTest(field=field, suffix=repr(suffix)):
                        record = dict(baseline)
                        record[field] += suffix
                        manifest.write_text(json.dumps({"records":[record]}), encoding="utf-8")
                        self.assertTrue(mod.verify(manifest, root))

    def test_valid_metadata_passes_integrity_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            raw = root / "raw.csv"
            raw.write_bytes(b"sample\n")
            record = dict(source_id="sample", raw_data_path="raw.csv",
                raw_sha256=hashlib.sha256(raw.read_bytes()).hexdigest(),
                measurement_date="2026-11-14", instrument_id="device-01",
                calibration_reference="cal-01", holdout_split_id="holdout-01")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records":[record]}), encoding="utf-8")
            self.assertEqual(mod.verify(manifest, root), [])

if __name__ == "__main__":
    unittest.main()
