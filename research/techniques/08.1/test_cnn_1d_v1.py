import unittest
import cnn_1d_v1 as m
class ResearchSmokeTests(unittest.TestCase):
    def test_primary_callable(self): self.assertTrue(callable(m.convolve_valid))
    def test_secondary_callable(self): self.assertTrue(callable(m.global_average_pool))
    def test_docstring(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
