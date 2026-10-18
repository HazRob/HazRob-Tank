import unittest
from pca_v1 import fit_pca,transform_pca
class PCATests(unittest.TestCase):
    def setUp(self):self.x=[[-2,0],[-1,0],[1,0],[2,0]]
    def test_first_axis(self):
        p=fit_pca(self.x,1)
        self.assertAlmostEqual(abs(p["components"][0][0]),1)
    def test_variance(self):self.assertAlmostEqual(fit_pca(self.x,1)["explained_variance_ratio"][0],1)
    def test_center(self):self.assertEqual(fit_pca(self.x,1)["mean"],[0,0])
    def test_transform(self):self.assertEqual(len(transform_pca(self.x,fit_pca(self.x,1))),4)
    def test_two(self):self.assertEqual(len(fit_pca(self.x,2)["components"]),2)
    def test_train_only(self):
        p=fit_pca(self.x,1);self.assertEqual(p["mean"],[0,0])
        self.assertEqual(len(transform_pca([[100,0],[200,0]],p)),2)
    def test_constant(self):self.assertEqual(fit_pca([[1,1],[1,1],[1,1]],1)["explained_variance_ratio"],[0])
    def test_invalid_k(self):
        with self.assertRaises(ValueError):fit_pca(self.x,3)
    def test_too_few(self):
        with self.assertRaises(ValueError):fit_pca([[1,2]],1)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_pca([[1,2],[float("nan"),3]],1)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_pca([[1,2],[1]],1)
    def test_mismatch(self):
        with self.assertRaises(ValueError):transform_pca([[1,2,3],[2,3,4]],fit_pca(self.x,1))
    def test_correlated(self):
        p=fit_pca([[1,1],[2,2],[3,3],[4,4]],1)
        self.assertAlmostEqual(p["explained_variance_ratio"][0],1)
    def test_two_samples(self):self.assertEqual(fit_pca([[1,2],[2,3]],1)["n_train"],2)
if __name__=="__main__":unittest.main()
