"""Research-only structural/independence regression tests: synthetic fixtures.

No laboratory data, no sensor-specific accuracy and no G3 promotion.
"""
import copy
import unittest
from tank_measurement_adapter_v1 import (
    validate_exposure,audit_cohort,holdout_session,
)

def fixture(*,record_id="e1",device_id="tank-A",session_id="day-1",
            exposure_id="exposure-1"):
    return {
        "schema_version":"tank-exposure-1.0",
        "record_id":record_id,
        "device_id":device_id,
        "session_id":session_id,
        "exposure_id":exposure_id,
        "firmware_version":"placeholder-research-fixture",
        "measurement_protocol_id":"protocol-v1",
        "timestamp_utc":"2026-10-20T10:30:00Z",
        "gas_label":"ethanol",
        "reference_type":"certified_gas",
        "reference_id":"traceable-reference-example",
        "concentration_ppm":1.0,
        "source_sha256":"ab"*32,
        "BME280":{"temperature_c":24.5,"humidity_percent":45.,"pressure_hpa":1013.25},
        "sensors":[
            {"sensor_id":"MQ135","sensor_instance_id":"tank-A:MQ135:001",
             "rl_ohm":10000.,"r0_ohm":12500.,"r0_calibration_id":"r0-A001",
             "heater_phase":"CONSTANT","time_s":[0.,1.,2.],
             "rs_ohm":[13000.,12900.,12700.],"raw_adc":[1800,1850,1870],
             "adc_full_scale_code":4095,"adc_reference_v":3.3,
             "voltage_divider_gain":1.0},
            {"sensor_id":"MQ7","sensor_instance_id":"tank-A:MQ7:002",
             "rl_ohm":10000.,"r0_ohm":8500.,"r0_calibration_id":"r0-A002",
             "heater_phase":"LOW","time_s":[0.,1.,2.],
             "rs_ohm":[9000.,8900.,8800.]}
        ]
    }

class TankRecordTests(unittest.TestCase):
    def test_positive_reference(self):
        a=validate_exposure(fixture())
        self.assertTrue(a["schema_valid"])
        self.assertFalse(a["tank_g3_scientific_validation"])
        self.assertEqual(a["n_sample_values"],6)

    def test_unit_conversion(self):
        a=validate_exposure(fixture())
        self.assertEqual(a["r0_kohm_by_instance"]["tank-A:MQ135:001"],12.5)

    def test_blank_requires_zero(self):
        a=fixture()
        a.update({"gas_label":"BLANK","reference_type":"blank","concentration_ppm":0})
        self.assertTrue(validate_exposure(a)["schema_valid"])

    def test_blank_nonzero_invalid(self):
        a=fixture()
        a.update({"gas_label":"BLANK","reference_type":"blank","concentration_ppm":1})
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_unknown_cannot_claim_ppm(self):
        a=fixture()
        a.update({"gas_label":"UNKNOWN","reference_type":"unknown","concentration_ppm":.1})
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_unknown_without_ppm_allowed(self):
        a=fixture()
        a.update({"gas_label":"UNKNOWN","reference_type":"unknown","concentration_ppm":None})
        self.assertFalse(validate_exposure(a)["independent_gas_reference_present"])

    def test_missing_reference(self):
        a=fixture();a["reference_id"]=None
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_bad_time_order(self):
        a=fixture();a["sensors"][0]["time_s"]=[0,2,1]
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_raw_adc_out_of_range(self):
        a=fixture();a["sensors"][0]["raw_adc"][1]=4096
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_raw_adc_wrong_count(self):
        a=fixture();a["sensors"][0]["raw_adc"]=[5]
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_duplicate_sensor_instances(self):
        a=fixture();a["sensors"][1]["sensor_instance_id"]=a["sensors"][0]["sensor_instance_id"]
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_nonfinite_rs(self):
        a=fixture();a["sensors"][0]["rs_ohm"][0]=float("nan")
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_r0_zero_invalid(self):
        a=fixture();a["sensors"][0]["r0_ohm"]=0
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_invalid_heater_phase(self):
        a=fixture();a["sensors"][0]["heater_phase"]="INVALID"
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_unknown_heater_never_ready(self):
        a=fixture();a["sensors"][0]["heater_phase"]="UNKNOWN"
        self.assertFalse(validate_exposure(a)["heater_phases_all_known"])

    def test_bme_negative_temp_allowed(self):
        a=fixture();a["BME280"]["temperature_c"]=-5.
        self.assertTrue(validate_exposure(a)["environment_present"])

    def test_bme_overhundred_humidity_invalid(self):
        a=fixture();a["BME280"]["humidity_percent"]=105
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_missing_bme_not_fabricated(self):
        a=fixture();del a["BME280"]
        self.assertFalse(validate_exposure(a)["environment_present"])

    def test_wrong_local_timestamp(self):
        a=fixture();a["timestamp_utc"]="2026-10-20T13:30:00+03:00"
        with self.assertRaises(ValueError):validate_exposure(a)

    def test_duplicate_exposure_rejected(self):
        a=fixture()
        b=fixture(record_id="other-file-id")
        with self.assertRaises(ValueError):audit_cohort([a,b])

    def test_distinct_device_sessions(self):
        a=fixture()
        b=fixture(record_id="e2",session_id="day-2",exposure_id="exposure-2")
        report=audit_cohort([a,b])
        self.assertEqual(report["valid_exposures"],2)
        self.assertTrue(report["ready_for_independent_session_splits"])
        self.assertFalse(report["ready_for_independent_device_splits"])
        self.assertFalse(report["tank_g3_scientific_validation"])

    def test_split_independent_sessions(self):
        a=fixture()
        b=fixture(record_id="e2",session_id="day-2",exposure_id="exposure-2")
        p=holdout_session([a,b],"tank-A","day-2")
        self.assertEqual(len(p["train"]),1)
        self.assertEqual(len(p["test"]),1)
        self.assertEqual(p["test"][0]["session_id"],"day-2")

    def test_same_exposure_across_sessions_rejected(self):
        a=fixture()
        b=fixture(record_id="e2",session_id="day-2",exposure_id="exposure-1")
        with self.assertRaises(ValueError):holdout_session([a,b],"tank-A","day-2")

    def test_duplicate_record_id_rejected(self):
        a=fixture()
        b=fixture(record_id="e1",session_id="day-2",exposure_id="exposure-2")
        with self.assertRaises(ValueError):audit_cohort([a,b])

if __name__=="__main__":unittest.main()
