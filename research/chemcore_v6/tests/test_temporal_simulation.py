"""Eight distinct SIM-B scenarios; only synthetic software invariants are tested."""
import math
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import temporal_simulation as t

class SimBTests(unittest.TestCase):
    def test_01_research_only(self):
        p=t.scenario_cases()
        self.assertEqual(p["scenario_count"],8)
        self.assertEqual(p["Tank_G3_validations"],0)
        self.assertIsNone(p["real_ppm_accuracy"])
        self.assertFalse(p["claims_m4"])
    def test_02_exact_scenario_ids(self):
        ids=[z["id"] for z in t.scenario_cases()["cases"]]
        self.assertEqual(ids,["SIM-B01","SIM-B02","SIM-B03","SIM-B04",
                              "SIM-B05","SIM-B06","SIM-B08","SIM-B09"])
    def test_03_monotone_time(self):
        rows=t.simulate()
        self.assertEqual(len(rows),211)
        self.assertTrue(t.chronology_ok(rows))
        self.assertFalse(t.chronology_ok([rows[12],rows[11]]))
        self.assertFalse(t.chronology_ok([rows[12],rows[12]]))
    def test_04_phases_and_recovery(self):
        rows=t.simulate(noise_sd=0)
        self.assertEqual(rows[0]["phase"],"BASELINE")
        self.assertEqual(rows[60]["phase"],"EXPOSURE")
        self.assertEqual(rows[150]["phase"],"RECOVERY")
        self.assertGreater(t.response_fraction(120),t.response_fraction(61))
        self.assertLess(t.response_fraction(200),t.response_fraction(150))
    def test_05_humidity_shift_is_assumed(self):
        a=t.simulate(noise_sd=0)
        b=t.simulate(noise_sd=0,rh=70)
        self.assertAlmostEqual(b[100]["log10_rs_over_r0"]["MQ2"]-
                               a[100]["log10_rs_over_r0"]["MQ2"],.04,places=7)
    def test_06_baseline_drift_is_assumed(self):
        a=t.simulate(noise_sd=0)
        b=t.simulate(noise_sd=0,days=30)
        self.assertAlmostEqual(b[40]["log10_rs_over_r0"]["MQ2"]-
                               a[40]["log10_rs_over_r0"]["MQ2"],.021,places=7)
    def test_07_missing_channels_remain_null(self):
        d=t.simulate(missing=("MQ4","MQ9"))
        self.assertTrue(all(r["log10_rs_over_r0"]["MQ4"] is None for r in d))
        self.assertEqual(sum(d[60]["log10_rs_over_r0"][s] is not None for s in t.SENSORS),6)
    def test_08_reproducible_seed(self):
        self.assertEqual(t.simulate(seed=17),t.simulate(seed=17))
        self.assertNotEqual(t.simulate(seed=17),t.simulate(seed=18))
    def test_09_mixture_not_additive(self):
        a=t.simulate(noise_sd=0)
        b=t.simulate(noise_sd=0,mixture_interaction=.16)
        self.assertGreater(b[100]["log10_rs_over_r0"]["MQ8"]-
                           a[100]["log10_rs_over_r0"]["MQ8"],0)
    def test_10_interferent_not_an_identity(self):
        a=t.simulate(noise_sd=0)
        b=t.simulate(noise_sd=0,interferent=.12)
        self.assertGreater(b[100]["log10_rs_over_r0"]["MQ2"]-
                           a[100]["log10_rs_over_r0"]["MQ2"],0)
    def test_11_reject_unphysical_inputs(self):
        with self.assertRaises(ValueError):t.simulate(noise_sd=-1)
        with self.assertRaises(ValueError):t.simulate(rh=110)
        with self.assertRaises(ValueError):t.simulate(missing=("MQ6",))
        with self.assertRaises(ValueError):t.response_fraction(100,rise_tau=0)
    def test_12_synthetic_labels_on_every_point(self):
        d=t.simulate(seed=1)
        self.assertTrue(all(r["synthetic"] and not r["physical_device_validated"] for r in d))

if __name__=="__main__":unittest.main()
