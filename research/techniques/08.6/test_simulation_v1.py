import unittest
import simulation_v1 as m
class SmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.simulate))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
