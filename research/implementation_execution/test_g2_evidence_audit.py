"""G2 audit regression tests; synthetic fixtures never become G2 evidence."""
import unittest
from g2_evidence_audit import audit_record, audit_all
from test_g2_signal_lineage_gate import normalized

SHA = "a"*64

def eligible_fixture():
    x=normalized()
    x.update({"id":"01.2", "source_kind":"INDEPENDENT_MEASURED", "raw_sha256":SHA,
              "dataset_uri":"https://example.org/dataset", "sensor_metadata":{"MQ135":"unit-001"},
              "grouping_key":"device_session_id", "grouping_source_verified":True,
              "grouping_source_field":"device_id,session_id", "train_group_ids":["d1-s1"],
              "test_group_ids":["d2-s1"], "target_definition":"lab gas reference",
              "metric_name":"classification_accuracy", "metric_value":0.85,
              "baseline_metric_value":0.5, "protocol_sha256":SHA,
              "evaluation_artifact_sha256":SHA, "raw_sha256_verified_from_bytes":True,
              "hyperparameters_selected_on_test":False, "blind_holdout":True,
              "measurement_units":"OHM", "confidence_interval":{"low":0.7,"high":0.9},
              "source_raw_data_accessible":True})
    return x

class AuditTests(unittest.TestCase):
    def test_synthetic_metadata_fixture_only_candidate(self):
        self.assertEqual(audit_record(eligible_fixture())["status"],"G2_PASS_CANDIDATE_REQUIRES_REVIEW")
    def test_actual_uci_ordinal_is_not_verified_group(self):
        x=eligible_fixture();x.update(grouping_key="replicate_ordinal",grouping_source_verified=False)
        self.assertIn("GROUPING_NOT_VERIFIED_PHYSICAL_UNIT",audit_record(x)["issues"])
    def test_group_overlap(self):
        x=eligible_fixture();x["test_group_ids"]=["d1-s1"]
        self.assertIn("GROUP_LEAKAGE",audit_record(x)["issues"])
    def test_raw_hash_must_be_verified(self):
        x=eligible_fixture();x["raw_sha256_verified_from_bytes"]=False
        self.assertIn("RAW_SOURCE_BYTES_NOT_SHA_VERIFIED",audit_record(x)["issues"])
    def test_no_signal_lineage(self):
        x=eligible_fixture();del x["signal_processing"]
        self.assertIn("MISSING_SIGNAL_PROCESSING_PROVENANCE",audit_record(x)["issues"])
    def test_uci_baseline_delta_not_r0(self):
        x=eligible_fixture();x["signal_processing"].update(feature_formula="MEDIAN_RESPONSE_MINUS_MEDIAN_BASELINE",r0_mode="NOT_USED",source_signal_units="SOURCE_UNIT_UNVERIFIED",derived_signal_units="SOURCE_UNIT_UNVERIFIED",baseline_window_s=[180,300],response_window_s=[780,900]);x["signal_processing"].pop("r0_reference")
        self.assertIn("SOURCE_SIGNAL_UNITS_UNVERIFIED_FOR_G2",audit_record(x)["issues"])
    def test_unverified_physical_source(self):
        x=eligible_fixture();x["source_kind"]="SYNTHETIC"
        self.assertIn("NOT_INDEPENDENT_MEASURED_SOURCE",audit_record(x)["issues"])
    def test_empty_80_evidence(self):
        a=audit_all([f"{i:02d}.1" for i in range(1,81)],[])
        self.assertEqual(sum(x["status"]=="G2_BLOCKED" for x in a),80)

class LegacyContractTests(unittest.TestCase):
    def test_missing_source_fails_closed(self):
        self.assertEqual(audit_record({"id":"01.1"})["status"],"G2_BLOCKED")
    def test_synthetic_source_rejected(self):
        self.assertIn("NOT_INDEPENDENT_MEASURED_SOURCE",audit_record({"id":"01.1","source_kind":"SYNTHETIC"})["issues"])
    def test_group_overlap_rejected(self):
        self.assertIn("GROUP_LEAKAGE",audit_record({"id":"01.1","train_group_ids":["session1"],"test_group_ids":["session1"]})["issues"])
    def test_no_test_tuning(self):
        self.assertIn("TEST_SET_SELECTION_LEAKAGE_OR_UNKNOWN",audit_record({"id":"01.1","hyperparameters_selected_on_test":True})["issues"])
    def test_80_missing_sources_all_blocked(self):
        ids=[str(i) for i in range(80)]
        a=audit_all(ids,[])
        self.assertEqual(len(a),80)
        self.assertTrue(all(x["status"]=="G2_BLOCKED" for x in a))
    def test_former_minimal_fake_candidate_now_blocked(self):
        from g2_evidence_audit import REQUIRED
        item={k:"sha-or-value" for k in REQUIRED}
        item.update(id="01.1",source_kind="INDEPENDENT_MEASURED",train_group_ids=["a"],test_group_ids=["b"],hyperparameters_selected_on_test=False,blind_holdout=True,measurement_units="Rs/R0",confidence_interval=[.1,.2],source_raw_data_accessible=True)
        self.assertEqual(audit_record(item)["status"],"G2_BLOCKED")

if __name__=="__main__": unittest.main()
