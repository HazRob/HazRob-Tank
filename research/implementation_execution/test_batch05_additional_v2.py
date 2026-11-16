"""V4 Batch05 additional negative scientific gates (synthetic inputs only)."""
import importlib.util
from pathlib import Path
import sys
import unittest

BASE=Path(__file__).resolve().parents[1]/"techniques"
def module(tid,name):
    p=BASE/tid/(name+".py")
    if str(p.parent) not in sys.path:sys.path.insert(0,str(p.parent))
    spec=importlib.util.spec_from_file_location("batch05_more_"+tid.replace(".","_")+"_"+name,p)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    return m

class Batch05OtherSevenTests(unittest.TestCase):
    def bayes_evidence(self,idx,group,session):
        return {"id":str(idx),"source_id":"toy","session_id":session,
         "sensor_array_id":"MQ8", "independent_group":group,
         "independence_audit_ref":"toy-claim-only",
         "likelihoods":{"A":.8,"B":.2}}
    def test_062_bayes_dependent_physical_source_abstains(self):
        m=module("06.2","independence_bayes_v2")
        a=m.guarded_bayesian_update({"A":.5,"B":.5},
           [self.bayes_evidence(1,"model1","S"),
            self.bayes_evidence(2,"model2","S")],source_id="toy")
        self.assertEqual(a["status"],"ABSTAIN_DEPENDENT_EVIDENCE")
    def test_062_bayes_disjoint_source_still_uncalibrated(self):
        m=module("06.2","independence_bayes_v2")
        x=m.guarded_bayesian_update({"A":.5,"B":.5},
            [self.bayes_evidence(1,"S1","S1"),self.bayes_evidence(2,"S2","S2")],source_id="toy")
        self.assertEqual(x["status"],"RESEARCH_BAYES_POSTERIOR_ASSUMPTION_ONLY")
        self.assertFalse(x["probability_empirically_calibrated"])
    def cal(self):
        logits=[(3.,0.),(0.,3.),(3.,0.),(0.,3.),(2.,0.),(0.,2.),(3.,0.),(0.,3.)]
        return logits,[0,1,0,1,0,1,0,1],["cal"+str(i) for i in range(8)]
    def test_063_group_disjoint_temperature_scaling(self):
        m=module("06.3","grouped_probability_calibration_v2")
        a,b,c=self.cal()
        x=m.calibrate_grouped(a,b,c,["train1","train2"],
                  source_id="toy",sensor_ids=["MQ7"],model_id="model1")
        self.assertEqual(x["n_calibration_exposures"],8)
        self.assertFalse(x["Tank_G3"])
    def test_063_rejects_training_calibration_reuse(self):
        m=module("06.3","grouped_probability_calibration_v2")
        a,b,c=self.cal()
        with self.assertRaises(ValueError):
            m.calibrate_grouped(a,b,c,["cal0"],source_id="toy",
                                sensor_ids=["MQ7"],model_id="model1")
    def test_064_conformal_group_disjoint_set(self):
        m=module("06.4","grouped_conformal_v2")
        probabilities=[(.9,.1),(.1,.9)]*6
        labels=[0,1]*6
        f=m.fit_grouped_conformal(probabilities,labels,
           ["cal"+str(i) for i in range(12)],["train"],source_id="synthetic",alpha=.1)
        out=m.grouped_conformal_set(f,(.9,.1),source_id="synthetic",test_group_id="test1")
        self.assertIn(0,out["candidate_indices"])
        self.assertFalse(out["Tank_G3"])
    def test_064_rejects_calibration_test_leak(self):
        m=module("06.4","grouped_conformal_v2")
        f=m.fit_grouped_conformal([(.9,.1),(.1,.9)]*6,[0,1]*6,
            ["cal"+str(i) for i in range(12)],["train"],source_id="toy",alpha=.1)
        with self.assertRaises(ValueError):
            m.grouped_conformal_set(f,(.9,.1),source_id="toy",test_group_id="cal1")
    def gasrow(self,i,gas,feature):
        return dict(exposure_id="exp"+str(i),gas_label=gas,scope="PURE_GAS",
                    features=(feature,),source_id="toy")
    def test_065_unknown_molecules_held_out(self):
        m=module("06.5","open_set_group_guard_v2")
        train=[self.gasrow(i,g,x) for i,(g,x) in enumerate([
            ("A",0.),("A",.1),("A",-.1),("B",10.),("B",10.1),("B",9.9)])]
        known=[self.gasrow(6,"A",.03),self.gasrow(7,"B",10.03)]
        unknown=[self.gasrow(8,"C",100.),self.gasrow(9,"C",120.)]
        out=m.evaluate_heldout_unknowns(train,known,unknown,
                                   source_id="toy",sensor_ids=("MQ135",))
        self.assertEqual(out["n_unknown"],2)
        self.assertEqual(out["unknown_reject_rate"],1.0)
        self.assertFalse(out["G2_real_source_verified"])
    def test_065_rejects_unknown_class_in_training(self):
        m=module("06.5","open_set_group_guard_v2")
        train=[self.gasrow(i,g,x) for i,(g,x) in enumerate([
            ("A",0.),("A",.1),("B",10.),("B",10.1)])]
        unknown=[self.gasrow(5,"A",100.)]
        with self.assertRaises(ValueError):
            m.evaluate_heldout_unknowns(train,[self.gasrow(4,"B",10.0)],
                unknown,source_id="toy",sensor_ids=("MQ135",))
    def test_066_ood_environment_abstains(self):
        m=module("06.6","source_ood_guard_v2")
        o=m.source_ood_check([(1.,),(2.,),(3.,)],(2.,),
             source_id="toy",sensor_ids=("MQ135",),heater_phase="CONSTANT",
             reference_r0_id="r0",temperature_c=20,rh_percent=50,
             observed_temperature_c=32,observed_rh_percent=50)
        self.assertEqual(o["status"],"ABSTAIN_ENVIRONMENT_OUTSIDE_SOURCE_CONTEXT")
    def test_066_ood_outside_envelope(self):
        m=module("06.6","source_ood_guard_v2")
        o=m.source_ood_check([(1.,),(2.,),(3.,)],(100.,),
             source_id="toy",sensor_ids=("MQ135",),heater_phase="CONSTANT",
             reference_r0_id="r0",temperature_c=20,rh_percent=50,
             observed_temperature_c=20,observed_rh_percent=50)
        self.assertEqual(o["status"],"OUTSIDE_TRAINING_ENVELOPE")
        self.assertFalse(o["gas_identity_verified"])
    def test_067_negative_power_without_reference_abstains(self):
        m=module("06.7","negative_evidence_guard_v2")
        x=m.source_negative_evidence("CO",{"MQ7":"NOT_DETECTED"},
            {"CO":{"MQ7":{"detection_power":.99}}},source_id="toy",
            reference_validation=False)
        self.assertEqual(x["status"],"ABSTAIN_NO_REFERENCED_DETECTION_POWER")
        self.assertFalse(x["hard_exclusion"])
    def test_067_verified_claim_still_no_hard_exclusion(self):
        m=module("06.7","negative_evidence_guard_v2")
        x=m.source_negative_evidence("CO",{"MQ7":"NOT_DETECTED"},
            {"CO":{"MQ7":{"detection_power":.99}}},source_id="toy",
            reference_validation=True)
        self.assertEqual(x["status"],"NEGATIVE_EVIDENCE_CONFLICT")
        self.assertFalse(x["hard_exclusion"])
    def test_068_shap_correlated_channels_remain_noncausal(self):
        m=module("06.8","shap_provenance_v2")
        x=m.explain_source_features(lambda z:z[0]+2*z[1],(0,0),(1,1),
            source_id="toy",sensor_ids=("MQ2","MQ135"),
            correlated_sensor_pairs=(("MQ2","MQ135"),))
        self.assertEqual(x["attributions"],[1.,2.])
        self.assertFalse(x["physical_causality_claim"])
    def test_068_rejects_invalid_channels(self):
        m=module("06.8","shap_provenance_v2")
        with self.assertRaises(ValueError):
            m.explain_source_features(lambda z:z[0],(0,),(1,),
                 source_id="toy",sensor_ids=("MQ2","MQ2"))
    def _open_set_calibration_rows(self):
        def make(i,gas,value):
            return {"source_id":"toy","scope":"PURE_GAS","exposure_id":"e"+str(i),
                    "gas_label":gas,"features":(value,)}
        train=[make(i,y,v) for i,(y,v) in enumerate([
            ("A",0.),("A",.1),("A",-.1),("B",10.),("B",10.1),("B",9.9)])]
        val=[make(i+6, "A" if i%2==0 else "B",
                  .05 if i%2==0 else 10.05) for i in range(12)]
        known=[make(18,"A",.04),make(19,"B",10.04)]
        unknown=[make(20,"C",100.),make(21,"C",120.)]
        return train,val,known,unknown
    def test_065_independent_known_calibration(self):
        m=module("06.5","calibrated_open_set_source_v2")
        r=m.calibrated_open_set_source(*self._open_set_calibration_rows(),
                                      source_id="toy",sensor_ids=("MQ135",),alpha=.1)
        self.assertEqual(r["known_test_n"],2)
        self.assertEqual(r["withheld_unknown_n"],2)
        self.assertEqual(r["unknown_reject_rate"],1.)
        self.assertFalse(r["Tank_G3"])
    def test_065_unknown_label_cannot_enter_train_or_calibration(self):
        m=module("06.5","calibrated_open_set_source_v2")
        train,val,known,unknown=self._open_set_calibration_rows()
        unknown[0]["gas_label"]="A"
        with self.assertRaises(ValueError):
            m.calibrated_open_set_source(train,val,known,unknown,
                                         source_id="toy",sensor_ids=("MQ135",),alpha=.1)

if __name__=="__main__":
    unittest.main()
