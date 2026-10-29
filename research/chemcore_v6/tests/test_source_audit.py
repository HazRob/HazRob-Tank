"""Executable regression tests for pinned source assets and scientific boundary cases.

These tests establish software integrity and numerical self-consistency, never
chemical recognition precision or physical concentration accuracy.
"""
import hashlib
import json
import math
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import source_audit as a


class SourceIntegrityTests(unittest.TestCase):
    def test_01_git_blob_formula(self):
        self.assertEqual(a.git_blob_sha1(b"hello\n"),
                         hashlib.sha1(b"blob 6\0hello\n").hexdigest())

    def test_02_all_13_gas_pinned_blobs(self):
        z = a.verify_source_snapshot()
        self.assertEqual(z["verified"], 13)
        self.assertEqual(len({v["git_blob_sha1"] for v in z["files"]}), 13)

    def test_03_modified_raw_source_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "sources"
            shutil.copytree(a.SNAPSHOTS, path)
            target = path / "mq_research_curves_43.json"
            target.write_bytes(target.read_bytes() + b"\n")
            with self.assertRaisesRegex(a.EvidenceError, "modified upstream"):
                a.verify_source_snapshot(root=path)

    def test_04_unregistered_source_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "sources"
            shutil.copytree(a.SNAPSHOTS, path)
            (path / "unknown.json").write_text("{}")
            with self.assertRaisesRegex(a.EvidenceError, "source-set mismatch"):
                a.verify_source_snapshot(root=path)


class CalibrationContractTests(unittest.TestCase):
    def test_05_circuit_rs_ohm(self):
        self.assertAlmostEqual(a.resistance_ohm(10000, 5.0, 2.0), 15000.0)

    def test_06_adc_invalid_input_rejected(self):
        for bad in (0.0, -1.0, 5.0, 6.0, float("nan"), float("inf")):
            with self.subTest(v=bad), self.assertRaises(a.EvidenceError):
                a.resistance_ohm(10000, 5.0, bad)

    def test_07_ohm_and_kohm_r0(self):
        self.assertAlmostEqual(a.r0_ohm_from_clean_air(98300, "MQ2"), 10000.0)
        self.assertAlmostEqual(a.r0_ohm_from_clean_air(700000, "MQ8"), 10000.0)

    def test_08_all_eight_r0_example_factors(self):
        self.assertEqual(set(a.CLEAN_AIR_FACTORS), set(a.SENSORS))
        self.assertEqual(len(a.CLEAN_AIR_FACTORS), 8)

    def test_09_source_r0_definition_match(self):
        value = a.normalized_ratio_for_source(
            15000, 10000, a.R0_FROM_CLEAN_AIR, a.R0_FROM_CLEAN_AIR)
        self.assertAlmostEqual(value, 1.5)

    def test_10_source_r0_definition_mismatch_rejects(self):
        with self.assertRaisesRegex(a.EvidenceError, "INCOMPATIBLE_R0_REFERENCE"):
            a.normalized_ratio_for_source(
                15000, 10000, a.R0_FROM_CLEAN_AIR, a.R0_FROM_REFERENCE_GAS)

    def test_11_missing_r0_definition_rejects(self):
        with self.assertRaises(a.EvidenceError):
            a.normalized_ratio_for_source(15000, 10000, "", a.R0_FROM_CLEAN_AIR)

    def test_12_direction_convention(self):
        self.assertAlmostEqual(a.ratio_rs_r0(15000, 10000, a.RATIO_FORWARD), 1.5)
        self.assertAlmostEqual(a.ratio_rs_r0(15000, 10000, a.RATIO_INVERSE), 2/3)
        self.assertAlmostEqual(a.normalized_ratio_for_source(15000,10000,
            a.R0_FROM_CLEAN_AIR,a.R0_FROM_CLEAN_AIR,
            a.RATIO_INVERSE,a.RATIO_FORWARD),1.5)

    def test_13_unknown_direction_rejects(self):
        with self.assertRaises(a.EvidenceError):
            a.ratio_rs_r0(100, 100, "unknown")

    def test_14_nan_zero_negative_r0_rejected(self):
        for value in (None, 0, -1, "foo", float("nan"), float("inf")):
            with self.subTest(v=value), self.assertRaises(a.EvidenceError):
                a.ratio_rs_r0(100, value)


class CurveAndFingerprintTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.curves = a.load_curves()

    def test_15_43_and_8_sensor_coverage(self):
        self.assertEqual(len(self.curves), 43)
        self.assertEqual(set(c.sensor for c in self.curves), set(a.SENSORS))

    def test_16_43_times_9_forward_inverse(self):
        z = a.validate_curve_roundtrips(self.curves)
        self.assertEqual(z["points_tested"], 43*9)
        self.assertLess(z["max_relative_roundtrip_error"], 1e-9)
        self.assertFalse(z["independent_ppm_accuracy_established"])

    def test_17_mq3_six_scale_fifty(self):
        selected = [c for c in self.curves if c.output_scale != 1]
        self.assertEqual(len(selected), 6)
        self.assertEqual(set(c.sensor for c in selected), {"MQ3"})
        self.assertTrue(all(c.output_scale == 50 for c in selected))
        for curve in selected:
            value = math.sqrt(curve.ppm_min * curve.ppm_max)
            self.assertAlmostEqual(curve.inverse(curve.forward(value)), value)

    def test_18_curve_domain_enforced(self):
        x = self.curves[0]
        with self.assertRaisesRegex(a.EvidenceError, "OUTSIDE_SOURCE_DOMAIN"):
            x.forward(x.ppm_min/2)
        with self.assertRaises(a.EvidenceError):
            x.inverse(-1)

    def test_19_curve_params_not_validated_on_tank(self):
        self.assertTrue(all(c.research_only for c in self.curves))

    def test_20_r0_counterfactuals_are_synthetic(self):
        cases = a.r0_sensitivity_simulation(self.curves)
        self.assertEqual(len(cases), 43*4)
        self.assertTrue(all(z["device_validated"] is False for z in cases))
        self.assertTrue(all(z["truth_kind"].startswith("SYNTHETIC_") for z in cases))
        self.assertTrue(all(z["fitted_ppm"] is None
                            for z in cases if z["state"] != "IN_DOMAIN"))

    def test_21_original_four_class_overlap(self):
        z = a.multi_class_fingerprint_audit()
        self.assertEqual(z["source_adc_rows"], 6400)
        self.assertIsNone(z["physical_exposure_count"])
        self.assertEqual(z["class_count"], 4)
        self.assertEqual(z["missing_channels"], ["MQ4", "MQ9"])
        self.assertFalse(z["pure_gas_identification"])

    def test_22_missing_fingerprint_channel_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            f = "multimodal_mq_transfer_fingerprints_v1.json"
            j = json.loads((a.SNAPSHOTS / f).read_text())
            i = j["hr069_sensor_order"].index("MQ4")
            j["records"][0]["log10_cross_sensor_geomean_shape_hr069_order"][i] = 0.1
            (d / f).write_text(json.dumps(j))
            with self.assertRaisesRegex(a.EvidenceError, "fabricated"):
                a.multi_class_fingerprint_audit(d)

    def test_23_evidence_counts_not_physical_claims(self):
        z = a.audit()
        self.assertEqual(z["source_integrity"]["verified"], 13)
        self.assertEqual(z["derived_source_fingerprint_families"], 13)
        self.assertEqual(z["UCI1081_derived_mq135_gas_level_records"], 18)
        self.assertTrue(z["no_real_Tank_gas_data_used"])
        self.assertIs(z["M4_or_safety_certification"], False)


if __name__ == "__main__":
    unittest.main()
