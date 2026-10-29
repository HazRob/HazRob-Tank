"""Negative-control tests for G2 artifact replay integrity."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from g2_replay_consistency import verify_replay


class G2ReplayConsistencyTests(unittest.TestCase):
    def test_matching_artifacts_yield_stable_digest_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            paths = [root / name for name in ("audit_a", "audit_b", "triage_a", "triage_b")]
            for p, value in zip(paths, (b'{"blocked":80}\n', b'{"blocked":80}\n',
                                        b'{"sources":14}\n', b'{"sources":14}\n')):
                p.write_bytes(value)
            result = verify_replay(*paths)
            self.assertEqual(result, verify_replay(*paths))
            self.assertEqual(result["scope"], "RESEARCH_ONLY_NOT_G2_OR_G3_APPROVAL")
            self.assertEqual(result["artifacts"]["g2_audit_results.json"]["sha256"],
                             hashlib.sha256(paths[0].read_bytes()).hexdigest())
            self.assertTrue(all(row["byte_identical"] for row in result["artifacts"].values()))

    def test_modified_audit_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            paths = [Path(tmp) / str(i) for i in range(4)]
            for path in paths:
                path.write_bytes(b"same")
            paths[1].write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "G2_REPLAY_BYTES_MISMATCH:audit"):
                verify_replay(*paths)

    def test_semantically_equal_but_byte_different_triage_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            paths = [Path(tmp) / str(i) for i in range(4)]
            for path in paths[:2]:
                path.write_bytes(b"identical")
            paths[2].write_bytes(b'{"a":1}\n')
            paths[3].write_bytes(b'{ "a": 1 }\n')
            with self.assertRaisesRegex(ValueError, "G2_REPLAY_BYTES_MISMATCH:triage"):
                verify_replay(*paths)

    def test_workflow_replays_both_outputs_and_uploads_manifest(self):
        workflow = (Path(__file__).resolve().parents[2] /
                    ".github/workflows/g2-signal-lineage-v4.yml").read_text(encoding="utf-8")
        self.assertEqual(workflow.count("python research/implementation_execution/g2_evidence_audit.py --output"), 2)
        self.assertEqual(workflow.count("python research/implementation_execution/g2_acquisition_triage.py"), 2)
        self.assertIn("python research/implementation_execution/g2_replay_consistency.py", workflow)
        self.assertIn("g2_replay_integrity.json", workflow)


if __name__ == "__main__":
    unittest.main()
