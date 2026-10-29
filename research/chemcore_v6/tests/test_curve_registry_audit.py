"""Research-inclusive source registry checks: no hard metadata exclusions."""
import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from curve_registry_audit import validate,REGISTRY
from source_audit import EvidenceError

class InclusiveCurveSourceAudit(unittest.TestCase):
    def test_01_original_43_curves_research_available(self):
        x=validate()
        self.assertEqual(x["total_source_curves"],43)
        self.assertEqual(x["curves_open_to_research_exploration"],43)
        self.assertEqual(x["research_with_caveats"],43)
        self.assertEqual(x["r0_definition_unverified"],43)
        self.assertEqual(x["source_dataset_root_count"],1)
        self.assertEqual(x["physically_validated_tank_curves"],0)
    def test_02_same_research_root_not_43_experiments(self):
        self.assertTrue(validate()["same_origin_curves_not_independent_exposures"])
    def test_03_source_parameters_untouched(self):
        j=json.loads(REGISTRY.read_text())
        self.assertEqual([r["record_id"] for r in j["records"]],
                         [f"MQDATA43-{i:03d}" for i in range(1,44)])
        self.assertEqual(sum(r["output_scale"]==50 for r in j["records"]),6)
    def test_04_no_research_row_suppressed_for_missing_r0(self):
        rows=json.loads(REGISTRY.read_text())["records"]
        self.assertTrue(all(r["status"]=="USABLE_RESEARCH_WITH_CAVEATS" for r in rows))
        self.assertTrue(all(not r["source_r0_definition_verified"] for r in rows))
        self.assertTrue(all("SOURCE_R0_DEFINITION_UNVERIFIED" in r["risk_flags"] for r in rows))
    def test_05_corrupted_numeric_parameter_rejected(self):
        j=json.loads(REGISTRY.read_text())
        j["records"][0]["ratio_a"]=float("nan")
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)/"registry.json"
            p.write_text(json.dumps(j))
            with self.assertRaises(EvidenceError):
                validate(registered=p)
    def test_06_metadata_without_warnings_rejected(self):
        j=json.loads(REGISTRY.read_text())
        j["records"][1]["risk_flags"]=[]
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)/"registry.json"
            p.write_text(json.dumps(j))
            with self.assertRaisesRegex(EvidenceError,"risk flags"):
                validate(registered=p)
    def test_07_source_mutation_does_not_override_qualified_data(self):
        j=json.loads(REGISTRY.read_text())
        j["records"][0]["status"]="HOLD_REFERENCE_PROVENANCE"
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)/"registry.json"
            p.write_text(json.dumps(j))
            with self.assertRaises(EvidenceError):
                validate(registered=p)

if __name__=="__main__":
    unittest.main()
