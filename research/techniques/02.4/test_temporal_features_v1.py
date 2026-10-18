import unittest
from math import log
from temporal_features_v1 import temporal_features
class TemporalTests(unittest.TestCase):
    def setUp(self):
        self.t=[0,1,2,3,4,5];self.y=[10,12,16,20,15,10]
    def test_peak(self): self.assertEqual(temporal_features(self.t,self.y,10)["peak_response"],10)
    def test_peak_time(self): self.assertEqual(temporal_features(self.t,self.y,10)["peak_time_s"],3)
    def test_auto(self): self.assertEqual(temporal_features(self.t,self.y,10)["direction"],"up")
    def test_t10(self): self.assertAlmostEqual(temporal_features(self.t,self.y,10)["t10_s"],.5)
    def test_t90(self): self.assertAlmostEqual(temporal_features(self.t,self.y,10)["t90_s"],2.75)
    def test_rise(self): self.assertAlmostEqual(temporal_features(self.t,self.y,10)["rise_10_90_s"],2.25)
    def test_slope(self): self.assertEqual(temporal_features(self.t,self.y,10)["max_rising_slope_per_s"],4)
    def test_area(self): self.assertAlmostEqual(temporal_features(self.t,self.y,10)["signed_response_area"],23)
    def test_recovery(self): self.assertEqual(temporal_features(self.t,self.y,10)["recovery_t50_s"],1)
    def test_no_recovery(self): self.assertIsNone(temporal_features([0,1,2],[10,12,20],10)["recovery_t50_s"])
    def test_down(self): self.assertEqual(temporal_features([0,1,2],[10,8,5],10)["direction"],"down")
    def test_no_signal(self):
        with self.assertRaises(ValueError): temporal_features([0,1,2],[10,10,10],10)
    def test_bad_time(self):
        with self.assertRaises(ValueError): temporal_features([0,1,1],[10,12,20],10)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): temporal_features([0,1,2],[10,float("nan"),20],10)
    def test_bad_direction(self):
        with self.assertRaises(ValueError): temporal_features([0,1,2],[10,12,20],10,direction="sideways")
    def test_length(self):
        with self.assertRaises(ValueError): temporal_features([0,1],[10,20],10)
if __name__=="__main__": unittest.main()
