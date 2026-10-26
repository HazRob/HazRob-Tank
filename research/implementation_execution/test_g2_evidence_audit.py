"""G2 audit regression tests; synthetic fixtures never become G2 evidence."""
import unittest
from g2_evidence_audit import audit_record, audit_all, validate_ledger_scope
from test_g2_signal_lineage_gate import normalized

SHA = "a"*64

def eligible_fixture():
    x=normalized()
    x.update({"id":"01.2", "source_kind":"INDEPENDENT_MEASURED", "raw_sha256":SHA,
              "dataset_uri":"https://example.org/dataset", "sensor_metadata":{"MQ135":"unit-001"},
              "grouping_key":"device_session_id", "grouping_source_verified":True,
              "grouping_source_field":"device_id,session_id", "train_group_ids":["d1-s1"],
              "calibration_group_ids":["d1-s2"], "test_group_ids":["d2-s1","d2-s2"], "target_definition":"lab gas reference",
              "metric_name":"classification_accuracy", "metric_value":0.5,
              "baseline_metric_value":0.5, "protocol_sha256":SHA,
              "evaluation_artifact_sha256":SHA, "raw_sha256_verified_from_bytes":True,
              "hyperparameters_selected_on_test":False, "blind_holdout":True,
              "measurement_units":"OHM", "confidence_interval":{"low":0.4,"high":0.9, "method":"GROUP_BOOTSTRAP",
                                     "grouping_key":"device_session_id", "independent_test_groups":2},
              "source_raw_data_accessible":True})
    return x

class AuditTests(unittest.TestCase):
    def test_synthetic_metadata_without_artifacts_must_be_blocked(self):
        result = audit_record(eligible_fixture())
        self.assertEqual(result["status"], "G2_BLOCKED")
        self.assertIn("INVALID_EVIDENCE_BUNDLE_ID_OR_PATH", result["issues"])
    def test_actual_uci_ordinal_is_not_verified_group(self):
        x=eligible_fixture();x.update(grouping_key="replicate_ordinal",grouping_source_verified=False)
        self.assertIn("GROUPING_NOT_VERIFIED_PHYSICAL_UNIT",audit_record(x)["issues"])
    def test_group_overlap(self):
        x=eligible_fixture();x["test_group_ids"]=["d1-s1"]
        self.assertIn("GROUP_LEAKAGE",audit_record(x)["issues"])
    def test_calibration_group_overlaps_train(self):
        x=eligible_fixture();x["calibration_group_ids"]=["d1-s1"]
        self.assertIn("GROUP_LEAKAGE",audit_record(x)["issues"])
    def test_calibration_group_overlaps_test(self):
        x=eligible_fixture();x["calibration_group_ids"]=["d2-s1"]
        self.assertIn("GROUP_LEAKAGE",audit_record(x)["issues"])
    def test_calibration_group_missing(self):
        x=eligible_fixture();del x["calibration_group_ids"]
        self.assertIn("GROUP_TRAIN_CALIBRATION_TEST_NOT_DOCUMENTED",audit_record(x)["issues"])
    def test_duplicate_evidence_record_cannot_override(self):
        x=eligible_fixture();y=eligible_fixture();y["metric_value"]=0.99
        r=audit_all(["01.2"],[x,y])[0]
        self.assertEqual(r["status"],"G2_BLOCKED")
        self.assertIn("DUPLICATE_EVIDENCE_RECORD_ID",r["issues"])
    def test_invalid_confidence_interval(self):
        x=eligible_fixture();x["confidence_interval"]={"low":0.95,"high":0.7}
        self.assertIn("INVALID_UNCERTAINTY_INTERVAL",audit_record(x)["issues"])
    def test_row_level_interval_cannot_qualify(self):
        x=eligible_fixture()
        x["confidence_interval"]["method"]="ROW_BOOTSTRAP"
        self.assertIn("UNCERTAINTY_GROUPING_NOT_VERIFIED",audit_record(x)["issues"])
    def test_grouping_mismatch_in_interval_cannot_qualify(self):
        x=eligible_fixture()
        x["confidence_interval"]["grouping_key"]="replicate_ordinal"
        self.assertIn("UNCERTAINTY_GROUPING_NOT_VERIFIED",audit_record(x)["issues"])
    def test_metric_outside_interval_cannot_qualify(self):
        x=eligible_fixture()
        x["confidence_interval"].update(low=0.1, high=0.2)
        self.assertIn("METRIC_OUTSIDE_REPORTED_CONFIDENCE_INTERVAL",audit_record(x)["issues"])
    def test_interval_group_count_must_match_test_split(self):
        x=eligible_fixture()
        x["confidence_interval"]["independent_test_groups"]=3
        self.assertIn("UNCERTAINTY_GROUPING_NOT_VERIFIED",audit_record(x)["issues"])
    def test_one_independent_test_group_cannot_qualify_uncertainty(self):
        x=eligible_fixture()
        x["confidence_interval"]["independent_test_groups"]=1
        self.assertIn("UNCERTAINTY_GROUPING_NOT_VERIFIED",audit_record(x)["issues"])
    def test_classification_metric_cannot_exceed_probability_domain(self):
        for field, value in (("metric_value", 1.01),
                             ("baseline_metric_value", -0.01)):
            x = eligible_fixture()
            x[field] = value
            self.assertIn("CLASSIFICATION_ACCURACY_OUT_OF_RANGE:" + field,
                          audit_record(x)["issues"])

    def test_classification_interval_must_be_within_zero_one(self):
        for field, value in (("low", -0.001), ("high", 1.001)):
            x = eligible_fixture()
            x["confidence_interval"][field] = value
            self.assertIn("CLASSIFICATION_INTERVAL_OUT_OF_RANGE:" + field,
                          audit_record(x)["issues"])

    def test_other_metric_families_do_not_inherit_accuracy_range(self):
        x = eligible_fixture()
        x.update(metric_name="rmse", metric_value=2.0, baseline_metric_value=3.0)
        x["confidence_interval"].update(low=1.5, high=2.5)
        issues = audit_record(x)["issues"]
        self.assertFalse(any(i.startswith("CLASSIFICATION_") for i in issues))

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
    def test_scope_exactly_80_unique_ids(self):
        ids = [f"T{i:02d}" for i in range(80)]
        self.assertTrue(validate_ledger_scope(ids, []))
        for broken in (ids[:-1], ids[:-1] + [ids[0]], ids[:-1] + [""]):
            with self.assertRaisesRegex(ValueError, "G2_LEDGER_SCOPE"):
                validate_ledger_scope(broken, [])

    def test_scope_unknown_evidence_record_fails(self):
        ids = [f"T{i:02d}" for i in range(80)]
        with self.assertRaisesRegex(ValueError, "UNKNOWN_G2_EVIDENCE"):
            validate_ledger_scope(ids, [{"id": "not-in-ledger"}])

    def test_scope_malformed_evidence_fails(self):
        ids = [f"T{i:02d}" for i in range(80)]
        with self.assertRaisesRegex(ValueError, "INVALID_G2_EVIDENCE"):
            validate_ledger_scope(ids, ["bad"])

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
