import unittest
from lod_loq_v1 import fit_blank_limits,classify_signal
class BlankLimitTests(unittest.TestCase):
    def setUp(self):
        self.blank=[-1.0+(.01 if i%2 else -.01) for i in range(20)]
        self.m=fit_blank_limits(self.blank,sensor_id="MQx",gas_label="synthetic")
    def test_count(self):self.assertEqual(self.m["n_blanks"],20)
    def test_mean(self):self.assertAlmostEqual(self.m["blank_mean"],-1)
    def test_sd(self):self.assertGreater(self.m["blank_sd"],0)
    def test_decreasing_lod(self):self.assertLess(self.m["lod_signal"],self.m["blank_mean"])
    def test_decreasing_loq(self):self.assertLess(self.m["loq_signal"],self.m["lod_signal"])
    def test_below(self):self.assertEqual(classify_signal(-1,self.m)["status"],"BELOW_SCREENING_LOD")
    def test_between(self):self.assertEqual(classify_signal(-1.05,self.m)["status"],"DETECTED_NOT_QUANTIFIABLE")
    def test_above(self):self.assertEqual(classify_signal(-1.2,self.m)["status"],"ABOVE_SCREENING_LOQ")
    def test_no_quant(self):self.assertFalse(classify_signal(-1.2,self.m)["quantifiable"])
    def test_increasing(self):
        m=fit_blank_limits(self.blank,sensor_id="x",gas_label="x",direction="increasing")
        self.assertEqual(classify_signal(-.8,m)["status"],"ABOVE_SCREENING_LOQ")
    def test_short(self):
        with self.assertRaises(ValueError):fit_blank_limits(self.blank[:9],sensor_id="x",gas_label="x")
    def test_flat(self):
        with self.assertRaises(ValueError):fit_blank_limits([1]*10,sensor_id="x",gas_label="x")
    def test_nan(self):
        with self.assertRaises(ValueError):fit_blank_limits([float("nan")]+self.blank[1:],sensor_id="x",gas_label="x")
    def test_bad_direction(self):
        with self.assertRaises(ValueError):fit_blank_limits(self.blank,sensor_id="x",gas_label="x",direction="both")
    def test_bad_sigma(self):
        with self.assertRaises(ValueError):fit_blank_limits(self.blank,sensor_id="x",gas_label="x",lod_sigma=10,loq_sigma=3)
    def test_bad_signal(self):
        with self.assertRaises(ValueError):classify_signal(float("inf"),self.m)
if __name__=="__main__":unittest.main()
