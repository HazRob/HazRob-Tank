import unittest
from curve_matching_v1 import predict_ratio,rank_curve_candidates
class CurveTests(unittest.TestCase):
    def setUp(self):
        self.a=[(10,10),(100,1),(1000,.1)]
        self.b=[(10,1),(100,.1),(1000,.01)]
        self.ref={"A":{"MQ2":self.a,"MQ3":self.a},"B":{"MQ2":self.b,"MQ3":self.b}}
    def test_anchor(self):self.assertAlmostEqual(predict_ratio(self.a,100),1)
    def test_interpolation(self):self.assertAlmostEqual(predict_ratio(self.a,31.6227766),3.16227766,places=5)
    def test_endpoint(self):self.assertAlmostEqual(predict_ratio(self.a,1000),.1)
    def test_no_extrapolation_low(self):
        with self.assertRaises(ValueError):predict_ratio(self.a,1)
    def test_no_extrapolation_high(self):
        with self.assertRaises(ValueError):predict_ratio(self.a,10000)
    def test_unsorted(self):
        with self.assertRaises(ValueError):predict_ratio([(100,1),(10,10)],50)
    def test_duplicate(self):
        with self.assertRaises(ValueError):predict_ratio([(10,1),(10,2)],10)
    def test_negative(self):
        with self.assertRaises(ValueError):predict_ratio([(10,-1),(100,1)],50)
    def test_zero_ppm(self):
        with self.assertRaises(ValueError):predict_ratio(self.a,0)
    def test_nan(self):
        with self.assertRaises(ValueError):predict_ratio(self.a,float("nan"))
    def test_ranking(self):
        r=rank_curve_candidates({"MQ2":1,"MQ3":1},self.ref,100)
        self.assertEqual(r[0]["gas"],"A")
    def test_zero_error(self):
        self.assertAlmostEqual(rank_curve_candidates({"MQ2":1,"MQ3":1},self.ref,100)[0]["rmse_log10_ratio"],0)
    def test_min_channels(self):
        self.assertEqual(rank_curve_candidates({"MQ2":1},self.ref,100),[])
    def test_missing_channel(self):
        self.assertEqual(rank_curve_candidates({"MQ2":1,"MQ3":1},{"A":{"MQ2":self.a}},100),[])
    def test_unknown_concentration(self):
        self.assertEqual(rank_curve_candidates({"MQ2":1,"MQ3":1},self.ref,1),[])
    def test_bad_observation(self):
        with self.assertRaises(ValueError):rank_curve_candidates({"MQ2":0},self.ref,100)
if __name__=="__main__":unittest.main()
