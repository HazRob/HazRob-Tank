import unittest
from math import log
from fingerprints_v1 import build_fingerprint,cosine_similarity,SENSORS
class FingerprintTests(unittest.TestCase):
    def test_eight(self): self.assertEqual(len(build_fingerprint([10]*8,[20]*8)["features"]),8)
    def test_identity(self): self.assertEqual(build_fingerprint([10]*8,[10]*8)["features"],(0,)*8)
    def test_positive(self): self.assertAlmostEqual(build_fingerprint([10]*8,[20]*8)["features"][0],log(2))
    def test_negative(self): self.assertAlmostEqual(build_fingerprint([20]*8,[10]*8)["features"][0],-log(2))
    def test_order(self): self.assertEqual(build_fingerprint([10]*8,[20]*8)["sensor_ids"],SENSORS)
    def test_wrong_order(self):
        with self.assertRaises(ValueError): build_fingerprint([10]*8,[20]*8,sensor_ids=SENSORS[::-1])
    def test_short(self):
        with self.assertRaises(ValueError): build_fingerprint([10]*7,[20]*8)
    def test_zero(self):
        with self.assertRaises(ValueError): build_fingerprint([10]*8,[0]*8)
    def test_nan(self):
        with self.assertRaises(ValueError): build_fingerprint([10]*8,[float("nan")]*8)
    def test_same_cosine(self): self.assertAlmostEqual(cosine_similarity([1]*8,[1]*8),1)
    def test_opposite(self): self.assertAlmostEqual(cosine_similarity([1]*8,[-1]*8),-1)
    def test_zero_norm(self): self.assertIsNone(cosine_similarity([0]*8,[1]*8))
    def test_wrong_length(self):
        with self.assertRaises(ValueError): cosine_similarity([1]*7,[1]*8)
    def test_distinct(self): self.assertAlmostEqual(cosine_similarity([1]+[0]*7,[0,1]+[0]*6),0)
if __name__=="__main__": unittest.main()
