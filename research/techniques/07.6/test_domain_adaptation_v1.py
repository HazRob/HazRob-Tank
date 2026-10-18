import unittest
import domain_adaptation_v1 as m
class BaselineSmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.fit_alignment))
    def test_secondary_export(self): self.assertTrue(callable(m.target_to_source))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
