import unittest
from multipoint_calibration_v1 import fit_multipoint,estimate_multipoint_ppm,expected_rs_at_ppm
class MultipointTests(unittest.TestCase):
    def setUp(self):
        self.m=fit_multipoint([(100,100),(10,1000),(1000,10)],gas_label="synthetic",sensor_id="MQx",r0_ohms=100)
    def test_points(self):self.assertEqual(self.m["n_points"],3)
    def test_sorted(self):self.assertEqual(self.m["min_ppm"],10)
    def test_direction(self):self.assertEqual(self.m["direction"],"decreasing")
    def test_first(self):self.assertAlmostEqual(estimate_multipoint_ppm(1000,self.m)["ppm"],10)
    def test_middle(self):self.assertAlmostEqual(estimate_multipoint_ppm(100,self.m)["ppm"],100)
    def test_last(self):self.assertAlmostEqual(estimate_multipoint_ppm(10,self.m)["ppm"],1000)
    def test_inbetween(self):self.assertAlmostEqual(estimate_multipoint_ppm(316.227766,self.m)["ppm"],31.6227766,places=3)
    def test_forward(self):self.assertAlmostEqual(expected_rs_at_ppm(100,self.m)["rs_ohms"],100)
    def test_forward_outside(self):self.assertIsNone(expected_rs_at_ppm(1,self.m)["rs_ohms"])
    def test_inverse_outside(self):self.assertIsNone(estimate_multipoint_ppm(1,self.m)["ppm"])
    def test_bad_r0(self):
        with self.assertRaises(ValueError):fit_multipoint([(1,10),(2,5),(3,2)],gas_label="x",sensor_id="x",r0_ohms=0)
    def test_duplicate(self):
        with self.assertRaises(ValueError):fit_multipoint([(1,10),(1,9),(3,2)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_nonmonotone(self):
        with self.assertRaises(ValueError):fit_multipoint([(1,10),(2,2),(3,5)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_flat(self):
        with self.assertRaises(ValueError):fit_multipoint([(1,10),(2,10),(3,5)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_two(self):
        with self.assertRaises(ValueError):fit_multipoint([(1,10),(2,5)],gas_label="x",sensor_id="x",r0_ohms=1)
    def test_nan(self):
        with self.assertRaises(ValueError):estimate_multipoint_ppm(float("nan"),self.m)
if __name__=="__main__":unittest.main()
