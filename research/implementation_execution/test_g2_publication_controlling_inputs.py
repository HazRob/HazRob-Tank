"""Independent G2 publication replay from actual research controlling inputs."""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from g2_acquisition_triage import (
    build_triage, PINNED_MATRIX, DEFAULT_LEDGER, DEFAULT_EVIDENCE,
)
from g2_artifact_publish_gate import verify_controlling_evidence

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]


class ControllingEvidencePublicationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.audit = Path(cls.temp.name) / "g2_audit_results.json"
        cls.triage = Path(cls.temp.name) / "g2_acquisition_triage.json"
        subprocess.run(
            [sys.executable, str(HERE / "g2_evidence_audit.py"),
             "--output", str(cls.audit)],
            cwd=ROOT, check=True, capture_output=True, text=True,
        )
        cls.expected = build_triage(cls.audit, PINNED_MATRIX,
                                    DEFAULT_LEDGER, DEFAULT_EVIDENCE)
        cls.triage.write_text(
            json.dumps(cls.expected, ensure_ascii=False, sort_keys=True) + "\n",
            encoding="utf-8",
        )

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def replay(self, audit=None, triage=None, matrix=None, ledger=None,
               evidence=None):
        return verify_controlling_evidence(
            audit or self.audit, triage or self.triage,
            matrix or PINNED_MATRIX, ledger or DEFAULT_LEDGER,
            evidence or DEFAULT_EVIDENCE,
        )

    def test_real_controlling_inputs_replay(self):
        digests = self.replay()
        self.assertEqual(len(digests), 5)
        self.assertEqual(digests, self.expected["input_sha256"])
        self.assertEqual(self.expected["g2_blocked"], 80)

    def test_rehashed_triage_with_forged_ledger_digest_is_rejected(self):
        forged = copy.deepcopy(self.expected)
        forged["input_sha256"]["ledger"] = "0" * 64
        path = Path(self.temp.name) / "forged_triage.json"
        path.write_text(json.dumps(forged), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_CONTROLLING_INPUT_SHA256_MISMATCH"):
            self.replay(triage=path)

    def test_ledger_byte_mutation_is_rejected_even_if_json_semantics_same(self):
        ledger = Path(self.temp.name) / "changed_ledger.json"
        ledger.write_bytes(DEFAULT_LEDGER.read_bytes() + b" ")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_CONTROLLING_INPUT_SHA256_MISMATCH"):
            self.replay(ledger=ledger)

    def test_evidence_byte_mutation_is_rejected_even_if_json_semantics_same(self):
        evidence = Path(self.temp.name) / "changed_evidence.json"
        evidence.write_bytes(DEFAULT_EVIDENCE.read_bytes() + b" ")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_CONTROLLING_INPUT_SHA256_MISMATCH"):
            self.replay(evidence=evidence)

    def test_self_consistent_forged_audit_is_rejected_after_rehash(self):
        forged_audit = json.loads(self.audit.read_text(encoding="utf-8"))
        forged_audit["records"][0]["issues"] = ["FORGED_PHYSICAL_EVIDENCE"]
        audit = Path(self.temp.name) / "forged_audit.json"
        audit.write_text(json.dumps(forged_audit), encoding="utf-8")
        forged_triage = copy.deepcopy(self.expected)
        forged_triage["techniques"][0]["blockers"] = ["FORGED_PHYSICAL_EVIDENCE"]
        forged_triage["input_sha256"]["audit"] = hashlib.sha256(audit.read_bytes()).hexdigest()
        triage = Path(self.temp.name) / "forged_audit_triage.json"
        triage.write_text(json.dumps(forged_triage), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "G2_PUBLISH_CONTROLLING_EVIDENCE_INVALID"):
            self.replay(audit=audit, triage=triage)

    def test_ci_publication_requires_controlling_inputs(self):
        workflow = (ROOT / ".github/workflows/g2-signal-lineage-v4.yml").read_text(
            encoding="utf-8")
        publication = workflow.split(
            'name: "Validate final G2 publication bytes and cross-report lineage"', 1
        )[1].split('name: "Upload reproducible G2 gate output"', 1)[0]
        for flag in ("--source-matrix", "--ledger", "--evidence"):
            self.assertIn(flag, publication)


if __name__ == "__main__":
    unittest.main()
