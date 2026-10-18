import unittest
import complex_products_v1 as m
class SmokeTests(unittest.TestCase):
    def test_export(self):self.assertTrue(callable(m.compare))
    def test_docstring(self):self.assertTrue(bool(m.__doc__))
if __name__=="__main__":unittest.main()
