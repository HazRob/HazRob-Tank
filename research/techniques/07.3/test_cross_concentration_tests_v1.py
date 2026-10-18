import unittest
import cross_concentration_tests_v1 as m
class BaselineSmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.leave_one_level_out))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
