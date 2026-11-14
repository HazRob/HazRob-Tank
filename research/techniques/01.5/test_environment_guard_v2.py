import unittest
from environment_guard_v2 import EnvironmentCalibration,no_group_leakage,normalize_reference_only


def cal(**kw):
    d=dict(model_id="env-002",source_id="MOX-EXT-TEST",physical_sensor_instance_id="sensor-MQ135-A",
           gas_label="ETHANOL",heater_phase="CONSTANT",ref_t_c=25.0,ref_rh_pct=50.0,
           ref_pressure_hpa=1013.25,coefficients=(.01,-.005,0.0),
           temperature_range=(10.0,40.0),rh_range=(20.0,80.0),
           pressure_range=(900.0,1100.0),training_group_ids=("session-01","session-02"))
    d.update(kw)
    return EnvironmentCalibration(**d)


def eval_c(c=None,**kw):
    d=dict(rs_ohm=10000.0,temp_c=25.0,rh_pct=50.0,pressure_hpa=1013.25,
           sensor_instance_id="sensor-MQ135-A",gas_label="ETHANOL",heater_phase="CONSTANT")
    d.update(kw)
    return normalize_reference_only(c or cal(),**d)


class EnvironmentalGuardTests(unittest.TestCase):
    def test_at_reference_identity(self):
        v=eval_c()
        self.assertAlmostEqual(v["corrected_rs_ohm"],10000)
        self.assertFalse(v["ppm_approved"])

    def test_positive_temperature_increases_observed_rs(self):
        v=eval_c(temp_c=35.0)
        self.assertAlmostEqual(v["corrected_rs_ohm"],10000*__import__("math").exp(-.1))

    def test_rh_effect(self):
        v=eval_c(rh_pct=70)
        self.assertGreater(v["corrected_rs_ohm"],10000)

    def test_gas_mismatch(self):
        with self.assertRaises(ValueError):eval_c(gas_label="ACETONE")

    def test_physical_unit_mismatch(self):
        with self.assertRaises(ValueError):eval_c(sensor_instance_id="sensor-MQ135-B")

    def test_heater_phase_mismatch(self):
        with self.assertRaises(ValueError):eval_c(heater_phase="LOW")

    def test_extrapolated_rh_rejected(self):
        with self.assertRaises(ValueError):eval_c(rh_pct=90)

    def test_extrapolated_pressure_rejected(self):
        with self.assertRaises(ValueError):eval_c(pressure_hpa=800)

    def test_bad_reference(self):
        with self.assertRaises(ValueError):cal(ref_t_c=100)

    def test_fitted_exposure_groups_disjoint(self):
        self.assertTrue(no_group_leakage(cal(),["day-3"]))

    def test_fitted_exposure_group_reuse_rejected(self):
        with self.assertRaises(ValueError):no_group_leakage(cal(),["session-01"])

    def test_duplicate_train_groups(self):
        with self.assertRaises(ValueError):cal(training_group_ids=("session-01","session-01"))

    def test_train_group_required(self):
        with self.assertRaises(ValueError):cal(training_group_ids=())

    def test_bad_coefficient(self):
        with self.assertRaises(ValueError):cal(coefficients=(float("nan"),0,0))

    def test_zero_rs(self):
        with self.assertRaises(ValueError):eval_c(rs_ohm=0)

    def test_bad_reference_rh(self):
        with self.assertRaises(ValueError):cal(ref_rh_pct=101)

    def test_bool_not_measurement(self):
        with self.assertRaises(ValueError):eval_c(rs_ohm=True)

    def test_hardware_not_validated(self):
        self.assertFalse(eval_c()["real_Tank_G3"])

if __name__=="__main__":unittest.main()
