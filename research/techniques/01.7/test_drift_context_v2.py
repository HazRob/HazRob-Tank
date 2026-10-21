import unittest
from drift_context_v2 import BaselineWindow,evaluate_drift

def window(**kwargs):
    d=dict(physical_sensor_id="MQ135-A",session_id="DAY1",reference_label="CLEAN_AIR",
           heater_phase="CONSTANT",temp_c=25,rh_percent=50,rs_ohm=(10000,)*5)
    d.update(kwargs);return BaselineWindow(**d)

class DriftContextTests(unittest.TestCase):
    def test_equal(self):
        r=evaluate_drift(window(),window(session_id="DAY2"))
        self.assertEqual(r["status"],"COMPARABLE_BASELINES_RESEARCH_ONLY")
        self.assertFalse(r["drift_flag"])
        self.assertFalse(r["gas_absence_proven"])
    def test_significant(self):
        r=evaluate_drift(window(),window(session_id="DAY2",rs_ohm=(12000,)*5))
        self.assertTrue(r["drift_flag"])
        self.assertFalse(r["r0_update_authorized"])
    def test_device_mismatch(self):
        r=evaluate_drift(window(),window(physical_sensor_id="MQ135-B"))
        self.assertEqual(r["status"],"INCOMPARABLE_DEVICE")
        self.assertIsNone(r["drift_flag"])
    def test_reference_label_mismatch(self):
        r=evaluate_drift(window(),window(reference_label="GAS"))
        self.assertEqual(r["status"],"INCOMPARABLE_GAS_OR_HEATER")
    def test_phase_mismatch(self):
        r=evaluate_drift(window(),window(heater_phase="LOW"))
        self.assertEqual(r["status"],"INCOMPARABLE_GAS_OR_HEATER")
    def test_temperature_mismatch(self):
        r=evaluate_drift(window(),window(temp_c=30))
        self.assertEqual(r["status"],"INCOMPARABLE_ENVIRONMENT")
    def test_rh_mismatch(self):
        r=evaluate_drift(window(),window(rh_percent=60))
        self.assertEqual(r["status"],"INCOMPARABLE_ENVIRONMENT")
    def test_noisy_reference(self):
        r=evaluate_drift(window(),window(rs_ohm=(4000,6000,10000,14000,16000)))
        self.assertEqual(r["status"],"UNSTABLE_REFERENCE_WINDOW")
    def test_not_enough_samples(self):
        with self.assertRaises(ValueError):window(rs_ohm=(1,2))
    def test_nonfinite(self):
        with self.assertRaises(ValueError):window(rs_ohm=(10000,)*4+(float("nan"),))
    def test_negative_rh(self):
        with self.assertRaises(ValueError):window(rh_percent=-1)
    def test_bool_rs(self):
        with self.assertRaises(ValueError):window(rs_ohm=(True,)*5)
    def test_bad_phase(self):
        with self.assertRaises(ValueError):window(heater_phase="UNKNOWN")
    def test_negative_threshold(self):
        with self.assertRaises(ValueError):evaluate_drift(window(),window(),max_temp_delta_c=-1)
    def test_zero_tolerance_allowed(self):
        self.assertFalse(evaluate_drift(window(),window(),max_temp_delta_c=0)["drift_flag"])
    def test_logged_identity(self):
        x=evaluate_drift(window(),window(session_id="DAY2"))
        self.assertEqual(x["current_session_id"],"DAY2")

if __name__=="__main__":unittest.main()
