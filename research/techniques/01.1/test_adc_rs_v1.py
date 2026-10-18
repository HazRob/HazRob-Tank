import unittest
from math import inf, nan
from adc_rs_v1 import adc_to_voltage, rs_from_vrl, rs_from_adc

class TestDivider(unittest.TestCase):
    def test_equal_resistors(self): self.assertAlmostEqual(rs_from_vrl(2.5,5,10000),10000)
    def test_known_ratio(self): self.assertAlmostEqual(rs_from_vrl(1,5,10000),40000)
    def test_adc_round_trip(self): self.assertAlmostEqual(rs_from_adc(2048,4096,3.3,2,5,10000),10000*(5/3.3-1))
    def test_adc_fullscale(self): self.assertEqual(adc_to_voltage(4095,4095,3.3),3.3)
    def test_divider_gain(self): self.assertAlmostEqual(adc_to_voltage(2048,4096,3.3,2),3.3)
    def test_rails_rejected(self):
        for v in (0,5,-1,6):
            with self.subTest(v=v),self.assertRaises(ValueError): rs_from_vrl(v,5,10000)
    def test_bad_resistance(self):
        with self.assertRaises(ValueError): rs_from_vrl(1,5,0)
    def test_nonfinite(self):
        for v in (nan,inf):
            with self.subTest(v=v),self.assertRaises(ValueError): rs_from_vrl(v,5,10000)
    def test_code_bounds(self):
        for code in (-1,4096):
            with self.subTest(code=code),self.assertRaises(ValueError): adc_to_voltage(code,4095,3.3)
    def test_bad_gain(self):
        with self.assertRaises(ValueError): adc_to_voltage(100,4095,3.3,0.5)
    def test_bad_type(self):
        with self.assertRaises(TypeError): adc_to_voltage(1.2,4095,3.3)

if __name__=="__main__": unittest.main()
