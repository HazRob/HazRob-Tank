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

    def test_nonstandard_json_numeric_constants_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            for constant in ("NaN", "Infinity", "-Infinity"):
                with self.subTest(constant=constant):
                    manifest.write_text('{"records":[{"extra":' + constant + '}]}', encoding="utf-8")
                    errors = mod.verify(manifest, root)
                    self.assertTrue(errors)
                    self.assertIn("invalid", errors[0])

    def test_json_exponent_overflow_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            for literal in ("1e999", "-1e999", "9.9e999"):
                with self.subTest(literal=literal):
                    manifest.write_text('{"records":[{"extra":' + literal + '}]}', encoding="utf-8")
                    errors = mod.verify(manifest, root)
                    self.assertTrue(errors)
                    self.assertIn("nonfinite JSON float", errors[0])

    def test_duplicate_json_keys_rejected_at_all_depths(self):
        # JSON object-pair parsing must reject ambiguous provenance, even in
        # unknown nested metadata, before evaluating physical evidence.
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            cases = (
                '{"records":[],"records":[]}',
                '{"records":[{"source_id":"a","source_id":"b"}]}',
                '{"records":[{"extra":{"instrument":"a","instrument":"b"}}]}',
            )
            for payload in cases:
                with self.subTest(payload=payload):
                    manifest.write_text(payload, encoding="utf-8")
                    errors = mod.verify(manifest, root)
                    self.assertTrue(errors)
                    self.assertIn("duplicate JSON key", errors[0])

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

    def test_raw_byte_tampering_and_empty_file_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            raw = root / "raw.csv"
            original = b"sample,adc\\n1,128\\n"
            raw.write_bytes(original)
            record = dict(source_id="sample", raw_data_path="raw.csv",
                raw_sha256=hashlib.sha256(original).hexdigest(),
                measurement_date="2026-11-14", instrument_id="device-01",
                calibration_reference="cal-01", holdout_split_id="holdout-01")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [record]}), encoding="utf-8")
            self.assertEqual(mod.verify(manifest, root), [])
            raw.write_bytes(original.replace(b"128", b"129"))
            self.assertTrue(any("SHA-256 mismatch" in e for e in mod.verify(manifest, root)))
            raw.write_bytes(b"")
            self.assertTrue(any("empty raw file" in e for e in mod.verify(manifest, root)))

    def test_parent_traversal_and_absolute_paths_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            raw = root / "raw.csv"
            raw.write_bytes(b"sample\\n")
            baseline = dict(source_id="sample", raw_data_path="raw.csv",
                raw_sha256=hashlib.sha256(raw.read_bytes()).hexdigest(),
                measurement_date="2026-11-14", instrument_id="device-01",
                calibration_reference="cal-01", holdout_split_id="holdout-01")
            manifest = root / "manifest.json"
            for unsafe in ("../raw.csv", "/tmp/raw.csv", r"..\\raw.csv"):
                with self.subTest(path=unsafe):
                    record = dict(baseline, raw_data_path=unsafe)
                    manifest.write_text(json.dumps({"records": [record]}), encoding="utf-8")
                    self.assertTrue(any("unsafe path" in e for e in mod.verify(manifest, root)))


    def test_symlink_escape_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            evidence = root / "evidence"
            evidence.mkdir()
            outside = root / "outside.csv"
            outside.write_bytes(b"sample,adc\\n1,42\\n")
            (evidence / "linked.csv").symlink_to(outside)
            record = dict(source_id="symlink-case", raw_data_path="linked.csv",
                raw_sha256=hashlib.sha256(outside.read_bytes()).hexdigest(),
                measurement_date="2026-11-15", instrument_id="device-01",
                calibration_reference="cal-01", holdout_split_id="holdout-01")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [record]}), encoding="utf-8")
            self.assertTrue(any("outside root" in e for e in mod.verify(manifest, evidence)))

    def test_duplicate_provenance_and_invalid_calendar_date_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            raw_a, raw_b = root / "a.csv", root / "b.csv"
            raw_a.write_bytes(b"sample,adc\\n1,10\\n")
            raw_b.write_bytes(b"sample,adc\\n1,11\\n")
            def make_record(source, raw):
                return dict(source_id=source, raw_data_path=raw.name,
                    raw_sha256=hashlib.sha256(raw.read_bytes()).hexdigest(),
                    measurement_date="2026-11-15", instrument_id="device-01",
                    calibration_reference="cal-01", holdout_split_id="holdout-01")
            a, b = make_record("a", raw_a), make_record("b", raw_b)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [a, b]}), encoding="utf-8")
            self.assertEqual(mod.verify(manifest, root), [])
            for records, expected in (
                ([a, dict(b, source_id="a")], "duplicate source_id"),
                ([a, dict(b, raw_sha256=a["raw_sha256"])], "duplicate raw SHA-256"),
                ([a, dict(b, raw_data_path="a.csv")], "duplicate raw file path"),
                ([dict(a, measurement_date="2026-02-30")], "invalid measurement_date"),
            ):
                with self.subTest(expected=expected):
                    manifest.write_text(json.dumps({"records": records}), encoding="utf-8")
                    self.assertTrue(any(expected in e for e in mod.verify(manifest, root)))

if __name__ == "__main__":
    unittest.main()
