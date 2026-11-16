#!/usr/bin/env python3
"""Reproducible regression: control characters in G2 raw paths are rejected."""
import hashlib
import json
import pathlib
import tempfile
import unittest
from g2_verify_raw_files import verify


class RawPathControlRegression(unittest.TestCase):
    def test_control_characters_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            payload = b"independent-fixture-not-scientific-evidence"
            (root / "sample.bin").write_bytes(payload)
            base = {
                "source_id": "fixture-1",
                "raw_data_path": "sample.bin",
                "raw_sha256": hashlib.sha256(payload).hexdigest(),
                "measurement_date": "2026-11-14",
                "instrument_id": "fixture-device",
                "calibration_reference": "fixture-only",
                "holdout_split_id": "fixture-only",
            }
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [base]}), encoding="utf-8")
            self.assertEqual(verify(manifest, root), [])
            for bad in ("sample.bin\x00", "sample.bin\n", "sample.bin\r", "sample.bin\t", "sample.bin\x7f"):
                with self.subTest(path=repr(bad)):
                    record = dict(base, raw_data_path=bad)
                    manifest.write_text(json.dumps({"records": [record]}), encoding="utf-8")
                    self.assertTrue(any("control character in raw path" in e for e in verify(manifest, root)))


if __name__ == "__main__":
    unittest.main()
