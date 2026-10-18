import unittest
import shap_v1 as m
class SmokeTests(unittest.TestCase):
    def test_module_exports(self):
        self.assertTrue(callable(m.explain))
    def test_documented(self):
        self.assertTrue(bool(m.__doc__))
if __name__ == "__main__": unittest.main()
