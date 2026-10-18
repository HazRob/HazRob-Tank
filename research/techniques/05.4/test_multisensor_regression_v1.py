import unittest
from multisensor_regression_v1 import fit_multisensor,estimate_multisensor
class MultisensorTests(unittest.TestCase):
    def setUp(self):
        self.ppm=[10,20,40,80,160,320,640,1280,2560,5120]
        self.rows=[[100/p,(100/p)**.5] for p in self.ppm]
        self.m=fit_multisensor(self.rows,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic",ridge=.001)
    def test_model(self):self.assertEqual(self.m["gas_label"],"synthetic")
    def test_ids(self):self.assertEqual(self.m["sensor_ids"],("MQ2","MQ3"))
    def test_n(self):self.assertEqual(self.m["n"],10)
    def test_estimate(self):self.assertAlmostEqual(estimate_multisensor([self.rows[4]],self.m)[0]["ppm"],160,delta=3)
    def test_batch(self):self.assertEqual(len(estimate_multisensor(self.rows,self.m)),10)
    def test_rmse(self):self.assertLess(self.m["training_log10_rmse"],.01)
    def test_outside(self):self.assertIsNone(estimate_multisensor([[1000,1000]],self.m)[0]["ppm"])
    def test_bad_ridge(self):
        with self.assertRaises(ValueError):fit_multisensor(self.rows,self.ppm,sensor_ids=["a","b"],gas_label="x",ridge=0)
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):fit_multisensor(self.rows,self.ppm,sensor_ids=["a","a"],gas_label="x")
    def test_bad_ppm(self):
        with self.assertRaises(ValueError):fit_multisensor(self.rows,[0]+self.ppm[1:],sensor_ids=["a","b"],gas_label="x")
    def test_bad_ratio(self):
        with self.assertRaises(ValueError):fit_multisensor([[0,1]]+self.rows[1:],self.ppm,sensor_ids=["a","b"],gas_label="x")
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_multisensor(self.rows,self.ppm,sensor_ids=["a","b"],gas_label="")
    def test_misaligned(self):
        with self.assertRaises(ValueError):estimate_multisensor([[1]],self.m)
    def test_nan(self):
        with self.assertRaises(ValueError):estimate_multisensor([[1,float("nan")]],self.m)
    def test_too_few(self):
        with self.assertRaises(ValueError):fit_multisensor(self.rows[:3],self.ppm[:3],sensor_ids=["a","b"],gas_label="x")
    def test_reproducible(self):
        self.assertEqual(self.m,fit_multisensor(self.rows,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic",ridge=.001))
if __name__=="__main__":unittest.main()
