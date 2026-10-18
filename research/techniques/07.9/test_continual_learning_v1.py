import unittest
import continual_learning_v1 as m
class ResearchSmokeTests(unittest.TestCase):
    def test_primary_callable(self): self.assertTrue(callable(m.new_state))
    def test_secondary_callable(self): self.assertTrue(callable(m.update))
    def test_docstring(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
