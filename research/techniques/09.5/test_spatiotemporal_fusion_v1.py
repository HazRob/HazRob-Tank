import unittest
import spatiotemporal_fusion_v1 as m
class ResearchTests(unittest.TestCase):
    def test_export(self):self.assertTrue(callable(m.fuse))
    def test_doc(self):self.assertTrue(bool(m.__doc__))
if __name__=="__main__":unittest.main()
