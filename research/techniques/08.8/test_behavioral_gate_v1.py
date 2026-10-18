import unittest
from mixture_detection_v1 import assess
S={"ethanol":[1.,0.],"acetone":[0.,1.]}
class BehavioralGate(unittest.TestCase):
    def test_exact_single(self): self.assertEqual(assess([1.,0.],S)["status"],"SINGLE_SIGNATURE_COMPATIBLE")
    def test_zero_residual(self): self.assertEqual(assess([1.,0.],S)["relative_residual"],0.)
    def test_nearest(self): self.assertEqual(assess([1.,0.],S)["nearest"],"ethanol")
    def test_unexplained(self): self.assertEqual(assess([1.,1.],S,threshold=.1)["status"],"UNEXPLAINED_RESPONSE")
    def test_large_threshold(self): self.assertEqual(assess([1.,1.],S,threshold=1.)["status"],"SINGLE_SIGNATURE_COMPATIBLE")
    def test_tie_order(self): self.assertEqual(assess([1.,1.],S)["nearest"],"acetone")
    def test_empty_signal(self):
        with self.assertRaises(ValueError): assess([],S)
    def test_empty_catalog(self):
        with self.assertRaises(ValueError): assess([1.,0.],{})
    def test_bad_catalog_type(self):
        with self.assertRaises(ValueError): assess([1.,0.],[[1.,0.]])
    def test_bad_catalog_label(self):
        with self.assertRaises(ValueError): assess([1.,0.],{"":[1.,0.]})
    def test_ragged_catalog(self):
        with self.assertRaises(ValueError): assess([1.,0.],{"a":[1.]})
    def test_nan_signature(self):
        with self.assertRaises(ValueError): assess([1.,0.],{"a":[float("nan"),0.]})
    def test_bool_observed(self):
        with self.assertRaises(ValueError): assess([True,0.],S)
    def test_bool_threshold(self):
        with self.assertRaises(ValueError): assess([1.,0.],S,threshold=True)
    def test_negative_threshold(self):
        with self.assertRaises(ValueError): assess([1.,0.],S,threshold=-1)
if __name__=="__main__": unittest.main()
