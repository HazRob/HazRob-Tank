"""V4 batch06: independent source, device, drift and update guards.
Entirely synthetic controls: no real external G2 or physical Tank G3 claim.
"""
from pathlib import Path
import importlib.util
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]/"techniques"

def module(id,name):
    path=ROOT/id/(name+".py")
    if str(path.parent) not in sys.path:sys.path.insert(0,str(path.parent))
    spec=importlib.util.spec_from_file_location("v4b06_"+id.replace(".","_")+"_"+name,path)
    obj=importlib.util.module_from_spec(spec)
    sys.modules[spec.name]=obj
    spec.loader.exec_module(obj)
    return obj

m69=module("06.9","selective_risk_holdout_v2")
m71=module("07.1","session_holdout_v2")
m72=module("07.2","physical_device_holdout_v2")
m73=module("07.3","concentration_group_holdout_v2")
m74=module("07.4","reference_drift_alarm_v2")
m75=module("07.5","matched_blank_correction_v2")
m76=module("07.6","guarded_domain_alignment_v2")
m77=module("07.7","negative_transfer_gate_v2")
m78=module("07.8","safe_doe_priority_v2")
m79=module("07.9","replay_approval_gate_v2")

class Batch06SourceContractTests(unittest.TestCase):
    def confidence(self,prefix,correct=(True,True,False,True)):
        return [dict(group_id=prefix+str(i),source_id="MQ-EXT",session_id=prefix+"session"+str(i),
                 confidence=score,correct=good)
            for i,(score,good) in enumerate(zip((.98,.90,.70,.50),correct))]
    def physical(self):
        return [dict(source_id="src",session_id="session"+str(i//2),
                     exposure_id="exp"+str(i),device_id="TankA")
                for i in range(8)]
    def devices(self):
        return [dict(source_id="src",session_id="ses"+str(i),
                    exposure_id="e"+str(i),physical_device_id="hardware"+str(i//3),
                    physical_sensor_instance_id="mq7-actual"+str(i//3))
                for i in range(6)]
    def levels(self):
        return [dict(source_id="src",session_id="ses"+str(i//2),
                    exposure_id="e"+str(i),gas_label="ethanol",
                    reference_trace_id="source-paper",reference_ppm=(10,20,30)[i//2])
                for i in range(6)]
    def blanks(self,shift=0):
        base=[dict(source_id="src",sensor_instance_id="MQ135-hardware",
            heater_phase="CONSTANT",blank_reference_id="air",
            day_id="d1",session_id="base"+str(i),
            timestamp_s=float(i),rs_ohm=100.+i) for i in range(3)]
        later=[dict(source_id="src",sensor_instance_id="MQ135-hardware",
            heater_phase="CONSTANT",blank_reference_id="air",
            day_id="d2",session_id="later"+str(i),
            timestamp_s=10.+i,rs_ohm=100.+i+shift) for i in range(3)]
        return base,later
    def reference(self,time,session,blank,features):
        return dict(source_id="src",sensor_ids=["MQ2","MQ7"],
                    reference_id="R0-A",session_id=session,
                    heater_phase="CONSTANT",sensor_array_id="physical-array-A",
                    environment_bin="20C,50RH",gas_baseline_label=blank,
                    timestamp_s=time,features=features)
    def domain(self,source,device,session_prefix):
        return [dict(source_id=source,sensor_ids=["MQ135"],
            physical_device_id=device,session_id=session_prefix+str(i),
            exposure_id=source+"exp"+str(i),sensor_family="MQ",heater_phase="CONSTANT",
            features=(float(i),)) for i in range(3)]
    def transfer(self):
        return [dict(source_id="src",session_id="eval"+str(i//2),
            exposure_id="exp"+str(i),reference_trace_id="trace",
            reference_value=float(i),baseline_prediction=float(i)+.1,
            transfer_prediction=float(i)+.5) for i in range(6)]
    def replay(self,good=True):
        return [dict(source_id="src",session_id="replay"+str(i//2),
            exposure_id="exp"+str(i),reference_trace_id="ref-standard",
            reference_value=float(i),rollback_prediction=float(i)+.4,
            candidate_prediction=float(i)+(.2 if good else .9)) for i in range(6)]
    def test_069_risk_coverage_calibration_only(self):
        result=m69.evaluate_selective_risk(self.confidence("cal"),
               self.confidence("test"))
        self.assertEqual(result["status"],"SOURCE_SELECTIVE_TEST_ONLY")
        self.assertAlmostEqual(result["coverage"],.5)
        self.assertFalse(result["Tank_G3"])
    def test_069_session_leak_rejected(self):
        test=self.confidence("test")
        test[0]["session_id"]="calsession0"
        with self.assertRaises(ValueError):m69.evaluate_selective_risk(self.confidence("cal"),test)
    def test_069_nonfinite_rejected(self):
        cal=self.confidence("cal");cal[0]["confidence"]=float("nan")
        with self.assertRaises(ValueError):m69.evaluate_selective_risk(cal,self.confidence("test"))
    def test_071_whole_session_group_holdout(self):
        x=m71.holdout_sessions(self.physical(),source_id="src",heldout_session_ids=["session3"])
        self.assertEqual(len(x["test"]),2)
        self.assertEqual(len(x["train"]),6)
    def test_071_exposure_duplicate_rejected(self):
        rows=self.physical();rows[7]["exposure_id"]="exp0"
        with self.assertRaises(ValueError):
            m71.holdout_sessions(rows,source_id="src",heldout_session_ids=["session3"])
    def test_072_device_out_of_sample(self):
        x=m72.split_physical_devices(self.devices(),source_id="src",heldout_device_ids=["hardware1"])
        self.assertEqual(len(x["test"]),3)
        self.assertFalse(x["Tank_G3"])
    def test_072_batch_not_a_device(self):
        d=self.devices();d[0]["physical_device_id"]="batch-1"
        with self.assertRaises(ValueError):
            m72.split_physical_devices(d,source_id="src",heldout_device_ids=["hardware1"])
    def test_072_inconsistent_sensor_id(self):
        d=self.devices();d[5]["physical_sensor_instance_id"]="mq7-actual0"
        with self.assertRaises(ValueError):
            m72.split_physical_devices(d,source_id="src",heldout_device_ids=["hardware1"])
    def test_073_concentration_holdout(self):
        x=m73.concentration_holdout(self.levels(),source_id="src",gas_label="ethanol",heldout_ppm=[20])
        self.assertEqual(x["heldout_domain"]["20"],"INTERPOLATION")
        self.assertEqual(len(x["test"]),2)
        self.assertFalse(x["reference_certificate_independently_verified"])
    def test_073_session_overlap(self):
        rows=self.levels();rows[2]["session_id"]="ses0"
        with self.assertRaises(ValueError):
            m73.concentration_holdout(rows,source_id="src",gas_label="ethanol",heldout_ppm=[20])
    def test_074_detect_matched_blank_change(self):
        a,b=self.blanks(25.)
        self.assertEqual(m74.matched_blank_drift(a,b)["status"],"SENSOR_REFERENCE_SHIFT_FLAG")
        self.assertFalse(m74.matched_blank_drift(a,b)["auto_recalibration"])
    def test_074_reject_phase_mismatch(self):
        a,b=self.blanks(25);b[1]["heater_phase"]="HIGH"
        with self.assertRaises(ValueError):m74.matched_blank_drift(a,b)
    def test_074_no_session_pseudoreplicates(self):
        a,b=self.blanks();b[2]["session_id"]=b[1]["session_id"]
        with self.assertRaises(ValueError):m74.matched_blank_drift(a,b)
    def test_075_matched_blank_does_not_erase_analyte(self):
        a=self.reference(1,"base","CLEAN_BLANK",[1,2])
        b=self.reference(2,"later","CLEAN_BLANK",[2,3])
        q=self.reference(3,"gas-exposure","ETHANOL",[7,9])
        x=m75.matched_blank_correction(a,b,q,source_id="src",
             sensor_ids=["MQ2","MQ7"],reference_id="R0-A")
        self.assertEqual(x["research_corrected_features"],[6.,8.])
        self.assertFalse(x["auto_apply_to_live_sensor"])
    def test_075_unmatched_environment_rejected(self):
        a=self.reference(1,"base","CLEAN_BLANK",[1,2])
        b=self.reference(2,"later","CLEAN_BLANK",[2,3])
        q=self.reference(3,"query","ETHANOL",[7,9]);q["environment_bin"]="90RH"
        with self.assertRaises(ValueError):
            m75.matched_blank_correction(a,b,q,source_id="src",sensor_ids=["MQ2","MQ7"],reference_id="R0-A")
    def test_075_blank_must_really_be_blank(self):
        a=self.reference(1,"base","ETHANOL",[1,2])
        b=self.reference(2,"later","CLEAN_BLANK",[2,3])
        q=self.reference(3,"query","ETHANOL",[7,9])
        with self.assertRaises(ValueError):
            m75.matched_blank_correction(a,b,q,source_id="src",sensor_ids=["MQ2","MQ7"],reference_id="R0-A")
    def test_076_domain_mean_proposal(self):
        source=self.domain("source","device-A","A")
        target=self.domain("target","device-B","B")
        x=m76.propose_domain_mean_alignment(source,target,source_id="source",
            target_source_id="target",sensor_ids=["MQ135"])
        self.assertEqual(x["candidate_shift"],[0.])
        self.assertFalse(x["safe_to_transfer_ppm"])
    def test_076_reject_same_physical_device(self):
        with self.assertRaises(ValueError):
            m76.propose_domain_mean_alignment(self.domain("source","device-A","A"),
                self.domain("target","device-A","B"),
                source_id="source",target_source_id="target",sensor_ids=["MQ135"])
    def test_076_reject_TGS_to_MQ_transfer(self):
        other=self.domain("target","device-B","B");other[0]["sensor_family"]="TGS"
        with self.assertRaises(ValueError):
            m76.propose_domain_mean_alignment(self.domain("source","device-A","A"),other,
                source_id="source",target_source_id="target",sensor_ids=["MQ135"])
    def test_077_reject_negative_transfer(self):
        x=m77.reject_negative_transfer(self.transfer(),training_session_ids=["train"],expected_source_id="src")
        self.assertEqual(x["status"],"REJECT_NEGATIVE_TRANSFER")
        self.assertGreater(x["transfer_mae"],x["baseline_mae"])
    def test_077_reject_overlap(self):
        rows=self.transfer();rows[0]["session_id"]="train"
        with self.assertRaises(ValueError):
            m77.reject_negative_transfer(rows,training_session_ids=["train"],expected_source_id="src")
    def test_077_allow_better_on_heldout_only(self):
        rows=self.transfer()
        for x in rows:x["transfer_prediction"]=x["reference_value"]
        self.assertEqual(m77.reject_negative_transfer(rows,training_session_ids=["train"],expected_source_id="src")["status"],"NO_WORSE_ON_THIS_HELDOUT_SOURCE")
    def lab(self,idx,ppm=10,uncertainty=.5):
        return dict(candidate_id=str(idx),gas_label="ethanol",sensor_model="MQ135",
            reference_trace_id="certified-gas-plan",reference_ppm=ppm,
            rh_percent=50,uncertainty=uncertainty,not_yet_measured=True,
            certified_lab_plan=True,gas_handling_safety_review=True)
    def test_078_prioritized_requests_not_execution(self):
        x=m78.prioritize_reference_experiments([self.lab(1,.5,.2),self.lab(2,.6,.8)],
            approved_sensor_models=["MQ135"],upper_safe_reference_ppm=1.0)
        self.assertEqual(x["proposals"][0]["candidate_id"],"2")
        self.assertFalse(x["experiment_executed"])
    def test_078_safety_approval_required(self):
        a=self.lab(1);a["certified_lab_plan"]=False
        with self.assertRaises(PermissionError):
            m78.prioritize_reference_experiments([a],approved_sensor_models=["MQ135"])
    def test_078_unreviewed_high_ppm_rejected(self):
        with self.assertRaises(ValueError):
            m78.prioritize_reference_experiments([self.lab(1,200)],approved_sensor_models=["MQ135"],upper_safe_reference_ppm=10)
    def test_079_guarded_replay_does_not_update(self):
        x=m79.assess_continual_update(candidate_model_id="v2",rollback_model_id="v1",
            source_id="src",training_session_ids=["train"],replay_records=self.replay(),
            reviewer_approved=True)
        self.assertEqual(x["status"],"RESEARCH_UPDATE_CANDIDATE_ACCEPTABLE")
        self.assertFalse(x["model_was_updated"])
    def test_079_negative_replay_rejected(self):
        x=m79.assess_continual_update(candidate_model_id="v2",rollback_model_id="v1",
            source_id="src",training_session_ids=["train"],replay_records=self.replay(False),
            reviewer_approved=True)
        self.assertEqual(x["status"],"REJECT_CATASTROPHIC_FORGETTING")
    def test_079_unapproved_auto_update_refused(self):
        with self.assertRaises(PermissionError):
            m79.assess_continual_update(candidate_model_id="v2",rollback_model_id="v1",
                source_id="src",training_session_ids=["train"],replay_records=self.replay())
    def test_079_train_replay_overlap(self):
        a=self.replay();a[0]["session_id"]="train"
        with self.assertRaises(ValueError):
            m79.assess_continual_update(candidate_model_id="v2",rollback_model_id="v1",
                source_id="src",training_session_ids=["train"],replay_records=a,reviewer_approved=True)


if __name__=="__main__":
    unittest.main()
