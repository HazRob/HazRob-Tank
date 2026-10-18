import unittest
from class_overlap_v1 import overlap_report
class OverlapTests(unittest.TestCase):
    def test_separated(self):
        x=[[0],[.1],[10],[10.1]];r=overlap_report(x,["A","A","B","B"])
        self.assertEqual(r["ambiguous_fraction"],0)
    def test_interleaved(self):
        r=overlap_report([[0],[2],[1],[3]],["A","A","B","B"])
        self.assertEqual(r["ambiguous_fraction"],1)
    def test_tie_is_ambiguous(self):
        r=overlap_report([[0],[0],[0],[0]],["A","A","B","B"])
        self.assertEqual(r["ambiguous_fraction"],1)
    def test_class_counts(self):
        r=overlap_report([[0],[1],[10],[11]],["A","A","B","B"])
        self.assertEqual(r["by_class"]["A"]["n"],2)
    def test_margin(self):
        r=overlap_report([[0],[1],[10],[11]],["A","A","B","B"])
        self.assertGreater(r["details"][0]["margin"],0)
    def test_multidimensional(self):
        self.assertEqual(overlap_report([[0,0],[1,0],[10,10],[11,10]],["A","A","B","B"])["n_samples"],4)
    def test_scaled(self):
        r=overlap_report([[0,0],[1,0],[0,10],[0,11]],["A","A","B","B"],scales=[1,10])
        self.assertEqual(r["n_samples"],4)
    def test_bad_scale(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1],[10],[11]],["A","A","B","B"],scales=[0])
    def test_missing_class(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1],[2],[3]],["A"]*4)
    def test_one_per_class(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1]],["A","B"])
    def test_nan(self):
        with self.assertRaises(ValueError):overlap_report([[0],[float("nan")],[10],[11]],["A","A","B","B"])
    def test_ragged(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1,2],[10],[11]],["A","A","B","B"])
    def test_length(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1],[10],[11]],["A","A","B"])
    def test_empty(self):
        with self.assertRaises(ValueError):overlap_report([],[])
    def test_nonstring_label(self):
        with self.assertRaises(ValueError):overlap_report([[0],[1],[10],[11]],[1,1,2,2])
    def test_permutation_invariance(self):
        x=[[0],[1],[10],[11]];y=["A","A","B","B"]
        self.assertEqual(overlap_report(x,y)["ambiguous_fraction"],overlap_report(x[::-1],y[::-1])["ambiguous_fraction"])
if __name__=="__main__":unittest.main()
