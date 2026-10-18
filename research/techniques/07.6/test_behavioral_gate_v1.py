import unittest
from domain_adaptation_v1 import fit_alignment,target_to_source
SOURCE=[[0.,0.],[1.,2.],[2.,4.]]
TARGET=[[10.,20.],[11.,24.],[12.,28.]]
class BehavioralGate(unittest.TestCase):
    def test_dimensions(self): self.assertEqual(fit_alignment(SOURCE,TARGET)["n"],2)
    def test_marginal_mean(self): self.assertEqual(fit_alignment(SOURCE,TARGET)["source_mean"],[1.,2.])
    def test_target_centroid(self): self.assertEqual(target_to_source(fit_alignment(SOURCE,TARGET),[11.,24.]),[1.,2.])
    def test_target_endpoint(self): self.assertEqual(target_to_source(fit_alignment(SOURCE,TARGET),[12.,28.]),[2.,4.])
    def test_short_source(self):
        with self.assertRaises(ValueError): fit_alignment([[1.],[2.]],[[4.],[5.],[6.]])
    def test_different_dimensions(self):
        with self.assertRaises(ValueError): fit_alignment(SOURCE,[[1.],[2.],[3.]])
    def test_constant_dimension(self):
        with self.assertRaises(ValueError): fit_alignment([[1.],[1.],[1.]],[[2.],[3.],[4.]])
    def test_nonfinite_source(self):
        with self.assertRaises(ValueError): fit_alignment([[1.],[2.],[float("nan")]],[[2.],[3.],[4.]])
    def test_bad_query_dim(self):
        with self.assertRaises(ValueError): target_to_source(fit_alignment(SOURCE,TARGET),[11.])
    def test_query_boolean(self):
        with self.assertRaises(ValueError): target_to_source(fit_alignment(SOURCE,TARGET),[True,1.])
    def test_corrupt_std(self):
        m=fit_alignment(SOURCE,TARGET);m["target_std"][0]=0.
        with self.assertRaises(ValueError): target_to_source(m,[1.,2.])
    def test_corrupt_model_shape(self):
        m=fit_alignment(SOURCE,TARGET);m["source_mean"]=[1.]
        with self.assertRaises(ValueError): target_to_source(m,[1.,2.])
    def test_missing_model(self):
        with self.assertRaises(ValueError): target_to_source({},[1.,2.])
    def test_boolean_n(self):
        with self.assertRaises(ValueError): target_to_source({"n":True,"source_mean":[0],"source_std":[1],"target_mean":[0],"target_std":[1]},[1])
if __name__=="__main__":unittest.main()
