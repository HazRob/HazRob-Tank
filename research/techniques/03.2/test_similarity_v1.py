import unittest
from similarity_v1 import compare, nearest_reference

class SimilarityTests(unittest.TestCase):
    def test_identical(self):
        self.assertEqual(compare([1, 2], [1, 2])['euclidean_distance'], 0)
    def test_cosine_same(self):
        self.assertAlmostEqual(compare([1, 2], [2, 4])['cosine_similarity'], 1)
    def test_cosine_opposite(self):
        self.assertAlmostEqual(compare([1, 2], [-1, -2])['cosine_similarity'], -1)
    def test_cosine_orthogonal(self):
        self.assertAlmostEqual(compare([1, 0], [0, 1])['cosine_similarity'], 0)
    def test_cosine_zero(self):
        self.assertIsNone(compare([0, 0], [1, 1])['cosine_similarity'])
    def test_euclidean(self):
        self.assertEqual(compare([0, 0], [3, 4])['euclidean_distance'], 5)
    def test_manhattan(self):
        self.assertEqual(compare([0, 0], [3, 4])['manhattan_distance'], 7)
    def test_scaled(self):
        self.assertAlmostEqual(compare([0, 0], [3, 4], scales=[3, 4])['scaled_euclidean_distance'], 2**0.5)
    def test_symmetry(self):
        self.assertEqual(compare([2, 3], [5, 7]), compare([5, 7], [2, 3]))
    def test_dimension_mismatch(self):
        with self.assertRaises(ValueError): compare([1], [1, 2])
    def test_nonfinite(self):
        with self.assertRaises(ValueError): compare([float('nan')], [1])
    def test_bad_scale(self):
        with self.assertRaises(ValueError): compare([1], [2], scales=[0])
    def test_nearest(self):
        self.assertEqual(nearest_reference([1, 1], [[4, 4], [1, 2]])['index'], 1)
    def test_tie(self):
        self.assertEqual(nearest_reference([0], [[-1], [1]])['index'], 0)
    def test_empty_refs(self):
        with self.assertRaises(ValueError): nearest_reference([1], [])
    def test_bool(self):
        with self.assertRaises(ValueError): compare([True], [1])

if __name__ == '__main__':
    unittest.main()
