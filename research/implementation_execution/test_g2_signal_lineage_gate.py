"""Negative controls and positive metadata controls; synthetic, not G2 evidence."""
import unittest
from g2_signal_lineage_gate import audit_signal_lineage

SHA = "a" * 64

def base():
    return {"signal_processing": {"sensor_ids": ["MQ135#unit-001"],
            "source_signal_units": "ADC_COUNTS", "derived_signal_units": "ADC_COUNTS",
            "feature_formula": "MEDIAN_RESPONSE_MINUS_MEDIAN_BASELINE",
            "baseline_window_s": [180, 300], "response_window_s": [780, 900],
            "r0_mode": "NOT_USED", "r0_subtracted_from_reading": False}}

def normalized():
    x = base()
    x["signal_processing"].update({"source_signal_units": "OHM", "derived_signal_units": "DIMENSIONLESS",
        "feature_formula": "RS_OVER_R0", "r0_mode": "MEASURED_REFERENCE",
        "r0_reference": {"r0_ohm": 4500.0, "sensor_instance_id": "MQ135#unit-001",
            "heater_phase": "CONSTANT", "reference_id": "std-01", "raw_sha256": SHA,
            "source_bytes_verified": True, "reference_kind": "MEASURED_GAS_STANDARD",
            "reference_concentration_ppm": 100.0, "reference_gas": "NH3"}})
    return x

class LineageGateTests(unittest.TestCase):
    def test_delta_not_r0(self):
        self.assertEqual(audit_signal_lineage(base()), [])
    def test_real_uci_unverified_source_units_blocks_g2(self):
        x=base();x["signal_processing"].update(source_signal_units="SOURCE_UNIT_UNVERIFIED", derived_signal_units="SOURCE_UNIT_UNVERIFIED")
        self.assertIn("SOURCE_SIGNAL_UNITS_UNVERIFIED_FOR_G2",audit_signal_lineage(x))
    def test_r0_subtraction_from_adc_rejected(self):
        x=base();x["signal_processing"]["r0_subtracted_from_adc"]=True
        self.assertIn("INVALID_R0_SUBTRACTION_AS_MQ_NORMALIZATION",audit_signal_lineage(x))
    def test_baseline_is_not_r0(self):
        x=base();x["signal_processing"]["r0_mode"]="MEASURED_REFERENCE"
        self.assertIn("BASELINE_DELTA_MISLABELLED_AS_R0_NORMALIZATION",audit_signal_lineage(x))
    def test_response_window_overlap(self):
        x=base();x["signal_processing"]["response_window_s"]=[290, 420]
        self.assertIn("BASELINE_RESPONSE_WINDOWS_OVERLAP",audit_signal_lineage(x))
    def test_malformed_window(self):
        x=base();x["signal_processing"]["baseline_window_s"]=[200, 100]
        self.assertIn("MISSING_OR_INVALID_BASELINE_RESPONSE_WINDOWS",audit_signal_lineage(x))
    def test_baseline_preserves_units(self):
        x=base();x["signal_processing"]["derived_signal_units"]="VOLT"
        self.assertIn("BASELINE_SUBTRACTION_MUST_PRESERVE_UNITS",audit_signal_lineage(x))
    def test_normalized_reference_structurally_complete(self):
        self.assertEqual(audit_signal_lineage(normalized()), [])
    def test_normalized_requires_sensor_bound_r0(self):
        x=normalized();x["signal_processing"]["r0_reference"]["sensor_instance_id"]="MQ7"
        self.assertIn("R0_SENSOR_INSTANCE_MISMATCH",audit_signal_lineage(x))
    def test_r0_prior_not_measured_calibration(self):
        x=normalized();x["signal_processing"]["r0_mode"]="MANUFACTURER_CURVE_PRIOR"
        self.assertIn("MANUFACTURER_R0_PRIOR_NOT_PHYSICAL_G2_CALIBRATION",audit_signal_lineage(x))
    def test_unverified_r0_bytes(self):
        x=normalized();x["signal_processing"]["r0_reference"]["source_bytes_verified"]=False
        self.assertIn("R0_SOURCE_BYTES_NOT_VERIFIED",audit_signal_lineage(x))
    def test_missing_r0(self):
        x=normalized();del x["signal_processing"]["r0_reference"]
        self.assertIn("MISSING_SENSOR_BOUND_R0_REFERENCE",audit_signal_lineage(x))
    def test_invalid_r0_nan(self):
        x=normalized();x["signal_processing"]["r0_reference"]["r0_ohm"]=float("nan")
        self.assertIn("INVALID_R0_OHMS",audit_signal_lineage(x))
    def test_kohm_ratio_requires_explicit_conversion(self):
        x = normalized()
        x["signal_processing"]["source_signal_units"] = "KOHM"
        self.assertIn("R0_RATIO_UNIT_CONVERSION_UNDOCUMENTED", audit_signal_lineage(x))

    def test_kohm_ratio_conversion_1000_is_accepted(self):
        x = normalized()
        x["signal_processing"].update(source_signal_units="KOHM", rs_to_ohm_multiplier=1000.0)
        self.assertEqual(audit_signal_lineage(x), [])

    def test_kohm_ratio_conversion_1_is_rejected(self):
        x = normalized()
        x["signal_processing"].update(source_signal_units="KOHM", rs_to_ohm_multiplier=1.0)
        self.assertIn("R0_RATIO_UNIT_CONVERSION_INVALID", audit_signal_lineage(x))

    def test_ohm_ratio_conversion_1000_is_rejected(self):
        x = normalized()
        x["signal_processing"]["rs_to_ohm_multiplier"] = 1000.0
        self.assertIn("R0_RATIO_UNIT_CONVERSION_INVALID", audit_signal_lineage(x))

    def test_kohm_ratio_bool_multiplier_is_rejected(self):
        x = normalized()
        x["signal_processing"].update(source_signal_units="KOHM", rs_to_ohm_multiplier=True)
        self.assertIn("R0_RATIO_UNIT_CONVERSION_INVALID", audit_signal_lineage(x))

    def test_r0_wrong_units(self):
        x=normalized();x["signal_processing"]["source_signal_units"]="ADC_COUNTS"
        self.assertIn("RS_OVER_R0_REQUIRES_RESISTANCE_INPUT_AND_DIMENSIONLESS_OUTPUT",audit_signal_lineage(x))
    def test_raw_signal(self):
        x=base();x["signal_processing"]["feature_formula"]="RAW_SIGNAL"
        self.assertEqual(audit_signal_lineage(x), [])
    def test_model_name_is_not_physical_instance(self):
        x=normalized()
        x["signal_processing"]["sensor_ids"]=["MQ135"]
        x["signal_processing"]["r0_reference"]["sensor_instance_id"]="MQ135"
        self.assertIn("R0_SENSOR_MODEL_ONLY_NOT_PHYSICAL_INSTANCE",audit_signal_lineage(x))
    def test_multisensor_cannot_share_one_r0(self):
        x=normalized()
        x["signal_processing"]["sensor_ids"].append("MQ7#unit-002")
        self.assertIn("MISSING_PER_SENSOR_R0_REFERENCES",audit_signal_lineage(x))
    def test_multisensor_requires_full_r0_coverage(self):
        x=normalized()
        p=x["signal_processing"]
        p["sensor_ids"].append("MQ7#unit-002")
        p["r0_references"]={p["sensor_ids"][0]:p.pop("r0_reference")}
        self.assertIn("PER_SENSOR_R0_COVERAGE_MISMATCH",audit_signal_lineage(x))
    def test_multisensor_rejects_cross_sensor_r0(self):
        x=normalized()
        p=x["signal_processing"]
        p["sensor_ids"].append("MQ7#unit-002")
        first=p.pop("r0_reference")
        p["r0_references"]={p["sensor_ids"][0]:first,p["sensor_ids"][1]:dict(first)}
        self.assertIn("R0_SENSOR_INSTANCE_MISMATCH",audit_signal_lineage(x))
    def test_multisensor_complete_metadata_only(self):
        x=normalized()
        p=x["signal_processing"]
        p["sensor_ids"].append("MQ7#unit-002")
        first=p.pop("r0_reference")
        second=dict(first,sensor_instance_id="MQ7#unit-002",heater_phase="LOW",
                    reference_id="std-02",r0_ohm=8100.0)
        p["r0_references"]={p["sensor_ids"][0]:first,p["sensor_ids"][1]:second}
        self.assertEqual(audit_signal_lineage(x),[])
    def test_ambiguous_r0_layout_is_blocked(self):
        x=normalized()
        p=x["signal_processing"]
        p["r0_references"]={p["sensor_ids"][0]:dict(p["r0_reference"])}
        self.assertIn("AMBIGUOUS_R0_REFERENCE_LAYOUT",audit_signal_lineage(x))
    def test_unhashable_source_units_fail_closed_for_rs_over_r0(self):
        for bad in ([], {"unit": "OHM"}):
            with self.subTest(bad=bad):
                x = normalized()
                x["signal_processing"]["source_signal_units"] = bad
                issues = audit_signal_lineage(x)
                self.assertIn("INVALID_SOURCE_SIGNAL_UNITS", issues)
                self.assertIn(
                    "RS_OVER_R0_REQUIRES_RESISTANCE_INPUT_AND_DIMENSIONLESS_OUTPUT",
                    issues)

    def test_unhashable_r0_mode_fail_closed(self):
        for bad in ([], {"mode": "MEASURED_REFERENCE"}):
            with self.subTest(bad=bad):
                x = normalized()
                x["signal_processing"]["r0_mode"] = bad
                self.assertIn("R0_MODE_UNDOCUMENTED", audit_signal_lineage(x))

    def test_unhashable_heater_phase_fail_closed(self):
        for bad in ([], {"phase": "HIGH"}):
            with self.subTest(bad=bad):
                x = normalized()
                x["signal_processing"]["r0_reference"]["heater_phase"] = bad
                self.assertIn("R0_HEATER_PHASE_UNDOCUMENTED", audit_signal_lineage(x))

    def test_unhashable_mode_with_raw_signal_fail_closed(self):
        x = base()
        x["signal_processing"]["feature_formula"] = "RAW_SIGNAL"
        x["signal_processing"]["r0_mode"] = {"invalid": "mode"}
        self.assertIn("R0_MODE_UNDOCUMENTED", audit_signal_lineage(x))

    def test_no_lineage(self):
        self.assertEqual(audit_signal_lineage({}),["MISSING_SIGNAL_PROCESSING_PROVENANCE"])

if __name__ == "__main__": unittest.main()
