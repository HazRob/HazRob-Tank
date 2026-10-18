import unittest
from mlp_v1 import fit_mlp,predict_mlp
class MLPTests(unittest.TestCase):
    def setUp(self):
        self.x=[[-4],[-3],[-2],[-1],[1],[2],[3],[4]]
        self.y=["A"]*4+["B"]*4
        self.m=fit_mlp(self.x,self.y,epochs=350)
    def test_a(self):self.assertEqual(predict_mlp([[-3]],self.m)[0]["predicted_label"],"A")
    def test_b(self):self.assertEqual(predict_mlp([[3]],self.m)[0]["predicted_label"],"B")
    def test_scores_sum(self):self.assertAlmostEqual(sum(predict_mlp([[0]],self.m)[0]["softmax_scores"].values()),1)
    def test_hidden(self):self.assertEqual(self.m["hidden"],8)
    def test_class_count(self):self.assertEqual(len(self.m["classes"]),2)
    def test_multiclass(self):
        m=fit_mlp([[-6],[-5],[-1],[0],[5],[6]],["A","A","B","B","C","C"],epochs=30)
        self.assertEqual(len(m["classes"]),3)
    def test_constant(self):
        m=fit_mlp([[1,5],[2,5],[3,5],[4,5]],["A","A","B","B"],epochs=10)
        self.assertEqual(m["scale"][1],1)
    def test_bad_hidden(self):
        with self.assertRaises(ValueError):fit_mlp(self.x,self.y,hidden=0)
    def test_bad_epochs(self):
        with self.assertRaises(ValueError):fit_mlp(self.x,self.y,epochs=0)
    def test_bad_lr(self):
        with self.assertRaises(ValueError):fit_mlp(self.x,self.y,learning_rate=0)
    def test_bad_l2(self):
        with self.assertRaises(ValueError):fit_mlp(self.x,self.y,l2=-1)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_mlp([[0],[1],[2],[float("nan")]],["A","A","B","B"])
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_mlp(self.x,["A"]*8)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_mlp([[0],[1,2],[2],[3]],["A","A","B","B"])
    def test_bad_query(self):
        with self.assertRaises(ValueError):predict_mlp([[1,2]],self.m)
    def test_deterministic(self):
        self.assertEqual(self.m["w1"],fit_mlp(self.x,self.y,epochs=350)["w1"])
if __name__=="__main__":unittest.main()
