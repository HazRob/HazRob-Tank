import unittest
from knn_v1 import fit_knn,predict_knn
class KNNTests(unittest.TestCase):
    def setUp(self):
        self.model=fit_knn([[0],[.1],[.2],[10],[10.1],[10.2]],["A"]*3+["B"]*3,["MQ2"])
    def test_a(self):self.assertEqual(predict_knn([0],self.model)["predicted_label"],"A")
    def test_b(self):self.assertEqual(predict_knn([10],self.model)["predicted_label"],"B")
    def test_k1(self):self.assertEqual(predict_knn([0],self.model,k=1)["predicted_label"],"A")
    def test_k3(self):self.assertEqual(len(predict_knn([0],self.model,k=3)["neighbors"]),3)
    def test_inverse(self):self.assertEqual(predict_knn([0],self.model,weighting="inverse_distance")["predicted_label"],"A")
    def test_zero_exact(self):self.assertEqual(predict_knn([0],self.model,weighting="inverse_distance")["votes"],{"A":1.})
    def test_tie(self):
        m=fit_knn([[0],[2]],["A","B"],["MQ2"])
        self.assertTrue(predict_knn([1],m,k=2)["tie"])
    def test_tie_deterministic(self):
        m=fit_knn([[0],[2]],["B","A"],["MQ2"])
        self.assertEqual(predict_knn([1],m,k=2)["predicted_label"],"A")
    def test_k_zero(self):
        with self.assertRaises(ValueError):predict_knn([0],self.model,k=0)
    def test_k_large(self):
        with self.assertRaises(ValueError):predict_knn([0],self.model,k=7)
    def test_invalid_weight(self):
        with self.assertRaises(ValueError):predict_knn([0],self.model,weighting="unknown")
    def test_bad_scale(self):
        with self.assertRaises(ValueError):fit_knn([[0]],["A"],["MQ2"],scales=[0])
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):fit_knn([[0,1]],["A"],["MQ2","MQ2"])
    def test_nan(self):
        with self.assertRaises(ValueError):predict_knn([float("nan")],self.model)
    def test_bad_dimension(self):
        with self.assertRaises(ValueError):predict_knn([0,1],self.model)
    def test_bad_labels(self):
        with self.assertRaises(ValueError):fit_knn([[0],[1]],["A"],["MQ2"])
if __name__=="__main__":unittest.main()
