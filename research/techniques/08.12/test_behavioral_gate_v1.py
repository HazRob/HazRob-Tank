import unittest
from complex_products_v1 import compare
S={"a":[1.,0.],"b":[0.,1.]}
class BehavioralGate(unittest.TestCase):
    def test_matches(self): self.assertEqual(compare([1.,0.],S)["matches"][0]["reference"],"a")
    def test_zero_distance(self): self.assertEqual(compare([1.,0.],S)["matches"][0]["distance"],0)
    def test_euclidean(self): self.assertAlmostEqual(compare([1.,1.],{"r":[0.,0.]})["matches"][0]["distance"],2**.5)
    def test_sorted(self): self.assertEqual([v["reference"] for v in compare([1,0],S)["matches"]],["a","b"])
    def test_tie(self): self.assertEqual(compare([.5,.5],S)["matches"][0]["reference"],"a")
    def test_one_reference(self): self.assertEqual(len(compare([1.,0.],{"r":[1.,0.]})["matches"]),1)
    def test_empty_sample(self):
        with self.assertRaises(ValueError): compare([],S)
    def test_empty_reference(self):
        with self.assertRaises(ValueError): compare([1.,0.],{})
    def test_bad_reference_type(self):
        with self.assertRaises(ValueError): compare([1.,0.],[[1.,0.]])
    def test_bad_reference_key(self):
        with self.assertRaises(ValueError): compare([1.,0.],{"":[1.,0.]})
    def test_reference_shape(self):
        with self.assertRaises(ValueError): compare([1.,0.],{"a":[1.]})
    def test_nonfinite_sample(self):
        with self.assertRaises(ValueError): compare([float("nan"),0.],S)
    def test_bool_ref(self):
        with self.assertRaises(ValueError): compare([1.,0.],{"a":[True,0.]})
    def test_ingredient_not_claimed(self): self.assertIn("cannot establish",compare([1.,0.],S)["warning"])
if __name__=="__main__":unittest.main()
