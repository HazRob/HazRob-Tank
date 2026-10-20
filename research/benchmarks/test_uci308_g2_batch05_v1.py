"""Protocol guards for real-data UCI308 G2-05; synthetic fixtures only."""
import unittest
from tempfile import TemporaryDirectory
from pathlib import Path
import uci308_g2_batch05_v1 as m
from uci1081_actual_modules_v1 import repo_module

def sample_rows():
    rows=[]
    for b in range(5):
        for gas,ace,eth in (("acetone",.1,0.),("ethanol",0.,.1),
                            ("mixture",.1,.1),("air",0.,0.)):
            ix=len(rows)
            v=[0.05+(b*.003)+((.5 if gas=="acetone" else .2 if gas=="ethanol" else .3 if gas=="mixture" else 0)*((i+1)/16.))
               for i in range(16)]
            rows.append({"exp":100+ix,"batch":f"day{b}","gas":gas,"lab":gas,
                         "ace_liquid_vol_percent":ace,
                         "eth_liquid_vol_percent":eth,"max_dR":v})
    return rows

class Batch05Protocol(unittest.TestCase):
    def test_ten_reviewed_ids(self):
        self.assertEqual(len(m.TECHNIQUES),10)
        self.assertEqual(len(set(m.TECHNIQUES)),10)
    def test_no_gas_ppm_assumptions(self):
        self.assertEqual(m.LIQUID_DOSES,{0.,.1,.3,1.})
    def test_locked_source_hash(self):
        with TemporaryDirectory() as td:
            p=Path(td)/"invalid.zip";p.write_bytes(b"not an official UCI308 source")
            with self.assertRaises(ValueError):m.validate_original(p)
    def test_training_only_endmembers(self):
        scale,sig=m.train_endmembers(sample_rows())
        self.assertEqual(set(sig),{"acetone","ethanol"})
        self.assertEqual(len(scale),16)
        self.assertEqual(len(sig["acetone"]),16)
    def test_endmember_few_pure(self):
        with self.assertRaises(ValueError):
            m.train_endmembers([r for r in sample_rows() if r["gas"]!="acetone"])
    def test_five_heldout_batches(self):
        v=m.score_folds(sample_rows())
        self.assertEqual(len(v["folds"]),5)
        self.assertEqual(v["n_scored_exposures"],15)
    def test_all_mixtures_are_numerically_processed(self):
        v=m.score_folds(sample_rows())
        self.assertEqual(v["08.9_08.10_n_numerical_mixture_fits"],5)
    def test_each_heldout_fold_separates_exposures(self):
        v=m.score_folds(sample_rows())
        self.assertTrue(all(f["n_training_exposures"]==16 and
                            f["n_testing_exposures"]==4 for f in v["folds"]))
    def test_fraction_metrics_bounded(self):
        v=m.score_folds(sample_rows())
        self.assertTrue(0<=v["08.8_mix_sensitivity"]<=1)
        self.assertTrue(0<=v["08.8_pure_specificity"]<=1)
    def test_fixed_noncalibrated_threshold(self):
        self.assertEqual(m.score_folds(sample_rows())["fixed_threshold"],.5)
    def test_unmix_no_ppm_output(self):
        x=repo_module("08.9","unmixing_v1.py").unmix_two([1.,1.],[1.,0.],[0.,1.],step=.5)
        self.assertNotIn("ppm",x)
        self.assertAlmostEqual(x["coefficient_a"],1.)
        self.assertAlmostEqual(x["coefficient_b"],1.)
    def test_multianalyte_coefficients_not_ppm(self):
        x=repo_module("08.10","multi_analyte_regression_v1.py").estimate(
            [1.,1.],[[1.,0.],[0.,1.]],iterations=500,step_size=.01)
        self.assertNotIn("ppm",x)
        self.assertEqual(len(x["coefficients"]),2)
    def test_detector_non_specific_warning(self):
        result=repo_module("08.8","mixture_detection_v1.py").assess(
            [0.2,0.3],{"a":[0.2,0.3],"b":[0.3,0.2]})
        self.assertEqual(result["status"],"SINGLE_SIGNATURE_COMPATIBLE")
        self.assertIn("unknown gases",result["warning"])
if __name__=="__main__":
    unittest.main()
