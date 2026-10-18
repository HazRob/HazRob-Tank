import unittest
from drift_monitor_v1 import assess_baseline_drift
class DriftTests(unittest.TestCase):
    def test_no_change(self): self.assertFalse(assess_baseline_drift([1000]*5,[1000]*5)["drift_flag"])
    def test_upward(self): self.assertTrue(assess_baseline_drift([1000]*5,[1200]*5)["drift_flag"])
    def test_downward(self): self.assertTrue(assess_baseline_drift([1000]*5,[800]*5)["drift_flag"])
    def test_small(self): self.assertFalse(assess_baseline_drift([1000]*5,[1050]*5)["drift_flag"])
    def test_unstable(self):
        x=assess_baseline_drift([1000]*5,[100,200,300,400,500])
        self.assertFalse(x["quality_ok"])
        self.assertEqual(x["interpretation"],"UNSTABLE_BASELINE")
    def test_median_outlier(self): self.assertEqual(assess_baseline_drift([1000]*5,[1000]*4+[10000])["current_median_ohms"],1000)
    def test_insufficient(self):
        with self.assertRaises(ValueError): assess_baseline_drift([1000]*4,[1000]*5)
    def test_zero_rs(self):
        with self.assertRaises(ValueError): assess_baseline_drift([1000]*5,[0]*5)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): assess_baseline_drift([1000]*5,[float("nan")]*5)
    def test_invalid_threshold(self):
        with self.assertRaises(ValueError): assess_baseline_drift([1000]*5,[1000]*5,max_log_change=-1)
    def test_ratio(self): self.assertAlmostEqual(assess_baseline_drift([1000]*5,[1100]*5)["relative_change"],.1)
    def test_quality_stable(self): self.assertTrue(assess_baseline_drift([1000]*5,[1000]*5)["quality_ok"])
if __name__=="__main__": unittest.main()
