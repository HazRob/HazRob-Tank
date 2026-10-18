import unittest
from svm_v1 import fit_linear_ovr_svm,decision_function
class SVMTests(unittest.TestCase):
    def setUp(self):
        self.x=[[-3],[-2],[-1],[1],[2],[3]];self.y=["A"]*3+["B"]*3
        self.m=fit_linear_ovr_svm(self.x,self.y)
    def test_a(self):self.assertEqual(decision_function([[-2]],self.m)[0]["predicted_label"],"A")
    def test_b(self):self.assertEqual(decision_function([[2]],self.m)[0]["predicted_label"],"B")
    def test_scores(self):self.assertEqual(len(decision_function([[0]],self.m)[0]["decision_scores"]),2)
    def test_mean(self):self.assertAlmostEqual(self.m["mean"][0],0)
    def test_scale(self):self.assertGreater(self.m["scale"][0],0)
    def test_multiclass(self):
        m=fit_linear_ovr_svm([[-5],[-4],[0],[1],[5],[6]],["A","A","B","B","C","C"])
        self.assertEqual(len(m["classes"]),3)
    def test_constant(self):
        m=fit_linear_ovr_svm([[-2,1],[-1,1],[1,1],[2,1]],["A","A","B","B"])
        self.assertEqual(m["scale"][1],1)
    def test_bad_c(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm(self.x,self.y,c=0)
    def test_bad_epochs(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm(self.x,self.y,epochs=0)
    def test_bad_rate(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm(self.x,self.y,learning_rate=0)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm([[0],[1],[2],[float("nan")]],["A","A","B","B"])
    def test_bad_class(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm(self.x,["A"]*6)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_linear_ovr_svm([[0],[1,2],[2],[3]],["A","A","B","B"])
    def test_mismatch(self):
        with self.assertRaises(ValueError):decision_function([[1,2]],self.m)
    def test_query_nan(self):
        with self.assertRaises(ValueError):decision_function([[float("nan")]],self.m)
    def test_deterministic(self):
        self.assertEqual(self.m["weights"],fit_linear_ovr_svm(self.x,self.y)["weights"])
if __name__=="__main__":unittest.main()
