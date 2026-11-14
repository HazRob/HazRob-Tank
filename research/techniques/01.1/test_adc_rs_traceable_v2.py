import unittest
from math import isnan
from adc_rs_traceable_v2 import Circuit, evaluate_adc, evaluate_series, resistance_from_voltage


def cfg(**changes):
    x = dict(sensor_instance_id="MQ135-physical-001", circuit_id="breadboard-A",
             rl_ohm=10000.0, vc_volts=5.0, adc_reference_volts=2.5,
             adc_max_code=4096, sensor_to_adc_voltage_gain=2.0)
    x.update(changes)
    return Circuit(**x)


class TraceableAdcR0Tests(unittest.TestCase):
    def test_known_equal_divider(self):
        self.assertAlmostEqual(resistance_from_voltage(2.5, cfg()), 10000)

    def test_provenance_and_ohm(self):
        result=evaluate_adc(2048,cfg())
        self.assertEqual(result["status"],"IDEAL_ADC_MODEL_ONLY")
        self.assertAlmostEqual(result["rs_ohm"],10000)
        self.assertAlmostEqual(result["rs_kohm"],10)
        self.assertEqual(result["sensor_instance_id"],"MQ135-physical-001")
        self.assertEqual(result["circuit_id"],"breadboard-A")
        self.assertFalse(result["ppm_qualified"])
        self.assertFalse(result["physical_calibration_verified"])

    def test_quantization_bound_order(self):
        x=evaluate_adc(1024,cfg())
        lo,hi=x["quantization_only_interval_ohm"]
        self.assertLess(lo,x["rs_ohm"])
        self.assertGreater(hi,x["rs_ohm"])

    def test_invalid_unknown_topology(self):
        with self.assertRaises(ValueError): cfg(circuit_topology="RL_SUPPLY_RS_GROUND")

    def test_invalid_negative_input(self):
        for rl in (-1,0,float("nan"),float("inf"),True):
            with self.subTest(rl=rl), self.assertRaises(ValueError): cfg(rl_ohm=rl)

    def test_gain_below_unity(self):
        with self.assertRaises(ValueError): cfg(sensor_to_adc_voltage_gain=0.5)

    def test_adc_code_type_and_bound(self):
        for code in (-1,4097,True,1000.5):
            with self.subTest(code=code), self.assertRaises(ValueError):
                evaluate_adc(code,cfg())

    def test_invalid_max_code(self):
        for v in (True,2.5,1):
            with self.subTest(maxcode=v), self.assertRaises(ValueError): cfg(adc_max_code=v)

    def test_rails_are_not_quantified(self):
        for code in (0,4096):
            with self.subTest(code=code):
                x=evaluate_adc(code,cfg())
                self.assertEqual(x["status"],"ADC_RAIL_SATURATION")
                self.assertIsNone(x["rs_ohm"])

    def test_loaded_voltage_over_rail(self):
        x=evaluate_adc(3072,cfg(vc_volts=3.0))
        self.assertEqual(x["status"],"OUTSIDE_PHYSICAL_DIVIDER")
        self.assertIsNone(x["rs_ohm"])

    def test_interval_crosses_rail(self):
        x=evaluate_adc(2048,cfg(vc_volts=2.5001))
        self.assertEqual(x["status"],"VOLTAGE_RAIL_UNCERTAIN")
        self.assertIsNone(x["rs_ohm"])

    def test_series_keeps_invalid(self):
        x=evaluate_series([0,2048,4096],cfg())
        self.assertEqual(x["n"],3)
        self.assertEqual(x["n_ideal_valid"],1)
        self.assertTrue(x["no_gas_reference"])

    def test_no_samples(self):
        with self.assertRaises(ValueError): evaluate_series([],cfg())

    def test_measured_voltage_must_be_below_vc(self):
        with self.assertRaises(ValueError): resistance_from_voltage(5,cfg())

    def test_sensor_id_required(self):
        with self.assertRaises(ValueError): cfg(sensor_instance_id="")

    def test_circuit_id_required(self):
        with self.assertRaises(ValueError): cfg(circuit_id=" ")

    def test_no_claim_of_actual_adc_calibration(self):
        x=evaluate_adc(2048,cfg(),quantization_bounds=False)
        self.assertIsNone(x["quantization_only_interval_ohm"])
        self.assertFalse(x["physical_calibration_verified"])

if __name__=="__main__": unittest.main()
