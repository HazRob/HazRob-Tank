#!/usr/bin/env python3
"""Fail-closed G2 physical evidence manifest preflight. Does NOT confer G2 approval.
Usage: python g2_physical_intake_gate.py manifest.json
       python g2_physical_intake_gate.py --self-test
"""
import hashlib
import json
import math
import pathlib
import sys
import unittest

REQUIRED = ("source_id", "raw_path", "sha256", "session_id", "device_id",
            "day_id", "site_id", "partition", "analyte", "concentration",
            "concentration_unit", "reference_standard", "reference_uncertainty",
            "sensor_model", "firmware_revision", "adc_reference_v",
            "heater_voltage_v", "load_resistance_ohm", "temperature_c",
            "humidity_percent", "sampling_interval_s", "exposure_id")
PARTITIONS = {"train", "validation", "test"}
UNITS = {"ppm_vv", "ppb_vv", "mg_m3"}

def audit(manifest, root):
    errors = []
    if not isinstance(manifest, dict):
        return ["manifest_not_object"]
    records = manifest.get("records")
    if not isinstance(records, list) or not records:
        return ["records_missing"]
    seen_bytes = {}
    seen_group = {}
    seen_path = set()
    root = pathlib.Path(root).resolve()
    for i, row in enumerate(records):
        prefix = f"records[{i}]"
        if not isinstance(row, dict):
            errors.append(prefix + ":not_object")
            continue
        for key in REQUIRED:
            if key not in row or row[key] is None or row[key] == "" or row[key] == "UNKNOWN":
                errors.append(prefix + ":missing_" + key)
        for key in ("concentration", "reference_uncertainty", "adc_reference_v",
                    "heater_voltage_v", "load_resistance_ohm", "temperature_c",
                    "humidity_percent", "sampling_interval_s"):
            value = row.get(key)
            if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
                errors.append(prefix + ":invalid_numeric_" + key)
            elif key in ("concentration", "reference_uncertainty") and value < 0:
                errors.append(prefix + ":negative_" + key)
            elif key in ("adc_reference_v", "heater_voltage_v", "load_resistance_ohm", "sampling_interval_s") and value <= 0:
                errors.append(prefix + ":nonpositive_" + key)
            elif key == "humidity_percent" and not 0 <= value <= 100:
                errors.append(prefix + ":humidity_out_of_range")
        part = row.get("partition")
        if not isinstance(part, str) or part not in PARTITIONS:
            errors.append(prefix + ":invalid_partition")
        if not isinstance(row.get("concentration_unit"), str) or row.get("concentration_unit") not in UNITS:
            errors.append(prefix + ":unsupported_concentration_unit")
        for key in ("session_id", "exposure_id", "device_id"):
            value = row.get(key)
            if isinstance(value, (str, int)) and not isinstance(value, bool) and value and isinstance(part, str) and part in PARTITIONS:
                identity = (key, str(value))
                prior = seen_group.setdefault(identity, part)
                if prior != part:
                    errors.append(prefix + ":partition_leak_" + key)
        raw = row.get("raw_path")
        if not isinstance(raw, str):
            continue
        raw_rel = pathlib.Path(raw)
        if raw_rel.is_absolute() or ".." in raw_rel.parts:
            errors.append(prefix + ":raw_path_invalid")
            continue
        p_unresolved = root / raw_rel
        if any(segment.is_symlink() for segment in (p_unresolved, *p_unresolved.parents) if segment != root and segment.is_relative_to(root)):
            errors.append(prefix + ":raw_path_symlink")
            continue
        p = p_unresolved.resolve()
        if not p.is_relative_to(root) or not p.is_file():
            errors.append(prefix + ":raw_path_invalid")
            continue
        if p in seen_path:
            errors.append(prefix + ":duplicate_path")
        seen_path.add(p)
        digest = hashlib.sha256(p.read_bytes()).hexdigest()
        if digest != row.get("sha256"):
            errors.append(prefix + ":sha256_mismatch")
        if digest in seen_bytes:
            errors.append(prefix + ":duplicate_raw_bytes")
            if seen_bytes[digest] != part:
                errors.append(prefix + ":duplicate_across_partitions")
        else:
            seen_bytes[digest] = part
    if not any(isinstance(r, dict) and r.get("partition") == "test" for r in records):
        errors.append("independent_test_partition_missing")
    return errors

class GateTests(unittest.TestCase):
    def test_non_object_manifest(self):
        self.assertEqual(audit([], "."), ["manifest_not_object"])

    def test_invalid_numeric_rejected(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            p = pathlib.Path(d) / "raw.csv"
            p.write_text("adc\\n123\\n")
            row = dict.fromkeys(REQUIRED, "measured")
            row.update(raw_path="raw.csv", sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                       partition="test", concentration_unit="ppm_vv",
                       concentration=float("nan"), reference_uncertainty=-1,
                       humidity_percent=110)
            errors = audit({"records": [row]}, d)
            self.assertTrue(any("invalid_numeric_concentration" in x for x in errors))
            self.assertTrue(any("negative_reference_uncertainty" in x for x in errors))
            self.assertTrue(any("humidity_out_of_range" in x for x in errors))

    def test_symlink_parent_rejected(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            (root / "actual").mkdir()
            p = root / "actual" / "raw.csv"
            p.write_text("adc\\n123\\n")
            (root / "alias").symlink_to(root / "actual", target_is_directory=True)
            row = dict.fromkeys(REQUIRED, "measured")
            row.update(raw_path="alias/raw.csv", sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                       partition="test", concentration_unit="ppm_vv")
            self.assertTrue(any("raw_path_symlink" in x for x in audit({"records": [row]}, d)))

    def test_valid_distinct_partition_records_preflight_only(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            rows = []
            for i, part in enumerate(("train", "validation", "test")):
                p = pathlib.Path(d) / f"sample_{i}.csv"
                p.write_text(f"adc\\n{i}\\n")
                row = dict.fromkeys(REQUIRED, "measured")
                row.update(raw_path=p.name, sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                           partition=part, concentration_unit="ppm_vv",
                           session_id=f"session_{i}", exposure_id=f"exposure_{i}",
                           device_id=f"device_{i}", concentration=10.0,
                           reference_uncertainty=0.1, adc_reference_v=3.3,
                           heater_voltage_v=5.0, load_resistance_ohm=10000.0,
                           temperature_c=25.0, humidity_percent=50.0,
                           sampling_interval_s=1.0)
                rows.append(row)
            self.assertEqual(audit({"records": rows}, d), [])

    def test_nonfinite_and_boolean_numeric_fields_rejected(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            p = pathlib.Path(d) / "raw.csv"
            p.write_text("adc\\n1\\n")
            row = dict.fromkeys(REQUIRED, "measured")
            row.update(raw_path=p.name, sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                       partition="test", concentration_unit="ppm_vv",
                       concentration=True, reference_uncertainty=float("inf"),
                       adc_reference_v=0, heater_voltage_v=5.0,
                       load_resistance_ohm=-1, temperature_c=25.0,
                       humidity_percent=-1, sampling_interval_s=0)
            errors = audit({"records": [row]}, d)
            for expected in ("invalid_numeric_concentration",
                             "invalid_numeric_reference_uncertainty",
                             "nonpositive_adc_reference_v",
                             "nonpositive_load_resistance_ohm",
                             "humidity_out_of_range",
                             "nonpositive_sampling_interval_s"):
                self.assertTrue(any(expected in error for error in errors), expected)

    def test_malformed_partition_and_unit_fail_closed(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            p = pathlib.Path(d) / "raw.csv"
            p.write_text("adc\\n1\\n")
            row = dict.fromkeys(REQUIRED, "measured")
            row.update(raw_path=p.name, sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                       partition=["test"], concentration_unit={"unit": "ppm_vv"},
                       session_id=["unhashable"], exposure_id={"id": "bad"},
                       concentration=1.0, reference_uncertainty=0.1,
                       adc_reference_v=3.3, heater_voltage_v=5.0,
                       load_resistance_ohm=10000.0, temperature_c=25.0,
                       humidity_percent=50.0, sampling_interval_s=1.0)
            issues = audit({"records": [row]}, d)
            self.assertTrue(any("invalid_partition" in x for x in issues))
            self.assertTrue(any("unsupported_concentration_unit" in x for x in issues))

    def test_missing_records(self):
        self.assertEqual(audit({}, "."), ["records_missing"])
    def test_unknown_not_accepted(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            p = pathlib.Path(d) / "raw.csv"
            p.write_text("adc\n123\n")
            row = dict.fromkeys(REQUIRED, "UNKNOWN")
            row.update(raw_path="raw.csv", sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                       partition="test", concentration_unit="ppm_vv")
            self.assertTrue(any("missing_" in x for x in audit({"records": [row]}, d)))
    def test_cross_partition_rejected(self):
        import tempfile
        with tempfile.TemporaryDirectory() as d:
            rows = []
            for i, part in enumerate(("train", "test")):
                p = pathlib.Path(d) / f"{i}.csv"
                p.write_text(f"adc\n{i}\n")
                row = dict.fromkeys(REQUIRED, "measured")
                row.update(raw_path=p.name, sha256=hashlib.sha256(p.read_bytes()).hexdigest(),
                           partition=part, concentration_unit="ppm_vv",
                           session_id="same_session", exposure_id="same_exposure")
                rows.append(row)
            self.assertTrue(any("partition_leak" in x for x in audit({"records": rows}, d)))

if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "--self-test":
        sys.argv = [sys.argv[0]]
        unittest.main()
    elif len(sys.argv) == 2:
        path = pathlib.Path(sys.argv[1])
        issues = audit(json.loads(path.read_text(encoding="utf-8")), path.parent)
        print(json.dumps({"gate": "BLOCKED" if issues else "PREFLIGHT_ONLY",
                          "issues": issues}, indent=2))
        sys.exit(bool(issues))
    else:
        sys.exit("usage: g2_physical_intake_gate.py manifest.json | --self-test")
