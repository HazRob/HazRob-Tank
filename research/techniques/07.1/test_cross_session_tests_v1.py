import unittest
import cross_session_tests_v1 as m
class SmokeTests(unittest.TestCase):
    def test_module_exports(self):
        self.assertTrue(callable(m.folds))
    def test_second_export(self):
        self.assertTrue(callable(m.summarize))
    def test_documented(self):
        self.assertTrue(bool(m.__doc__))
if __name__ == "__main__": unittest.main()
