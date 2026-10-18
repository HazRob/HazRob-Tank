import unittest
import lstm_gru_v1 as m
class SmokeTests(unittest.TestCase):
    def test_primary_export(self): self.assertTrue(callable(m.gru_scalar_step))
    def test_secondary_export(self): self.assertTrue(callable(m.sigmoid))
    def test_module_doc(self): self.assertTrue(bool(m.__doc__))
if __name__=="__main__": unittest.main()
