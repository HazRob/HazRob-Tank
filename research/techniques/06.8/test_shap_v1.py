import unittest
import shap_v1 as m

class ExactShapleyTests(unittest.TestCase):
    def test_additive_attributions(self):
        r=m.explain(lambda x:2*x[0]+3*x[1],[0,0],[1,2])
        self.assertAlmostEqual(r["attributions"][0],2)
        self.assertAlmostEqual(r["attributions"][1],6)
    def test_output_endpoints(self):
        r=m.explain(lambda x:x[0]+x[1],[1,3],[2,4])
        self.assertEqual(r["baseline_output"],4)
        self.assertEqual(r["query_output"],6)
    def test_efficiency(self):
        r=m.explain(lambda x:x[0]*x[1],[0,0],[1,1])
        self.assertAlmostEqual(sum(r["attributions"]),r["query_output"]-r["baseline_output"])
        self.assertAlmostEqual(r["residual"],0)
    def test_interaction_split_evenly(self):
        r=m.explain(lambda x:x[0]*x[1],[0,0],[1,1])
        self.assertAlmostEqual(r["attributions"][0],.5)
        self.assertAlmostEqual(r["attributions"][1],.5)
    def test_single_feature(self):
        self.assertEqual(m.explain(lambda x:x[0]**2,[1],[3])["attributions"],[8])
    def test_identical_vectors(self):
        r=m.explain(lambda x:sum(x),[1,2],[1,2])
        self.assertEqual(r["attributions"],[0,0])
    def test_constant_function(self):
        self.assertEqual(m.explain(lambda x:2,[0,0],[9,9])["attributions"],[0,0])
    def test_mismatched_length(self):
        with self.assertRaises(ValueError):m.explain(sum,[1],[1,2])
    def test_empty_vectors(self):
        with self.assertRaises(ValueError):m.explain(sum,[],[])
    def test_too_many_features(self):
        with self.assertRaises(ValueError):m.explain(sum,[0]*9,[1]*9)
    def test_nonfinite_features(self):
        with self.assertRaises(ValueError):m.explain(sum,[float("nan")],[1])
    def test_bool_feature(self):
        with self.assertRaises(ValueError):m.explain(sum,[True],[1])
    def test_invalid_predictor_result(self):
        with self.assertRaises(ValueError):m.explain(lambda x:float("inf"),[0],[1])
    def test_invalid_max_features(self):
        with self.assertRaises(ValueError):m.explain(sum,[0],[1],max_features=True)
    def test_noncallable_predictor(self):
        with self.assertRaises(ValueError):m.explain(None,[0],[1])

if __name__=="__main__":unittest.main()
