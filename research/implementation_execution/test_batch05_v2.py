"""Batch05 start: source blank, concentration/session holdouts, correlation guard."""
import importlib.util
from pathlib import Path
import sys
import unittest

BASE=Path(__file__).resolve().parents[1]/"techniques"
def mod(id,name):
    p=BASE/id/(name+".py")
    if str(p.parent) not in sys.path:
        sys.path.insert(0,str(p.parent))
    spec=importlib.util.spec_from_file_location("batch05_"+id.replace(".","_")+"_"+name,p)
    m=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m

class Batch05ResearchEvidenceTests(unittest.TestCase):
    def blanks(self):
        return [{"session_id":"session"+str(i),"day_id":"day"+str(i%3),
            "source_id":"synthetic","sensor_instance_id":"MQ135-1",
            "heater_phase":"CONSTANT","signal":1.0+(i%4)*.01}
            for i in range(12)]
    def test_058_screening_stays_in_signal_units(self):
        m=mod("05.8","source_blank_limits_v2")
        x=m.source_blank_screening(self.blanks(),source_id="synthetic",
            sensor_instance_id="MQ135-1",heater_phase="CONSTANT",signal_unit="Rs/R0")
        self.assertEqual(x["blank_sessions"],12)
        self.assertGreater(x["screening_10sigma_signal"],x["screening_3sigma_signal"])
        self.assertIsNone(x["calibrated_ppm_limits"])
        self.assertFalse(x["Tank_G3"])
    def test_058_cannot_double_count_one_session(self):
        m=mod("05.8","source_blank_limits_v2")
        data=self.blanks();data[1]["session_id"]=data[0]["session_id"]
        with self.assertRaises(ValueError):
            m.source_blank_screening(data,source_id="synthetic",
                sensor_instance_id="MQ135-1",heater_phase="CONSTANT",signal_unit="Rs/R0")
    def test_058_cannot_call_one_day_long_series_independent(self):
        m=mod("05.8","source_blank_limits_v2")
        data=self.blanks()
        for x in data:x["day_id"]="day1"
        with self.assertRaises(ValueError):
            m.source_blank_screening(data,source_id="synthetic",
                sensor_instance_id="MQ135-1",heater_phase="CONSTANT",signal_unit="Rs/R0")
    def dataset(self):
        d=[]
        for i,c in enumerate([10.,10.,20.,20.,40.,40.,80.,80.,30.,30.]):
            d.append(dict(source_id="toy",sensor_instance_id="MQ2-physical",
                session_id="S"+str(i//2),exposure_id="E"+str(i),
                gas_label="ethanol",reference_trace_id="claimed-trace",
                reference_ppm=c))
        return d
    def test_059_holdout_complete_levels_and_sessions(self):
        m=mod("05.9","source_concentration_holdout_v2")
        x=m.grouped_concentration_plan(self.dataset(),heldout_ppm=[30],
                 source_id="toy",sensor_instance_id="MQ2-physical")
        self.assertEqual(x["heldout_region"]["30.0"],"INTERPOLATION")
        self.assertEqual(x["n_heldout_sessions"],1)
        self.assertFalse(x["real_source_reference_independently_verified"])
    def test_059_detects_session_leak_even_different_exposures(self):
        m=mod("05.9","source_concentration_holdout_v2")
        data=self.dataset()
        data[8]["session_id"]=data[0]["session_id"]
        with self.assertRaises(ValueError):
            m.grouped_concentration_plan(data,heldout_ppm=[30],
                        source_id="toy",sensor_instance_id="MQ2-physical")
    def test_059_rejects_missing_reference_trace(self):
        m=mod("05.9","source_concentration_holdout_v2")
        data=self.dataset();del data[0]["reference_trace_id"]
        with self.assertRaises(ValueError):
            m.grouped_concentration_plan(data,heldout_ppm=[30],
                        source_id="toy",sensor_instance_id="MQ2-physical")
    def evidence(self,group,source="dataset",session="S1",array="MQ-array-1"):
        return {"group":group,"source_id":source,"physical_session_id":session,
          "sensor_array_id":array,"probabilities":{"CO":.8,"NH3":.2}}
    def test_061_cannot_fake_independent_evidence_groups(self):
        m=mod("06.1","dependency_guard_v2")
        with self.assertRaises(ValueError):
            m.guarded_dependency_fusion([self.evidence("method-A"),
                self.evidence("method-B")],prior={"CO":.5,"NH3":.5})
    def test_061_abstain_if_only_same_group(self):
        m=mod("06.1","dependency_guard_v2")
        x=m.guarded_dependency_fusion([self.evidence("same"),
                  self.evidence("same")],prior={"CO":.5,"NH3":.5})
        self.assertEqual(x["status"],"ABSTAIN")
        self.assertFalse(x["Tank_G3"])
    def test_061_allow_distinct_source_groups_only_provisionally(self):
        m=mod("06.1","dependency_guard_v2")
        x=m.guarded_dependency_fusion([self.evidence("source-A"),
           self.evidence("source-B",source="other",session="S-other")],
           prior={"CO":.5,"NH3":.5})
        self.assertEqual(x["status"],"FUSED_RESEARCH_SCORES")
        self.assertEqual(x["n_groups"],2)
        self.assertFalse(x["calibrated_probabilities"])
    def test_061_source_provenance_missing(self):
        m=mod("06.1","dependency_guard_v2")
        src=self.evidence("g");del src["sensor_array_id"]
        with self.assertRaises(ValueError):
            m.guarded_dependency_fusion([src],prior={"CO":.5,"NH3":.5})

if __name__=="__main__":
    unittest.main()
