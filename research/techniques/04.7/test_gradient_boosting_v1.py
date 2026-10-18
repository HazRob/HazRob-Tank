import unittest
from gradient_boosting_v1 import fit_boosted_stumps,predict_boosted_stumps
class BoostTests(unittest.TestCase):
    def setUp(self):
        self.x=[[-4],[-3],[-2],[-1],[1],[2],[3],[4]]
        self.y=["A"]*4+["B"]*4
        self.m=fit_boosted_stumps(self.x,self.y,rounds=25)
    def test_a(self):self.assertEqual(predict_boosted_stumps([[-3]],self.m)[0]["predicted_label"],"A")
    def test_b(self):self.assertEqual(predict_boosted_stumps([[3]],self.m)[0]["predicted_label"],"B")
    def test_prob_sum(self):self.assertAlmostEqual(sum(predict_boosted_stumps([[0]],self.m)[0]["softmax_scores"].values()),1)
    def test_rounds(self):self.assertEqual(len(self.m["trees"]),25)
    def test_classes(self):self.assertEqual(self.m["classes"],["A","B"])
    def test_multiclass(self):
        m=fit_boosted_stumps([[-6],[-5],[-1],[0],[5],[6]],["A","A","B","B","C","C"],rounds=30)
        self.assertEqual(len(m["classes"]),3)
    def test_constant(self):
        m=fit_boosted_stumps([[1],[1],[1],[1]],["A","A","B","B"],rounds=2)
        self.assertIsNone(m["trees"][0][0]["feature"])
    def test_bad_rounds(self):
        with self.assertRaises(ValueError):fit_boosted_stumps(self.x,self.y,rounds=0)
    def test_bad_rate(self):
        with self.assertRaises(ValueError):fit_boosted_stumps(self.x,self.y,learning_rate=0)
    def test_bad_l2(self):
        with self.assertRaises(ValueError):fit_boosted_stumps(self.x,self.y,l2=-1)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_boosted_stumps([[0],[1],[2],[float("nan")]],["A","A","B","B"])
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_boosted_stumps(self.x,["A"]*8)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_boosted_stumps([[0],[1,2],[2],[3]],["A","A","B","B"])
    def test_query_length(self):
        with self.assertRaises(ValueError):predict_boosted_stumps([[1,2]],self.m)
    def test_query_nan(self):
        with self.assertRaises(ValueError):predict_boosted_stumps([[float("nan")]],self.m)
    def test_deterministic(self):
        self.assertEqual(self.m["trees"],fit_boosted_stumps(self.x,self.y,rounds=25)["trees"])
if __name__=="__main__":unittest.main()
