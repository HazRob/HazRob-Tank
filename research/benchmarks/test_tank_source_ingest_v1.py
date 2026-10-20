"""Synthetic regression fixtures only: NO genuine Tank experiment in these tests."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tank_source_ingest_v1 import ingest_bundle
from test_tank_measurement_adapter_v1 import fixture


class SourceLockedBundleTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "raw").mkdir()

    def _record(self, **kwargs):
        return fixture(**kwargs)

    def _eight(self, **kwargs):
        item = self._record(**kwargs)
        sensors = item["sensors"]
        names = ["MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135"]
        original = copy.deepcopy(sensors[0])
        for i, name in enumerate(names):
            s = copy.deepcopy(original)
            s["sensor_id"] = name
            s["sensor_instance_id"] = "tank-A:" + name + ":" + str(i)
            s["r0_calibration_id"] = "synthetic-r0-" + str(i)
            s["heater_phase"] = "LOW" if name in ("MQ7", "MQ9") else "CONSTANT"
            sensors[i:i+1] = [s] if i < len(sensors) else []
            if i >= len(sensors):
                sensors.append(s)
        # Assignment above ensures exactly 8 channels without claiming they exist.
        item["sensors"] = [
            {**copy.deepcopy(original),
             "sensor_id": name,
             "sensor_instance_id": "tank-A:" + name + ":" + str(i),
             "r0_calibration_id": "synthetic-r0-" + str(i),
             "heater_phase": "LOW" if name in ("MQ7", "MQ9") else "CONSTANT"}
            for i, name in enumerate(names)]
        return item

    def _bundle(self, entries, *, map_instances=False):
        data = {"schema_version": "tank-source-bundle-1", "exposures": []}
        if map_instances:
            data["expected_sensor_instances"] = {
                s["sensor_instance_id"]: s["sensor_id"] for s in entries[0]["sensors"]
            }
        for n, item in enumerate(entries):
            original = copy.deepcopy(item)
            original.pop("source_sha256", None)
            raw = json.dumps(original, sort_keys=True, ensure_ascii=False).encode()
            name = "raw/exposure-" + str(n) + ".json"
            (self.root / name).write_bytes(raw)
            sidecar = copy.deepcopy(item)
            sidecar["source_sha256"] = hashlib.sha256(raw).hexdigest()
            sidecar["source_artifact"] = name
            data["exposures"].append(sidecar)
        dest = self.root / "bundle.json"
        dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        return dest, data

    def _rewrite(self, manifest, data):
        manifest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")

    def test_integrity_partial_array_is_structural_only(self):
        path, _ = self._bundle([self._record()])
        result = ingest_bundle(path)
        self.assertEqual(result["raw_artifact_count_verified"], 1)
        self.assertFalse(result["candidate_data_structure_ready"])
        self.assertFalse(result["gas_standard_authenticity_verified"])
        self.assertFalse(result["tank_g3_scientifically_validated"])

    def test_candidate_eight_sensors_with_explicit_map(self):
        path, _ = self._bundle([self._eight()], map_instances=True)
        result = ingest_bundle(path, mode="candidate_g3_data")
        self.assertTrue(result["candidate_data_structure_ready"])
        self.assertEqual(result["cohort"]["valid_exposures"], 1)
        self.assertFalse(result["measurement_physics_verified"])

    def test_candidate_rejects_partial_sensor_array(self):
        path, _ = self._bundle([self._record()], map_instances=True)
        with self.assertRaises(ValueError):
            ingest_bundle(path, mode="candidate_g3_data")

    def test_candidate_needs_map(self):
        path, _ = self._bundle([self._eight()])
        with self.assertRaises(ValueError):
            ingest_bundle(path, mode="candidate_g3_data")

    def test_candidate_rejects_missing_environment(self):
        a = self._eight()
        a.pop("BME280")
        path, _ = self._bundle([a], map_instances=True)
        with self.assertRaises(ValueError):
            ingest_bundle(path, mode="candidate_g3_data")

    def test_candidate_rejects_unknown_heater_phase(self):
        a = self._eight()
        a["sensors"][0]["heater_phase"] = "UNKNOWN"
        path, _ = self._bundle([a], map_instances=True)
        with self.assertRaises(ValueError):
            ingest_bundle(path, mode="candidate_g3_data")

    def test_candidate_rejects_unknown_reference(self):
        a = self._eight()
        a.update({"reference_type":"unknown", "gas_label":"UNKNOWN",
                  "concentration_ppm":None})
        path, _ = self._bundle([a], map_instances=True)
        with self.assertRaises(ValueError):
            ingest_bundle(path, mode="candidate_g3_data")
        self.assertEqual(ingest_bundle(path)["cohort"]["valid_exposures"], 1)

    def test_digest_forgery_rejected(self):
        path, data = self._bundle([self._record()])
        data["exposures"][0]["source_sha256"] = "0"*64
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"SHA256"):
            ingest_bundle(path)

    def test_mutated_raw_bytes_detected(self):
        path, data = self._bundle([self._record()])
        source = self.root / data["exposures"][0]["source_artifact"]
        source.write_bytes(source.read_bytes() + b" ")
        with self.assertRaisesRegex(ValueError,"SHA256"):
            ingest_bundle(path)

    def test_forged_sidecar_even_with_valid_hash_detected(self):
        path, data = self._bundle([self._record()])
        data["exposures"][0]["gas_label"] = "methane"
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"sidecar fields"):
            ingest_bundle(path)

    def test_absolute_raw_path_rejected(self):
        path, data = self._bundle([self._record()])
        data["exposures"][0]["source_artifact"] = str(self.root / "raw/exposure-0.json")
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"relative raw"):
            ingest_bundle(path)

    def test_directory_traversal_rejected(self):
        path, data = self._bundle([self._record()])
        data["exposures"][0]["source_artifact"] = "raw/../../other.json"
        self._rewrite(path, data)
        with self.assertRaises(ValueError):
            ingest_bundle(path)

    def test_symlink_raw_file_rejected(self):
        path, data = self._bundle([self._record()])
        src = self.root / "raw/exposure-0.json"
        alias = self.root / "raw/alias.json"
        alias.symlink_to(src)
        data["exposures"][0]["source_artifact"] = "raw/alias.json"
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"symlinks"):
            ingest_bundle(path)

    def test_sensor_map_mismatch_rejected(self):
        path, data = self._bundle([self._eight()], map_instances=True)
        data["expected_sensor_instances"][next(iter(data["expected_sensor_instances"]))] = "OTHER"
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"mapping"):
            ingest_bundle(path)

    def test_duplicate_record_in_cohort_rejected(self):
        row = self._record()
        row2 = self._record(exposure_id="e2")
        path, _ = self._bundle([row, row2])
        with self.assertRaisesRegex(ValueError,"duplicate source record"):
            ingest_bundle(path)

    def test_duplicate_exposure_in_cohort_rejected(self):
        row = self._record()
        row2 = self._record(record_id="other")
        path, _ = self._bundle([row, row2])
        with self.assertRaisesRegex(ValueError,"duplicate exposure"):
            ingest_bundle(path)

    def test_duplicate_artifact_reference_rejected(self):
        a = self._record()
        path, data = self._bundle([a, self._record(record_id="second", exposure_id="ex2")])
        data["exposures"][1] = dict(data["exposures"][0])
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"masquerade"):
            ingest_bundle(path)

    def test_manifest_duplicate_json_key_rejected(self):
        path, _ = self._bundle([self._record()])
        original = path.read_text(encoding="utf-8")
        path.write_text('{"schema_version":"a","schema_version":"b",'+original[1:],
                        encoding="utf-8")
        with self.assertRaisesRegex(ValueError,"duplicate JSON"):
            ingest_bundle(path)

    def test_raw_duplicate_json_key_rejected(self):
        path, data = self._bundle([self._record()])
        raw_path = self.root / data["exposures"][0]["source_artifact"]
        payload = raw_path.read_bytes()
        payload = b'{"record_id":"fake",' + payload[1:]
        raw_path.write_bytes(payload)
        data["exposures"][0]["source_sha256"] = hashlib.sha256(payload).hexdigest()
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"duplicate JSON"):
            ingest_bundle(path)

    def test_nonfinite_json_rejected(self):
        path, data = self._bundle([self._record()])
        data["exposures"][0]["sensors"][0]["rs_ohm"][0] = float("nan")
        self._rewrite(path, data)
        with self.assertRaises(ValueError):
            ingest_bundle(path)

    def test_unsupported_mode_rejected(self):
        path, _ = self._bundle([self._record()])
        with self.assertRaisesRegex(ValueError,"mode"):
            ingest_bundle(path, mode="certified")

    def test_wrong_bundle_schema_rejected(self):
        path, data = self._bundle([self._record()])
        data["schema_version"] = "wrong"
        self._rewrite(path, data)
        with self.assertRaisesRegex(ValueError,"bundle schema"):
            ingest_bundle(path)

    def test_missing_raw_artifact_rejected(self):
        path, data = self._bundle([self._record()])
        (self.root / data["exposures"][0]["source_artifact"]).unlink()
        with self.assertRaises(ValueError):
            ingest_bundle(path)

    def test_independent_session_count_preserved(self):
        path, _ = self._bundle([
            self._record(),
            self._record(record_id="e2", session_id="day-2", exposure_id="ex2"),
        ])
        report = ingest_bundle(path)
        self.assertEqual(report["cohort"]["distinct_device_sessions"], 2)


if __name__ == "__main__":
    unittest.main()
