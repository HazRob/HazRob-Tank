import unittest
import ood_detection_v1 as m

class OODTests(unittest.TestCase):
    def setUp(self):
        self.rows=[[0.0,1.0],[1.0,2.0],[2.0,3.0]]
    def test_fit_dimensions(self):
        model=m.fit(self.rows)
        self.assertEqual(model["n"],2)
    def test_inside_envelope(self):
        self.assertEqual(m.screen(m.fit(self.rows),[1,2])["status"],"INSIDE_ENVELOPE_NOT_PROOF_OF_ID")
    def test_high_outside(self):
        self.assertEqual(m.screen(m.fit(self.rows,0),[3,2])["violating_features"],[0])
    def test_low_outside(self):
        self.assertEqual(m.screen(m.fit(self.rows,0),[1,0])["violating_features"],[1])
    def test_both_channels_outside(self):
        self.assertEqual(m.screen(m.fit(self.rows,0),[-1,5])["violating_features"],[0,1])
    def test_boundary_inside(self):
        self.assertEqual(m.screen(m.fit(self.rows,0),[0,3])["violating_features"],[])
    def test_margin_expands_envelope(self):
        self.assertEqual(m.screen(m.fit(self.rows,.2),[-.3,2])["violating_features"],[])
    def test_constant_feature(self):
        self.assertEqual(m.screen(m.fit([[1],[1],[1]],0),[2])["violating_features"],[0])
    def test_not_enough_training(self):
        with self.assertRaises(ValueError):m.fit([[1],[2]])
    def test_ragged_training(self):
        with self.assertRaises(ValueError):m.fit([[1],[1,2],[3]])
    def test_nan_training(self):
        with self.assertRaises(ValueError):m.fit([[1],[2],[float("nan")]])
    def test_bool_training(self):
        with self.assertRaises(ValueError):m.fit([[1],[2],[True]])
    def test_invalid_margin(self):
        with self.assertRaises(ValueError):m.fit(self.rows,-.1)
    def test_invalid_query_size(self):
        with self.assertRaises(ValueError):m.screen(m.fit(self.rows),[1])
    def test_nonfinite_query(self):
        with self.assertRaises(ValueError):m.screen(m.fit(self.rows),[float("inf"),1])

if __name__=="__main__":unittest.main()
