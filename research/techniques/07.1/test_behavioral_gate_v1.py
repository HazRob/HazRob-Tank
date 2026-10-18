import unittest
from cross_session_tests_v1 import folds,summarize
DATA=[{"session":"s1"},{"session":"s2"},{"session":"s1"},{"session":"s3"}]
class BehavioralGate(unittest.TestCase):
    def test_holdout_count(self): self.assertEqual(len(folds(DATA)),3)
    def test_no_leakage(self):
        for f in folds(DATA):
            self.assertFalse(set(f["train_indices"]) & set(f["test_indices"]))
            self.assertEqual(sorted(f["train_indices"]+f["test_indices"]),list(range(4)))
    def test_session_order(self): self.assertEqual([f["held_out_session"] for f in folds(DATA)],["s1","s2","s3"])
    def test_grouped_repeat(self): self.assertEqual(folds(DATA)[0]["test_indices"],[0,2])
    def test_empty(self):
        with self.assertRaises(ValueError): folds([])
    def test_single_session(self):
        with self.assertRaises(ValueError): folds([{"session":"s1"}])
    def test_missing_session(self):
        with self.assertRaises(ValueError): folds([{"session":"s1"},{}])
    def test_bad_session_type(self):
        with self.assertRaises(ValueError): folds([{"session":1},{"session":"s2"}])
    def test_micro_accuracy(self):
        self.assertAlmostEqual(summarize([{"n":9,"correct":9},{"n":1,"correct":0}])["micro_accuracy"],.9)
    def test_macro_accuracy(self):
        self.assertAlmostEqual(summarize([{"n":9,"correct":9},{"n":1,"correct":0}])["macro_session_accuracy"],.5)
    def test_perfect(self): self.assertEqual(summarize([{"n":2,"correct":2}])["micro_accuracy"],1)
    def test_empty_summary(self):
        with self.assertRaises(ValueError): summarize([])
    def test_bad_correct(self):
        with self.assertRaises(ValueError): summarize([{"n":2,"correct":3}])
    def test_boolean_count_rejected(self):
        with self.assertRaises(ValueError): summarize([{"n":True,"correct":1}])
    def test_boolean_correct_rejected(self):
        with self.assertRaises(ValueError): summarize([{"n":2,"correct":True}])
if __name__=="__main__": unittest.main()
