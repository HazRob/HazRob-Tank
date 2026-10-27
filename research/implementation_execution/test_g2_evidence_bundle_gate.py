"""Negative controls for G2 evidence artifact availability and integrity.

Temporary synthetic files validate software only, not gas identity or ppm.
"""
import hashlib
import io
import csv
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from g2_evidence_bundle_gate import FILES, audit_evidence_bundle
from g2_evidence_audit import audit_record
from g2_cross_artifact_gate import audit_cross_artifact
from g2_raw_source_gate import audit_raw_source_bytes
from g2_r0_source_gate import audit_r0_source_bytes
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
    # R0 is a separate measured-reference claim, requiring separate source bytes.
    r0_bytes = b"synthetic R0 reference source: not a gas standard\\n"
    r0_file = Path(root) / "research/g2/raw/synthetic_r0_source.bin"
    r0_file.write_bytes(r0_bytes)
    record["signal_processing"]["r0_reference"].update(
        raw_local_path="research/g2/raw/synthetic_r0_source.bin",
        raw_size_bytes=len(r0_bytes),
        raw_sha256=hashlib.sha256(r0_bytes).hexdigest())
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
            "selected_on_test": False,
            "training_labels_sha256": "3128ffc949ec6b8404b83489c4349b18eb8098c57e381a5189ee2afe95e7de47"}},
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
    # Canonical exposure/group/physical allocation; no blind truth labels.
    allocation = sorted(
        [[row["exposure_id"], row["group_id"],
          [[name, row[name]] for name in ("device_id", "session_id")]]
         for row in csv.DictReader(io.StringIO(csv_payloads["group_manifest.csv"]))],
        key=lambda values: (values[0], values[1]))
    documents["protocol_locked.json"]["group_assignment_sha256"] = hashlib.sha256(
        json.dumps(allocation, ensure_ascii=False,
                   separators=(",", ":")).encode("utf-8")).hexdigest()
    # Calibration reference labels are committed before blind evaluation,
    # independently of the baseline's training-label commitment.
    calibration_rows = sorted(
        [[row["exposure_id"], row["group_id"], row["gas_reference_id"]]
         for row in csv.DictReader(io.StringIO(csv_payloads["group_manifest.csv"]))
         if row["group_id"] in record["calibration_group_ids"]],
        key=lambda values: (values[0], values[1]))
    documents["protocol_locked.json"]["calibration_labels_sha256"] = hashlib.sha256(
        json.dumps(calibration_rows, ensure_ascii=False,
                   separators=(",", ":")).encode("utf-8")).hexdigest()
    # Post-unblinding reference release: internal integrity, not source proof.
    blind_rows = sorted(
        [[row["exposure_id"], row["group_id"], row["gas_reference_id"]]
         for row in csv.DictReader(io.StringIO(csv_payloads["group_manifest.csv"]))
         if row["group_id"] in record["test_group_ids"]],
        key=lambda values: (values[0], values[1]))
    documents["review_decision.json"] = {
        "blind_truth_release_sha256": hashlib.sha256(
            json.dumps(blind_rows, ensure_ascii=False,
                       separators=(",", ":")).encode("utf-8")).hexdigest(),
        "review_status": "SYNTHETIC_NOT_SCIENTIFIC_REVIEW",
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


def relock_assignment(record, base):
    """Re-freeze a deliberately revised *synthetic* holdout allocation.

    Only positive fixture tests may call this after they change source groups.
    Negative controls must leave the original lock intact.
    """
    fields = sorted(part.strip() for part in record["grouping_source_field"].split(","))
    with (base / "group_manifest.csv").open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    allocation = sorted(
        [[row["exposure_id"], row["group_id"],
          [[name, row[name]] for name in fields]] for row in rows],
        key=lambda values: (values[0], values[1]))
    protocol_path = base / "protocol_locked.json"
    protocol = json.loads(protocol_path.read_text(encoding="utf-8"))
    protocol["group_assignment_sha256"] = hashlib.sha256(
        json.dumps(allocation, ensure_ascii=False,
                   separators=(",", ":")).encode("utf-8")).hexdigest()
    payload = (json.dumps(protocol) + "\n").encode("utf-8")
    protocol_path.write_bytes(payload)
    record["evidence_files"]["protocol_locked.json"] = hashlib.sha256(payload).hexdigest()
    record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]


class MalformedPhysicalGroupingKeyTests(unittest.TestCase):
    def test_unhashable_grouping_key_fails_closed_during_cross_artifact_replay(self):
        for bad in ([], {"grouping": "device_session_id"}):
            with self.subTest(bad=bad), tempfile.TemporaryDirectory() as temp:
                record, _ = synthetic_bundle(temp)
                record["grouping_key"] = bad
                issues = audit_cross_artifact(record, temp)
                self.assertIn("GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY", issues)
                self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")



class R0RawSourceReplayTests(unittest.TestCase):
    def test_verified_r0_reference_replays_original_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertEqual(audit_r0_source_bytes(record, temp), [])
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_boolean_r0_verification_without_archive_is_not_enough(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            (Path(temp) / "research/g2/raw/synthetic_r0_source.bin").unlink()
            self.assertIn("R0_RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE",
                          audit_record(record, temp)["issues"])

    def test_rehashed_bundle_does_not_hide_r0_source_mutation(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            raw = Path(temp) / "research/g2/raw/synthetic_r0_source.bin"
            raw.write_bytes(b"x" * raw.stat().st_size)
            self.assertIn("R0_RAW_SOURCE_BYTE_DIGEST_MISMATCH",
                          audit_record(record, temp)["issues"])

    def test_r0_source_path_traversal_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            record["signal_processing"]["r0_reference"]["raw_local_path"] = (
                "research/g2/raw/../outside.bin")
            self.assertIn("R0_RAW_SOURCE_PATH_MISSING_OR_UNSAFE",
                          audit_r0_source_bytes(record, temp))

    def test_r0_lfs_pointer_is_not_source_archive(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            pointer = (b"version https://git-lfs.github.com/spec/v1\n"
                       b"oid sha256:" + b"a" * 64 + b"\nsize 42\n")
            raw = Path(temp) / "research/g2/raw/synthetic_r0_source.bin"
            raw.write_bytes(pointer)
            ref = record["signal_processing"]["r0_reference"]
            ref["raw_size_bytes"] = len(pointer)
            ref["raw_sha256"] = hashlib.sha256(pointer).hexdigest()
            self.assertIn("R0_RAW_SOURCE_GIT_LFS_POINTER_NOT_MATERIALIZED",
                          audit_r0_source_bytes(record, temp))

    def test_r0_missing_archive_size_is_blocking(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            del record["signal_processing"]["r0_reference"]["raw_size_bytes"]
            self.assertIn("R0_RAW_SOURCE_SIZE_NOT_DOCUMENTED",
                          audit_r0_source_bytes(record, temp))

    def test_r0_source_streamed_length_rechecked_after_stat(self):
        # Simulate a source being truncated between stat() and open().
        # Its digest is recomputed for the truncated stream, while the
        # originally declared file size remains unchanged.
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            raw = Path(temp) / "research/g2/raw/synthetic_r0_source.bin"
            truncated = raw.read_bytes()[:-5]
            record["signal_processing"]["r0_reference"]["raw_sha256"] = (
                hashlib.sha256(truncated).hexdigest())
            original_open = Path.open

            def changed_stream(path, mode="r", *args, **kwargs):
                if path == raw and mode == "rb":
                    return io.BytesIO(truncated)
                return original_open(path, mode, *args, **kwargs)

            with patch.object(Path, "open", changed_stream):
                self.assertIn("R0_RAW_SOURCE_BYTE_SIZE_MISMATCH",
                              audit_r0_source_bytes(record, temp))

    def test_r0_source_symlink_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            raw = Path(temp) / "research/g2/raw/synthetic_r0_source.bin"
            target = raw.with_name("actual_r0_source.bin")
            raw.rename(target)
            raw.symlink_to(target)
            self.assertIn("R0_RAW_SOURCE_BYTES_UNAVAILABLE_OR_UNSAFE",
                          audit_r0_source_bytes(record, temp))

    def test_all_sensor_r0_sources_must_replay(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            p = record["signal_processing"]
            first_id = p["sensor_ids"][0]
            first = p.pop("r0_reference")
            second_id = "MQ7#unit-002"
            second_raw = Path(temp) / "research/g2/raw/second_r0.bin"
            second_bytes = b"synthetic second sensor R0 source\\n"
            second_raw.write_bytes(second_bytes)
            second = dict(first, sensor_instance_id=second_id,
                          raw_local_path="research/g2/raw/second_r0.bin",
                          raw_size_bytes=len(second_bytes),
                          raw_sha256=hashlib.sha256(second_bytes).hexdigest())
            p["sensor_ids"] = [first_id, second_id]
            p["r0_references"] = {first_id: first, second_id: second}
            self.assertEqual(audit_r0_source_bytes(record, temp), [])
            second_raw.write_bytes(b"X" + second_bytes[1:])
            self.assertIn("R0_RAW_SOURCE_BYTE_DIGEST_MISMATCH",
                          audit_r0_source_bytes(record, temp))

class EvidenceBundleTests(unittest.TestCase):
    def test_raw_source_streamed_length_rechecked_after_stat(self):
        # An archive can change after stat(); matching the bytes read to
        # a newly claimed digest is not sufficient when size is stale.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw = Path(temp) / "research/g2/raw/synthetic_source.bin"
            truncated = raw.read_bytes()[:-4]
            digest = hashlib.sha256(truncated).hexdigest()
            record["raw_sha256"] = digest
            manifest_path = base / "raw_manifest.json"
            manifest = json.loads(manifest_path.read_text())
            manifest["raw_sha256"] = digest
            manifest_path.write_text(json.dumps(manifest))
            original_open = Path.open

            def changed_stream(path, mode="r", *args, **kwargs):
                if path == raw and mode == "rb":
                    return io.BytesIO(truncated)
                return original_open(path, mode, *args, **kwargs)

            with patch.object(Path, "open", changed_stream):
                self.assertIn("RAW_SOURCE_BYTE_SIZE_MISMATCH",
                              audit_raw_source_bytes(record, temp))

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

    def test_non_object_json_replay_fails_closed_without_crash(self):
        # Even with correctly rehashed bytes, replay must not throw on a
        # JSON array, null, string, or number in any required document.
        # Synthetic controls are not measured gas evidence.
        names = (
            "raw_manifest.json", "source_qualification.json",
            "signal_lineage.json", "split_manifest.json",
            "protocol_locked.json", "calibration_manifest.json",
            "metrics_summary.json", "uncertainty_grouped.json",
        )
        for name in names:
            for payload in ("[]", "null", '"unexpected"', "123"):
                with self.subTest(name=name, payload=payload):
                    with tempfile.TemporaryDirectory() as temp:
                        record, base = synthetic_bundle(temp)
                        self.replace_bytes(record, base, name, payload)
                        if name == "protocol_locked.json":
                            record["protocol_sha256"] = record["evidence_files"][name]
                        self.assertIn(
                            "INVALID_EVIDENCE_FILE_CONTENT:" + name,
                            audit_evidence_bundle(record, temp))
                        self.assertIn(
                            "CROSS_ARTIFACT_JSON_SCHEMA_INVALID",
                            audit_cross_artifact(record, temp))
                        self.assertEqual(audit_record(record, temp)["status"],
                                         "G2_BLOCKED")

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
            relock_assignment(record, base)
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
            relock_assignment(record, base)
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

    def test_malformed_test_group_ids_fail_closed_without_typeerror(self):
        # JSON-valid nested values previously crashed set() and bootstrap
        # comprehensions during cross-artifact replay instead of blocking.
        for invalid in (
            ["d2-s1", {"nested": "d2-s2"}],
            ["d2-s1", ["d2-s2"]],
            42,
        ):
            with self.subTest(invalid=invalid), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                record["test_group_ids"] = invalid
                issues = audit_record(record, temp)["issues"]
                self.assertIn("SPLIT_MANIFEST_RECORD_MISMATCH", issues)
                self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

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
                "tr,d1-s1,GAS_A,d1,s1\n",
                "tr,d1-s1,GAS_A,d1,s1\ntr2,d1-s1,GAS_B,d1,s1\n")
            self.replace_bytes(record, base, name, data)
            self.assertIn("BASELINE_MAJORITY_TIE_POLICY_UNSPECIFIED",
                          audit_record(record, temp)["issues"])

    def test_locked_lexicographic_tie_policy_replays_majority_baseline(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            data = (base / name).read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1\n",
                "tr,d1-s1,GAS_A,d1,s1\ntr2,d1-s1,GAS_B,d1,s1\n")
            self.replace_bytes(record, base, name, data)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["baseline_model"]["tie_policy"] = "LEXICOGRAPHIC_ASC"
            protocol["baseline_model"]["training_labels_sha256"] = (
                "ce26be9a5f7e2df591ad0863d4899bc1dad0c97cad96360940e2afc8c485e8e0")
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            relock_assignment(record, base)
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_majority_training_labels_digest_is_required(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            del protocol["baseline_model"]["training_labels_sha256"]
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_TRAINING_LABEL_DIGEST_NOT_LOCKED",
                          audit_record(record, temp)["issues"])

    def test_invalid_majority_training_digest_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["baseline_model"]["training_labels_sha256"] = "not-a-sha"
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertIn("BASELINE_TRAINING_LABEL_DIGEST_NOT_LOCKED",
                          audit_record(record, temp)["issues"])

    def test_rehashed_training_rows_cannot_keep_stale_locked_label_digest(self):
        # A second training row with the same gas does not change the
        # majority prediction, but must invalidate the locked training digest.
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            data = (base / name).read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1\n",
                "tr,d1-s1,GAS_A,d1,s1\ntr2,d1-s1,GAS_A,d1,s1\n")
            self.replace_bytes(record, base, name, data)
            issues = audit_record(record, temp)["issues"]
            self.assertIn("BASELINE_TRAINING_LABEL_DIGEST_MISMATCH", issues)
            self.assertNotIn("BASELINE_MAJORITY_TRAIN_REPLAY_MISMATCH", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_majority_train_blank_training_truth_cannot_be_replayed(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            data = (base / name).read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1", "tr,d1-s1,,d1,s1")
            self.replace_bytes(record, base, name, data)
            self.assertIn("BASELINE_TRAINING_LABELS_UNAVAILABLE",
                          audit_record(record, temp)["issues"])

    def test_overflowed_json_numbers_block_rehashed_evidence(self):
        # Rehashing an artifact does not make an infinite numeric value valid.
        # json.loads without parse_float silently turns 1e9999 into inf.
        for filename in ("raw_manifest.json", "ci_run.json",
                         "uncertainty_grouped.json"):
            for token in ("1e9999", "-1e9999"):
                with self.subTest(filename=filename, token=token), tempfile.TemporaryDirectory() as temp:
                    record, base = synthetic_bundle(temp)
                    original = (base / filename).read_text(encoding="utf-8")
                    altered = original.rstrip()
                    self.assertTrue(altered.endswith("}"))
                    altered = altered[:-1] + ',"overflow_control":{"numeric":' + token + '}}'
                    self.replace_bytes(record, base, filename, altered + "\n")
                    self.assertIn(
                        "INVALID_EVIDENCE_FILE_CONTENT:" + filename,
                        audit_evidence_bundle(record, temp))
                    self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_huge_integer_with_matching_hash_is_rejected_before_metric_audit(self):
        # Python's json.loads accepts large integer tokens, but
        # math.isfinite(10**310) raises OverflowError downstream.
        for filename in ("metrics_summary.json", "raw_manifest.json",
                         "uncertainty_grouped.json"):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                payload = (base / filename).read_text(encoding="utf-8").rstrip()
                payload = payload[:-1] + ',"huge_integer":' + ("9" * 310) + '}'
                self.replace_bytes(record, base, filename, payload + "\n")
                self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + filename,
                              audit_evidence_bundle(record, temp))
                self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_invalid_digest_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            record["evidence_files"]["ci_run.json"] = "not-a-sha"
            self.assertIn("INVALID_EVIDENCE_SHA256:ci_run.json",
                          audit_evidence_bundle(record, temp))


    def test_majority_train_rejects_partial_training_group_fit(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["train_group_ids"].append("d1-s3")
            split = json.loads((base / "split_manifest.json").read_text())
            split["train_group_ids"] = record["train_group_ids"]
            self.replace_bytes(record, base, "split_manifest.json",
                               json.dumps(split))
            data = (base / "group_manifest.csv").read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1\n",
                "tr,d1-s1,GAS_A,d1,s1\ntr2,d1-s3,GAS_A,d1,s3\n")
            self.assertIn("tr2,d1-s3", data)
            self.replace_bytes(record, base, "group_manifest.csv", data)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["train_group_ids"] = record["train_group_ids"]
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            self.assertIn("BASELINE_MAJORITY_TRAIN_PARTIAL_FIT",
                          audit_record(record, temp)["issues"])
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_majority_train_full_training_group_fit_replays(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["train_group_ids"].append("d1-s3")
            split = json.loads((base / "split_manifest.json").read_text())
            split["train_group_ids"] = record["train_group_ids"]
            self.replace_bytes(record, base, "split_manifest.json",
                               json.dumps(split))
            data = (base / "group_manifest.csv").read_text().replace(
                "tr,d1-s1,GAS_A,d1,s1\n",
                "tr,d1-s1,GAS_A,d1,s1\ntr2,d1-s3,GAS_A,d1,s3\n")
            self.assertIn("tr2,d1-s3", data)
            self.replace_bytes(record, base, "group_manifest.csv", data)
            protocol = json.loads((base / "protocol_locked.json").read_text())
            protocol["train_group_ids"] = record["train_group_ids"]
            protocol["baseline_model"]["fitted_group_ids"] = list(record["train_group_ids"])
            labels = [["tr", "d1-s1", "GAS_A"], ["tr2", "d1-s3", "GAS_A"]]
            protocol["baseline_model"]["training_labels_sha256"] = hashlib.sha256(
                json.dumps(labels, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            ).hexdigest()
            self.replace_bytes(record, base, "protocol_locked.json",
                               json.dumps(protocol))
            record["protocol_sha256"] = record["evidence_files"]["protocol_locked.json"]
            relock_assignment(record, base)
            self.assertEqual(audit_evidence_bundle(record, temp), [])
            result = audit_record(record, temp)
            self.assertEqual(result["status"], "G2_PASS_CANDIDATE_REQUIRES_REVIEW",
                             result["issues"])


class CsvControlCharacterEvidenceTests(unittest.TestCase):
    """Byte-valid CSV with embedded controls must still fail closed."""

    def test_control_character_in_header_rejected_after_rehash(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            altered = (base / name).read_text().replace(
                "exposure_id,group_id", "exposure_id,group\\x00_id")
            altered = altered.replace("\\x00", "\x00")
            payload = altered.encode("utf-8")
            (base / name).write_bytes(payload)
            record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + name,
                          audit_evidence_bundle(record, temp))
            self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                          audit_cross_artifact(record, temp))

    def test_control_character_in_blind_label_rejected_after_rehash(self):
        for char in ("\x00", "\x1f", "\x7f"):
            with self.subTest(char=repr(char)), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                name = "predictions_blind.csv"
                payload = (base / name).read_text().replace(
                    "GAS_A,GAS_A,GAS_A", "GAS_A,GAS_" + char + "A,GAS_A").encode("utf-8")
                (base / name).write_bytes(payload)
                record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
                record["evaluation_artifact_sha256"] = record["evidence_files"][name]
                self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + name,
                              audit_evidence_bundle(record, temp))
                self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                              audit_cross_artifact(record, temp))
                self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")



class UnicodeInvisibleCsvEvidenceTests(unittest.TestCase):
    """Synthetic regression controls; these do not establish measured G2 evidence."""

    def test_multilingual_printable_labels_remain_allowed(self):
        from g2_evidence_bundle_gate import _csv_has_control as bundle_has_control
        from g2_cross_artifact_gate import _csv_has_control as replay_has_control
        for label in ("غاز_الإيثانول", "éthanol", "ethanol_50 ppm"):
            with self.subTest(label=label):
                self.assertFalse(bundle_has_control(label))
                self.assertFalse(replay_has_control(label))

    def test_unicode_invisible_header_rejected_with_valid_sha256(self):
        from g2_cross_artifact_gate import _read_csv
        for char in ("\u0085", "\u200b", "\u202e", "\ufeff", "\u2028", "\u2060"):
            with self.subTest(char=repr(char)), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                name = "group_manifest.csv"
                altered = (base / name).read_text(encoding="utf-8").replace(
                    "gas_reference_id", "gas" + char + "_reference_id")
                payload = altered.encode("utf-8")
                (base / name).write_bytes(payload)
                record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
                self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + name,
                              audit_evidence_bundle(record, temp))
                with self.assertRaisesRegex(ValueError, "CSV_HEADER_AMBIGUOUS"):
                    _read_csv(base, name)
                self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                              audit_cross_artifact(record, temp))
                self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_unicode_invisible_cells_rejected_with_valid_sha256(self):
        from g2_cross_artifact_gate import _read_csv
        mutations = (
            ("group_manifest.csv", "GAS_A,d1,s1", "GAS_A{char},d1,s1"),
            ("predictions_blind.csv", "GAS_A,GAS_A,GAS_A",
             "GAS_A,GAS_{char}A,GAS_A"),
            ("metrics_per_group.csv", "d2-s1,1,1,1", "d2-s1{char},1,1,1"),
        )
        for char in ("\u0085", "\u200b", "\u202e", "\ufeff", "\u2028", "\u2060"):
            for name, original, changed in mutations:
                with self.subTest(char=repr(char), file=name), tempfile.TemporaryDirectory() as temp:
                    record, base = synthetic_bundle(temp)
                    altered = (base / name).read_text(encoding="utf-8").replace(
                        original, changed.format(char=char))
                    payload = altered.encode("utf-8")
                    (base / name).write_bytes(payload)
                    record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
                    if name == "predictions_blind.csv":
                        record["evaluation_artifact_sha256"] = record["evidence_files"][name]
                    self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + name,
                                  audit_evidence_bundle(record, temp))
                    with self.assertRaisesRegex(ValueError, "CSV_ROW_WIDTH_MISMATCH"):
                        _read_csv(base, name)
                    self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                                  audit_cross_artifact(record, temp))
                    self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")



class UnicodeCanonicalCsvEvidenceTests(unittest.TestCase):
    """Rehashed negative controls for NFC aliases and nonstandard spaces."""

    def _assert_rehashed_mutation_blocked(self, temp, record, base, name, old, new):
        from g2_cross_artifact_gate import _read_csv
        source = (base / name).read_text(encoding="utf-8")
        self.assertIn(old, source)
        payload = source.replace(old, new, 1).encode("utf-8")
        (base / name).write_bytes(payload)
        record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
        if name == "predictions_blind.csv":
            record["evaluation_artifact_sha256"] = record["evidence_files"][name]
        self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:" + name,
                      audit_evidence_bundle(record, temp))
        with self.assertRaises(ValueError):
            _read_csv(base, name)
        self.assertIn("CROSS_ARTIFACT_READ_FAILED",
                      audit_cross_artifact(record, temp))
        self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_nonstandard_unicode_spaces_and_non_nfc_csv_headers_fail_closed(self):
        from g2_evidence_bundle_gate import _csv_has_control as bundle_invalid
        from g2_cross_artifact_gate import _csv_has_control as replay_invalid
        for label in ("غاز_الإيثانول", "éthanol", "ethanol 50 ppm"):
            self.assertFalse(bundle_invalid(label))
            self.assertFalse(replay_invalid(label))
        for token in (chr(0x00A0), chr(0x2009), chr(0x202F), chr(0x3000), "e" + chr(0x0301)):
            with self.subTest(token=repr(token)), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                self.assertTrue(bundle_invalid(token))
                self.assertTrue(replay_invalid(token))
                self._assert_rehashed_mutation_blocked(
                    temp, record, base, "group_manifest.csv",
                    "gas_reference_id", "gas" + token + "_reference_id")

    def test_nonstandard_unicode_spaces_and_non_nfc_cells_fail_closed(self):
        mutations = (
            ("group_manifest.csv", "GAS_A,d1,s1", "GAS_A{token},d1,s1"),
            ("predictions_blind.csv", "GAS_A,GAS_A,GAS_A",
             "GAS_A,GAS_A{token},GAS_A"),
            ("metrics_per_group.csv", "d2-s1,1,1,1", "d2-s1{token},1,1,1"),
        )
        for token in (chr(0x00A0), chr(0x2009), chr(0x202F), chr(0x3000), "e" + chr(0x0301)):
            for name, old, new in mutations:
                with self.subTest(token=repr(token), file=name), tempfile.TemporaryDirectory() as temp:
                    record, base = synthetic_bundle(temp)
                    self._assert_rehashed_mutation_blocked(
                        temp, record, base, name, old, new.format(token=token))


    def test_ascii_edge_whitespace_identity_tokens_rejected(self):
        from g2_evidence_bundle_gate import _csv_has_control as bundle_invalid
        from g2_cross_artifact_gate import _csv_has_control as replay_invalid
        for valid in ("GAS_A", "ethanol 50 ppm", "غاز الإيثانول"):
            with self.subTest(valid=valid):
                self.assertFalse(bundle_invalid(valid))
                self.assertFalse(replay_invalid(valid))
        for invalid in (" GAS_A", "GAS_A ", " GAS_A ", "50 ppm ", " gas_reference_id"):
            with self.subTest(invalid=repr(invalid)):
                self.assertTrue(bundle_invalid(invalid))
                self.assertTrue(replay_invalid(invalid))

    def test_rehashed_ascii_edge_whitespace_cannot_change_identity_or_metrics(self):
        # SHA256 alone cannot prevent labels with trailing/leading ASCII spaces
        # from weakening the majority baseline or aliasing physical groups.
        mutations = (
            ("group_manifest.csv", "GAS_A,d1,s1", "GAS_A ,d1,s1"),
            ("group_manifest.csv", "gas_reference_id", " gas_reference_id"),
            ("predictions_blind.csv", "GAS_A,GAS_A,GAS_A", "GAS_A, GAS_A,GAS_A"),
            ("predictions_blind.csv", "GAS_A,GAS_A,GAS_A", "GAS_A,GAS_A,GAS_A "),
            ("metrics_per_group.csv", "d2-s1,1,1,1", " d2-s1,1,1,1"),
            ("metrics_per_group.csv", "sample_count", "sample_count "),
        )
        for name, old, new in mutations:
            with self.subTest(file=name, mutation=new), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                self._assert_rehashed_mutation_blocked(
                    temp, record, base, name, old, new)


class LockedPhysicalHoldoutAssignmentTests(unittest.TestCase):
    """Synthetic negative controls; never independent physical G2 evidence."""

    @staticmethod
    def rehash(record, base, name, text):
        payload = text.encode("utf-8")
        (base / name).write_bytes(payload)
        record["evidence_files"][name] = hashlib.sha256(payload).hexdigest()
        if name == "predictions_blind.csv":
            record["evaluation_artifact_sha256"] = record["evidence_files"][name]
        if name == "protocol_locked.json":
            record["protocol_sha256"] = record["evidence_files"][name]

    def test_unmodified_locked_assignment_replays(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_rehashed_blind_exposure_rename_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            for name in ("group_manifest.csv", "predictions_blind.csv"):
                original = (base / name).read_text(encoding="utf-8")
                self.assertIn("te1,", original)
                self.rehash(record, base, name, original.replace("te1,", "te1-new,"))
            issues = audit_record(record, temp)["issues"]
            self.assertIn("GROUP_ASSIGNMENT_DIGEST_MISMATCH", issues)
            self.assertNotIn("BLIND_PREDICTION_COVERAGE_MISMATCH", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_rehashed_physical_identity_change_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            original = (base / name).read_text(encoding="utf-8")
            self.assertIn("te1,d2-s1,GAS_A,d2,s1", original)
            self.rehash(record, base, name, original.replace(
                "te1,d2-s1,GAS_A,d2,s1", "te1,d2-s1,GAS_A,d9,s1"))
            issues = audit_record(record, temp)["issues"]
            self.assertIn("GROUP_ASSIGNMENT_DIGEST_MISMATCH", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_missing_locked_assignment_is_rejected_with_valid_hash(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "protocol_locked.json"
            protocol = json.loads((base / name).read_text(encoding="utf-8"))
            del protocol["group_assignment_sha256"]
            self.rehash(record, base, name, json.dumps(protocol) + "\n")
            self.assertIn("GROUP_ASSIGNMENT_DIGEST_NOT_LOCKED",
                          audit_record(record, temp)["issues"])


class LockedCalibrationLabelsTests(unittest.TestCase):
    """Calibration-label commitments are synthetic integrity checks only."""

    def test_calibration_labels_replay_with_unmodified_fixture(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            result = audit_record(record, temp)
            self.assertEqual(result["status"], "G2_PASS_CANDIDATE_REQUIRES_REVIEW",
                             result["issues"])

    def test_rehashed_calibration_relabel_cannot_escape_locked_protocol(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            old = (base / name).read_text(encoding="utf-8")
            self.assertIn("ca,d1-s2,GAS_B,d1,s2", old)
            new = old.replace("ca,d1-s2,GAS_B,d1,s2",
                              "ca,d1-s2,GAS_A,d1,s2")
            LockedPhysicalHoldoutAssignmentTests.rehash(record, base, name, new)
            issues = audit_record(record, temp)["issues"]
            self.assertIn("CALIBRATION_LABEL_DIGEST_MISMATCH", issues)
            self.assertNotIn("GROUP_ASSIGNMENT_DIGEST_MISMATCH", issues)
            self.assertNotIn("BASELINE_TRAINING_LABEL_DIGEST_MISMATCH", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_missing_calibration_commitment_blocks_even_with_valid_sha(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "protocol_locked.json"
            protocol = json.loads((base / name).read_text(encoding="utf-8"))
            del protocol["calibration_labels_sha256"]
            LockedPhysicalHoldoutAssignmentTests.rehash(
                record, base, name, json.dumps(protocol) + "\n")
            self.assertIn("CALIBRATION_LABEL_DIGEST_NOT_LOCKED",
                          audit_record(record, temp)["issues"])

    def test_blank_calibration_truth_blocks_even_with_rehashed_csv(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            name = "group_manifest.csv"
            old = (base / name).read_text(encoding="utf-8")
            new = old.replace("ca,d1-s2,GAS_B,d1,s2", "ca,d1-s2,,d1,s2")
            LockedPhysicalHoldoutAssignmentTests.rehash(record, base, name, new)
            self.assertIn("CALIBRATION_LABELS_UNAVAILABLE",
                          audit_record(record, temp)["issues"])



class PostUnblindingTruthReleaseTests(unittest.TestCase):
    """Synthetic integrity checks; no independent physical truth implied."""

    def test_unchanged_release_replays(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            result = audit_record(record, temp)
            self.assertEqual(result["status"], "G2_PASS_CANDIDATE_REQUIRES_REVIEW",
                             result["issues"])

    def test_rehashed_blind_truth_relabel_is_detected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            old = (base / "group_manifest.csv").read_text(encoding="utf-8")
            LockedPhysicalHoldoutAssignmentTests.rehash(
                record, base, "group_manifest.csv",
                old.replace("te2,d2-s2,GAS_B,d2,s2", "te2,d2-s2,GAS_A,d2,s2"))
            old = (base / "predictions_blind.csv").read_text(encoding="utf-8")
            LockedPhysicalHoldoutAssignmentTests.rehash(
                record, base, "predictions_blind.csv",
                old.replace("te2,d2-s2,GAS_B,GAS_A,GAS_A",
                            "te2,d2-s2,GAS_A,GAS_A,GAS_A"))
            issues = audit_record(record, temp)["issues"]
            self.assertIn("BLIND_TRUTH_RELEASE_DIGEST_MISMATCH", issues)
            self.assertNotIn("GROUP_ASSIGNMENT_DIGEST_MISMATCH", issues)
            self.assertNotIn("CALIBRATION_LABEL_DIGEST_MISMATCH", issues)

    def test_missing_release_digest_is_rejected_after_rehash(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            review = json.loads((base / "review_decision.json").read_text())
            del review["blind_truth_release_sha256"]
            LockedPhysicalHoldoutAssignmentTests.rehash(
                record, base, "review_decision.json", json.dumps(review) + "\n")
            self.assertIn("BLIND_TRUTH_RELEASE_DIGEST_NOT_LOCKED",
                          audit_record(record, temp)["issues"])

    def test_empty_blind_reference_is_rejected_after_rehash(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            old = (base / "group_manifest.csv").read_text(encoding="utf-8")
            LockedPhysicalHoldoutAssignmentTests.rehash(
                record, base, "group_manifest.csv",
                old.replace("te2,d2-s2,GAS_B,d2,s2", "te2,d2-s2,,d2,s2"))
            self.assertIn("BLIND_TRUTH_RELEASE_UNAVAILABLE",
                          audit_record(record, temp)["issues"])


class CentralReferenceCatalogReadinessTests(unittest.TestCase):
    """Catalog metadata must not be mistaken for independent G2 measurements."""

    @staticmethod
    def fixture():
        return {
            "record_count": 2,
            "counts_by_type": {"EXTERNAL_MEASUREMENT_DATASET": 1,
                               "ANALYTICAL_METHOD_STUDY": 1},
            "primary_catalog_entries": [
                {"id": "MQ-EXT-01", "category": "EXTERNAL_MEASUREMENT_DATASET",
                 "title": "synthetic catalogue entry",
                 "measurement_qualification": "claim_not_verified",
                 "experiment_independence": "NOT_AUDITED",
                 "raw_file_status": "RAW_INSPECTED_OR_PREVIOUSLY_VERIFIED",
                 "raw_sha256": "a" * 64,
                 "physical_reference_traceability": "reported_only"},
                {"id": "METHOD-01", "category": "ANALYTICAL_METHOD_STUDY"},
            ],
        }

    def test_truthy_claim_strings_never_qualify_a_dataset(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        result = audit_source_catalog(self.fixture(), expected_total=2,
                                      expected_datasets=1)
        self.assertEqual(result["source_review_ready"], 0)
        self.assertEqual(result["source_review_blocked"], 1)
        self.assertIn("MEASUREMENT_QUALIFICATION_NOT_VERIFIED",
                      result["datasets"][0]["blockers"])
        self.assertIn("PHYSICAL_REFERENCE_TRACEABILITY_NOT_VERIFIED",
                      result["datasets"][0]["blockers"])

    def test_raw_inspected_and_git_mirror_sha_do_not_prove_original_bytes(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        result = audit_source_catalog(self.fixture(), expected_total=2,
                                      expected_datasets=1)
        self.assertIn("ORIGINAL_RAW_BYTES_NOT_CATALOG_VERIFIED",
                      result["datasets"][0]["blockers"])
        self.assertIn("INDEPENDENT_PHYSICAL_GROUPS_NOT_VERIFIED",
                      result["datasets"][0]["blockers"])

    def test_catalogue_metadata_only_ready_never_claims_g2_or_g3(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        catalog = self.fixture()
        row = catalog["primary_catalog_entries"][0]
        row.update(measurement_qualification=True,
                   experiment_independence="PHYSICAL_GROUPS_VERIFIED",
                   raw_file_status="RAW_BYTES_SHA256_VERIFIED",
                   physical_reference_traceability=True)
        result = audit_source_catalog(catalog, expected_total=2,
                                      expected_datasets=1)
        self.assertEqual(result["source_review_ready"], 1)
        self.assertEqual(result["source_review_blocked"], 0)
        self.assertEqual(result["scope"],
                         "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL")
        self.assertIn("NOT A G2 PASS", result["datasets"][0]["notes"])

    def test_pinned_14_source_matrix_is_fail_closed_and_noncertifying(self):
        from g2_reference_catalog_readiness import PINNED_CATALOG_BLOB
        path = Path(__file__).parent / "G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json"
        matrix = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(matrix["source_git_blob_sha1"], PINNED_CATALOG_BLOB)
        self.assertEqual(matrix["catalog_entries"], 80)
        self.assertEqual(matrix["external_measurement_datasets"], 14)
        self.assertEqual(matrix["source_review_ready"], 0)
        self.assertEqual(matrix["source_review_blocked"], 14)
        self.assertEqual(len({row["id"] for row in matrix["datasets"]}), 14)
        self.assertTrue(all(row["blockers"] and
                            row["catalog_source_review_ready"] is False
                            for row in matrix["datasets"]))
        self.assertEqual(matrix["scope"],
                         "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL")

    def test_catalogue_id_count_and_blob_integrity_fail_closed(self):
        from g2_reference_catalog_readiness import (
            audit_source_catalog, audit_catalog_bytes, git_blob_sha)
        catalog = self.fixture()
        catalog["primary_catalog_entries"][1]["id"] = "MQ-EXT-01"
        with self.assertRaisesRegex(ValueError, "CENTRAL_CATALOG_DUPLICATE_ID"):
            audit_source_catalog(catalog, expected_total=2, expected_datasets=1)
        catalog = self.fixture()
        with self.assertRaisesRegex(ValueError, "CENTRAL_CATALOG_DATASET_COUNT_MISMATCH"):
            audit_source_catalog(catalog, expected_total=2, expected_datasets=2)
        raw = json.dumps(catalog).encode("utf-8")
        with self.assertRaisesRegex(ValueError, "CENTRAL_CATALOG_GIT_BLOB_MISMATCH"):
            audit_catalog_bytes(raw, "0" * 40)
        self.assertEqual(len(git_blob_sha(raw)), 40)


    def test_category_inventory_cannot_hide_relabelled_non_dataset_entries(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        catalog = self.fixture()
        catalog["primary_catalog_entries"][1]["category"] = "SCIENTIFIC_REVIEW"
        with self.assertRaisesRegex(
                ValueError, "CENTRAL_CATALOG_CATEGORY_INVENTORY_MISMATCH"):
            audit_source_catalog(catalog, expected_total=2, expected_datasets=1)
        catalog = self.fixture()
        catalog["counts_by_type"]["ANALYTICAL_METHOD_STUDY"] = 0
        with self.assertRaisesRegex(
                ValueError, "CENTRAL_CATALOG_CATEGORY_INVENTORY_MISMATCH"):
            audit_source_catalog(catalog, expected_total=2, expected_datasets=1)

    def test_boolean_and_float_catalog_counts_fail_closed(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        for invalid in (2.0, True, "2"):
            with self.subTest(record_count=repr(invalid)):
                catalog = self.fixture()
                catalog["record_count"] = invalid
                with self.assertRaisesRegex(
                        ValueError, "CENTRAL_CATALOG_ENTRY_COUNT_OR_SCHEMA_INVALID"):
                    audit_source_catalog(catalog, expected_total=2,
                                         expected_datasets=1)
        for invalid in (1.0, True, "1", -1):
            with self.subTest(category_count=repr(invalid)):
                catalog = self.fixture()
                catalog["counts_by_type"]["EXTERNAL_MEASUREMENT_DATASET"] = invalid
                with self.assertRaisesRegex(
                        ValueError, "CENTRAL_CATALOG_CATEGORY_INVENTORY_MISMATCH"):
                    audit_source_catalog(catalog, expected_total=2,
                                         expected_datasets=1)

    def test_ambiguous_unicode_and_whitespace_catalog_identifiers_fail_closed(self):
        from g2_reference_catalog_readiness import audit_source_catalog
        for invalid in (" MQ-EXT-01", "MQ-EXT-01 ", "MQ–EXT–01",
                        "MQ-EXT-01\\u200b", "MQ-EXT-01\\n"):
            with self.subTest(identifier=repr(invalid)):
                catalog = self.fixture()
                catalog["primary_catalog_entries"][0]["id"] = invalid
                with self.assertRaisesRegex(
                        ValueError, "CENTRAL_CATALOG_ENTRY_COUNT_OR_SCHEMA_INVALID"):
                    audit_source_catalog(catalog, expected_total=2,
                                         expected_datasets=1)

    def test_duplicate_json_keys_are_rejected_even_when_blob_hash_matches(self):
        from g2_reference_catalog_readiness import audit_catalog_bytes, git_blob_sha
        source = json.dumps(self.fixture())
        variants = (
            source.replace('"record_count": 2',
                           '"record_count": 2, "record_count": 2', 1),
            source.replace('"ANALYTICAL_METHOD_STUDY": 1',
                           '"ANALYTICAL_METHOD_STUDY": 1, "ANALYTICAL_METHOD_STUDY": 1', 1),
        )
        for raw_text in variants:
            with self.subTest(payload=raw_text[:100]):
                raw = raw_text.encode("utf-8")
                with self.assertRaisesRegex(
                        ValueError, "CENTRAL_CATALOG_JSON_INVALID") as caught:
                    audit_catalog_bytes(raw, expected_blob=git_blob_sha(raw))
                self.assertIn("CENTRAL_CATALOG_DUPLICATE_JSON_KEY",
                              str(caught.exception.__cause__))

    def test_nonfinite_json_numbers_fail_even_when_blob_hash_matches(self):
        from g2_reference_catalog_readiness import audit_catalog_bytes, git_blob_sha
        source = json.dumps(self.fixture())
        for token in ("NaN", "Infinity", "-Infinity", "1e309"):
            with self.subTest(token=token):
                raw = source.replace('"record_count": 2',
                                     '"record_count": ' + token, 1).encode("utf-8")
                with self.assertRaisesRegex(
                        ValueError, "CENTRAL_CATALOG_JSON_INVALID"):
                    audit_catalog_bytes(raw, expected_blob=git_blob_sha(raw))


if __name__ == "__main__":
    unittest.main()
