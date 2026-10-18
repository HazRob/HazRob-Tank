import unittest
from transfer_learning_v1 import fit_head,predict
DATA=[[0.,0.],[0.,2.],[10.,10.],[10.,12.]]
Y=["ethanol","ethanol","acetone","acetone"]
class BehavioralGate(unittest.TestCase):
    def test_classes(self): self.assertEqual(set(fit_head(DATA,Y)["centroids"]),set(Y))
    def test_dims(self): self.assertEqual(fit_head(DATA,Y)["n_features"],2)
    def test_centroid(self): self.assertEqual(fit_head(DATA,Y)["centroids"]["ethanol"],[0.,1.])
    def test_ethanol(self): self.assertEqual(predict(fit_head(DATA,Y),[0.,1.]),"ethanol")
    def test_acetone(self): self.assertEqual(predict(fit_head(DATA,Y),[10.,11.]),"acetone")
    def test_tie_deterministic(self):
        m=fit_head([[0.],[2.]],["a","b"]);self.assertEqual(predict(m,[1.]),"a")
    def test_single_class(self):
        with self.assertRaises(ValueError): fit_head([[1.],[2.]],["a","a"])
    def test_empty(self):
        with self.assertRaises(ValueError): fit_head([],[])
    def test_ragged(self):
        with self.assertRaises(ValueError): fit_head([[1.],[2.,3.]],["a","b"])
    def test_nan(self):
        with self.assertRaises(ValueError): fit_head([[1.],[float("nan")]],["a","b"])
    def test_bad_ridge(self):
        with self.assertRaises(ValueError): fit_head(DATA,Y,ridge=0)
    def test_query_dimension(self):
        with self.assertRaises(ValueError): predict(fit_head(DATA,Y),[1.])
    def test_query_nan(self):
        with self.assertRaises(ValueError): predict(fit_head(DATA,Y),[1.,float("nan")])
    def test_query_bool(self):
        with self.assertRaises(ValueError): predict(fit_head(DATA,Y),[True,1.])
if __name__=="__main__":unittest.main()
