"""G2-06 invariant and behavior tests using synthetic fixtures, not source evidence."""
import unittest
import math
from tempfile import TemporaryDirectory
from pathlib import Path
import uci308_g2_batch06_v1 as m
from uci1081_actual_modules_v1 import repo_module

def fixture(n=20):
    gases=("acetone","ethanol","mixture","air")
    rows=[];raw={}
    for i in range(n):
        gas=gases[i%4]
        row={"exp":i+1,"gas":gas,"batch":"batch"+str(i//4),
             "max_dR":[float((j+1)*.05+(i%4)*.08) for j in range(16)]}
        rows.append(row)
        raw[i+1]={j:[
            (i%4)*.02+(j*.002)+(math.sin(t/30.)*.01)+(0.1 if t>=100 else 0.)
            for t in range(300)
        ] for j in range(1,17)}
    return rows,raw

class G2Batch06Protocol(unittest.TestCase):
    def test_ten_distinct_ids(self):
        self.assertEqual(len(m.IDS),10)
        self.assertEqual(len(set(m.IDS)),10)
        self.assertEqual(m.IDS[0],"08.1")
        self.assertEqual(m.IDS[-1],"09.3")

    def test_locked_original_archive_hash(self):
        from uci308_g2_batch05_v1 import validate_original
        with TemporaryDirectory() as td:
            p=Path(td)/"not_original.zip"
            p.write_bytes(b"fake")
            with self.assertRaises(ValueError):
                validate_original(p)

    def test_08_1_convolution_correct_shape(self):
        fn=repo_module("08.1","cnn_1d_v1.py")
        self.assertEqual(fn.convolve_valid([1,2,3,4],[1,1],activation="linear"),
                         [3,5,7])

    def test_08_2_causal_no_future(self):
        fn=repo_module("08.2","tcn_v1.py")
        a=fn.causal_conv([1.,2.,3.,4.],[1.,1.],dilation=2)
        self.assertEqual(a,[1.,2.,4.,6.])

    def test_08_3_gru_numeric_forward_only(self):
        fn=repo_module("08.3","lstm_gru_v1.py")
        self.assertTrue(math.isfinite(fn.gru_scalar_step(.1,0.,m.GRU_WEIGHTS)))

    def test_08_4_graph_has_arbitrary_sensor_edges(self):
        fn=repo_module("08.4","gnn_v1.py")
        self.assertEqual(fn.aggregate([[0.],[1.],[2.]],[(0,1),(1,2)]),
                         [[.5],[1.],[1.5]])

    def test_08_5_ensemble_probability_shape(self):
        fn=repo_module("08.5","temporal_ensemble_v1.py")
        r=fn.ensemble([[.8,.2],[.2,.8]],weights=[1,1])
        self.assertEqual(r["probabilities"],[.5,.5])

    def test_08_5_fit_heldout_batch_splits(self):
        rows,raw=fixture(20)
        out=m.temporal_ensemble_by_batch(rows,raw)
        self.assertEqual(out["n_heldout_source_exposures"],20)
        self.assertEqual(len(out["folds"]),5)
        self.assertEqual(sum(f["test_exposures"] for f in out["folds"]),20)

    def test_08_6_synthetic_is_not_source_ppm(self):
        fn=repo_module("08.6","simulation_v1.py")
        a=fn.simulate([0,0,1,1],.1,baseline=0,tau_seconds=5)
        self.assertEqual(len(a["responses"]),4)
        self.assertIn("toy model",a["warning"])

    def test_08_7_mean_gap_only(self):
        fn=repo_module("08.7","sim_to_real_v1.py")
        a=fn.compare([[0.],[0.]],[[1.],[1.]])
        self.assertEqual(a["mean_gap_rmse"],1.)

    def test_09_1_events_numeric_only(self):
        fn=repo_module("09.1","robotic_olfaction_v1.py")
        e=fn.encounters([0,1,2,0,3],baseline=0,threshold=1)
        self.assertEqual(e["events"],[{"start":1,"end_exclusive":3},
                                      {"start":4,"end_exclusive":5}])

    def test_09_2_needs_real_coordinates(self):
        # No coordinates are present in source raw/feature schema.
        rows,raw=fixture()
        self.assertFalse(any("robot_x" in r or "robot_y" in r for r in rows))

    def test_09_3_needs_source_ground_truth(self):
        rows,raw=fixture()
        self.assertFalse(any("source_x" in r or "source_y" in r for r in rows))

    def test_numerical_58_exposure_invariants(self):
        rows,raw=fixture(58)
        out=m.signal_primitives(raw,rows)
        self.assertEqual(out["08.1_numeric_convolution"]["n"],928)
        self.assertEqual(out["08.2_numeric_causal_convolution"]["n"],928)
        self.assertEqual(out["08.3_numeric_GRU_cell"]["n"],928)
        self.assertEqual(out["08.4_numeric_graph_aggregation"]["n"],58)
        self.assertEqual(out["09.1_numeric_events"]["real_sensor_traces_examined"],58)

if __name__=="__main__":
    unittest.main()
