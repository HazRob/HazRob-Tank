import unittest
from drift_compensation_v1 import estimate_offsets,compensate
class BehavioralGate(unittest.TestCase):
    def test_offsets(self): self.assertEqual(estimate_offsets([10,20],[13,18])["offsets"],[3,-2])
    def test_count(self): self.assertEqual(estimate_offsets([10,20],[13,18])["n"],2)
    def test_compensates(self):
        model=estimate_offsets([10,20],[13,18])
        self.assertEqual(compensate(model,[14,30]),[11,32])
    def test_identity(self):
        m=estimate_offsets([1,2],[1,2]);self.assertEqual(compensate(m,[3,4]),[3,4])
    def test_empty(self):
        with self.assertRaises(ValueError): estimate_offsets([],[])
    def test_unequal(self):
        with self.assertRaises(ValueError): estimate_offsets([1,2],[3])
    def test_nonfinite_before(self):
        with self.assertRaises(ValueError): estimate_offsets([float("nan")],[1])
    def test_bool_after(self):
        with self.assertRaises(ValueError): estimate_offsets([1],[True])
    def test_query_dimension(self):
        with self.assertRaises(ValueError): compensate(estimate_offsets([1,2],[2,3]),[2])
    def test_nonfinite_query(self):
        with self.assertRaises(ValueError): compensate(estimate_offsets([1],[2]),[float("inf")])
    def test_corrupt_model_shape(self):
        with self.assertRaises(ValueError): compensate({"n":2,"offsets":[1]},[2,3])
    def test_corrupt_model_nonfinite(self):
        with self.assertRaises(ValueError): compensate({"n":1,"offsets":[float("nan")]},[2])
    def test_missing_model(self):
        with self.assertRaises(ValueError): compensate({},[2])
    def test_boolean_n(self):
        with self.assertRaises(ValueError): compensate({"n":True,"offsets":[1]},[2])
if __name__=="__main__":unittest.main()
