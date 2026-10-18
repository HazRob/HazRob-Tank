import unittest
import active_learning_doe_v1 as m
class ResearchSmokeTests(unittest.TestCase):
    def test_primary_callable(self): self.assertTrue(callable(m.rank_candidates))
    def test_docstring(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
