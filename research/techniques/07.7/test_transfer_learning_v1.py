import unittest
import transfer_learning_v1 as m
class ResearchSmokeTests(unittest.TestCase):
    def test_primary_callable(self): self.assertTrue(callable(m.fit_head))
    def test_secondary_callable(self): self.assertTrue(callable(m.predict))
    def test_docstring(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
