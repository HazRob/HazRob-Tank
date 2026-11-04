#!/usr/bin/env python3
"""Fail-closed G2 physical evidence manifest preflight. Does NOT confer G2 approval.
Usage: python g2_physical_intake_gate.py manifest.json
       python g2_physical_intake_gate.py --self-test
"""
import hashlib
import json
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
        part = row.get("partition")
        if part not in PARTITIONS:
            errors.append(prefix + ":invalid_partition")
        if row.get("concentration_unit") not in UNITS:
            errors.append(prefix + ":unsupported_concentration_unit")
        for key in ("session_id", "exposure_id", "device_id"):
            value = row.get(key)
            if value and part in PARTITIONS:
                identity = (key, str(value))
                prior = seen_group.setdefault(identity, part)
                if prior != part:
                    errors.append(prefix + ":partition_leak_" + key)
        raw = row.get("raw_path")
        if not isinstance(raw, str):
            continue
        p = (root / raw).resolve()
        if not p.is_relative_to(root) or not p.is_file() or p.is_symlink():
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
