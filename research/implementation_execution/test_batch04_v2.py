"""V4 batch04 first three: actual MLP, candidate-wise QSPR and source byte auditing."""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]
def load(id,name):
    p=ROOT/"techniques"/id/(name+".py")
    if str(p.parent) not in sys.path:sys.path.insert(0,str(p.parent))
    spec=importlib.util.spec_from_file_location("batch04_"+id.replace(".","_")+"_"+name,p)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module

def synthetic_qspr():
    rows=[]
    for gas,property_value in [("compound-A",0.),("compound-B",1.),("compound-C",2.)]:
        for ppm in (10.,100.):
            rows.append({"source_id":"toy","heater_phase":"CONSTANT","r0_definition_id":"test-curve",
                         "exposure_id":gas+"_"+str(ppm),"molecule_id":gas,
                         "concentration_ppm":ppm,"descriptors":{"XLogP":property_value},
                         "ratios":{"MQ2":10**(.2+.1*__import__("math").log10(ppm)+.3*property_value),
                                   "MQ135":10**(.4+.1*__import__("math").log10(ppm)-.25*property_value)}})
    return rows

class Batch04FirstThreeTests(unittest.TestCase):
    def test_mlp_real_grouped_training(self):
        m=load("04.8","grouped_mlp_v2")
        tx=[];ty=[];tg=[]
        for i in range(6):
            tx.extend([(-3+i*.01,-4),(3+i*.01,4)])
            ty.extend(["A","B"]);tg.extend(["A"+str(i),"B"+str(i)])
        x=m.grouped_mlp_benchmark(tx[:-4],ty[:-4],tg[:-4],tx[-4:],ty[-4:],tg[-4:],
                                 ("MQ2","MQ135"),source_id="synthetic-unit")
        self.assertEqual(x["method"],"MLP_ONE_HIDDEN_LAYER")
        self.assertEqual(x["heldout_n"],4)
        self.assertFalse(x["Tank_G3"])
    def test_mlp_group_leakage_blocked(self):
        m=load("04.8","grouped_mlp_v2")
        train=[(1,2),(2,3),(3,4),(4,5),(5,6),(6,7)]
        labels=["A","B","A","B","A","B"];groups=list("abcdef")
        with self.assertRaises(ValueError):
            m.grouped_mlp_benchmark(train,labels,groups,[(1,2),(2,3)],["A","B"],["a","z"],
                      ("MQ2","MQ135"),source_id="x")
    def test_qspr_forward_and_candidate_inversion(self):
        m=load("04.9","molecular_forward_qspr_v2")
        inverse=load("04.9","inverse_molecular_qspr_v2")
        model=m.fit_source_qspr(synthetic_qspr(),("MQ2","MQ135"),("XLogP",),
                      source_id="toy",heater_phase="CONSTANT",r0_definition_id="test-curve")
        observed={"MQ2":10**(.2+.1*1.5+.3*1.5),
                  "MQ135":10**(.4+.1*1.5-.25*1.5)}
        out=inverse.inverse_molecular_hypotheses(model,observed,
                  {"target":{"XLogP":1.5},"other":{"XLogP":0.}},
                  [10.,100.],source_id="toy",heater_phase="CONSTANT",
                  r0_definition_id="test-curve",heldout_exposure_id="new-exposure")
        self.assertEqual(out["ranking"][0]["candidate"],"target")
        self.assertFalse(out["PPM_Validated"])
        self.assertFalse(out["Gas_Identity_Verified"])
    def test_qspr_no_heldout_exposure_reuse(self):
        m=load("04.9","molecular_forward_qspr_v2")
        inverse=load("04.9","inverse_molecular_qspr_v2")
        model=m.fit_source_qspr(synthetic_qspr(),("MQ2","MQ135"),("XLogP",),
                    source_id="toy",heater_phase="CONSTANT",r0_definition_id="test-curve")
        with self.assertRaises(ValueError):
            inverse.inverse_molecular_hypotheses(model,{"MQ2":1,"MQ135":1},
                  {"candidate":{"XLogP":1.}},[10.],source_id="toy",
                  heater_phase="CONSTANT",r0_definition_id="test-curve",
                  heldout_exposure_id="compound-A_10.0")
    def test_qspr_no_unqualified_extrapolation(self):
        m=load("04.9","molecular_forward_qspr_v2")
        inv=load("04.9","inverse_molecular_qspr_v2")
        model=m.fit_source_qspr(synthetic_qspr(),("MQ2","MQ135"),("XLogP",),
                   source_id="toy",heater_phase="CONSTANT",r0_definition_id="test-curve")
        with self.assertRaises(ValueError):
            inv.inverse_molecular_hypotheses(model,{"MQ2":1,"MQ135":1},
                  {"candidate":{"XLogP":1.}},[1000.],source_id="toy",
                  heater_phase="CONSTANT",r0_definition_id="test-curve")
    def test_qspr_duplicate_exposure_rejected(self):
        m=load("04.9","molecular_forward_qspr_v2")
        x=synthetic_qspr();x[1]["exposure_id"]=x[0]["exposure_id"]
        with self.assertRaises(ValueError):
            m.fit_source_qspr(x,("MQ2","MQ135"),("XLogP",),source_id="toy",
                       heater_phase="CONSTANT",r0_definition_id="test-curve")
    def test_audit_group_split_and_sha(self):
        m=load("04.10","source_benchmark_audit_v2")
        records=[]
        for i in range(5):
            records.append({"sample_id":i,"exposure_id":str(i),"source_id":"toy",
                    "sensor_ids":["MQ2","MQ135"],"partition":"TRAIN" if i<3 else "TEST",
                    "gas_label":"A" if i%2==0 else "B",
                    "prediction":"A" if i%2==0 else "B"})
        content=("\n".join(json.dumps(r) for r in records)+"\n").encode()
        value=m.source_split_report(content,hashlib.sha256(content).hexdigest(),
                                  source_id="toy",sensor_ids=("MQ2","MQ135"))
        self.assertEqual(value["heldout"]["coverage"],1.)
        self.assertFalse(value["G2_real_source_approved"])
    def _calibration_source(self):
        # All data are synthetic; ppm trace and R0 are only caller claims.
        meta={"source_id":"toy-synthetic","device_id":"device1","gas_label":"EthOH",
              "heater_phase":"CONSTANT","reference_trace_id":"claimed-gas-standard",
              "r0_definition_id":"gas-curve","sensor_ids":("MQ2","MQ135"),
              "r0_ohm":{"MQ2":1000.,"MQ135":2000.},
              "rl_ohm":{"MQ2":10000.,"MQ135":10000.}}
        def record(i,ppm):
            return {"source_id":meta["source_id"],"gas_label":meta["gas_label"],
                    "exposure_id":"exposure"+str(i),"reference_ppm":ppm,
                    "reference_trace_id":meta["reference_trace_id"],
                    "rs_ohm":{"MQ2":5000.*ppm**(-.25),
                              "MQ135":8000.*ppm**(-.28)}}
        return meta,[record(i,x) for i,x in enumerate([10,20,30,40,50,60,70,80])],[
            record(8,25),record(9,45)]
    def test_six_source_conditional_regressors(self):
        meta,train,test=self._calibration_source()
        for id in ("05.1","05.2","05.3","05.4","05.5","05.6"):
            with self.subTest(id=id):
                m=load(id,"source_calibration_audit_v2")
                out=m.audit_source_calibration(train,test,metadata=meta)
                self.assertEqual(out["method"],id)
                self.assertEqual(out["test_independent_exposure_ids"],2)
                self.assertFalse(out["production_ppm_qualified"])
                self.assertFalse(out["Tank_G3"])
    def test_six_regressors_reject_shared_exposure(self):
        meta,train,test=self._calibration_source()
        test[0]["exposure_id"]=train[0]["exposure_id"]
        for id in ("05.1","05.2","05.3","05.4","05.5","05.6"):
            with self.subTest(id=id),self.assertRaises(ValueError):
                load(id,"source_calibration_audit_v2").audit_source_calibration(train,test,metadata=meta)
    def test_six_regressors_require_individually_measured_r0(self):
        meta,train,test=self._calibration_source()
        del meta["r0_ohm"]["MQ135"]
        with self.assertRaises(ValueError):
            load("05.4","source_calibration_audit_v2").audit_source_calibration(train,test,metadata=meta)
    def test_identity_conditional_guard_rejects_uncalibrated_class(self):
        m=load("05.7","identity_calibration_gate_v2")
        x=m.guarded_identity_conditional({"CO":.9,"NH3":.1},{"CO":lambda _:{"ppm":10,"status":"WITHIN_CALIBRATED_RANGE"}},
             123,source_id="toy",probability_calibration_source="",
             conditional_calibration_source="")
        self.assertEqual(x["status"],"ABSTAIN")
        y=m.guarded_identity_conditional({"CO":.9,"NH3":.1},{"CO":lambda _:{"ppm":10,"status":"WITHIN_CALIBRATED_RANGE"}},
             123,source_id="toy",probability_calibration_source="claimed-cal",
             conditional_calibration_source="claimed-ppm")
        self.assertEqual(y["status"],"RESEARCH_CONDITIONAL_RESULT")
        self.assertFalse(y["production_ppm_approved"])
    def test_audit_bytes_or_leakage_detected(self):
        m=load("04.10","source_benchmark_audit_v2")
        records=[]
        for i in range(5):
            records.append({"sample_id":i,"exposure_id":"same" if i in (0,4) else str(i),
                    "source_id":"toy","sensor_ids":["MQ2","MQ135"],
                    "partition":"TRAIN" if i<3 else "TEST",
                    "gas_label":"A","prediction":"A"})
        content=("\n".join(json.dumps(r) for r in records)+"\n").encode()
        with self.assertRaises(ValueError):
            m.source_split_report(content,hashlib.sha256(content).hexdigest(),
                                 source_id="toy",sensor_ids=("MQ2","MQ135"))
        with self.assertRaises(ValueError):
            m.source_split_report(content,"bad",source_id="toy",sensor_ids=("MQ2","MQ135"))

if __name__=="__main__":
    unittest.main()
