import unittest
from source_localization_v1 import weighted_centroid
class BehavioralGate(unittest.TestCase):
    def test_midpoint(self): self.assertEqual(weighted_centroid([(0,0,1),(2,0,1)])["x"],1)
    def test_weighted_x(self): self.assertEqual(weighted_centroid([(0,0,1),(3,0,2)])["x"],2)
    def test_weighted_y(self): self.assertEqual(weighted_centroid([(0,0,1),(0,3,2)])["y"],2)
    def test_zero_weight_ignored(self): self.assertEqual(weighted_centroid([(0,0,0),(3,4,1)])["x"],3)
    def test_negative_coordinates(self): self.assertEqual(weighted_centroid([(-2,0,1),(2,0,1)])["x"],0)
    def test_uniform(self): self.assertEqual(weighted_centroid([(0,0,1),(0,0,1)])["y"],0)
    def test_single_sample(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0,1)])
    def test_empty(self):
        with self.assertRaises(ValueError): weighted_centroid([])
    def test_all_zero(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0,0),(1,1,0)])
    def test_negative_response(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0,-1),(1,1,2)])
    def test_invalid_shape(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0),(1,1,2)])
    def test_nonfinite(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0,1),(1,float("nan"),2)])
    def test_bool_response(self):
        with self.assertRaises(ValueError): weighted_centroid([(0,0,True),(1,1,2)])
    def test_not_claim_source(self): self.assertIn("not leak-source",weighted_centroid([(0,0,1),(1,1,2)])["warning"])
if __name__=="__main__":unittest.main()
