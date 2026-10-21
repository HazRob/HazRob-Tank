"""Synthetic-only contract tests for first ten HazRob Tank V3 research IDs.

Do not present test success as gas calibration or device certification.
"""
import hashlib
import math
import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parent))
from measurement_reference_contract_v2 import (
    adc_to_rs_guarded, heater_sample_contract, r0_from_source_window,
    sensitivity_curve_prior, reference_curve_ratio, sampling_window_check,
    label_safe_fingerprint, verify_source_bytes, holdout_whole_sessions,
    fit_known_gas_source_curve, screening_lod_from_unique_blanks,
)


def r0_args(**changes):
    x=dict(rs_samples_ohm=[9000,9050,9100,9000,8950,9020],
           sensor_model="MQ135",sensor_instance_id="MQ135-A",
           heater_phase="CONSTANT",heater_protocol_id="manufacturer-cycle",
           reference_ratio=3.6,reference_kind="clean_air_ratio",
           calibration_id="TEST-R0-001",reference_id="CLEAN-AIR-PAPER-REFERENCE")
    x.update(changes)
    return x


def curve_args(**changes):
    x=dict(source_id="MQDATASCIENCE-CURVE",sensor_model="MQ3",
           gas_label="ethanol",a=2.,b=-1.,low_ppm=5.15,
           high_ppm=500.,output_scale=50.)
    x.update(changes)
    return x


def fit_args(**changes):
    concentrations=(10.,20.,40.,80.)
    points=[dict(concentration_ppm=c,rs_ohm=1000*2*c**(-0.5),
                 sensor_instance_id="MQ135-1",gas_label="ETHANOL",
                 reference_kind="certified_gas",reference_id="STD-GAS-01")
            for c in concentrations]
    x=dict(points=points,sensor_instance_id="MQ135-1",
           gas_label="ETHANOL",calibration_id="CAL-1",
           reference_id="STD-GAS-01",r0_ohm=1000.,
           heater_phase="CONSTANT",heater_protocol_id="HK-1",
           sensor_model="MQ135")
    x.update(changes)
    return x


def blank_rows(n=12):
    return [dict(exposure_id=f"blank-{i}",label="BLANK",signal=i/100)
            for i in range(n)]


class ReferenceContractsBatch01Test(unittest.TestCase):
    # ID 01.1 ADC -> Rs
    def test_01_adc_rs_physically_correct_ohms_and_kohms(self):
        out=adc_to_rs_guarded(adc_code=2000,adc_full_scale_code=4095,
              adc_full_scale_volts=3.3,divider_gain=2,vc_volts=5,rl_ohms=10000)
        self.assertAlmostEqual(out["rs_ohm"],out["rs_kohm"]*1000)
        self.assertAlmostEqual(out["rs_ohm"],10000*(5/out["vrl_volts"]-1))
        self.assertFalse(out["device_calibrated_ppm"])

    def test_02_adc_rail_rejected(self):
        for code in (0,4095):
            with self.subTest(code=code),self.assertRaises(ValueError):
                adc_to_rs_guarded(adc_code=code,adc_full_scale_code=4095,
                 adc_full_scale_volts=3.3,divider_gain=2,vc_volts=5,rl_ohms=10000)

    def test_03_adc_invalid_divider_rejected(self):
        with self.assertRaises(ValueError):
            adc_to_rs_guarded(adc_code=4000,adc_full_scale_code=4095,
             adc_full_scale_volts=3.3,divider_gain=2,vc_volts=5,rl_ohms=10000)

    def test_04_adc_invalid_bool_and_load(self):
        with self.assertRaises(ValueError):
            adc_to_rs_guarded(adc_code=True,adc_full_scale_code=4095,
             adc_full_scale_volts=3.3,divider_gain=2,vc_volts=5,rl_ohms=10000)
        with self.assertRaises(ValueError):
            adc_to_rs_guarded(adc_code=10,adc_full_scale_code=4095,
             adc_full_scale_volts=3.3,divider_gain=2,vc_volts=5,rl_ohms=-1000)

    # ID 01.2 R0 evidence trace
    def test_05_r0_clean_air_ratio_not_certified(self):
        out=r0_from_source_window(**r0_args())
        self.assertAlmostEqual(out["r0_ohm"],out["rs_median_ohm"]/3.6)
        self.assertAlmostEqual(out["r0_kohm"],out["r0_ohm"]/1000)
        self.assertFalse(out["independent_gas_reference_recorded"])
        self.assertFalse(out["device_qualified_for_safety"])

    def test_06_r0_gas_reference_requires_gas_ppm(self):
        with self.assertRaises(ValueError):
            r0_from_source_window(**r0_args(reference_kind="certified_gas"))
        d=r0_from_source_window(**r0_args(reference_kind="certified_gas",
             reference_gas="NH3",reference_ppm=100.))
        self.assertTrue(d["independent_gas_reference_recorded"])
        self.assertFalse(d["device_qualified_for_safety"])

    def test_07_r0_never_mix_clean_air_and_ppm(self):
        with self.assertRaises(ValueError):
            r0_from_source_window(**r0_args(reference_ppm=100,reference_gas="NH3"))

    def test_08_r0_bad_sensor_heater_or_mad(self):
        with self.assertRaises(ValueError):
            r0_from_source_window(**r0_args(sensor_model="MQ7"))
        with self.assertRaises(ValueError):
            r0_from_source_window(**r0_args(rs_samples_ohm=[1000,1000,1000,10000,20000]))

    # ID 01.3 curves
    def test_09_curve_factor_50_and_log_equivalence(self):
        curve=sensitivity_curve_prior(**curve_args())
        self.assertAlmostEqual(reference_curve_ratio(curve,50),2)
        self.assertAlmostEqual(reference_curve_ratio(curve,100),1)
        self.assertEqual(curve["curve_evidence"],"SOURCE_PRIOR_NOT_DEVICE_CALIBRATION")

    def test_10_curve_extrapolation_and_zero_disallowed(self):
        curve=sensitivity_curve_prior(**curve_args())
        for c in (5,501,0):
            with self.subTest(c=c),self.assertRaises(ValueError):
                reference_curve_ratio(curve,c)

    def test_11_curve_missing_r0_information_not_safety(self):
        curve=sensitivity_curve_prior(**curve_args(source_id="2024-GRAPH"))
        self.assertNotIn("device_r0_ohm",curve)
        with self.assertRaises(ValueError):
            sensitivity_curve_prior(**curve_args(b=0.))

    # ID 01.4 heater
    def test_12_heater_mq7_low_valid_but_not_actuated(self):
        out=heater_sample_contract(sensor_model="MQ7",phase="LOW",
             protocol_id="MQ7-MANUFACTURER",phase_elapsed_s=10,settle_s=3)
        self.assertTrue(out["eligible_to_sample_phase"])
        self.assertFalse(out["heater_actuation"])

    def test_13_heater_cycled_constant_rejected(self):
        for model in ("MQ7","MQ9"):
            with self.subTest(model=model),self.assertRaises(ValueError):
                heater_sample_contract(sensor_model=model,phase="CONSTANT",
                   protocol_id="default",phase_elapsed_s=10,settle_s=3)

    def test_14_heater_noncycled_unknown_or_early(self):
        out=heater_sample_contract(sensor_model="MQ135",phase="CONSTANT",
             protocol_id="5V-constant",phase_elapsed_s=0.5,settle_s=2)
        self.assertFalse(out["eligible_to_sample_phase"])
        with self.assertRaises(ValueError):
            heater_sample_contract(sensor_model="MQ135",phase="UNKNOWN",
             protocol_id="5V-constant",phase_elapsed_s=10,settle_s=0)

    # ID 01.6 flow/sampling
    def test_15_sampling_purge_is_explicit(self):
        x=sampling_window_check(timestamps_s=[0,1,2,3,4],
           sensor_signal=[2,3,4,3,2],flushing_observed=False,
           airflow_reference_id=None)
        self.assertFalse(x["ready_for_uncontaminated_fingerprint"])
        self.assertIn("PURGE_NOT_VERIFIED",x["flags"])
        self.assertIn("AIRFLOW_NOT_DOCUMENTED",x["flags"])

    def test_16_sampling_time_order(self):
        with self.assertRaises(ValueError):
            sampling_window_check(timestamps_s=[0,1,1,3,4],
              sensor_signal=[2,3,4,3,2],flushing_observed=True,
              airflow_reference_id="fan-1")

    def test_17_sampling_verified_window_is_not_gas_identity(self):
        x=sampling_window_check(timestamps_s=[0,1,2,3,4],
           sensor_signal=[2,3,4,3,2],flushing_observed=True,
           airflow_reference_id="fan-cal1")
        self.assertTrue(x["ready_for_uncontaminated_fingerprint"])
        self.assertFalse(x["gas_identity_assigned"])

    # ID 02.5 fingerprints
    def test_18_fingerprint_one_external_MQ_allowed(self):
        x=label_safe_fingerprint(source_id="UCI1081",label_kind="PURE_GAS",
          sensor_models=["MQ135"],baseline_rs_ohm=[10000],
          sample_rs_ohm=[5000],label="ETHANOL",
          independent_gas_identity=True)
        self.assertAlmostEqual(x["features_log_rs_fold"][0],math.log(0.5))
        self.assertTrue(x["can_train_pure_gas_identity"])
        self.assertFalse(x["real_tank_8mq_validated"])

    def test_19_fingerprint_complex_product_not_pure(self):
        x=label_safe_fingerprint(source_id="WOOD",label_kind="COMPLEX_HEADSPACE",
          sensor_models=["MQ135"],baseline_rs_ohm=[10000],
          sample_rs_ohm=[5000],label="WOOD",independent_gas_identity=False)
        self.assertFalse(x["can_train_pure_gas_identity"])
        with self.assertRaises(ValueError):
            label_safe_fingerprint(source_id="WOOD",label_kind="COMPLEX_HEADSPACE",
              sensor_models=["MQ135"],baseline_rs_ohm=[10000],
              sample_rs_ohm=[5000],label="WOOD",independent_gas_identity=True)

    def test_20_fingerprint_sensor_array_shape(self):
        with self.assertRaises(ValueError):
            label_safe_fingerprint(source_id="X",label_kind="PURE_GAS",
              sensor_models=["MQ2","MQ3"],baseline_rs_ohm=[10000],
              sample_rs_ohm=[5000,3000],label="X")

    # ID 02.8 real hash
    def test_21_source_bytes_verified_and_tamper_rejected(self):
        source=b"a calibrated file-like research source, not gas evidence"
        digest=hashlib.sha256(source).hexdigest()
        self.assertTrue(verify_source_bytes(payload=source,
          expected_sha256=digest,source_id="SOURCE01")["raw_bytes_verified"])
        with self.assertRaisesRegex(ValueError,"SOURCE_BYTES_CHECKSUM_MISMATCH"):
            verify_source_bytes(payload=source+b"change",expected_sha256=digest,
              source_id="SOURCE01")

    def test_22_source_hex_checksum_syntax(self):
        with self.assertRaises(ValueError):
            verify_source_bytes(payload=b"x",expected_sha256="q"*64,
              source_id="SOURCE01")

    # ID 04.10 groups
    def test_23_benchmark_whole_session_group(self):
        rows=[dict(source_id="S",device_id="D",session_id="S1",
                   exposure_id="E1",label="ETHANOL"),
              dict(source_id="S",device_id="D",session_id="S1",
                   exposure_id="E1",label="ETHANOL"),
              dict(source_id="S",device_id="D",session_id="S2",
                   exposure_id="E2",label="ACETONE")]
        x=holdout_whole_sessions(rows,holdout_session_ids={"S2"})
        self.assertEqual(len(x["train"]),2)
        self.assertEqual(len(x["test"]),1)
        self.assertTrue(x["leakage_checked"])

    def test_24_benchmark_exposure_leak_detected(self):
        rows=[dict(source_id="S",device_id="D",session_id="S1",
                   exposure_id="E1",label="ETHANOL"),
              dict(source_id="S",device_id="D",session_id="S2",
                   exposure_id="E1",label="ETHANOL")]
        with self.assertRaisesRegex(ValueError,"leakage"):
            holdout_whole_sessions(rows,holdout_session_ids={"S2"})

    # ID 05.1 empirical curve fit
    def test_25_known_gas_fitted_correctly(self):
        x=fit_known_gas_source_curve(**fit_args())
        self.assertAlmostEqual(x["slope_log10"],-.5,places=8)
        self.assertAlmostEqual(x["intercept_log10"],math.log10(2),places=8)
        self.assertFalse(x["heldout_performance_verified"])
        self.assertFalse(x["safety_grade_ppm"])

    def test_26_known_gas_unreferenced_not_allowed(self):
        x=fit_args()
        x["points"][0]["reference_kind"]="manufacturer_graph"
        with self.assertRaises(ValueError):
            fit_known_gas_source_curve(**x)

    def test_27_known_gas_nonuniform_identity_not_allowed(self):
        x=fit_args()
        x["points"][-1]["sensor_instance_id"]="DIFFERENT"
        with self.assertRaises(ValueError):
            fit_known_gas_source_curve(**x)

    # ID 05.8 blanks / LOD and LOQ
    def test_28_lod_no_ppm_from_blank_only(self):
        x=screening_lod_from_unique_blanks(blank_rows())
        self.assertEqual(x["n_independent_blank_records"],12)
        self.assertIsNone(x["lod_ppm"])
        self.assertEqual(x["status"],"SCREENING_ONLY_NO_PPM")

    def test_29_lod_disallows_fake_repeated_time_rows(self):
        rows=blank_rows()
        rows[-1]["exposure_id"]=rows[0]["exposure_id"]
        with self.assertRaises(ValueError):
            screening_lod_from_unique_blanks(rows)

    def test_30_lod_requires_independent_blanks(self):
        with self.assertRaises(ValueError):
            screening_lod_from_unique_blanks(blank_rows(9))

    def test_31_lod_zero_sd_not_validated(self):
        data=blank_rows()
        for row in data:row["signal"]=1.
        with self.assertRaises(ValueError):
            screening_lod_from_unique_blanks(data)


if __name__=="__main__":
    unittest.main()
