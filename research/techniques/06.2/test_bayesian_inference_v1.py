import unittest
from bayesian_inference_v1 import update_posterior
P={"a":.5,"b":.5}
def e(i,a,b,independent=True):return {"id":i,"likelihoods":{"a":a,"b":b},"conditional_independence_justified":independent}
class BayesTests(unittest.TestCase):
    def test_prior_only(self):self.assertEqual(update_posterior(P,[])["status"],"PRIOR_ONLY")
    def test_one(self):self.assertAlmostEqual(update_posterior(P,[e("x",.8,.2)])["posterior"]["a"],.8)
    def test_two(self):self.assertAlmostEqual(update_posterior(P,[e("x",.8,.2),e("y",.6,.4)])["posterior"]["a"],.85714285714)
    def test_prior(self):self.assertGreater(update_posterior({"a":.9,"b":.1},[e("x",.6,.4)])["posterior"]["a"],.9)
    def test_rank(self):self.assertEqual(update_posterior(P,[e("x",.8,.2)])["ranking"][0],"a")
    def test_sum(self):self.assertAlmostEqual(sum(update_posterior(P,[e("x",.8,.2)])["posterior"].values()),1)
    def test_zero_likelihood(self):self.assertLess(update_posterior(P,[e("x",0,.9)])["posterior"]["a"],1e-10)
    def test_duplicate(self):
        with self.assertRaises(ValueError):update_posterior(P,[e("x",.8,.2),e("x",.8,.2)])
    def test_dependent(self):
        with self.assertRaises(ValueError):update_posterior(P,[e("x",.8,.2),e("y",.8,.2,False)])
    def test_single_unjustified(self):self.assertEqual(update_posterior(P,[e("x",.8,.2,False)])["n_evidence"],1)
    def test_bad_prior(self):
        with self.assertRaises(ValueError):update_posterior({"a":.2,"b":.2},[])
    def test_bad_likelihood(self):
        with self.assertRaises(ValueError):update_posterior(P,[e("x",1.1,.2)])
    def test_missing_label(self):
        with self.assertRaises(ValueError):update_posterior(P,[{"id":"x","likelihoods":{"a":.8}}])
    def test_all_zero(self):
        with self.assertRaises(ValueError):update_posterior(P,[e("x",0,0)])
    def test_bad_floor(self):
        with self.assertRaises(ValueError):update_posterior(P,[e("x",.8,.2)],min_likelihood=0)
    def test_deterministic(self):self.assertEqual(update_posterior(P,[e("x",.8,.2)]),update_posterior(P,[e("x",.8,.2)]))
if __name__=="__main__":unittest.main()
