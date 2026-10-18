import unittest
from cross_concentration_tests_v1 import leave_one_level_out
DATA=[{"gas":"ethanol","ppm":10},{"gas":"ethanol","ppm":10},{"gas":"ethanol","ppm":20},{"gas":"acetone","ppm":20},{"gas":"acetone","ppm":30}]
class BehavioralGate(unittest.TestCase):
    def test_three_levels(self): self.assertEqual(len(leave_one_level_out(DATA)),3)
    def test_sorted(self): self.assertEqual([f["held_out_ppm"] for f in leave_one_level_out(DATA)],[10.,20.,30.])
    def test_holdout_replicates(self): self.assertEqual(leave_one_level_out(DATA)[0]["test_indices"],[0,1])
    def test_disjoint(self):
        for f in leave_one_level_out(DATA):
            self.assertFalse(set(f["train_indices"])&set(f["test_indices"]))
            self.assertEqual(sorted(f["train_indices"]+f["test_indices"]),list(range(5)))
    def test_middle_interpolation(self): self.assertFalse(leave_one_level_out(DATA)[1]["extrapolation"])
    def test_boundary_extrapolation(self):
        fs=leave_one_level_out(DATA);self.assertTrue(fs[0]["extrapolation"]);self.assertTrue(fs[-1]["extrapolation"])
    def test_single_level(self):
        with self.assertRaises(ValueError): leave_one_level_out([DATA[0],DATA[1]])
    def test_empty(self):
        with self.assertRaises(ValueError): leave_one_level_out([])
    def test_negative(self):
        with self.assertRaises(ValueError): leave_one_level_out([DATA[0],{"gas":"a","ppm":-1}])
    def test_nonfinite(self):
        with self.assertRaises(ValueError): leave_one_level_out([DATA[0],{"gas":"a","ppm":float("inf")}])
    def test_bool_ppm(self):
        with self.assertRaises(ValueError): leave_one_level_out([DATA[0],{"gas":"a","ppm":True}])
    def test_missing_identity(self):
        with self.assertRaises(ValueError): leave_one_level_out([DATA[0],{"ppm":20}])
    def test_warning_not_gas_stratified(self):
        # This method holds out numeric levels across all gases; presence of a gas in
        # training is NOT guaranteed. Audit this before using for classifier accuracy.
        f=leave_one_level_out(DATA)[0]
        gases_train={DATA[i]["gas"] for i in f["train_indices"]}
        self.assertIn("ethanol",gases_train)
if __name__=="__main__": unittest.main()
