"""Fail-closed G2 artifact publication negative controls."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from g2_artifact_publish_gate import verify_publication

class PublishGateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        base = Path(self.tmp.name)
        self.audit = base / "g2_audit_results.json"
        self.triage = base / "g2_acquisition_triage.json"
        self.manifest = base / "g2_replay_integrity.json"
        ids = [str(i) for i in range(80)]
        self.a = {"schema": "hazrob.g2.audit.v1",
                  "scope": "new V4 independent source qualification",
                  "g2_blocked": 80, "g2_pass_candidates": 0,
                  "records": [{"id": i, "status": "G2_BLOCKED"} for i in ids]}
        self.t = {"schema": "hazrob.g2.acquisition-triage.v1",
                  "scope": "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL",
                  "techniques_total": 80, "external_sources_total": 14,
                  "g2_blocked": 80, "g2_pass_candidates_requiring_review": 0,
                  "external_sources_blocked": 14,
                  "techniques": [{"id": i, "status": "G2_BLOCKED"} for i in ids],
                  "sources": [{"id": str(i), "ready_for_source_review": False}
                              for i in range(14)]}
        self.write()

    def write(self):
        self.audit.write_text(json.dumps(self.a), encoding="utf-8")
        self.t["input_sha256"] = {"audit": hashlib.sha256(
            self.audit.read_bytes()).hexdigest()}
        self.triage.write_text(json.dumps(self.t), encoding="utf-8")
        m = {"schema": "hazrob.g2.replay-integrity.v1",
             "scope": "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL",
             "artifacts": {
                 path.name: {"bytes": len(path.read_bytes()),
                             "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                             "byte_identical": True}
                 for path in (self.audit, self.triage)}}
        self.manifest.write_text(json.dumps(m), encoding="utf-8")

    def verify(self):
        return verify_publication(self.audit, self.triage, self.manifest)

    def test_valid(self):
        self.assertEqual(self.verify()["g2_blocked"], 80)

    def test_changed_audit_after_replay(self):
        self.audit.write_bytes(self.audit.read_bytes() + b" ")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_BYTES_MISMATCH"):
            self.verify()

    def test_changed_triage_after_replay(self):
        self.triage.write_bytes(self.triage.read_bytes() + b" ")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_BYTES_MISMATCH"):
            self.verify()

    def test_rehashed_unlinked_triage(self):
        self.t["input_sha256"] = {"audit": "0" * 64}
        self.triage.write_text(json.dumps(self.t), encoding="utf-8")
        m = json.loads(self.manifest.read_text(encoding="utf-8"))
        m["artifacts"][self.triage.name]["bytes"] = len(self.triage.read_bytes())
        m["artifacts"][self.triage.name]["sha256"] = hashlib.sha256(
            self.triage.read_bytes()).hexdigest()
        self.manifest.write_text(json.dumps(m), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_LINEAGE_MISMATCH"):
            self.verify()

    def test_rehashed_forged_counts(self):
        self.t["g2_blocked"] = 79
        self.write()
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_COUNTS_OR_STATUS_MISMATCH"):
            self.verify()

    def test_missing_manifest_entry(self):
        m = json.loads(self.manifest.read_text(encoding="utf-8"))
        del m["artifacts"][self.audit.name]
        self.manifest.write_text(json.dumps(m), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_MANIFEST_INVALID"):
            self.verify()

    def test_ci_success_only_publication(self):
        workflow = (Path(__file__).resolve().parents[2] /
                    ".github/workflows/g2-signal-lineage-v4.yml").read_text(encoding="utf-8")
        self.assertIn("g2_artifact_publish_gate.py", workflow)
        upload = workflow.split('name: "Upload reproducible G2 gate output"', 1)[1]
        self.assertIn("if: success()", upload)
        self.assertIn("if-no-files-found: error", upload)
        self.assertNotIn("if: always()", upload)

if __name__ == "__main__":
    unittest.main()
