import unittest
from logistic_regression_v1 import fit_softmax,predict_softmax
class SoftmaxTests(unittest.TestCase):
    def setUp(self):
        self.x=[[-3],[-2],[-1],[1],[2],[3]];self.y=["A"]*3+["B"]*3
        self.model=fit_softmax(self.x,self.y,epochs=200)
    def test_class_a(self):self.assertEqual(predict_softmax([[-2]],self.model)[0]["predicted_label"],"A")
    def test_class_b(self):self.assertEqual(predict_softmax([[2]],self.model)[0]["predicted_label"],"B")
    def test_scores_sum(self):self.assertAlmostEqual(sum(predict_softmax([[0]],self.model)[0]["softmax_scores"].values()),1)
    def test_train_mean(self):self.assertAlmostEqual(self.model["mean"][0],0)
    def test_train_scale(self):self.assertGreater(self.model["scale"][0],0)
    def test_multiclass(self):
        m=fit_softmax([[-4],[-3],[0],[.5],[3],[4]],["A","A","B","B","C","C"],epochs=500)
        self.assertEqual(len(m["classes"]),3)
    def test_constant_feature(self):
        m=fit_softmax([[-1,5],[-2,5],[1,5],[2,5]],["A","A","B","B"],epochs=100)
        self.assertEqual(m["scale"][1],1)
    def test_invalid_epochs(self):
        with self.assertRaises(ValueError):fit_softmax(self.x,self.y,epochs=0)
    def test_invalid_l2(self):
        with self.assertRaises(ValueError):fit_softmax(self.x,self.y,l2=-1)
    def test_invalid_rate(self):
        with self.assertRaises(ValueError):fit_softmax(self.x,self.y,learning_rate=0)
    def test_nan_train(self):
        with self.assertRaises(ValueError):fit_softmax([[0],[1],[2],[float("nan")]],["A","A","B","B"])
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_softmax(self.x,["A"]*6)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_softmax([[0],[1,2],[2],[3]],["A","A","B","B"])
    def test_predict_mismatch(self):
        with self.assertRaises(ValueError):predict_softmax([[1,2]],self.model)
    def test_predict_nan(self):
        with self.assertRaises(ValueError):predict_softmax([[float("nan")]],self.model)
    def test_deterministic(self):
        m=fit_softmax(self.x,self.y,epochs=200)
        self.assertEqual(self.model["weights"],m["weights"])
if __name__=="__main__":unittest.main()
