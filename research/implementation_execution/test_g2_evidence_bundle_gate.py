"""Negative controls for G2 evidence artifact availability and integrity.

Temporary synthetic files validate software only, not gas identity or ppm.
"""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from g2_evidence_bundle_gate import FILES, audit_evidence_bundle
from g2_evidence_audit import audit_record
from g2_cross_artifact_gate import audit_cross_artifact
from g2_raw_source_gate import audit_raw_source_bytes
from test_g2_evidence_audit import eligible_fixture


def synthetic_bundle(root):
    record = eligible_fixture()
    record.update(dataset_id="synthetic-dataset", run_id="synthetic-run")
    # Original source bytes are mandatory even for a synthetic control.
    raw_bytes = b"synthetic raw source: not a physical gas measurement\\n"
    raw_file = Path(root) / "research/g2/raw/synthetic_source.bin"
    raw_file.parent.mkdir(parents=True, exist_ok=True)
    raw_file.write_bytes(raw_bytes)
    record["raw_sha256"] = hashlib.sha256(raw_bytes).hexdigest()
    base = (Path(root) / "research/g2/evidence" / record["id"]
            / record["dataset_id"] / record["run_id"])
    base.mkdir(parents=True)
    documents = {
        "raw_manifest.json": {
            "raw_sha256": record["raw_sha256"], "dataset_uri": record["dataset_uri"],
            "raw_local_path": "research/g2/raw/synthetic_source.bin",
            "raw_size_bytes": len(raw_bytes)
        },
        "source_qualification.json": {
            key: record[key] for key in
            ("source_kind", "dataset_uri", "raw_sha256",
             "raw_sha256_verified_from_bytes", "source_raw_data_accessible")
        },
        "signal_lineage.json": {
            key: record[key] for key in
            ("signal_processing", "measurement_units", "sensor_metadata")
        },
        "split_manifest.json": {
            key: record[key] for key in
            ("train_group_ids", "calibration_group_ids", "test_group_ids")
        },
        "calibration_manifest.json": {
            "calibration_group_ids": record["calibration_group_ids"]
        },
        "protocol_locked.json": {
            key: record[key] for key in
            ("target_definition", "metric_name", "grouping_key",
             "train_group_ids", "calibration_group_ids", "test_group_ids")
        } | {"baseline_model": {
            "algorithm_id": "majority_train", "fit_policy": "TRAIN_ONLY",
            "fitted_group_ids": ["d1-s1"],
            "prediction_column": "baseline_predicted_reference",
            "selected_on_test": False}},
        "metrics_summary.json": {
            key: record[key] for key in
            ("metric_name", "metric_value", "baseline_metric_value")
        },
        "uncertainty_grouped.json": record["confidence_interval"],
    }
    csv_payloads = {
        "group_manifest.csv": "exposure_id,group_id,gas_reference_id,device_id,session_id\ntr,d1-s1,GAS_A,d1,s1\nca,d1-s2,GAS_B,d1,s2\nte1,d2-s1,GAS_A,d2,s1\nte2,d2-s2,GAS_B,d2,s2\n",
        "predictions_blind.csv": "exposure_id,group_id,true_reference,predicted_reference,baseline_predicted_reference\nte1,d2-s1,GAS_A,GAS_A,GAS_A\nte2,d2-s2,GAS_B,GAS_A,GAS_A\n",
        "metrics_per_group.csv": "group_id,sample_count,correct_count,baseline_correct_count\nd2-s1,1,1,1\nd2-s2,1,0,0\n",
    }
    manifest = {}
    for name in FILES:
        if name.endswith(".json"):
            payload = (json.dumps(documents.get(name, {"fixture": "synthetic", "file": name})) + "\n").encode()
        elif name.endswith(".csv"):
            payload = csv_payloads[name].encode()
        else:
            payload = b"#!/bin/sh\nexit 0\n"
        (base / name).write_bytes(payload)
        manifest[name] = hashlib.sha256(payload).hexdigest()
    record["evidence_files"] = manifest
    record["protocol_sha256"] = manifest["protocol_locked.json"]
    record["evaluation_artifact_sha256"] = manifest["predictions_blind.csv"]
    return record, base


class EvidenceBundleTests(unittest.TestCase):
    def test_raw_source_bytes_required_for_review_candidate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw_file = Path(temp) / "research/g2/raw/synthetic_source.bin"
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")
            raw_file.unlink()
            result = audit_record(record, temp)
            self.assertEqual(result["status"], "G2_BLOCKED")
            self.assertIn("RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE", result["issues"])

    def test_rehashed_raw_manifest_does_not_prove_source_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw_file = Path(temp) / "research/g2/raw/synthetic_source.bin"
            raw_file.write_bytes(b"modified synthetic raw source")
            # The evidence bundle is still intact; only raw source bytes drifted.
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("RAW_SOURCE_BYTE_SIZE_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_raw_source_digest_mismatch_with_same_byte_count(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw_file = Path(temp) / "research/g2/raw/synthetic_source.bin"
            original = raw_file.read_bytes()
            raw_file.write_bytes(b"X" + original[1:])
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("RAW_SOURCE_BYTE_DIGEST_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_raw_source_manifest_path_traversal_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            manifest = json.loads((base / "raw_manifest.json").read_text())
            for raw_path in ("../../secret.bin", "/tmp/secret.bin",
                             "research/g2/raw/../synthetic_source.bin",
                             "research/g2/raw\\\\synthetic_source.bin"):
                with self.subTest(path=raw_path):
                    manifest["raw_local_path"] = raw_path
                    self.replace_bytes(record, base, "raw_manifest.json",
                                       json.dumps(manifest))
                    self.assertIn("RAW_SOURCE_LOCAL_PATH_MISSING_OR_UNSAFE",
                                  audit_record(record, temp)["issues"])

    def test_raw_source_symlink_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw_file = Path(temp) / "research/g2/raw/synthetic_source.bin"
            target = raw_file.with_name("real_source.bin")
            raw_file.rename(target)
            raw_file.symlink_to(target)
            self.assertIn("RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE",
                          audit_record(record, temp)["issues"])

    def test_unmaterialized_lfs_pointer_cannot_pass_raw_byte_gate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            pointer = (b"version https://git-lfs.github.com/spec/v1\\n"
                       b"oid sha256:" + b"0" * 64 + b"\\nsize 100000\\n")
            raw_file = Path(temp) / "research/g2/raw/synthetic_source.bin"
            raw_file.write_bytes(pointer)
            manifest = json.loads((base / "raw_manifest.json").read_text())
            manifest["raw_sha256"] = hashlib.sha256(pointer).hexdigest()
            manifest["raw_size_bytes"] = len(pointer)
            record["raw_sha256"] = manifest["raw_sha256"]
            self.replace_bytes(record, base, "raw_manifest.json",
                               json.dumps(manifest))
            self.assertIn("RAW_SOURCE_GIT_LFS_POINTER_NOT_MATERIALIZED",
                          audit_raw_source_bytes(record, temp))

    def test_raw_source_size_metadata_is_mandatory(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            manifest = json.loads((base / "raw_manifest.json").read_text())
            manifest.pop("raw_size_bytes")
            self.replace_bytes(record, base, "raw_manifest.json",
                               json.dumps(manifest))
            self.assertIn("RAW_SOURCE_SIZE_NOT_DOCUMENTED",
                          audit_record(record, temp)["issues"])

    def test_metadata_only_cannot_be_candidate(self):
        record = eligible_fixture()
        record.update(dataset_id="d", run_id="r")
        self.assertIn("MISSING_EVIDENCE_BUNDLE", audit_evidence_bundle(record))

    def test_all_fourteen_byte_verified_files_can_reach_review_only(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_locked_protocol_missing_baseline_cannot_pass(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol.pop("baseline_model")
            self.replace_bytes(record, base, "protocol_locked.json", json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_PROTOCOL_NOT_LOCKED", audit_record(record, temp)["issues"])

    def test_locked_baseline_cannot_fit_blind_group(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["baseline_model"]["fitted_group_ids"] = ["d2-s1"]
            self.replace_bytes(record, base, "protocol_locked.json", json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_TRAINING_GROUP_LEAKAGE_OR_INVALID_FIT",
                          audit_record(record, temp)["issues"])

    def test_locked_baseline_cannot_be_selected_on_blind_test(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["baseline_model"]["selected_on_test"] = True
            self.replace_bytes(record, base, "protocol_locked.json", json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_TEST_SELECTION_NOT_FROZEN",
                          audit_record(record, temp)["issues"])

    def test_locked_protocol_cannot_relabel_target_or_splits(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["test_group_ids"] = ["d1-s1"]
            self.replace_bytes(record, base, "protocol_locked.json", json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("LOCKED_PROTOCOL_RECORD_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_protocol_digest_must_match_byte_verified_locked_protocol(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            record["protocol_sha256"] = "f" * 64
            issues = audit_evidence_bundle(record, temp)
            self.assertIn("PROTOCOL_DIGEST_NOT_BOUND_TO_LOCKED_FILE", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_rehashed_protocol_cannot_bypass_stale_protocol_claim(self):
        # An attacker could edit the locked protocol and update the file
        # digest in the bundle manifest while retaining the old claim digest.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(
                record, base, "protocol_locked.json",
                json.dumps({"fixture": "synthetic", "changed_after_lock": True}))
            self.assertEqual(
                hashlib.sha256((base / "protocol_locked.json").read_bytes()).hexdigest(),
                record["evidence_files"]["protocol_locked.json"])
            self.assertIn("PROTOCOL_DIGEST_NOT_BOUND_TO_LOCKED_FILE",
                          audit_evidence_bundle(record, temp))
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_consistent_protocol_hashes_are_integrity_only(self):
        # Hash agreement alone never constitutes an independent review.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(
                record, base, "protocol_locked.json",
                json.dumps({"fixture": "synthetic", "new_protocol": True}))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("LOCKED_PROTOCOL_RECORD_MISMATCH",
                          audit_record(record, temp)["issues"])
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_evaluation_digest_must_bind_blind_prediction_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            record["evaluation_artifact_sha256"] = "f" * 64
            self.assertIn("EVALUATION_DIGEST_NOT_BOUND_TO_BLIND_PREDICTIONS",
                          audit_evidence_bundle(record, temp))
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_rehashed_predictions_cannot_bypass_stale_evaluation_claim(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            original = (base / "predictions_blind.csv").read_text()
            self.replace_bytes(record, base, "predictions_blind.csv",
                               original.replace("GAS_A,GAS_A,GAS_A", "GAS_A,GAS_B,GAS_A"))
            self.assertIn("EVALUATION_DIGEST_NOT_BOUND_TO_BLIND_PREDICTIONS",
                          audit_evidence_bundle(record, temp))
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_updated_evaluation_hash_is_integrity_only_not_metric_approval(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            original = (base / "predictions_blind.csv").read_text()
            self.replace_bytes(record, base, "predictions_blind.csv",
                               original.replace("GAS_A,GAS_A,GAS_A", "GAS_A,GAS_B,GAS_A"))
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            result = audit_record(record, temp)
            self.assertEqual(result["status"], "G2_BLOCKED")
            self.assertIn("BLIND_ACCURACY_RECOMPUTATION_MISMATCH", result["issues"])

    def test_non_accuracy_metric_with_matching_hashes_cannot_reach_review(self):
        # These metrics require their own exposure-level recomputation and
        # grouped uncertainty algorithms; repeated summaries prove neither.
        for metric in ("rmse", "macro_f1", "balanced_accuracy", "custom_score"):
            with self.subTest(metric=metric), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                record["metric_name"] = metric
                self.replace_bytes(
                    record, base, "metrics_summary.json",
                    json.dumps({
                        "metric_name": metric,
                        "metric_value": record["metric_value"],
                        "baseline_metric_value": record["baseline_metric_value"],
                    }))
                self.assertEqual(audit_evidence_bundle(record, temp), [])
                result = audit_record(record, temp)
                self.assertIn("METRIC_FAMILY_NOT_RECOMPUTABLE", result["issues"])
                self.assertEqual(result["status"], "G2_BLOCKED")

    @staticmethod
    def replace_bytes(record, base, name, data):
        data = data.encode("utf-8")
        (base / name).write_bytes(data)
        record["evidence_files"][name] = hashlib.sha256(data).hexdigest()

    def test_duplicate_csv_headers_cannot_hide_conflicting_physical_fields(self):
        # DictReader normally silently keeps the last occurrence of a name.
        # Hash verification alone cannot disambiguate the original columns.
        for filename in ("group_manifest.csv", "predictions_blind.csv",
                         "metrics_per_group.csv"):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                lines = (base / filename).read_text().splitlines()
                column = lines[0].split(",")[0]
                altered = [lines[0] + "," + column]
                altered += [line + "," + line.split(",")[0] for line in lines[1:]]
                self.replace_bytes(record, base, filename, "\n".join(altered) + "\n")
                self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + filename,
                              audit_evidence_bundle(record, temp))
                result = audit_record(record, temp)
                self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                              audit_cross_artifact(record, temp))
                self.assertEqual(result["status"], "G2_BLOCKED")

    def test_surplus_csv_cells_cannot_be_silently_discarded(self):
        # DictReader stores unlabelled surplus cells under the None key.
        for filename in ("group_manifest.csv", "predictions_blind.csv",
                         "metrics_per_group.csv"):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                lines = (base / filename).read_text().splitlines()
                lines[1] += ",EXTRA"
                self.replace_bytes(record, base, filename, "\n".join(lines) + "\n")
                self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + filename,
                              audit_evidence_bundle(record, temp))
                self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                              audit_cross_artifact(record, temp))

    def test_missing_csv_cells_in_unused_provenance_column_are_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            filename = "group_manifest.csv"
            lines = (base / filename).read_text().splitlines()
            altered = [lines[0] + ",extra_provenance"]
            altered += [line + ",tag" for line in lines[1:]]
            altered[2] = lines[2]  # Short row, all mandatory columns still present.
            self.replace_bytes(record, base, filename, "\n".join(altered) + "\n")
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + filename,
                              audit_evidence_bundle(record, temp))
            self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                          audit_cross_artifact(record, temp))

    def test_physical_group_columns_required_not_just_metadata(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id\n"
                "tr,d1-s1,GAS_A\nca,d1-s2,GAS_B\n"
                "te1,d2-s1,GAS_A\nte2,d2-s2,GAS_B\n")
            self.assertIn("GROUPING_SOURCE_COLUMNS_MISSING",
                          audit_record(record, temp)["issues"])

    def test_sample_id_cannot_masquerade_as_device_session(self):
        # One sample per group previously let an arbitrary sample_id appear
        # to be an independent physical session, even with matching SHA256.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["grouping_source_field"] = "sample_id"
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,sample_id\n"
                "tr,d1-s1,GAS_A,unique1\nca,d1-s2,GAS_B,unique2\n"
                "te1,d2-s1,GAS_A,unique3\nte2,d2-s2,GAS_B,unique4\n")
            issues = audit_record(record, temp)["issues"]
            self.assertIn("GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_device_session_requires_both_physical_columns(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["grouping_source_field"] = "session_id"
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,session_id\n"
                "tr,d1-s1,GAS_A,s1\nca,d1-s2,GAS_B,s2\n"
                "te1,d2-s1,GAS_A,s3\nte2,d2-s2,GAS_B,s4\n")
            self.assertIn("GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY",
                          audit_record(record, temp)["issues"])

    def test_session_grouping_accepts_real_session_column(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["grouping_key"] = "session_id"
            record["grouping_source_field"] = "session_id"
            record["confidence_interval"]["grouping_key"] = "session_id"
            # Session-only identity requires globally distinct session IDs.
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id\n"
                "tr,d1-s1,GAS_A,d1,unique-s1\n"
                "ca,d1-s2,GAS_B,d1,unique-s2\n"
                "te1,d2-s1,GAS_A,d2,unique-s3\n"
                "te2,d2-s2,GAS_B,d2,unique-s4\n")
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["grouping_key"] = "session_id"
            self.replace_bytes(record, base, "protocol_locked.json", json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                               json.dumps(record["confidence_interval"]))
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_extra_sample_id_cannot_disguise_reused_physical_session(self):
        # Regression: adding sample_id to declared provenance previously
        # made the same device/session appear independent across splits.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["grouping_source_field"] = "device_id,session_id,sample_id"
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id,sample_id\n"
                "tr,d1-s1,GAS_A,d1,s1,train-unique\n"
                "ca,d1-s2,GAS_B,d1,s2,calibration-unique\n"
                "te1,d2-s1,GAS_A,d1,s1,blind-unique\n"
                "te2,d2-s2,GAS_B,d2,s2,blind-two\n")
            issues = audit_record(record, temp)["issues"]
            self.assertIn("PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_extra_provenance_column_does_not_block_distinct_sessions(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["grouping_source_field"] = "device_id,session_id,sample_id"
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id,sample_id\n"
                "tr,d1-s1,GAS_A,d1,s1,sample-1\n"
                "ca,d1-s2,GAS_B,d1,s2,sample-2\n"
                "te1,d2-s1,GAS_A,d2,s1,sample-3\n"
                "te2,d2-s2,GAS_B,d2,s2,sample-4\n")
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_physical_session_alias_across_split_ids_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id\n"
                "tr,d1-s1,GAS_A,d1,s1\nca,d1-s2,GAS_B,d1,s2\n"
                "te1,d2-s1,GAS_A,d1,s1\nte2,d2-s2,GAS_B,d2,s2\n")
            self.assertIn("PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS",
                          audit_record(record, temp)["issues"])

    def test_one_group_cannot_represent_two_physical_sessions(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id\n"
                "tr,d1-s1,GAS_A,d1,s1\nca,d1-s2,GAS_B,d1,s2\n"
                "te1,d2-s1,GAS_A,d2,s1\nte1b,d2-s1,GAS_A,d2,s9\n"
                "te2,d2-s2,GAS_B,d2,s2\n")
            self.assertIn("GROUP_ID_MAPS_TO_MULTIPLE_PHYSICAL_UNITS",
                          audit_record(record, temp)["issues"])

    def test_blank_source_session_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id,gas_reference_id,device_id,session_id\n"
                "tr,d1-s1,GAS_A,d1,s1\nca,d1-s2,GAS_B,d1,s2\n"
                "te1,d2-s1,GAS_A,d2,\nte2,d2-s2,GAS_B,d2,s2\n")
            self.assertIn("GROUPING_SOURCE_VALUE_MISSING",
                          audit_record(record, temp)["issues"])

    def test_split_manifest_mismatch_with_valid_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "split_manifest.json",
                '{"train_group_ids":["d2-s1"],"calibration_group_ids":["d1-s2"],'
                '"test_group_ids":["d1-s1","d2-s2"]}')
            self.assertIn("SPLIT_MANIFEST_RECORD_MISMATCH", audit_record(record, temp)["issues"])

    def test_train_exposure_leaked_to_blind_predictions_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id\ntr,d1-s1\nte2,d2-s2\n")
            # Rebind the altered synthetic prediction bytes to test the deeper
            # arithmetic/leakage gate rather than failing at digest integrity.
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            issues = audit_record(record, temp)["issues"]
            self.assertIn("BLIND_PREDICTION_EXPOSURE_OR_GROUP_LEAKAGE", issues)
            self.assertIn("BLIND_PREDICTION_COVERAGE_MISMATCH", issues)

    def test_duplicate_exposure_with_valid_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "group_manifest.csv",
                "exposure_id,group_id\ntr,d1-s1\nca,d1-s2\nte1,d2-s1\nte1,d2-s2\n")
            self.assertIn("DUPLICATE_OR_EMPTY_EXPOSURE_ID", audit_record(record, temp)["issues"])

    def test_wrong_per_group_sample_count_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_per_group.csv",
                "group_id,sample_count,correct_count,baseline_correct_count\nd2-s1,2,1,1\nd2-s2,1,0,0\n")
            self.assertIn("METRICS_PER_GROUP_COUNT_MISMATCH", audit_record(record, temp)["issues"])

    def test_metric_summary_conflicts_with_evidence_record(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_summary.json",
                '{"metric_name":"classification_accuracy","metric_value":0.99,'
                '"baseline_metric_value":0.5}')
            self.assertIn("METRICS_SUMMARY_RECORD_MISMATCH", audit_record(record, temp)["issues"])

    def test_uncertainty_conflicts_with_evidence_record(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                '{"low":0.7,"high":0.9,"method":"ROW_BOOTSTRAP",'
                '"grouping_key":"device_session_id","independent_test_groups":2}')
            self.assertIn("UNCERTAINTY_RECORD_MISMATCH", audit_record(record, temp)["issues"])

    def test_source_qualification_conflict_even_with_matching_sha_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            source = json.loads((base / "source_qualification.json").read_text())
            source["source_kind"] = "SYNTHETIC"
            self.replace_bytes(record, base, "source_qualification.json",
                               json.dumps(source))
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("SOURCE_QUALIFICATION_RECORD_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_signal_lineage_conflict_even_with_matching_sha_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            signal = json.loads((base / "signal_lineage.json").read_text())
            signal["signal_processing"]["r0_mode"] = "NOT_USED"
            self.replace_bytes(record, base, "signal_lineage.json",
                               json.dumps(signal))
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("SIGNAL_LINEAGE_RECORD_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_placeholder_source_or_signal_cannot_be_candidate(self):
        for filename, issue in (
            ("source_qualification.json", "SOURCE_QUALIFICATION_RECORD_MISMATCH"),
            ("signal_lineage.json", "SIGNAL_LINEAGE_RECORD_MISMATCH"),
        ):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                self.replace_bytes(record, base, filename, '{"fixture":"synthetic"}')
                self.assertIn(issue, audit_record(record, temp)["issues"])

    def test_raw_manifest_conflicts_with_evidence_record(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "raw_manifest.json",
                '{"raw_sha256":"' + "b"*64 + '","dataset_uri":"https://example.org/dataset"}')
            self.assertIn("RAW_MANIFEST_RECORD_MISMATCH", audit_record(record, temp)["issues"])

    def test_inflated_accuracy_with_matching_record_and_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["metric_value"] = 1.0
            record["confidence_interval"]["high"] = 1.0
            self.replace_bytes(record, base, "metrics_summary.json",
                '{"metric_name":"classification_accuracy","metric_value":1.0,'
                '"baseline_metric_value":0.5}')
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                json.dumps(record["confidence_interval"]))
            self.assertIn("BLIND_ACCURACY_RECOMPUTATION_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_inflated_baseline_with_matching_json_and_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["baseline_metric_value"] = 0.9
            self.replace_bytes(record, base, "metrics_summary.json",
                '{"metric_name":"classification_accuracy","metric_value":0.5,'
                '"baseline_metric_value":0.9}')
            self.assertIn("BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_missing_baseline_predictions_block_classification_candidate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id,true_reference,predicted_reference\n"
                "te1,d2-s1,GAS_A,GAS_A\n"
                "te2,d2-s2,GAS_B,GAS_A\n")
            # Rebind the altered synthetic prediction bytes to test the deeper
            # arithmetic/leakage gate rather than failing at digest integrity.
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            self.assertIn("ACCURACY_RECOMPUTATION_FIELDS_MISSING",
                          audit_record(record, temp)["issues"])

    def test_baseline_predictions_must_match_declared_baseline(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id,true_reference,predicted_reference,baseline_predicted_reference\n"
                "te1,d2-s1,GAS_A,GAS_A,GAS_A\n"
                "te2,d2-s2,GAS_B,GAS_A,GAS_B\n")
            # Rebind the altered synthetic prediction bytes to test the deeper
            # arithmetic/leakage gate rather than failing at digest integrity.
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            self.assertIn("BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_false_truth_label_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id,true_reference,predicted_reference,baseline_predicted_reference\n"
                "te1,d2-s1,GAS_B,GAS_A,GAS_A\nte2,d2-s2,GAS_B,GAS_A,GAS_A\n")
            # Rebind the altered synthetic prediction bytes to test the deeper
            # arithmetic/leakage gate rather than failing at digest integrity.
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            self.assertIn("BLIND_PREDICTION_TRUTH_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_inflated_per_group_correct_count_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_per_group.csv",
                "group_id,sample_count,correct_count,baseline_correct_count\nd2-s1,1,1,1\nd2-s2,1,1,0\n")
            self.assertIn("PER_GROUP_CORRECT_COUNT_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_baseline_group_count_required_for_blind_audit(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_per_group.csv",
                "group_id,sample_count,correct_count\nd2-s1,1,1\nd2-s2,1,0\n")
            self.assertIn("ACCURACY_RECOMPUTATION_FIELDS_MISSING",
                          audit_record(record, temp)["issues"])

    def test_forged_baseline_per_group_counts_blocked_even_if_global_unchanged(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_per_group.csv",
                "group_id,sample_count,correct_count,baseline_correct_count\n"
                "d2-s1,1,1,0\nd2-s2,1,0,1\n")
            issues = audit_record(record, temp)["issues"]
            self.assertIn("PER_GROUP_BASELINE_CORRECT_COUNT_MISMATCH", issues)
            self.assertNotIn("BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH", issues)

    def test_baseline_prediction_group_swap_cannot_hide_behind_same_global_accuracy(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id,true_reference,predicted_reference,baseline_predicted_reference\n"
                "te1,d2-s1,GAS_A,GAS_A,GAS_B\n"
                "te2,d2-s2,GAS_B,GAS_A,GAS_B\n")
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            issues = audit_record(record, temp)["issues"]
            self.assertIn("PER_GROUP_BASELINE_CORRECT_COUNT_MISMATCH", issues)
            self.assertNotIn("BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH", issues)

    def test_accuracy_missing_truth_columns_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id\nte1,d2-s1\nte2,d2-s2\n")
            # Rebind the altered synthetic prediction bytes to test the deeper
            # arithmetic/leakage gate rather than failing at digest integrity.
            record["evaluation_artifact_sha256"] = record["evidence_files"]["predictions_blind.csv"]
            self.assertIn("ACCURACY_RECOMPUTATION_FIELDS_MISSING",
                          audit_record(record, temp)["issues"])

    def test_forged_group_bootstrap_interval_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["confidence_interval"].update(low=0.1, high=0.9)
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                               json.dumps(record["confidence_interval"]))
            self.assertIn("GROUP_BOOTSTRAP_INTERVAL_RECOMPUTATION_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_group_bootstrap_seed_missing_in_both_records_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            del record["confidence_interval"]["seed"]
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                               json.dumps(record["confidence_interval"]))
            self.assertIn("GROUP_BOOTSTRAP_PROTOCOL_INVALID",
                          audit_record(record, temp)["issues"])

    def test_group_bootstrap_metadata_disagreement_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            uncertainty = dict(record["confidence_interval"], resamples=5000)
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                               json.dumps(uncertainty))
            self.assertIn("UNCERTAINTY_RECORD_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_unsupported_classification_ci_method_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["confidence_interval"]["method"] = "CLUSTER_ROBUST"
            self.replace_bytes(record, base, "uncertainty_grouped.json",
                               json.dumps(record["confidence_interval"]))
            self.assertIn("CLASSIFICATION_INTERVAL_METHOD_NOT_RECOMPUTABLE",
                          audit_record(record, temp)["issues"])

    def test_bootstrap_group_order_invariance(self):
        from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy
        interval = dict(eligible_fixture()["confidence_interval"])
        counts = {"d2-s1": 1, "d2-s2": 1}
        correct = {"d2-s1": 1, "d2-s2": 0}
        self.assertEqual(audit_group_bootstrap_accuracy(counts, correct, interval), [])
        self.assertEqual(audit_group_bootstrap_accuracy(
            {"d2-s2": 1, "d2-s1": 1}, {"d2-s2": 0, "d2-s1": 1}, interval), [])

    def test_weighted_cluster_bootstrap_pinned_nontrivial_quantiles(self):
        from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy
        interval = dict(eligible_fixture()["confidence_interval"])
        # Four heterogeneous groups: point accuracy is 4/10, not the
        # unweighted mean of per-group accuracies (0.5625).
        counts = {"g1": 2, "g2": 3, "g3": 1, "g4": 4}
        correct = {"g1": 2, "g2": 0, "g3": 1, "g4": 1}
        interval.update(low=0.14178571428571435, high=1.0)
        self.assertEqual(audit_group_bootstrap_accuracy(counts, correct, interval), [])
        interval["low"] = 0.14
        self.assertIn("GROUP_BOOTSTRAP_INTERVAL_RECOMPUTATION_MISMATCH",
                      audit_group_bootstrap_accuracy(counts, correct, interval))

    def test_group_bootstrap_rejects_invalid_protocol_and_group_counts(self):
        from g2_cluster_bootstrap_gate import audit_group_bootstrap_accuracy
        base = eligible_fixture()["confidence_interval"]
        for key, value in (("seed", True), ("seed", -1),
                           ("resamples", 10), ("confidence_level", 0.8),
                           ("quantile_method", "nearest")):
            with self.subTest(key=key, value=value):
                interval = dict(base, **{key: value})
                self.assertIn("GROUP_BOOTSTRAP_PROTOCOL_INVALID",
                              audit_group_bootstrap_accuracy({"a": 1, "b": 1},
                                                             {"a": 1, "b": 0}, interval))
        self.assertIn("GROUP_BOOTSTRAP_GROUP_COUNTS_INVALID",
                      audit_group_bootstrap_accuracy({"a": 1, "b": 0},
                                                     {"a": 1, "b": 0}, base))

    def test_tampered_file_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            (base / "predictions_blind.csv").write_text("changed\n")
            issues = audit_evidence_bundle(record, temp)
            self.assertIn("EVIDENCE_FILE_SHA256_MISMATCH:predictions_blind.csv", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_missing_file_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            (base / "raw_manifest.json").unlink()
            self.assertIn("EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE:raw_manifest.json",
                          audit_evidence_bundle(record, temp))

    def test_missing_manifest_entry_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            del record["evidence_files"]["ci_run.json"]
            self.assertTrue(any(x.startswith("MISSING_EVIDENCE_FILES:")
                                for x in audit_evidence_bundle(record, temp)))

    def test_extra_manifest_entry_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            record["evidence_files"]["made_up.csv"] = "a" * 64
            self.assertTrue(any(x.startswith("UNEXPECTED_EVIDENCE_FILES:")
                                for x in audit_evidence_bundle(record, temp)))

    def test_traversal_ids_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            for key in ("id", "dataset_id", "run_id"):
                changed = dict(record, **{key: "../escape"})
                self.assertEqual(audit_evidence_bundle(changed, temp),
                                 ["INVALID_EVIDENCE_BUNDLE_ID_OR_PATH"])

    def test_symlinked_file_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            (base / "reproduce.sh").unlink()
            (base / "reproduce.sh").symlink_to(base / "raw_manifest.json")
            self.assertIn("EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE:reproduce.sh",
                          audit_evidence_bundle(record, temp))

    def test_valid_hash_but_invalid_json_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            bad = b"not-json"
            (base / "raw_manifest.json").write_bytes(bad)
            record["evidence_files"]["raw_manifest.json"] = hashlib.sha256(bad).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:raw_manifest.json",
                          audit_evidence_bundle(record, temp))

    def test_late_malformed_csv_row_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            bad = b"sensor,score\nMQ2,0.5\nMQ3,0.6,EXTRA\n"
            (base / "predictions_blind.csv").write_bytes(bad)
            record["evidence_files"]["predictions_blind.csv"] = hashlib.sha256(bad).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:predictions_blind.csv",
                          audit_evidence_bundle(record, temp))

    def test_duplicate_csv_header_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            bad = b"sensor,sensor\nMQ2,0.5\n"
            (base / "metrics_per_group.csv").write_bytes(bad)
            record["evidence_files"]["metrics_per_group.csv"] = hashlib.sha256(bad).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:metrics_per_group.csv",
                          audit_evidence_bundle(record, temp))

    def test_duplicate_json_keys_with_matching_hash_are_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            bad = b'{"metric":1,"metric":2}'
            (base / "metrics_summary.json").write_bytes(bad)
            record["evidence_files"]["metrics_summary.json"] = hashlib.sha256(bad).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:metrics_summary.json",
                          audit_evidence_bundle(record, temp))

    def test_nonfinite_json_constant_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            bad = b'{"metric":NaN}'
            (base / "metrics_summary.json").write_bytes(bad)
            record["evidence_files"]["metrics_summary.json"] = hashlib.sha256(bad).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:metrics_summary.json",
                          audit_evidence_bundle(record, temp))

    def test_majority_train_baseline_replays_from_training_labels(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertNotIn("BASELINE_MAJORITY_TRAIN_REPLAY_MISMATCH",
                             audit_record(record, temp)["issues"])
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_rehashed_blind_baseline_predictions_must_match_train_majority(self):
        # A baseline can be internally consistent with per-group metrics but
        # still contradict its declared majority_train algorithm.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "predictions_blind.csv"
            data = (base / name).read_text().replace(
                "GAS_A,GAS_A,GAS_A", "GAS_A,GAS_A,GAS_B")
            self.replace_bytes(record, base, name, data)
            record["evaluation_artifact_sha256"] = record["evidence_files"][name]
            self.assertIn("BASELINE_MAJORITY_TRAIN_REPLAY_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_unimplemented_baseline_algorithm_cannot_claim_replay(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["baseline_model"]["algorithm_id"] = "unverified_oracle"
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_ALGORITHM_REPLAY_UNAVAILABLE",
                          audit_record(record, temp)["issues"])

    def test_majority_train_tie_requires_locked_tie_policy(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            data = (base / name).read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1\\n",
                "tr,d1-s1,GAS_A,d1,s1\\ntr2,d1-s1,GAS_B,d1,s1\\n")
            self.replace_bytes(record, base, name, data)
            self.assertIn("BASELINE_MAJORITY_TIE_POLICY_UNSPECIFIED",
                          audit_record(record, temp)["issues"])

    def test_majority_train_blank_training_truth_cannot_be_replayed(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            data = (base / name).read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1", "tr,d1-s1,,d1,s1")
            self.replace_bytes(record, base, name, data)
            self.assertIn("BASELINE_TRAINING_LABELS_UNAVAILABLE",
                          audit_record(record, temp)["issues"])

    def test_invalid_digest_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            record["evidence_files"]["ci_run.json"] = "not-a-sha"
            self.assertIn("INVALID_EVIDENCE_SHA256:ci_run.json",
                          audit_evidence_bundle(record, temp))


if __name__ == "__main__":
    unittest.main()
