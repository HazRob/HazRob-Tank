import unittest
import drift_compensation_v1 as m
class BaselineSmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.estimate_offsets))
    def test_secondary_export(self): self.assertTrue(callable(m.compensate))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
