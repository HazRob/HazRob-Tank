import unittest
from tcn_v1 import causal_conv,residual_block
class BehavioralGate(unittest.TestCase):
    def test_past_only(self): self.assertEqual(causal_conv([1,2,3],[1,1]),[1,3,5])
    def test_dilated(self): self.assertEqual(causal_conv([1,2,3],[1,1],dilation=2),[1,2,4])
    def test_identity(self): self.assertEqual(causal_conv([1,2,3],[1]),[1,2,3])
    def test_long_kernel(self): self.assertEqual(causal_conv([1],[1,2,3]),[1])
    def test_residual(self): self.assertEqual(residual_block([1,2],[1]),[2,4])
    def test_relu_inside_residual(self): self.assertEqual(residual_block([1,2],[-1]),[1,2])
    def test_zero_kernel(self): self.assertEqual(causal_conv([1,2],[0]),[0,0])
    def test_empty_signal(self):
        with self.assertRaises(ValueError): causal_conv([],[1])
    def test_empty_kernel(self):
        with self.assertRaises(ValueError): causal_conv([1],[])
    def test_bad_dilation(self):
        with self.assertRaises(ValueError): causal_conv([1],[1],dilation=0)
    def test_bool_dilation(self):
        with self.assertRaises(ValueError): causal_conv([1],[1],dilation=True)
    def test_bool_feature(self):
        with self.assertRaises(ValueError): causal_conv([True],[1])
    def test_nan_kernel(self):
        with self.assertRaises(ValueError): causal_conv([1],[float("nan")])
if __name__=="__main__": unittest.main()
