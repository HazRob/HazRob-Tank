import unittest
from open_set_recognition_v1 import fit_open_set,predict_open_set,evaluate_open_set
DATA=[("a",[0,0]),("a",[0.1,0]),("a",[0,0.1]),("b",[5,5]),("b",[5.1,5]),("b",[5,5.1])]
class Tests(unittest.TestCase):
    def setUp(self):self.m=fit_open_set(DATA)
    def test_classes(self):self.assertEqual(self.m["known_classes"],["a","b"])
    def test_features(self):self.assertEqual(self.m["n_features"],2)
    def test_known(self):self.assertEqual(predict_open_set(self.m,[0,0])["candidate"],"a")
    def test_other(self):self.assertEqual(predict_open_set(self.m,[5,5])["candidate"],"b")
    def test_unknown(self):self.assertEqual(predict_open_set(self.m,[50,50])["status"],"UNKNOWN_OR_UNSUPPORTED")
    def test_unknown_none(self):self.assertIsNone(predict_open_set(self.m,[50,50])["candidate"])
    def test_nearest(self):self.assertEqual(predict_open_set(self.m,[50,50])["nearest_known"],"b")
    def test_metrics(self):self.assertEqual(evaluate_open_set(self.m,[[0,0]],[[50,50]])["unknown_reject_rate"],1)
    def test_bad_quantile(self):
        with self.assertRaises(ValueError):fit_open_set(DATA,rejection_quantile=1)
    def test_single_class(self):
        with self.assertRaises(ValueError):fit_open_set(DATA[:3])
    def test_few_examples(self):
        with self.assertRaises(ValueError):fit_open_set([("a",[0]),("b",[2])])
    def test_nonfinite(self):
        with self.assertRaises(ValueError):fit_open_set([("a",[float("nan")])]+DATA)
    def test_wrong_dimensions(self):
        with self.assertRaises(ValueError):predict_open_set(self.m,[0])
    def test_bad_test_split(self):
        with self.assertRaises(ValueError):evaluate_open_set(self.m,[],[[50,50]])
    def test_determinism(self):self.assertEqual(fit_open_set(DATA),fit_open_set(DATA))
    def test_threshold(self):self.assertGreaterEqual(self.m["threshold"],0)
if __name__=="__main__":unittest.main()
