import unittest
from r0_calibration_v1 import r0_from_rs,estimate_r0

class R0Tests(unittest.TestCase):
    def test_ratio_one(self): self.assertEqual(r0_from_rs(10000,1),10000)
    def test_ratio_two(self): self.assertEqual(r0_from_rs(20000,2),10000)
    def test_kohms(self): self.assertEqual(estimate_r0([10000]*5,1)["r0_kohms"],10)
    def test_outlier_robustness(self): self.assertEqual(estimate_r0([10000,10000,10000,10000,100000],1)["r0_ohms"],10000)
    def test_sample_count(self):
        with self.assertRaises(ValueError): estimate_r0([10000]*4,1)
    def test_zero_ratio(self):
        with self.assertRaises(ValueError): r0_from_rs(10000,0)
    def test_negative_rs(self):
        with self.assertRaises(ValueError): r0_from_rs(-1,1)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): r0_from_rs(float("nan"),1)
    def test_unstable(self):
        with self.assertRaises(ValueError): estimate_r0([1000,2000,3000,4000,5000],1)
    def test_negative_threshold(self):
        with self.assertRaises(ValueError): estimate_r0([10000]*5,1,max_relative_mad=-1)
    def test_sample_validation(self):
        with self.assertRaises(ValueError): estimate_r0([10000]*4+[0],1)

if __name__=="__main__": unittest.main()
