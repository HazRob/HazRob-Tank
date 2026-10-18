import unittest
from cnn_1d_v1 import convolve_valid,global_average_pool
class BehavioralGate(unittest.TestCase):
    def test_linear(self): self.assertEqual(convolve_valid([1,2,3],[1,1],activation="linear"),[3.,5.])
    def test_relu_clipping(self): self.assertEqual(convolve_valid([1,2,3],[1,-1]),[0,0])
    def test_single_point(self): self.assertEqual(convolve_valid([2],[3],activation="linear"),[6.])
    def test_bias(self): self.assertEqual(convolve_valid([1,1],[1,1],bias=3,activation="linear"),[5])
    def test_kernel_longer(self):
        with self.assertRaises(ValueError): convolve_valid([1],[1,2])
    def test_empty_kernel(self):
        with self.assertRaises(ValueError): convolve_valid([1],[])
    def test_bad_activation(self):
        with self.assertRaises(ValueError): convolve_valid([1],[1],activation="unknown")
    def test_nan_input(self):
        with self.assertRaises(ValueError): convolve_valid([float("nan")],[1])
    def test_bool_input(self):
        with self.assertRaises(ValueError): convolve_valid([True],[1])
    def test_pool(self): self.assertEqual(global_average_pool([2,4,6]),4)
    def test_pool_negative(self): self.assertEqual(global_average_pool([-1,1]),0)
    def test_pool_empty(self):
        with self.assertRaises(ValueError): global_average_pool([])
    def test_pool_inf(self):
        with self.assertRaises(ValueError): global_average_pool([float("inf")])
if __name__=="__main__": unittest.main()
