"""Offline CI metadata binding controls. These are not real CI attestations."""
import hashlib
import json
import tempfile
import unittest

from g2_cross_artifact_gate import audit_cross_artifact
from g2_evidence_audit import audit_record
from test_g2_evidence_bundle_gate import synthetic_bundle


def rewrite_ci(record, base, changes):
    path = base / "ci_run.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(changes)
    raw = (json.dumps(data, sort_keys=True) + "\n").encode("utf-8")
    path.write_bytes(raw)
    record["evidence_files"]["ci_run.json"] = hashlib.sha256(raw).hexdigest()


class G2CiRunBindingTests(unittest.TestCase):
    def test_synthetic_fixture_replays_but_is_not_scientific_evidence(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertNotIn("CI_RUN_METADATA_NOT_BOUND_TO_G2_WORKFLOW",
                             audit_cross_artifact(record, temp))
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_rehashed_ci_run_id_and_workflow_spoofing_are_blocked(self):
        for changed in (
            {"run_id": 123456790,
             "html_url": "https://github.com/HazRob/HazRob-Tank/actions/runs/123456790"},
            {"workflow_path": ".github/workflows/unrelated-success.yml"},
            {"repository": "other/project"},
            {"conclusion": "failure"},
            {"event": "schedule"},
            {"run_id": True},
            {"head_sha": "B" * 40},
        ):
            with self.subTest(changed=changed), tempfile.TemporaryDirectory() as temp:
                record, base = synthetic_bundle(temp)
                rewrite_ci(record, base, changed)
                self.assertIn("CI_RUN_METADATA_NOT_BOUND_TO_G2_WORKFLOW",
                              audit_record(record, temp)["issues"])

    def test_rehashed_ci_run_url_mismatch_is_blocked(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            rewrite_ci(record, base, {
                "html_url": "https://github.com/HazRob/HazRob-Tank/actions/runs/999"
            })
            self.assertIn("CI_RUN_METADATA_NOT_BOUND_TO_G2_WORKFLOW",
                          audit_record(record, temp)["issues"])

    def test_record_cannot_claim_different_run_or_commit(self):
        for field, value in (("ci_run_id", 987654321),
                             ("ci_head_sha", "b" * 40)):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as temp:
                record, _ = synthetic_bundle(temp)
                record[field] = value
                self.assertIn("CI_RUN_METADATA_NOT_BOUND_TO_G2_WORKFLOW",
                              audit_record(record, temp)["issues"])

    def test_nonobject_ci_json_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            raw = b'["not a CI run"]\n'
            (base / "ci_run.json").write_bytes(raw)
            record["evidence_files"]["ci_run.json"] = hashlib.sha256(raw).hexdigest()
            self.assertIn("INVALID_EVIDENCE_FILE_CONTENT:ci_run.json",
                          audit_record(record, temp)["issues"])


if __name__ == "__main__":
    unittest.main()
