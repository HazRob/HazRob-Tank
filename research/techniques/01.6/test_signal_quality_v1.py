import unittest
from signal_quality_v1 import assess_signal
class SignalQualityTests(unittest.TestCase):
    def test_stable(self): self.assertTrue(assess_signal([1000]*10)["ok"])
    def test_missing(self): self.assertIn("MISSING",assess_signal([None]*2+[1000]*8)["flags"])
    def test_out_of_range(self): self.assertIn("OUT_OF_RANGE",assess_signal([5000]+[1000]*9)["flags"])
    def test_low_rail(self): self.assertIn("RAIL_SATURATION",assess_signal([0]*10)["flags"])
    def test_high_rail(self): self.assertIn("RAIL_SATURATION",assess_signal([4095]*10)["flags"])
    def test_noisy(self): self.assertIn("NOISY_WINDOW",assess_signal([100,200]*5)["flags"])
    def test_insufficient(self): self.assertIn("INSUFFICIENT_VALID",assess_signal([1000]*3)["flags"])
    def test_empty(self):
        with self.assertRaises(ValueError): assess_signal([])
    def test_bad_config(self):
        with self.assertRaises(ValueError): assess_signal([1]*10,adc_min=10,adc_max=5)
    def test_nan_missing(self): self.assertEqual(assess_signal([float("nan")]+[1000]*9)["missing_fraction"],.1)
    def test_bool_rejected(self):
        with self.assertRaises(TypeError): assess_signal([True]*10)
    def test_zero_median(self): self.assertEqual(assess_signal([0]*10)["relative_mad"],0)
    def test_clean_stats(self): self.assertEqual(assess_signal([1000]*10)["mad_code"],0)
if __name__=="__main__": unittest.main()
