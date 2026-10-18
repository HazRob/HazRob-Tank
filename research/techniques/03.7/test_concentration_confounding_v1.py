import unittest
from concentration_confounding_v1 import concentration_report
class ConcentrationTests(unittest.TestCase):
    def setUp(self):
        self.x=[[1,4],[2,4],[3,4],[10,2],[20,2],[30,2]]
        self.y=["A"]*3+["B"]*3;self.c=[1,10,100]*2
    def test_groups(self):self.assertEqual(set(concentration_report(self.x,self.y,self.c)["by_gas"]),{"A","B"})
    def test_positive_corr(self):self.assertAlmostEqual(concentration_report(self.x,self.y,self.c)["by_gas"]["A"]["correlations"]["feature_0"],1)
    def test_constant_feature(self):self.assertIsNone(concentration_report(self.x,self.y,self.c)["by_gas"]["A"]["correlations"]["feature_1"])
    def test_ids(self):self.assertIn("MQ2",concentration_report(self.x,self.y,self.c,sensor_ids=["MQ2","MQ3"])["by_gas"]["A"]["correlations"])
    def test_count(self):self.assertEqual(concentration_report(self.x,self.y,self.c)["by_gas"]["B"]["n"],3)
    def test_unit(self):self.assertEqual(concentration_report(self.x,self.y,self.c)["unit"],"ppm")
    def test_insufficient(self):
        r=concentration_report([[1],[2]],["A","A"],[1,10])
        self.assertEqual(r["by_gas"]["A"]["status"],"INSUFFICIENT")
    def test_same_concentration(self):
        r=concentration_report([[1],[2],[3]],["A"]*3,[1]*3)
        self.assertEqual(r["by_gas"]["A"]["status"],"INSUFFICIENT")
    def test_zero_ppm(self):
        with self.assertRaises(ValueError):concentration_report(self.x,self.y,[0]+self.c[1:])
    def test_negative_ppm(self):
        with self.assertRaises(ValueError):concentration_report(self.x,self.y,[-1]+self.c[1:])
    def test_nan(self):
        with self.assertRaises(ValueError):concentration_report(self.x,self.y,[float("nan")]+self.c[1:])
    def test_mismatch(self):
        with self.assertRaises(ValueError):concentration_report(self.x,self.y,self.c[:-1])
    def test_ragged(self):
        with self.assertRaises(ValueError):concentration_report([[1],[1,2]],["A","A"],[1,2])
    def test_invalid_ids(self):
        with self.assertRaises(ValueError):concentration_report(self.x,self.y,self.c,sensor_ids=["MQ2","MQ2"])
    def test_bad_label(self):
        with self.assertRaises(ValueError):concentration_report(self.x,[None]+self.y[1:],self.c)
    def test_negative_correlation(self):
        r=concentration_report([[3],[2],[1]],["A"]*3,[1,10,100])
        self.assertAlmostEqual(r["by_gas"]["A"]["correlations"]["feature_0"],-1)
if __name__=="__main__":unittest.main()
