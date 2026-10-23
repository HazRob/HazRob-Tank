"""V4 Batch07 source-aware numerical mechanism controls; synthetic fixtures only."""
import importlib.util
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]/"techniques"

def load(id,name):
    path=ROOT/id/(name+".py")
    if str(path.parent) not in sys.path:sys.path.insert(0,str(path.parent))
    s=importlib.util.spec_from_file_location("batch07_"+id.replace(".","_")+"_"+name,path)
    x=importlib.util.module_from_spec(s);sys.modules[s.name]=x;s.loader.exec_module(x)
    return x

m710=load("07.10","cpython_research_profile_v2")
m81=load("08.1","conv1d_research_reference_v2")
m82=load("08.2","causal_dilated_tcn_v2")
m83=load("08.3","recurrent_state_isolation_v2")
m84=load("08.4","graph_topology_source_v2")
m85=load("08.5","dependent_ensemble_v2")
m86=load("08.6","first_order_gas_simulator_v2")
m87=load("08.7","paired_sim_real_gap_v2")
m88=load("08.8","mixture_hypothesis_competition_v2")
m89=load("08.9","nonnegative_rank_guard_v2")

class Batch07EngineeringEvidence(unittest.TestCase):
    def test_0710_only_claim_cpython(self):
        x=m710.profile_python_call(sum,[1,2,3],repeats=3)
        self.assertEqual(x["runtime"],"CPython")
        self.assertIsNone(x["hardware_power_watts"])
        self.assertGreaterEqual(x["median_wall_ns"],0)
    def test_0710_cannot_claim_esp32_watts(self):
        with self.assertRaises(ValueError):
            m710.profile_python_call(sum,[1,2,3],device_label="ESP32")
    def test_081_real_conv1d_kernel_no_trained_claim(self):
        x=m81.conv1d_valid([[1,2],[3,4],[5,6]],
              [[1,0],[0,1]],sensor_ids=["MQ2","MQ7"],source_id="toy",exposure_id="e1")
        self.assertEqual(x["conv_relu"],[5.,9.])
        self.assertFalse(x["trained"])
    def test_081_channel_order_missing(self):
        with self.assertRaises(ValueError):
            m81.conv1d_valid([[1,2],[3,4],[5,6]],[[1,0],[0,1]],
                 sensor_ids=["MQ2"],source_id="toy",exposure_id="e1")
    def test_082_prefix_independent_of_future(self):
        opts=dict(kernel=[1,.5,.25],dilation=2,source_id="toy",exposure_id="e")
        a=m82.causal_dilated_filter([1,2,3,4,5],**opts)["filtered_signal"]
        b=m82.causal_dilated_filter([1,2,3,4,5,999,999],**opts)["filtered_signal"]
        self.assertEqual(a,b[:5])
        self.assertEqual(len(a),5)
    def test_082_invalid_dilation(self):
        with self.assertRaises(ValueError):
            m82.causal_dilated_filter([1,2,3],[1,.5],dilation=0,source_id="s",exposure_id="e")
    def test_083_isolate_state_each_exposure(self):
        rows=[dict(source_id="toy",session_id="s",exposure_id="e1",signal=[1,2,3]),
              dict(source_id="toy",session_id="s",exposure_id="e2",signal=[1,2,3])]
        x=m83.isolated_recurrence(rows,source_id="toy")
        self.assertEqual(x["per_exposure"][0]["trajectory"],x["per_exposure"][1]["trajectory"])
        self.assertFalse(x["actual_LSTM_or_GRU_trained"])
    def test_083_reject_duplicate_exposure(self):
        rows=[dict(source_id="toy",session_id="s",exposure_id="e",signal=[1]),
              dict(source_id="toy",session_id="s2",exposure_id="e",signal=[2])]
        with self.assertRaises(ValueError):m83.isolated_recurrence(rows,source_id="toy")
    def test_084_graph_message_mean(self):
        a=m84.graph_message_aggregate([1,3],[[1,1],[1,1]],
            sensor_ids=["MQ2","MQ135"],source_id="toy",graph_source_id="graph1")
        self.assertEqual(a["node_aggregate"],[2.,2.])
        self.assertFalse(a["trained_GNN"])
    def test_084_bad_graph_rejected(self):
        with self.assertRaises(ValueError):
            m84.graph_message_aggregate([1,3],[[1,1],[0,1]],
                sensor_ids=["MQ2","MQ135"],source_id="toy",graph_source_id="graph1")
    def ensembles(self):
        return [dict(source_id="s",physical_session_id="sess",sensor_array_id="a",
            model_id=model,probabilities={"A":p,"B":1-p})
                for model,p in (("CNN",.8),("GRU",.6))]
    def test_085_average_correlated_probabilities(self):
        x=m85.ensemble_same_array(self.ensembles(),source_id="s",physical_session_id="sess",array_id="a")
        self.assertAlmostEqual(x["candidate_scores"]["A"],.7)
        self.assertFalse(x["evidence_groups_independent"])
    def test_085_mismatch_prevents_ensemble(self):
        rows=self.ensembles();rows[1]["physical_session_id"]="other"
        with self.assertRaises(ValueError):
            m85.ensemble_same_array(rows,source_id="s",physical_session_id="sess",array_id="a")
    def test_086_first_order_step(self):
        x=m86.simulate_first_order(times_s=[0,1,2,3],gas_reference_ppm=100,tau_s=1,
                   source_model_id="kinetics-paper",source_gas_label="ethanol",heater_phase="CONSTANT")
        self.assertEqual(x["synthetic_chamber_reference_ppm"][0],0)
        self.assertTrue(x["synthetic_chamber_reference_ppm"][1]>0)
        self.assertFalse(x["is_real_exposure"])
    def test_086_invalid_tau(self):
        with self.assertRaises(ValueError):
            m86.simulate_first_order(times_s=[0,1],gas_reference_ppm=100,tau_s=0,
               source_model_id="paper",source_gas_label="ethanol",heater_phase="LOW")
    def paired(self):
        return [dict(source_id="s",sensor_instance_id="MQ135-1",heater_phase="CONSTANT",
                 exposure_id="e"+str(i),session_id="S"+str(i//2),
                 reference_trace_id="claimed-standard",
                 simulated_rs_over_r0=1.,observed_rs_over_r0=1.2) for i in range(6)]
    def test_087_paired_gap_not_approval(self):
        x=m87.paired_source_gap(self.paired(),source_id="s",
            sensor_instance_id="MQ135-1",heater_phase="CONSTANT")
        self.assertAlmostEqual(x["rs_over_r0_rmse"],.2)
        self.assertFalse(x["deployment_ready"])
    def test_087_no_duplicate_exposure(self):
        p=self.paired();p[2]["exposure_id"]="e0"
        with self.assertRaises(ValueError):
            m87.paired_source_gap(p,source_id="s",sensor_instance_id="MQ135-1",heater_phase="CONSTANT")
    def test_088_mixture_hypothesis_not_proof(self):
        x=m88.propose_binary_mixture_hypothesis([.5,.5],[1,0],[0,1],
            source_id="s",sensor_ids=["MQ2","MQ135"],reference_r0_id="r0",heater_phase="CONSTANT")
        self.assertAlmostEqual(x["proposed_fraction_a"],.5)
        self.assertFalse(x["mixture_is_verified"])
        self.assertIsNone(x["mixture_reference_ppm"])
    def test_088_nonfinite_rejected(self):
        with self.assertRaises(ValueError):
            m88.propose_binary_mixture_hypothesis([float("nan"),1],[1,0],[0,1],
                source_id="s",sensor_ids=["MQ2","MQ135"],reference_r0_id="r0",heater_phase="CONSTANT")
    def nnls(self,y,a,b):
        return m89.nnls_two_component(y,a,b,source_id="s",sensor_ids=["MQ2","MQ3","MQ135"],
            reference_r0_id="r0",heater_phase="CONSTANT")
    def test_089_exact_nonnegative_solution(self):
        x=self.nnls([2,3,5],[1,0,1],[0,1,1])
        self.assertEqual(x["status"],"NUMERICAL_NNLS_SOURCE_PRIOR_ONLY")
        self.assertAlmostEqual(x["coefficients"]["a"],2)
        self.assertAlmostEqual(x["coefficients"]["b"],3)
        self.assertAlmostEqual(x["residual_rmse"],0)
        self.assertIsNone(x["gas_ppm"])
    def test_089_collinear_not_identifiable(self):
        x=self.nnls([1,2,3],[1,2,3],[2,4,6])
        self.assertEqual(x["status"],"NOT_IDENTIFIABLE_SOURCE_COLLINEAR")
        self.assertIsNone(x["coefficients"])
    def test_089_nonnegative_boundary(self):
        x=self.nnls([1,0,1],[1,0,1],[0,1,1])
        self.assertGreaterEqual(x["coefficients"]["a"],0)
        self.assertGreaterEqual(x["coefficients"]["b"],0)
    def test_089_reject_nan(self):
        with self.assertRaises(ValueError):
            self.nnls([1,2,float("nan")],[1,0,1],[0,1,1])

if __name__=="__main__":
    unittest.main()
