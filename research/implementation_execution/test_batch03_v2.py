"""V4 batch03 cross-technique synthetic G1 tests; no physical MQ gas/ppm claim."""
import importlib.util
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

def mod(id,name):
    path=ROOT/"techniques"/id/(name+".py")
    spec=importlib.util.spec_from_file_location("v4_"+id.replace(".","_")+"_"+name,path)
    m=importlib.util.module_from_spec(spec)
    # Existing V1 dependencies imported by module from its own folder.
    import sys
    if str(path.parent) not in sys.path:
        sys.path.insert(0,str(path.parent))
    spec.loader.exec_module(m)
    return m

def samples():
    # Each group is notionally an independent exposure, NOT raw time frames.
    x=[];y=[];g=[]
    for i in range(6):
        x.extend([(-4+i*.05,-3+i*.02),(4+i*.05,3+i*.02)])
        y.extend(["A","B"]);g.extend([f"E{i}a",f"E{i}b"])
    return x,y,g

class SourceTechniqueBatch03Tests(unittest.TestCase):
    def test_036_grouped_subset_selection(self):
        m=mod("03.6","grouped_sensor_subsets_v2")
        x,y,g=samples()
        out=m.grouped_sensor_subsets(x,y,g,("MQ2","MQ135"),source_id="synthetic",max_sensors=2)
        self.assertEqual(out["best_exploratory"]["accuracy"],1.)
        self.assertEqual(len(out["candidates"]),3)
        self.assertFalse(out["final_independent_test"])
    def test_036_invalid_groups(self):
        m=mod("03.6","grouped_sensor_subsets_v2")
        x,y,_=samples()
        with self.assertRaises(ValueError):
            m.grouped_sensor_subsets(x,y,["one"]*len(x),("MQ2","MQ135"),source_id="test")
    def test_037_concentration_level_holdout(self):
        m=mod("03.7","concentration_holdout_v2")
        p=[c for c in (10,50,100) for _ in range(4)]
        x,y,g=samples()
        out=m.concentration_holdout(x,y,p,g,source_id="experimental-source",
                                    sensor_ids=("MQ2","MQ135"))
        self.assertEqual([v["heldout_ppm"] for v in out["folds"]],[10,50,100])
        self.assertFalse(out["Tank_G3"])
    def test_037_no_liquid_units_substitution(self):
        m=mod("03.7","concentration_holdout_v2")
        x,y,g=samples()
        with self.assertRaises(ValueError):
            m.concentration_holdout(x,y,[0]*len(x),g,source_id="wine-liquid-vv",
                                    sensor_ids=("MQ2","MQ135"))
    def test_038_rank_conditioned_mixture(self):
        m=mod("03.8","local_identifiability_v2")
        output=m.rank_mixture_candidates([[1,0],[0,1],[0,0]],("MQ2","MQ7","MQ135"),
                        ("CO","EtOH"),source_id="synthetic-model")
        self.assertEqual(output["rank"],2)
        self.assertEqual(output["condition"],1.)
        self.assertFalse(output["Tank_G3"])
    def test_038_unidentifiable_collinear(self):
        m=mod("03.8","local_identifiability_v2")
        out=m.rank_mixture_candidates([[1,2],[2,4]],("MQ2","MQ7"),("CO","EtOH"),source_id="synthetic")
        self.assertEqual(out["decision"],"NON_IDENTIFIABLE")
    def test_038_unknown_more_gases_than_sensors(self):
        m=mod("03.8","local_identifiability_v2")
        with self.assertRaises(ValueError):
            m.rank_mixture_candidates([[1,2,3],[2,3,4]],("MQ2","MQ7"),
                                     ("CO","EtOH","NH3"),source_id="source")
    def test_041_unknown_concentration_ranking(self):
        m=mod("04.1","source_curve_candidates_v2")
        sensors=("MQ2","MQ135")
        info={s:{"r0_definition_id":"factory-graph","heater_phase":"CONSTANT"} for s in sensors}
        curves={}
        for gas,rate in [("A",1.),("B",3.)]:
            curves[gas]={s:{"points":[(10,rate),(100,rate/2.)],
                            "source_id":"paper","r0_definition_id":"factory-graph",
                            "heater_phase":"CONSTANT"} for s in sensors}
        out=m.curve_candidate_grid({"MQ2":.75,"MQ135":.75},curves,[10,50,100],sensor_metadata=info)
        self.assertEqual(out["ranking"][0]["candidate_gas"],"A")
        self.assertTrue(out["ranking"][0]["not_calibrated_ppm"])
    def test_041_r0_mismatch_abstains(self):
        m=mod("04.1","source_curve_candidates_v2")
        info={s:{"r0_definition_id":"wrong","heater_phase":"CONSTANT"} for s in ("MQ2","MQ135")}
        curve={s:{"points":[(10,1.),(100,.5)],"source_id":"m",
                  "r0_definition_id":"correct","heater_phase":"CONSTANT"} for s in info}
        self.assertEqual(m.curve_candidate_grid({"MQ2":.75,"MQ135":.75},{"A":curve},
                          [10,100],sensor_metadata=info)["decision"],"ABSTAIN_NO_COMPATIBLE_CURVES")
    def test_042_operational_class_not_pure_gas(self):
        m=mod("04.2","source_fingerprint_matching_v2")
        ref={"source_id":"x","signal_unit":"ADC_COUNTS","r0_definition_id":"ADC",
              "heater_phase":"CONSTANT","identity_kind":"OPERATIONAL_CLASS",
              "label":"Perfume","sensor_values":{"MQ2":100,"MQ135":200}}
        out=m.match_source_fingerprints({"MQ2":100,"MQ135":201},[ref],
                source_id="x",signal_unit="ADC_COUNTS",r0_definition_id="ADC",
                heater_phase="CONSTANT")
        self.assertEqual(out["ranked"][0]["identity_kind"],"OPERATIONAL_CLASS")
        self.assertFalse(out["ranked"][0]["chemical_gas_identification"])
    def test_042_unknown_source_has_no_match(self):
        m=mod("04.2","source_fingerprint_matching_v2")
        ref={"source_id":"x","signal_unit":"ADC_COUNTS","r0_definition_id":"ADC",
              "heater_phase":"CONSTANT","identity_kind":"PURE_GAS",
              "label":"CO","sensor_values":{"MQ2":100,"MQ135":200}}
        out=m.match_source_fingerprints({"MQ2":100,"MQ135":200},[ref],
                source_id="other",signal_unit="ADC_COUNTS",r0_definition_id="ADC",
                heater_phase="CONSTANT")
        self.assertEqual(out["decision"],"NO_COMPARABLE_REFERENCE")
    def test_043_to_047_five_grouped_models(self):
        x,y,g=samples()
        test=x[-4:];truth=y[-4:];held=g[-4:]
        for id in ["04.3","04.4","04.5","04.6","04.7"]:
            with self.subTest(id=id):
                m=mod(id,"grouped_source_benchmark_v2")
                out=m.benchmark_source(x[:-4],y[:-4],g[:-4],test,truth,held,
                                  ("MQ2","MQ135"),source_id="toy-not-gas")
                self.assertEqual(out["heldout_n"],4)
                self.assertGreaterEqual(out["heldout_accuracy"],0.)
                self.assertLessEqual(out["heldout_accuracy"],1.)
                self.assertFalse(out["Tank_G3"])
                self.assertTrue(out["V4_grouped_source_contract"])
                if id=="04.7":
                    self.assertEqual(out["method"],"BOOSTED_STUMPS_NOT_XGBOOST")
    def test_043_to_047_leakage_rejection(self):
        x,y,g=samples()
        for id in ["04.3","04.4","04.5","04.6","04.7"]:
            with self.subTest(id=id):
                m=mod(id,"grouped_source_benchmark_v2")
                with self.assertRaises(ValueError):
                    m.benchmark_source(x[:-4],y[:-4],g[:-4],x[-4:],y[-4:],g[:-3],
                                       ("MQ2","MQ135"),source_id="toy")
    def test_04_closed_set_unseen_class_rejected(self):
        m=mod("04.3","grouped_source_benchmark_v2")
        x,y,g=samples()
        with self.assertRaises(ValueError):
            m.benchmark_source(x[:-4],y[:-4],g[:-4],x[-4:],["A","novel","A","B"],g[-4:],
                               ("MQ2","MQ135"),source_id="toy")

if __name__=="__main__":
    unittest.main()
