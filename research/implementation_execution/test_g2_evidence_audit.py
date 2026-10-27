"""G2 audit regression tests; synthetic fixtures never become G2 evidence."""
import unittest
import tempfile
from pathlib import Path
from g2_evidence_audit import (audit_record, audit_all, validate_ledger_scope,
                               load_g2_inputs, load_canonical_technique_ids)
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
              "measurement_units":"OHM", "confidence_interval":{"low":0.0,"high":1.0, "method":"GROUP_BOOTSTRAP",
                                     "grouping_key":"device_session_id", "independent_test_groups":2,
                                     "seed":20261026, "resamples":2000, "confidence_level":0.95,
                                     "resampling_unit":"group_id", "statistic":"pooled_accuracy",
                                     "quantile_method":"linear"},
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

class StrictInputManifestTests(unittest.TestCase):
    """Synthetic parser controls; no gas or ppm qualification is implied."""

    def read_pair(self, evidence_text, ledger_text=None):
        if ledger_text is None:
            ledger_text = ('{"schema_version":"hazrob.tank.chemcore80.execution-ledger-v4",'
                           '"work_items":[{"id":"01.1"}]}')
        with tempfile.TemporaryDirectory() as temp:
            ledger = Path(temp) / "ledger.json"
            evidence = Path(temp) / "evidence.json"
            ledger.write_text(ledger_text, encoding="utf-8")
            evidence.write_text(evidence_text, encoding="utf-8")
            return load_g2_inputs(ledger, evidence)

    def test_valid_empty_register_is_explicit_not_inferred(self):
        ledger, evidence = self.read_pair(
            '{"schema":"hazrob.g2.independent-evidence.v1","records":[]}')
        self.assertEqual(evidence["records"], [])
        self.assertEqual(ledger["work_items"][0]["id"], "01.1")

    def test_duplicate_top_level_records_key_rejected(self):
        with self.assertRaisesRegex(ValueError, "G2_INPUT_DUPLICATE_JSON_KEY:records"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1",'
                '"records":[{"id":"01.1"}],"records":[]}')

    def test_duplicate_nested_evidence_record_key_rejected(self):
        with self.assertRaisesRegex(ValueError, "G2_INPUT_DUPLICATE_JSON_KEY:id"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1",'
                '"records":[{"id":"01.1","id":"02.1"}]}')

    def test_duplicate_ledger_work_items_key_rejected(self):
        with self.assertRaisesRegex(ValueError, "G2_INPUT_DUPLICATE_JSON_KEY:work_items"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1","records":[]}',
                '{"schema_version":"hazrob.tank.chemcore80.execution-ledger-v4",'
                '"work_items":[{"id":"01.1"}],"work_items":[]}')

    def test_nan_in_evidence_register_rejected(self):
        with self.assertRaisesRegex(ValueError, "G2_INPUT_NONFINITE_NUMBER:NaN"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1",'
                '"records":[{"id":"01.1","metric_value":NaN}]}')

    def test_overflowed_json_number_rejected(self):
        with self.assertRaisesRegex(ValueError, "G2_INPUT_NONFINITE_NUMBER:1e9999"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1",'
                '"records":[{"id":"01.1","metric_value":1e9999}]}')

    def test_huge_integer_in_evidence_register_fails_closed(self):
        huge = "9" * 310
        with self.assertRaisesRegex(ValueError, "G2_INPUT_INTEGER_OUT_OF_RANGE"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1",'
                '"records":[{"id":"01.1","metric_value":' + huge + '}]}')

    def test_huge_integer_in_execution_ledger_fails_closed(self):
        huge = "9" * 310
        with self.assertRaisesRegex(ValueError, "G2_INPUT_INTEGER_OUT_OF_RANGE"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1","records":[]}',
                '{"schema_version":"hazrob.tank.chemcore80.execution-ledger-v4",'
                '"work_items":[{"id":"01.1","source_counter":' + huge + '}]}')

    def test_missing_records_not_silently_treated_as_empty(self):
        with self.assertRaisesRegex(ValueError, "G2_EVIDENCE_REGISTER_SCHEMA_INVALID"):
            self.read_pair('{"schema":"hazrob.g2.independent-evidence.v1"}')

    def test_wrong_schema_and_invalid_ledger_fail_closed(self):
        with self.assertRaisesRegex(ValueError, "G2_EVIDENCE_REGISTER_SCHEMA_INVALID"):
            self.read_pair('{"schema":"unknown","records":[]}')
        with self.assertRaisesRegex(ValueError, "G2_EXECUTION_LEDGER_SCHEMA_INVALID"):
            self.read_pair(
                '{"schema":"hazrob.g2.independent-evidence.v1","records":[]}',
                '{"schema_version":"hazrob.tank.chemcore80.execution-ledger-v4",'
                '"work_items":[{"id":null}]}')



class CanonicalTechniqueScopeTests(unittest.TestCase):
    """A passing 80-count is not evidence that the correct 80 IDs were audited."""

    def test_actual_research_registry_and_ledger_match(self):
        research = Path(__file__).resolve().parents[1]
        ids = load_canonical_technique_ids(research / "PROGRESS_80.json")
        ledger, evidence = load_g2_inputs(
            Path(__file__).resolve().parent / "EXECUTION_LEDGER_2026-10-21.json",
            Path(__file__).resolve().parent / "G2_INDEPENDENT_EVIDENCE.json")
        self.assertEqual(len(ids), 80)
        self.assertTrue(validate_ledger_scope(
            [item["id"] for item in ledger["work_items"]],
            evidence["records"], canonical_ids=ids))

    def test_substituted_id_with_same_total_is_rejected(self):
        canonical = [f"T{i:02d}" for i in range(80)]
        substituted = canonical[:-1] + ["T80"]
        with self.assertRaisesRegex(ValueError, "G2_LEDGER_CANONICAL_TECHNIQUE_IDS_MISMATCH"):
            validate_ledger_scope(substituted, [], canonical_ids=canonical)

    def test_invalid_record_ids_fail_closed_without_type_error(self):
        ids = [f"T{i:02d}" for i in range(80)]
        for bad in (None, [], {}, 1, "", "  ", "T01 "):
            with self.subTest(bad=bad):
                with self.assertRaisesRegex(ValueError, "INVALID_G2_EVIDENCE_RECORD_ID"):
                    validate_ledger_scope(ids, [{"id": bad}])

    def test_invalid_ledger_id_type_fails_closed(self):
        ids = [f"T{i:02d}" for i in range(79)] + [[]]
        with self.assertRaisesRegex(ValueError, "G2_LEDGER_SCOPE"):
            validate_ledger_scope(ids, [])

    def test_registry_requires_80_unique_well_formed_ids(self):
        import json
        ids = [f"01.{i}" for i in range(1, 81)]
        valid = {"total_techniques": 80,
                 "techniques": [{"id": tid} for tid in ids]}
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "progress.json"
            path.write_text(json.dumps(valid), encoding="utf-8")
            self.assertEqual(load_canonical_technique_ids(path), ids)
            invalid_cases = [
                {**valid, "total_techniques": 79},
                {**valid, "techniques": valid["techniques"][:-1]},
                {**valid, "techniques": valid["techniques"][:-1] + [valid["techniques"][0]]},
                {**valid, "techniques": valid["techniques"][:-1] + [{"id": "bad"}]},
                {**valid, "techniques": valid["techniques"][:-1] + [{"id": []}]},
            ]
            for case in invalid_cases:
                with self.subTest(case=case["techniques"][-1]):
                    path.write_text(json.dumps(case), encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "G2_CANONICAL_TECHNIQUE_REGISTRY_INVALID"):
                        load_canonical_technique_ids(path)
            path.unlink()
            with self.assertRaisesRegex(ValueError, "G2_CANONICAL_TECHNIQUE_REGISTRY_INVALID"):
                load_canonical_technique_ids(path)


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
