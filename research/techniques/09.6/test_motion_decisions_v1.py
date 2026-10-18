import unittest
import motion_decisions_v1 as m
class ResearchTests(unittest.TestCase):
    def test_export(self):self.assertTrue(callable(m.recommend))
    def test_doc(self):self.assertTrue(bool(m.__doc__))
if __name__=="__main__":unittest.main()
