import unittest
from scaling_v1 import fit_scaler,transform

class ScalingTests(unittest.TestCase):
    def test_standard_center(self):
        s=fit_scaler([[1,10],[3,30]])
        self.assertEqual(transform([[2,20]],s),[[0,0]])
    def test_standard_scale(self):
        s=fit_scaler([[1],[3]])
        self.assertEqual(transform([[3]],s),[[1]])
    def test_robust_median(self):
        s=fit_scaler([[1],[2],[3],[100]],"robust")
        self.assertAlmostEqual(s["center"][0],2.5)
    def test_constant(self):
        s=fit_scaler([[5],[5]])
        self.assertEqual(transform([[5]],s),[[0]])
    def test_train_only(self):
        s=fit_scaler([[0],[2]])
        self.assertEqual(s["center"],[1])
        self.assertEqual(transform([[100]],s),[[99]])
    def test_multifeature(self):
        s=fit_scaler([[0,10],[2,30]])
        self.assertEqual(transform([[1,20]],s),[[0,0]])
    def test_bad_method(self):
        with self.assertRaises(ValueError): fit_scaler([[1]],"minmax")
    def test_empty(self):
        with self.assertRaises(ValueError): fit_scaler([])
    def test_ragged(self):
        with self.assertRaises(ValueError): fit_scaler([[1],[1,2]])
    def test_nonfinite(self):
        with self.assertRaises(ValueError): fit_scaler([[float("nan")]])
    def test_feature_mismatch(self):
        with self.assertRaises(ValueError): transform([[1,2]],fit_scaler([[1]]))
    def test_bool(self):
        with self.assertRaises(ValueError): fit_scaler([[True]])
    def test_robust_constant(self):
        s=fit_scaler([[5],[5],[5]],"robust")
        self.assertTrue(s["constant"][0])
if __name__=="__main__": unittest.main()
