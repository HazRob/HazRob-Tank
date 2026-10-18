import unittest
from probability_calibration_v1 import softmax, fit_temperature, apply_temperature, expected_calibration_error
X=[[3,0],[0,3],[2,0],[0,2]]
Y=[0,1,0,1]
class CalibrationTests(unittest.TestCase):
    def test_normalization(self): self.assertAlmostEqual(sum(softmax([2,1])),1)
    def test_order(self): self.assertGreater(softmax([2,1])[0],.5)
    def test_softening(self): self.assertLess(softmax([2,0],2)[0],softmax([2,0],1)[0])
    def test_fit(self): self.assertGreater(fit_temperature(X,Y)["temperature"],0)
    def test_count(self): self.assertEqual(fit_temperature(X,Y)["n_calibration"],4)
    def test_apply(self): self.assertEqual(len(apply_temperature([2,0],fit_temperature(X,Y))),2)
    def test_mismatch(self):
        with self.assertRaises(ValueError): apply_temperature([2,0,1],fit_temperature(X,Y))
    def test_ece_perfect(self): self.assertAlmostEqual(expected_calibration_error([[1,0],[0,1]],[0,1])["ece"],0)
    def test_ece_overconfidence(self): self.assertGreater(expected_calibration_error([[.9,.1],[.9,.1]],[0,1])["ece"],0)
    def test_single_class(self):
        with self.assertRaises(ValueError): fit_temperature(X,[0]*4)
    def test_nan(self):
        with self.assertRaises(ValueError): fit_temperature([[float("nan"),1],[1,2]],[0,1])
    def test_bad_labels(self):
        with self.assertRaises(ValueError): fit_temperature(X,[0,1,0,2])
    def test_bad_grid(self):
        with self.assertRaises(ValueError): fit_temperature(X,Y,candidates=[0])
    def test_bad_temp(self):
        with self.assertRaises(ValueError): softmax([1,2],0)
    def test_bad_sum(self):
        with self.assertRaises(ValueError): expected_calibration_error([[.2,.2]],[0])
    def test_repeatable(self): self.assertEqual(fit_temperature(X,Y),fit_temperature(X,Y))
if __name__=="__main__": unittest.main()
