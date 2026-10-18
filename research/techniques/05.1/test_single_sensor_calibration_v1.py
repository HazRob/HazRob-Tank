import unittest
from single_sensor_calibration_v1 import fit_single_sensor,estimate_known_gas_ppm
class CalibrationTests(unittest.TestCase):
    def setUp(self):
        self.points=[(10,1000),(100,100),(1000,10)]
        self.m=fit_single_sensor(self.points,gas_label="synthetic",sensor_id="MQx",r0_ohms=100)
    def test_slope(self):self.assertAlmostEqual(self.m["slope"],-1)
    def test_intercept(self):self.assertAlmostEqual(self.m["intercept"],2)
    def test_middle(self):self.assertAlmostEqual(estimate_known_gas_ppm(100,self.m)["ppm"],100)
    def test_lower(self):self.assertAlmostEqual(estimate_known_gas_ppm(1000,self.m)["ppm"],10)
    def test_upper(self):self.assertAlmostEqual(estimate_known_gas_ppm(10,self.m)["ppm"],1000)
    def test_low_outside(self):self.assertIsNone(estimate_known_gas_ppm(2000,self.m)["ppm"])
    def test_high_outside(self):self.assertIsNone(estimate_known_gas_ppm(5,self.m)["ppm"])
    def test_rmse(self):self.assertAlmostEqual(self.m["log10_rmse"],0)
    def test_three_points(self):self.assertEqual(self.m["n_points"],3)
    def test_bad_r0(self):
        with self.assertRaises(ValueError):fit_single_sensor(self.points,gas_label="x",sensor_id="x",r0_ohms=0)
    def test_bad_rs(self):
        with self.assertRaises(ValueError):estimate_known_gas_ppm(0,self.m)
    def test_two_points(self):
        with self.assertRaises(ValueError):fit_single_sensor(self.points[:2],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_duplicate_ppm(self):
        with self.assertRaises(ValueError):fit_single_sensor([(10,10),(10,9),(20,8)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_flat_response(self):
        with self.assertRaises(ValueError):fit_single_sensor([(10,10),(20,10),(30,10)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_nonfinite(self):
        with self.assertRaises(ValueError):fit_single_sensor([(10,10),(20,20),(30,float("nan"))],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_identity(self):self.assertEqual(self.m["gas_label"],"synthetic")
if __name__=="__main__":unittest.main()
