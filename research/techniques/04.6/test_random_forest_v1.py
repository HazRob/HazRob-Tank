import unittest
from random_forest_v1 import fit_forest,predict_forest
class ForestTests(unittest.TestCase):
    def setUp(self):
        self.x=[[-5],[-4],[-3],[-2],[-1],[1],[2],[3],[4],[5]]
        self.y=["A"]*5+["B"]*5
        self.m=fit_forest(self.x,self.y,n_trees=21,min_leaf=1)
    def test_class_a(self):self.assertEqual(predict_forest([[-4]],self.m)[0]["predicted_label"],"A")
    def test_class_b(self):self.assertEqual(predict_forest([[4]],self.m)[0]["predicted_label"],"B")
    def test_votes_sum(self):self.assertAlmostEqual(sum(predict_forest([[0]],self.m)[0]["vote_fractions"].values()),1)
    def test_trees(self):self.assertEqual(len(self.m["trees"]),21)
    def test_seed(self):
        self.assertEqual(self.m["trees"],fit_forest(self.x,self.y,n_trees=21,min_leaf=1)["trees"])
    def test_multiclass(self):
        m=fit_forest([[-3],[-2],[-1],[0],[1],[2],[3],[4]],["A","A","B","B","C","C","C","C"],min_leaf=1)
        self.assertEqual(len(m["classes"]),3)
    def test_constant_feature(self):
        m=fit_forest([[1,0],[1,1],[1,2],[1,3]],["A","A","B","B"],min_leaf=1)
        self.assertEqual(m["n_features"],2)
    def test_bad_trees(self):
        with self.assertRaises(ValueError):fit_forest(self.x,self.y,n_trees=0)
    def test_bad_depth(self):
        with self.assertRaises(ValueError):fit_forest(self.x,self.y,max_depth=0)
    def test_bad_leaf(self):
        with self.assertRaises(ValueError):fit_forest(self.x,self.y,min_leaf=0)
    def test_bad_features(self):
        with self.assertRaises(ValueError):fit_forest(self.x,self.y,max_features=2)
    def test_bad_seed(self):
        with self.assertRaises(ValueError):fit_forest(self.x,self.y,seed="abc")
    def test_nan(self):
        with self.assertRaises(ValueError):fit_forest([[0],[1],[2],[float("nan")]],["A","A","B","B"])
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_forest(self.x,["A"]*10)
    def test_query_length(self):
        with self.assertRaises(ValueError):predict_forest([[0,1]],self.m)
    def test_query_nan(self):
        with self.assertRaises(ValueError):predict_forest([[float("nan")]],self.m)
if __name__=="__main__":unittest.main()
