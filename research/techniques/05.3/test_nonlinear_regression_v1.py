import unittest
from nonlinear_regression_v1 import fit_nonlinear,estimate_ppm,expected_rs
class NonlinearTests(unittest.TestCase):
    def setUp(self):
        self.points=[(10**(i/2),100*10**(2-.7*(i/2)-.05*(i/2)**2)) for i in range(7)]
        self.m=fit_nonlinear(self.points,gas_label="synthetic",sensor_id="MQx",r0_ohms=100)
    def test_count(self):self.assertEqual(self.m["n_points"],7)
    def test_rmse(self):self.assertLess(self.m["in_sample_log_rmse"],1e-9)
    def test_inverse(self):
        for ppm,rs in self.points:self.assertAlmostEqual(estimate_ppm(rs,self.m)["ppm"],ppm,places=5)
    def test_forward(self):self.assertAlmostEqual(expected_rs(self.points[2][0],self.m)["rs_ohms"],self.points[2][1],places=5)
    def test_outside_low(self):self.assertIsNone(expected_rs(.01,self.m)["rs_ohms"])
    def test_outside_high(self):self.assertIsNone(expected_rs(100000,self.m)["rs_ohms"])
    def test_invalid_rs(self):
        with self.assertRaises(ValueError):estimate_ppm(0,self.m)
    def test_invalid_ppm(self):
        with self.assertRaises(ValueError):expected_rs(0,self.m)
    def test_ridge(self):self.assertEqual(fit_nonlinear(self.points,gas_label="x",sensor_id="x",r0_ohms=100,ridge=.1)["ridge"],.1)
    def test_negative_ridge(self):
        with self.assertRaises(ValueError):fit_nonlinear(self.points,gas_label="x",sensor_id="x",r0_ohms=100,ridge=-1)
    def test_bad_r0(self):
        with self.assertRaises(ValueError):fit_nonlinear(self.points,gas_label="x",sensor_id="x",r0_ohms=0)
    def test_three(self):
        with self.assertRaises(ValueError):fit_nonlinear(self.points[:3],gas_label="x",sensor_id="x",r0_ohms=100)
    def test_duplicate(self):
        with self.assertRaises(ValueError):fit_nonlinear([(1,10),(1,9),(2,8),(3,7)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_nonlinear([(1,10),(2,9),(3,8),(4,float("nan"))],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_flat(self):
        with self.assertRaises(ValueError):fit_nonlinear([(1,10),(2,10),(3,10),(4,10)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_deterministic(self):self.assertEqual(self.m,fit_nonlinear(self.points,gas_label="synthetic",sensor_id="MQx",r0_ohms=100))
if __name__=="__main__":unittest.main()
