import unittest
from math import log
from static_features_v1 import extract_static_features
class StaticFeatureTests(unittest.TestCase):
    def test_equal(self): self.assertEqual(extract_static_features([10]*3,[10]*3)["relative_delta"],0)
    def test_positive_delta(self): self.assertEqual(extract_static_features([10]*3,[20]*3)["delta_median_ohms"],10)
    def test_negative_delta(self): self.assertEqual(extract_static_features([20]*3,[10]*3)["delta_median_ohms"],-10)
    def test_relative(self): self.assertEqual(extract_static_features([10]*3,[20]*3)["relative_delta"],1)
    def test_log_fold(self): self.assertAlmostEqual(extract_static_features([10]*3,[20]*3)["log_fold_change"],log(2))
    def test_minmax(self):
        x=extract_static_features([10]*3,[10,20,30])
        self.assertEqual((x["exposure_min_ohms"],x["exposure_max_ohms"]),(10,30))
    def test_range(self): self.assertEqual(extract_static_features([10]*3,[10,20,30])["exposure_range_ohms"],20)
    def test_mean(self): self.assertEqual(extract_static_features([10]*3,[10,20,30])["exposure_mean_ohms"],20)
    def test_median_outlier(self): self.assertEqual(extract_static_features([10]*3,[20,20,1000])["exposure_median_ohms"],20)
    def test_count(self): self.assertEqual(extract_static_features([10]*3,[20]*4)["exposure_n"],4)
    def test_short(self):
        with self.assertRaises(ValueError): extract_static_features([10]*2,[20]*3)
    def test_zero(self):
        with self.assertRaises(ValueError): extract_static_features([10]*3,[0]*3)
    def test_nan(self):
        with self.assertRaises(ValueError): extract_static_features([10]*3,[float("nan")]*3)
    def test_bool(self):
        with self.assertRaises(ValueError): extract_static_features([10]*3,[True]*3)
if __name__=="__main__": unittest.main()
