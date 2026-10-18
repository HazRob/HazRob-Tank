import unittest
import gas_mapping_v1 as m
class SmokeTests(unittest.TestCase):
    def test_export(self):self.assertTrue(callable(m.grid_map))
    def test_docstring(self):self.assertTrue(bool(m.__doc__))
if __name__=="__main__":unittest.main()
