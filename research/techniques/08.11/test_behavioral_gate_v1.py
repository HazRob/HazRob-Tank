import unittest
from interference_v1 import paired_effect
class BehavioralGate(unittest.TestCase):
    def test_zero_effect(self): self.assertEqual(paired_effect([1,2],[1,2])["mean_effect"],0)
    def test_positive_effect(self): self.assertEqual(paired_effect([1,1],[2,4])["mean_effect"],2)
    def test_negative_effect(self): self.assertEqual(paired_effect([3,4],[2,2])["mean_effect"],-1.5)
    def test_pair_count(self): self.assertEqual(paired_effect([1,2,3],[2,3,4])["n_pairs"],3)
    def test_max_abs(self): self.assertEqual(paired_effect([2,10],[0,16])["max_absolute_effect"],6)
    def test_cancel(self): self.assertEqual(paired_effect([0,0],[1,-1])["mean_effect"],0)
    def test_empty(self):
        with self.assertRaises(ValueError): paired_effect([],[])
    def test_one_pair(self):
        with self.assertRaises(ValueError): paired_effect([1],[2])
    def test_mismatch(self):
        with self.assertRaises(ValueError): paired_effect([1,2],[2])
    def test_nan(self):
        with self.assertRaises(ValueError): paired_effect([1,2],[1,float("nan")])
    def test_inf(self):
        with self.assertRaises(ValueError): paired_effect([1,float("inf")],[2,3])
    def test_bool(self):
        with self.assertRaises(ValueError): paired_effect([1,True],[2,3])
    def test_replicate_zero(self): self.assertEqual(paired_effect([0,0],[0,0])["n_pairs"],2)
if __name__=="__main__":unittest.main()
