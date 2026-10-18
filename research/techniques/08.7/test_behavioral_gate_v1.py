import unittest
from sim_to_real_v1 import compare
A=[[0.,2.],[2.,4.]]
B=[[2.,4.],[4.,6.]]
class BehavioralGate(unittest.TestCase):
    def test_means_sim(self): self.assertEqual(compare(A,B)["mean_sim"],[1.,3.])
    def test_means_real(self): self.assertEqual(compare(A,B)["mean_real"],[3.,5.])
    def test_mean_gap(self): self.assertAlmostEqual(compare(A,B)["mean_gap_rmse"],2.)
    def test_identical(self): self.assertEqual(compare(A,A)["mean_gap_rmse"],0)
    def test_one_dimension(self): self.assertAlmostEqual(compare([[1.],[3.]],[[2.],[4.]])["mean_gap_rmse"],1.)
    def test_different_counts(self): self.assertEqual(compare(A,[[2.,4.],[4.,6.],[3.,5.]])["mean_real"],[3.,5.])
    def test_short_sim(self):
        with self.assertRaises(ValueError): compare([[1.]],[[1.],[2.]])
    def test_short_real(self):
        with self.assertRaises(ValueError): compare([[1.],[2.]],[[1.]])
    def test_empty(self):
        with self.assertRaises(ValueError): compare([],[])
    def test_mismatched_dims(self):
        with self.assertRaises(ValueError): compare(A,[[1.],[2.]])
    def test_ragged(self):
        with self.assertRaises(ValueError): compare([[1.],[2.,3.]],[[1.],[2.]])
    def test_nonfinite(self):
        with self.assertRaises(ValueError): compare([[1.],[float("nan")]],[[1.],[2.]])
    def test_bool(self):
        with self.assertRaises(ValueError): compare([[True],[1]],[[2],[3]])
if __name__=="__main__": unittest.main()
