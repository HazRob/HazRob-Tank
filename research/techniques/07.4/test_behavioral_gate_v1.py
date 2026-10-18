import unittest
from drift_detection_v1 import fit_baseline,detect
D=[[1.,10.],[2.,20.],[3.,30.],[4.,40.]]
class BehavioralGate(unittest.TestCase):
    def test_fit_dims(self): self.assertEqual(fit_baseline(D)["n"],2)
    def test_fit_means(self): self.assertEqual(fit_baseline(D)["means"],[2.5,25.])
    def test_fit_scales(self): self.assertTrue(all(s>0 for s in fit_baseline(D)["scales"]))
    def test_no_shift(self): self.assertEqual(detect(fit_baseline(D),[2.5,25.])["status"],"NO_SHIFT_FLAG")
    def test_shift(self): self.assertEqual(detect(fit_baseline(D),[100.,25.])["status"],"SHIFT_FLAG")
    def test_threshold_override(self): self.assertEqual(detect(fit_baseline(D),[4.,40.],threshold=100)["status"],"NO_SHIFT_FLAG")
    def test_short_fit(self):
        with self.assertRaises(ValueError): fit_baseline([[1.],[2.]])
    def test_nan_fit(self):
        with self.assertRaises(ValueError): fit_baseline([[1.],[2.],[float("nan")]])
    def test_bad_fit_shape(self):
        with self.assertRaises(ValueError): fit_baseline([[1.],[2.,3.],[3.]])
    def test_wrong_query_dimensions(self):
        with self.assertRaises(ValueError): detect(fit_baseline(D),[1.])
    def test_bad_threshold(self):
        with self.assertRaises(ValueError): detect(fit_baseline(D),[2.,20.],threshold=0)
    def test_boolean_threshold(self):
        with self.assertRaises(ValueError): detect(fit_baseline(D),[2.,20.],threshold=True)
    def test_boolean_std(self):
        with self.assertRaises(ValueError): fit_baseline(D,min_std=True)
    def test_invalid_model_zero_scale(self):
        with self.assertRaises(ValueError): detect({"n":1,"means":[0.],"scales":[0.]},[1.])
    def test_invalid_model_short_scale(self):
        with self.assertRaises(ValueError): detect({"n":2,"means":[0.,0.],"scales":[1.]},[1.,2.])
if __name__=="__main__": unittest.main()
