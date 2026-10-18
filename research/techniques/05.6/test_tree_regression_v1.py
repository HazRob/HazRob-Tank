import unittest
from tree_regression_v1 import fit_tree_ensemble,estimate_tree_ensemble
class TreeRegressionTests(unittest.TestCase):
    def setUp(self):
        self.ppm=[10,10,20,20,40,40,80,80,160,160,320,320]
        self.x=[[100/p,(100/p)**.5] for p in self.ppm]
        self.m=fit_tree_ensemble(self.x,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic",min_leaf=1,n_trees=31)
    def test_count(self):self.assertEqual(len(self.m["trees"]),31)
    def test_gas(self):self.assertEqual(self.m["gas_label"],"synthetic")
    def test_ids(self):self.assertEqual(self.m["sensor_ids"],("MQ2","MQ3"))
    def test_batch(self):self.assertEqual(len(estimate_tree_ensemble(self.x,self.m)),len(self.x))
    def test_finite(self):self.assertGreater(estimate_tree_ensemble([[1,1]],self.m)[0]["ppm"],0)
    def test_std(self):self.assertGreaterEqual(estimate_tree_ensemble([[1,1]],self.m)[0]["tree_log10_std"],0)
    def test_deterministic(self):self.assertEqual(self.m,fit_tree_ensemble(self.x,self.ppm,sensor_ids=["MQ2","MQ3"],gas_label="synthetic",min_leaf=1,n_trees=31))
    def test_bad_trees(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",n_trees=0)
    def test_bad_depth(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",max_depth=0)
    def test_bad_leaf(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",min_leaf=0)
    def test_bad_features(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",max_features=3)
    def test_bad_seed(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","b"],gas_label="x",seed="x")
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,self.ppm,sensor_ids=["a","a"],gas_label="x")
    def test_bad_ppm(self):
        with self.assertRaises(ValueError):fit_tree_ensemble(self.x,[0]+self.ppm[1:],sensor_ids=["a","b"],gas_label="x")
    def test_query_length(self):
        with self.assertRaises(ValueError):estimate_tree_ensemble([[1]],self.m)
    def test_query_nan(self):
        with self.assertRaises(ValueError):estimate_tree_ensemble([[float("nan"),1]],self.m)
if __name__=="__main__":unittest.main()
