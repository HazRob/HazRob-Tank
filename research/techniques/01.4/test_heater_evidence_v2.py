import unittest
from heater_evidence_v2 import DynamicHeaterSpec,evaluate_heater_sample


def spec(**kw):
    d=dict(sensor_instance_id="physical-mq7-A",model="MQ7",
           source_reference="sensor-specific-verified-manual",high_v=5.0,
           low_v=1.4,high_seconds=60,low_seconds=90,
           settle_seconds=5,voltage_tolerance_v=0.2)
    d.update(kw);return DynamicHeaterSpec(**d)


class HeaterEvidenceTests(unittest.TestCase):
    def test_high_after_settle(self):
        x=evaluate_heater_sample(spec(),10,5.01,"HIGH")
        self.assertEqual(x["status"],"DECLARED_PROFILE_CONSISTENT")
        self.assertTrue(x["sample_eligible_in_declared_model"])
        self.assertFalse(x["control_actuation"])

    def test_low(self):
        x=evaluate_heater_sample(spec(),70,1.41,"LOW")
        self.assertTrue(x["sample_eligible_in_declared_model"])
        self.assertEqual(x["expected_phase"],"LOW")

    def test_cycle_repeat(self):
        x=evaluate_heater_sample(spec(),160,5.0,"HIGH")
        self.assertTrue(x["sample_eligible_in_declared_model"])

    def test_transition(self):
        x=evaluate_heater_sample(spec(),60.1,1.4,"LOW")
        self.assertEqual(x["status"],"HEATER_TRANSITION_WINDOW")

    def test_unknown_phase(self):
        x=evaluate_heater_sample(spec(),70,1.4,"UNKNOWN")
        self.assertEqual(x["status"],"HEATER_PHASE_UNOBSERVED")

    def test_phase_mismatch(self):
        x=evaluate_heater_sample(spec(),70,1.4,"HIGH")
        self.assertEqual(x["status"],"PHASE_LOG_MISMATCH")

    def test_voltage_mismatch(self):
        x=evaluate_heater_sample(spec(),70,2.8,"LOW")
        self.assertEqual(x["status"],"HEATER_VOLTAGE_MISMATCH")

    def test_no_ppm_certificate(self):
        x=evaluate_heater_sample(spec(),70,1.4,"LOW")
        self.assertFalse(x["gas_concentration_approved"])

    def test_blank_id(self):
        with self.assertRaises(ValueError):spec(sensor_instance_id=" ")

    def test_high_below_low(self):
        with self.assertRaises(ValueError):spec(high_v=1.0)

    def test_bad_settle(self):
        with self.assertRaises(ValueError):spec(settle_seconds=61)

    def test_negative_tolerance(self):
        with self.assertRaises(ValueError):spec(voltage_tolerance_v=-1)

    def test_bad_elapsed(self):
        for x in (-1,float("nan"),True):
            with self.subTest(x=x),self.assertRaises(ValueError):evaluate_heater_sample(spec(),x,1.4,"LOW")

    def test_bad_voltage(self):
        with self.assertRaises(ValueError):evaluate_heater_sample(spec(),70,0,"LOW")

    def test_unknown_phase_string(self):
        with self.assertRaises(ValueError):evaluate_heater_sample(spec(),70,1.4,"IDLE")

    def test_phase_setpoint_is_specific_not_auto_control(self):
        x=evaluate_heater_sample(spec(low_v=1.5),70,1.4,"LOW")
        self.assertTrue(x["sample_eligible_in_declared_model"])
        self.assertEqual(x["source_reference"],"sensor-specific-verified-manual")

if __name__=="__main__":unittest.main()
