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
from test_g2_evidence_audit import eligible_fixture


def synthetic_bundle(root):
    record = eligible_fixture()
    record.update(dataset_id="synthetic-dataset", run_id="synthetic-run")
    base = (Path(root) / "research/g2/evidence" / record["id"]
            / record["dataset_id"] / record["run_id"])
    base.mkdir(parents=True)
    documents = {
        "raw_manifest.json": {
            "raw_sha256": record["raw_sha256"], "dataset_uri": record["dataset_uri"]
        },
        "split_manifest.json": {
            key: record[key] for key in
            ("train_group_ids", "calibration_group_ids", "test_group_ids")
        },
        "calibration_manifest.json": {
            "calibration_group_ids": record["calibration_group_ids"]
        },
        "metrics_summary.json": {
            key: record[key] for key in
            ("metric_name", "metric_value", "baseline_metric_value")
        },
        "uncertainty_grouped.json": record["confidence_interval"],
    }
    csv_payloads = {
        "group_manifest.csv": "exposure_id,group_id,gas_reference_id\ntr,d1-s1,GAS_A\nca,d1-s2,GAS_B\nte1,d2-s1,GAS_A\nte2,d2-s2,GAS_B\n",
        "predictions_blind.csv": "exposure_id,group_id,true_reference,predicted_reference\nte1,d2-s1,GAS_A,GAS_A\nte2,d2-s2,GAS_B,GAS_A\n",
        "metrics_per_group.csv": "group_id,sample_count,correct_count\nd2-s1,1,1\nd2-s2,1,0\n",
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
    return record, base


class EvidenceBundleTests(unittest.TestCase):
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

    @staticmethod
    def replace_bytes(record, base, name, data):
        data = data.encode("utf-8")
        (base / name).write_bytes(data)
        record["evidence_files"][name] = hashlib.sha256(data).hexdigest()

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
                "group_id,sample_count\nd2-s1,2\nd2-s2,1\n")
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

    def test_false_truth_label_with_matching_hash_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id,true_reference,predicted_reference\n"
                "te1,d2-s1,GAS_B,GAS_A\nte2,d2-s2,GAS_B,GAS_A\n")
            self.assertIn("BLIND_PREDICTION_TRUTH_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_inflated_per_group_correct_count_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "metrics_per_group.csv",
                "group_id,sample_count,correct_count\nd2-s1,1,1\nd2-s2,1,1\n")
            self.assertIn("PER_GROUP_CORRECT_COUNT_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_accuracy_missing_truth_columns_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            self.replace_bytes(record, base, "predictions_blind.csv",
                "exposure_id,group_id\nte1,d2-s1\nte2,d2-s2\n")
            self.assertIn("ACCURACY_RECOMPUTATION_FIELDS_MISSING",
                          audit_record(record, temp)["issues"])

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

    def test_invalid_digest_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            record["evidence_files"]["ci_run.json"] = "not-a-sha"
            self.assertIn("INVALID_EVIDENCE_SHA256:ci_run.json",
                          audit_evidence_bundle(record, temp))


if __name__ == "__main__":
    unittest.main()
