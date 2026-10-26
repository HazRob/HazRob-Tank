"""Negative controls for G2 evidence artifact availability and integrity.

Temporary synthetic files validate software only, not gas identity or ppm.
"""
import hashlib
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
    manifest = {}
    for name in FILES:
        if name.endswith(".json"):
            payload = ('{"fixture":"synthetic","file":"' + name + '"}\n').encode()
        elif name.endswith(".csv"):
            payload = b"column\nsynthetic\n"
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
