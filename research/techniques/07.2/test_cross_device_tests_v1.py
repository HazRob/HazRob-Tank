import unittest
import cross_device_tests_v1 as m
class BaselineSmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.leave_one_device_out))
    def test_secondary_export(self): self.assertTrue(callable(m.summarize))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
