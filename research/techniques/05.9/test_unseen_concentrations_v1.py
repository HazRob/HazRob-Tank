import unittest
from unseen_concentrations_v1 import split_by_concentration,score_predictions
class UnseenConcentrationTests(unittest.TestCase):
    def setUp(self):
        self.rows=[{"ppm":p,"session":s} for p in (10,20,40,80,160) for s in ("a","b")]
    def test_split(self):self.assertEqual(len(split_by_concentration(self.rows,{40})["test"]),2)
    def test_train(self):self.assertEqual(len(split_by_concentration(self.rows,{40})["train"]),8)
    def test_levels(self):self.assertEqual(split_by_concentration(self.rows,{40})["test_levels"],[40])
    def test_interpolation(self):self.assertEqual(split_by_concentration(self.rows,{40})["test_region"][40],"INTERPOLATION")
    def test_extrapolation(self):self.assertEqual(split_by_concentration(self.rows,{160})["test_region"][160],"EXTRAPOLATION")
    def test_no_leakage(self):
        v=split_by_concentration(self.rows,{20,80})
        self.assertFalse(set(r["ppm"] for r in v["train"]) & set(r["ppm"] for r in v["test"]))
    def test_perfect(self):self.assertEqual(score_predictions([20,40],[20,40],train_range_ppm=(10,80))["overall"]["mae_ppm"],0)
    def test_error(self):self.assertEqual(score_predictions([20],[25],train_range_ppm=(10,80))["overall"]["mae_ppm"],5)
    def test_abstention(self):self.assertEqual(score_predictions([20,40],[None,40],train_range_ppm=(10,80))["overall"]["coverage"],.5)
    def test_no_estimates(self):self.assertIsNone(score_predictions([20],[None],train_range_ppm=(10,80))["overall"]["rmse_ppm"])
    def test_region_count(self):self.assertEqual(score_predictions([20,160],[20,None],train_range_ppm=(10,80))["extrapolation"]["n"],1)
    def test_bad_reference(self):
        with self.assertRaises(ValueError):score_predictions([0],[1],train_range_ppm=(10,80))
    def test_bad_prediction(self):
        with self.assertRaises(ValueError):score_predictions([20],[-1],train_range_ppm=(10,80))
    def test_missing_level(self):
        with self.assertRaises(ValueError):split_by_concentration(self.rows,{30})
    def test_too_few_training_levels(self):
        with self.assertRaises(ValueError):split_by_concentration(self.rows,{20,40,80,160})
    def test_invalid_range(self):
        with self.assertRaises(ValueError):score_predictions([20],[20],train_range_ppm=(80,10))
if __name__=="__main__":unittest.main()
