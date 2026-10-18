import unittest
import tcn_v1 as m
class SmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.causal_conv))
    def test_secondary_export(self): self.assertTrue(callable(m.residual_block))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
