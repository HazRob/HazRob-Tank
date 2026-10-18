import unittest
import active_sampling_v1 as m
class SmokeTests(unittest.TestCase):
    def test_export(self):self.assertTrue(callable(m.rank_waypoints))
    def test_docstring(self):self.assertTrue(bool(m.__doc__))
if __name__=="__main__":unittest.main()
