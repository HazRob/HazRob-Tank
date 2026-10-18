import unittest
from active_learning_doe_v1 import rank_candidates
def c(p,ppm): return {"probabilities":p,"ppm":ppm}
class BehavioralGate(unittest.TestCase):
    def test_uncertainty_priority(self):
        r=rank_candidates([c([.99,.01],100),c([.5,.5],100)]);self.assertEqual(r[0]["index"],1)
    def test_normalized(self): self.assertAlmostEqual(rank_candidates([c([.5,.5],10)])[0]["uncertainty"],.5)
    def test_tie_order(self): self.assertEqual([r["index"] for r in rank_candidates([c([.5,.5],10),c([.5,.5],10)])],[0,1])
    def test_lower_ppm_coverage(self): self.assertEqual(rank_candidates([c([.5,.5],100),c([.5,.5],10)])[0]["index"],1)
    def test_no_weight(self): self.assertEqual(rank_candidates([c([.5,.5],100),c([.5,.5],10)],concentration_weight=0)[0]["index"],0)
    def test_empty(self): self.assertEqual(rank_candidates([]),[])
    def test_sum_bad(self):
        with self.assertRaises(ValueError): rank_candidates([c([.1,.2],10)])
    def test_negative_probability(self):
        with self.assertRaises(ValueError): rank_candidates([c([-1.,2.],10)])
    def test_boolean_probability(self):
        with self.assertRaises(ValueError): rank_candidates([c([True,0],10)])
    def test_bool_ppm(self):
        with self.assertRaises(ValueError): rank_candidates([c([.5,.5],True)])
    def test_zero_ppm(self):
        with self.assertRaises(ValueError): rank_candidates([c([.5,.5],0)])
    def test_nan_ppm(self):
        with self.assertRaises(ValueError): rank_candidates([c([.5,.5],float("nan"))])
    def test_bad_weight(self):
        with self.assertRaises(ValueError): rank_candidates([c([.5,.5],10)],concentration_weight=-1)
    def test_boolean_weight(self):
        with self.assertRaises(ValueError): rank_candidates([c([.5,.5],10)],concentration_weight=True)
    def test_missing_probabilities(self):
        with self.assertRaises(ValueError): rank_candidates([{"ppm":1}])
if __name__=="__main__":unittest.main()
