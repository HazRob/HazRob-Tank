import unittest
from conformal_prediction_v1 import fit_split_conformal,predict_set,evaluate_sets
P=[[.9,.1],[.1,.9],[.8,.2],[.2,.8],[.7,.3],[.3,.7],[.85,.15],[.15,.85],[.75,.25],[.25,.75]]
Y=[0,1,0,1,0,1,0,1,0,1]
class ConformalTests(unittest.TestCase):
    def test_fit(self):self.assertEqual(fit_split_conformal(P,Y)["n_calibration"],10)
    def test_classes(self):self.assertEqual(fit_split_conformal(P,Y)["n_classes"],2)
    def test_rank(self):self.assertEqual(fit_split_conformal(P,Y,alpha=.2)["quantile_rank"],9)
    def test_threshold(self):self.assertAlmostEqual(fit_split_conformal(P,Y,alpha=.2)["threshold"],.3)
    def test_infinite(self):self.assertEqual(fit_split_conformal(P,Y,alpha=.05)["threshold"],float("inf"))
    def test_singleton(self):self.assertEqual(predict_set([.9,.1],fit_split_conformal(P,Y,alpha=.2))["candidate_indices"],[0])
    def test_both(self):self.assertEqual(predict_set([.5,.5],fit_split_conformal(P,Y,alpha=.05))["set_size"],2)
    def test_coverage(self):self.assertEqual(evaluate_sets([[0],[1]],[0,1])["empirical_coverage"],1)
    def test_mean_size(self):self.assertEqual(evaluate_sets([[0,1],[1]],[0,1])["mean_set_size"],1.5)
    def test_bad_alpha(self):
        with self.assertRaises(ValueError):fit_split_conformal(P,Y,alpha=1)
    def test_bad_sum(self):
        with self.assertRaises(ValueError):fit_split_conformal([[.4,.4]], [0])
    def test_bad_label(self):
        with self.assertRaises(ValueError):fit_split_conformal(P,[2]*10)
    def test_bad_count(self):
        with self.assertRaises(ValueError):fit_split_conformal(P,Y[:-1])
    def test_bad_predict_shape(self):
        with self.assertRaises(ValueError):predict_set([.1,.2,.7],fit_split_conformal(P,Y))
    def test_bad_eval(self):
        with self.assertRaises(ValueError):evaluate_sets([[True]],[0])
    def test_repeatable(self):self.assertEqual(fit_split_conformal(P,Y),fit_split_conformal(P,Y))
if __name__=="__main__":unittest.main()
