"""V4 batch02: synthetic engineering regressions only, no claim of measured Tank gas truth."""
import hashlib
import importlib
import sys
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def module(id,stem):
    parent=ROOT/"techniques"/id
    if str(parent) not in sys.path:
        sys.path.insert(0,str(parent))
    return importlib.import_module(stem)

def labelled():
    # six independent source exposure groups, with two labelled observations each
    x=[];y=[];groups=[]
    for i in range(6):
        x.extend([(i*.01, i*.01),(10+i*.01, 10+i*.01)])
        y.extend(["A","B"]);groups.extend([f"exp{i}",f"exp{i}"])
    return x,y,groups

class V4Batch02Tests(unittest.TestCase):
    def test_024_positive_differential(self):
        m=module("02.4","segmental_differentiation_v2")
        x=m.segmental_difference(list(range(8)),[10,10,9,9,8,8,7,7],10,
              sensor_instance_id="unit",exposure_group_id="E1",source_id="p",heater_phase="CONSTANT")
        self.assertTrue(all(z<0 for z in x["segment_difference_per_second"]))
        self.assertFalse(x["ppm_validated"])
    def test_024_reject_mixed_time_or_heater(self):
        m=module("02.4","segmental_differentiation_v2")
        for tim,phase in [([0,1,2,2,4,5,6,7],"CONSTANT"),(list(range(8)),"UNKNOWN")]:
            with self.subTest(t=tim,phase=phase),self.assertRaises(ValueError):
                m.segmental_difference(tim,[10]*8,10,sensor_instance_id="U",
                   exposure_group_id="E",source_id="S",heater_phase=phase)
    def test_025_complex_labels_are_not_gases(self):
        m=module("02.5","source_fingerprint_v2")
        x=m.fingerprint(sensor_ids=("MQ2","MQ135"),baseline_ohm=(100,200),
           exposure_ohm=(50,220),source_id="s",exposure_id="A",label="Perfume",
           identity_kind="OPERATIONAL_CLASS")
        self.assertEqual(x["identity_kind"],"OPERATIONAL_CLASS")
        self.assertFalse(x["device_ppm_approved"])
        self.assertEqual(m.unique_exposure_count([x,x])["distinct_exposure_count"],1)
    def test_025_no_ppm_in_complex_class(self):
        m=module("02.5","source_fingerprint_v2")
        with self.assertRaises(ValueError):
            m.fingerprint(sensor_ids=("MQ3",),baseline_ohm=(100,),exposure_ohm=(50,),
                          source_id="s",exposure_id="x",identity_kind="KNOWN_MIXTURE",
                          label="alcohol+acetone",verified_gas_ppm=15,ppm_reference_id="a")
    def test_026_4d_requires_independent_ppm(self):
        m=module("02.6","physical_trajectory_v2")
        kw=dict(sensor_instance_id="U",source_id="S",baseline_rs_ohm=100)
        with self.assertRaises(ValueError):
            m.build_multiaxis([0,1,2],[100,90,80],[40,41,42],concentration_ppm=[2,3,4],**kw)
        x=m.build_multiaxis([0,1,2],[100,90,80],[40,41,42],concentration_ppm=[2,3,4],
                             independent_ppm_reference="external reference",**kw)
        self.assertEqual(x["dimensions"],4);self.assertFalse(x["Tank_G3"])
    def test_026_3d_without_chemical_dimension(self):
        m=module("02.6","physical_trajectory_v2")
        x=m.build_multiaxis([0,1,2],[100,90,80],[40,41,42],
                            sensor_instance_id="U",source_id="S",baseline_rs_ohm=100)
        self.assertEqual(x["dimensions"],3)
    def test_027_group_heldout_selection(self):
        m=module("02.7","grouped_feature_selection_v2")
        x,y,g=labelled()
        study=m.grouped_feature_study(x,y,g,("MQ2","MQ135"))
        self.assertEqual(study["best_feature_count_exploratory"],1)
        self.assertEqual(len(study["candidate_results"][0]["folds"]),6)
        self.assertFalse(study["nested_outer_holdout_performed"])
    def test_027_groups_insufficient(self):
        m=module("02.7","grouped_feature_selection_v2")
        with self.assertRaises(ValueError):
            m.grouped_feature_study([[0],[2],[0],[2]],["A","B","A","B"],
                                    ["x","x","y","y"],["MQ2"])
    def test_028_hash_real_bytes(self):
        m=module("02.8","source_provenance_v2")
        b=b"test:MQ readings"
        values=dict(source_bytes=b,claimed_sha256=hashlib.sha256(b).hexdigest(),
             source_id="m",licence="CC BY",source_url="https://example.org/raw",
             sensor_models=["MQ135"],channel_unit="ADC_CODE",event_id="E1",
             session_id="S1",utc_timestamp="2026-10-21T00:00:00Z",
             identity_kind="OPERATIONAL_CLASS")
        out=m.audited_record(**values)
        self.assertFalse(out["actual_reference_gas_calibration_verified"])
        with self.assertRaises(ValueError):
            m.audited_record(**{**values,"claimed_sha256":"b"*64})
    def test_028_no_unit_or_uncertified_ppm(self):
        m=module("02.8","source_provenance_v2")
        args=dict(source_bytes=b"abc",claimed_sha256=hashlib.sha256(b"abc").hexdigest(),
             source_id="m",licence="CC BY",source_url="http://e",
             sensor_models=["MQ135"],channel_unit="ADC_CODE",event_id="E1",
             session_id="S1",utc_timestamp="2026-10-21T00:00:00Z",
             identity_kind="OPERATIONAL_CLASS")
        with self.assertRaises(ValueError):
            m.audited_record(**{**args,"reference_ppm":100,"reference_certificate":"claimed"})
        with self.assertRaises(ValueError):
            m.audited_record(**{**args,"channel_unit":"OHM_MAYBE"})
    def test_031_pca_train_only(self):
        m=module("03.1","grouped_pca_v2")
        f=m.grouped_pca_fit([[0,0],[1,1],[2,2],[3,3]],["A","A","B","B"],
                            ["MQ2","MQ135"],source_id="UCI",n_components=1)
        self.assertEqual(len(f["loadings"]),1)
        x=m.transform_grouped_pca(f,[[4,4],[5,5]],["C","C"],source_id="UCI",sensor_ids=["MQ2","MQ135"])
        self.assertEqual(len(x["scores"]),2)
        with self.assertRaises(ValueError):
            m.transform_grouped_pca(f,[[4,4],[5,5]],["A","C"],source_id="UCI",sensor_ids=["MQ2","MQ135"])
    def test_032_similarity_rejects_cross_source(self):
        m=module("03.2","source_similarity_v2")
        a=dict(sensor_ids=["MQ2","MQ135"],source_id="A",feature_units="log ratio",
               reference_definition="R0-M",heater_phase="CONSTANT",features=[1,2])
        b={**a,"features":[1,3]}
        self.assertEqual(m.context_similarity(a,b,fitted_scales=[1,1])["scaled_distance"],1)
        with self.assertRaises(ValueError):
            m.context_similarity(a,{**b,"source_id":"another"},fitted_scales=[1,1])
    def test_033_identifiability_must_not_certify(self):
        m=module("03.3","identifiability_gate_v2")
        ortho=m.two_component_identifiability([1,0],[0,1],sensor_ids=["MQ2","MQ3"])
        same=m.two_component_identifiability([1,0],[2,0],sensor_ids=["MQ2","MQ3"])
        self.assertEqual(ortho["linear_rank"],2)
        self.assertFalse(ortho["ppm_approved"])
        self.assertEqual(same["linear_rank"],1)
        self.assertIn("NON_IDENTIFIABLE",same["status"])
    def test_034_real_grouped_lda_and_pls(self):
        m=module("03.4","grouped_discriminant_v2")
        x,y,g=labelled()
        for method in ("LDA","PLS1DA"):
            with self.subTest(method=method):
                result=m.grouped_binary_discriminant(x,y,g,source_id="UCI",method=method)
                self.assertEqual(result["n_heldout_predictions"],12)
                self.assertEqual(result["exploratory_group_heldout_accuracy"],1)
                self.assertFalse(result["Tank_G3"])
    def test_034_refuse_label_starvation(self):
        m=module("03.4","grouped_discriminant_v2")
        with self.assertRaises(ValueError):
            m.grouped_binary_discriminant([[0],[0],[0],[10],[10],[10]],
                ["A"]*3+["B"]*3,["A"]*3+["B"]*3,source_id="x")
    def test_035_source_specific_sensor_ablation(self):
        m=module("03.5","sensor_drop_column_v2")
        x,y,g=labelled()
        result=m.sensor_drop_column(x,y,g,("MQ2","MQ135"),source_id="UCI")
        self.assertEqual(len(result["drop_one"]),2)
        self.assertFalse(result["independent_final_test"])
        self.assertFalse(result["gas_selectivity_proven"])
    def test_035_reject_single_sensor(self):
        m=module("03.5","sensor_drop_column_v2")
        x,y,g=labelled()
        with self.assertRaises(ValueError):
            m.sensor_drop_column([[r[0]] for r in x],y,g,("MQ2",),source_id="s")
if __name__=="__main__":
    unittest.main()
