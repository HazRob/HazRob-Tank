"""G2-06 source integrity and scientific-evidence guard tests.

Synthetic test fixtures ONLY. Official UCI309 measurement run is separate.
"""
import collections, tempfile, unittest, zipfile
from pathlib import Path
import uci309_g2_batch06_v1 as m

def synthetic_records():
    out=[]
    i=0
    for eth in ("n","L","M","H"):
        for gas in ("CO","Me"):
            for level in ("n","L","M","H"):
                if eth=="n" and level=="n":
                    continue
                # 15 per secondary gas, 30 configs, 6 repetitions.
                for r in range(6):
                    amp=.15 if gas=="CO" else -.15
                    c=.02*("nLMH".index(eth))
                    out.append({
                        "source_config":eth+"_"+gas+"_"+level,
                        "second_species":gas,"second_level":level,
                        "experiment":i,"ethylene_mean_gcms_ppm":m.NOMINAL_PPM["Et"][eth],
                        "second_mean_gcms_ppm":m.NOMINAL_PPM[gas][level],
                        "early_signed_rs_fold_delta":[amp+c+j*.001+r*.0005 for j in range(8)],
                        "late_signed_rs_fold_delta":[1.5*amp+c+j*.001+r*.0005 for j in range(8)]
                    })
                    i+=1
    return out

def synthetic_trace_row():
    r={
      "experiment":2,
      "temperature_c_baseline_median":23.0,
      "relative_humidity_pct_baseline_median":40.0,
      "ethylene_mean_gcms_ppm":31.0,"second_mean_gcms_ppm":51.0,
      "times":list(range(300)),
      "fold_deltas":[[0. if t<60 else .2 if t<240 else .01 for t in range(300)]
                     for _ in range(8)]
    }
    return r

class UCI309G2Integrity(unittest.TestCase):
    def test_ten_unique_technique_ids(self):
        self.assertEqual(len(m.TECHNIQUES),10)
        self.assertEqual(len(set(m.TECHNIQUES)),10)
    def test_180_configurations_without_duplication(self):
        s=synthetic_records()
        self.assertEqual(len(s),180)
        c=collections.Counter(x["source_config"] for x in s)
        self.assertEqual(len(c),30)
        self.assertEqual(set(c.values()),{6})
    def test_24_active_secondary_groups(self):
        s=[r for r in synthetic_records() if r["second_level"]!="n"]
        self.assertEqual(len(s),144)
        self.assertEqual(len(set(r["source_config"] for r in s)),24)
    def test_ppm_source_levels(self):
        self.assertEqual(m.NOMINAL_PPM["Et"]["L"],31.)
        self.assertEqual(m.NOMINAL_PPM["Me"]["M"],115.)
        self.assertEqual(m.NOMINAL_PPM["CO"]["H"],460.)
        self.assertEqual(m.NOMINAL_PPM["Et"]["n"],0.)
    def test_filename_labels(self):
        a=m.EXPERIMENT_REGEX.fullmatch("007_Et_L_Me_H")
        self.assertEqual(a.groups(),("007","L","Me","H"))
    def test_bad_filename(self):
        self.assertIsNone(m.EXPERIMENT_REGEX.fullmatch("001_bad_label"))
    def test_source_sha_mismatch_refused(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"invalid.zip"
            with zipfile.ZipFile(p,"w") as z:z.writestr("dummy","hello")
            with self.assertRaises(ValueError):m.inspect_zip(p)
    def test_synthetic_group_heldout_ensemble(self):
        o=m.train_fold_probabilistic_ensemble(synthetic_records())
        self.assertEqual(o["heldout_source_exposures"],144)
        self.assertEqual(o["heldout_complete_mixture_configurations"],24)
        self.assertEqual(len(o["folds"]),24)
        self.assertTrue(all(z["n_train_exposures"]==138 and z["n_heldout_exposures"]==6
                            for z in o["folds"]))
    def test_08_1_to_08_4_09_1_behavior_on_one_trace(self):
        o=m.primitive_temporal_features([synthetic_trace_row()])
        c=o["counts"]
        self.assertEqual(c["08.1_convolutions"],8)
        self.assertEqual(c["08.2_causal_and_residual"],8)
        self.assertEqual(c["08.3_fixed_weight_gru_traces"],8)
        self.assertEqual(c["08.4_arbitrary_channel_graph_aggregation_exposures"],1)
        self.assertEqual(c["09.1_thresholded_stationary_sensor_traces"],8)
    def test_toy_model_cannot_be_counted_as_device_calibration(self):
        rows=[synthetic_trace_row(),dict(synthetic_trace_row(),experiment=3)]
        o=m.toy_simulation_gap(rows)
        self.assertEqual(o["08.6_toy_simulated_profiles"],2)
        self.assertEqual(o["08.7_numeric_domain_gap_test_exposures"],2)
        self.assertIn("NOT_PHYSICALLY_CALIBRATED",o["status"])
    def test_no_mq8_identity_asserted(self):
        self.assertTrue(all(not n.startswith("MQ") for n in m.SENSORS))
    def test_no_spatial_coordinates_in_source_fixture(self):
        s=synthetic_trace_row()
        self.assertNotIn("x",s)
        self.assertNotIn("y",s)
if __name__=="__main__":
    unittest.main()
