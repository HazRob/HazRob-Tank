import unittest
from pls_regression_v1 import fit_pls,estimate_pls
class PLSTests(unittest.TestCase):
    def setUp(self):
        self.ppm=[10,20,40,80,160,320,640,1280,2560,5120]
        self.x=[[100/p,(100/p)**.5] for p in self.ppm]
        self.m=fit_pls(self.x,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic")
    def test_component(self):self.assertEqual(self.m["n_components"],1)
    def test_identity(self):self.assertEqual(self.m["gas_label"],"synthetic")
    def test_ids(self):self.assertEqual(self.m["sensor_ids"],("MQ2","MQ3"))
    def test_count(self):self.assertEqual(self.m["n"],10)
    def test_estimate(self):self.assertAlmostEqual(estimate_pls([self.x[4]],self.m)[0]["ppm"],160,places=5)
    def test_batch(self):self.assertEqual(len(estimate_pls(self.x,self.m)),10)
    def test_outside(self):self.assertIsNone(estimate_pls([[1000,1000]],self.m)[0]["ppm"])
    def test_two_components(self):self.assertLessEqual(fit_pls(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",n_components=2)["n_components"],2)
    def test_bad_components(self):
        with self.assertRaises(ValueError):fit_pls(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",n_components=0)
    def test_bad_ids(self):
        with self.assertRaises(ValueError):fit_pls(self.x,self.ppm,sensor_ids=["a","a"],gas_label="x")
    def test_bad_ppm(self):
        with self.assertRaises(ValueError):fit_pls(self.x,[0]+self.ppm[1:],sensor_ids=["a","b"],gas_label="x")
    def test_bad_ratios(self):
        with self.assertRaises(ValueError):fit_pls([[0,1]]+self.x[1:],self.ppm,sensor_ids=["a","b"],gas_label="x")
    def test_bad_label(self):
        with self.assertRaises(ValueError):fit_pls(self.x,self.ppm,sensor_ids=["a","b"],gas_label="")
    def test_short(self):
        with self.assertRaises(ValueError):fit_pls(self.x[:3],self.ppm[:3],sensor_ids=["a","b"],gas_label="x")
    def test_query_length(self):
        with self.assertRaises(ValueError):estimate_pls([[1]],self.m)
    def test_deterministic(self):self.assertEqual(self.m,fit_pls(self.x,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic"))
if __name__=="__main__":unittest.main()
