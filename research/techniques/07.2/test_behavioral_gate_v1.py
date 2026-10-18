import unittest
from cross_device_tests_v1 import leave_one_device_out,summarize
DATA=[{"device":"A"},{"device":"B"},{"device":"A"},{"device":"C"}]
class BehavioralGate(unittest.TestCase):
    def test_nfolds(self): self.assertEqual(len(leave_one_device_out(DATA)),3)
    def test_device_order(self): self.assertEqual([f["device"] for f in leave_one_device_out(DATA)],["A","B","C"])
    def test_disjoint(self):
        for f in leave_one_device_out(DATA):
            self.assertFalse(set(f["train_indices"])&set(f["test_indices"]))
            self.assertEqual(sorted(f["train_indices"]+f["test_indices"]),list(range(4)))
    def test_device_repeat(self): self.assertEqual(leave_one_device_out(DATA)[0]["test_indices"],[0,2])
    def test_empty(self):
        with self.assertRaises(ValueError): leave_one_device_out([])
    def test_single_device(self):
        with self.assertRaises(ValueError): leave_one_device_out([{"device":"A"}])
    def test_bad_id(self):
        with self.assertRaises(ValueError): leave_one_device_out([{"device":"A"},{"device":None}])
    def test_summary_micro(self): self.assertAlmostEqual(summarize([{"total":9,"correct":9},{"total":1,"correct":0}])["micro_accuracy"],.9)
    def test_summary_macro(self): self.assertAlmostEqual(summarize([{"total":9,"correct":9},{"total":1,"correct":0}])["macro_accuracy"],.5)
    def test_summary_n(self): self.assertEqual(summarize([{"total":2,"correct":1}])["n_devices"],1)
    def test_empty_summary(self):
        with self.assertRaises(ValueError): summarize([])
    def test_wrong_counts(self):
        with self.assertRaises(ValueError): summarize([{"total":2,"correct":3}])
    def test_boolean_total(self):
        with self.assertRaises(ValueError): summarize([{"total":True,"correct":0}])
    def test_boolean_correct(self):
        with self.assertRaises(ValueError): summarize([{"total":2,"correct":True}])
if __name__=="__main__": unittest.main()
