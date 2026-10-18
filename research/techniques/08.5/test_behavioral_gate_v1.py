import unittest
from temporal_ensemble_v1 import ensemble
P=[[.9,.1],[.1,.9]]
class BehavioralGate(unittest.TestCase):
    def test_equal_average(self): self.assertEqual(ensemble(P)["probabilities"],[.5,.5])
    def test_tie_order(self): self.assertEqual(ensemble(P)["candidate_index"],0)
    def test_weighting(self): self.assertAlmostEqual(ensemble(P,weights=[1,3])["probabilities"][1],.7)
    def test_zero_weight(self): self.assertEqual(ensemble(P,weights=[1,0])["candidate_index"],0)
    def test_single_row(self): self.assertEqual(ensemble([[.2,.8]])["candidate_index"],1)
    def test_one_class(self):
        with self.assertRaises(ValueError): ensemble([[1.]])
    def test_empty(self):
        with self.assertRaises(ValueError): ensemble([])
    def test_bad_sum(self):
        with self.assertRaises(ValueError): ensemble([[.1,.1]])
    def test_negative(self):
        with self.assertRaises(ValueError): ensemble([[-1,2]])
    def test_nan(self):
        with self.assertRaises(ValueError): ensemble([[float("nan"),0]])
    def test_boolean_probability(self):
        with self.assertRaises(ValueError): ensemble([[True,0]])
    def test_ragged(self):
        with self.assertRaises(ValueError): ensemble([[.5,.5],[.3,.3,.4]])
    def test_weight_mismatch(self):
        with self.assertRaises(ValueError): ensemble(P,weights=[1])
    def test_zero_total_weight(self):
        with self.assertRaises(ValueError): ensemble(P,weights=[0,0])
    def test_boolean_weight(self):
        with self.assertRaises(ValueError): ensemble(P,weights=[1,True])
if __name__=="__main__": unittest.main()
