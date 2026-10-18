import unittest
from discriminant_v1 import fit_binary_lda,fit_binary_plsda,project
class DiscriminantTests(unittest.TestCase):
    def setUp(self):
        self.x=[[0,0],[0,1],[10,0],[10,1]];self.y=["A","A","B","B"]
    def test_lda_separation(self):
        s=project(self.x,fit_binary_lda(self.x,self.y));self.assertLess(max(s[:2]),min(s[2:]))
    def test_pls_separation(self):
        s=project(self.x,fit_binary_plsda(self.x,self.y));self.assertLess(max(s[:2]),min(s[2:]))
    def test_lda_dim(self):self.assertEqual(len(fit_binary_lda(self.x,self.y)["direction"]),2)
    def test_pls_dim(self):self.assertEqual(len(fit_binary_plsda(self.x,self.y)["direction"]),2)
    def test_projection(self):self.assertEqual(len(project([[20,1]],fit_binary_lda(self.x,self.y))),1)
    def test_train_only(self):
        m=fit_binary_plsda(self.x,self.y);self.assertEqual(m["center"],[5,.5])
    def test_multiclass_rejected(self):
        with self.assertRaises(ValueError):fit_binary_lda(self.x,["A","A","B","C"])
    def test_short_class(self):
        with self.assertRaises(ValueError):fit_binary_plsda(self.x,["A","A","A","B"])
    def test_bad_ridge(self):
        with self.assertRaises(ValueError):fit_binary_lda(self.x,self.y,ridge=0)
    def test_ragged(self):
        with self.assertRaises(ValueError):fit_binary_lda([[0],[1,2],[3],[4]],self.y)
    def test_nan(self):
        with self.assertRaises(ValueError):fit_binary_plsda([[0,0],[0,1],[10,float("nan")],[10,1]],self.y)
    def test_mismatch(self):
        with self.assertRaises(ValueError):project([[1,2,3]],fit_binary_lda(self.x,self.y))
    def test_identical_centroids(self):
        with self.assertRaises(ValueError):fit_binary_lda([[0],[1],[0],[1]],self.y)
    def test_zero_covariance(self):
        with self.assertRaises(ValueError):fit_binary_plsda([[0],[1],[0],[1]],self.y)
if __name__=="__main__":unittest.main()
