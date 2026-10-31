"""Offline CI coverage guard for all research-only G2 test modules.

This verifies test discovery and compilation, not scientific G2 qualification.
"""
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/g2-signal-lineage-v4.yml"
TEST_DIRECTORY = ROOT / "research/implementation_execution"


class G2CompleteOfflineCICoverageTests(unittest.TestCase):
    def test_workflow_discovers_every_g2_test_module(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        command = (
            "python -m unittest discover -s research/implementation_execution "
            "-p 'test_g2_*.py' -v"
        )
        self.assertIn(command, workflow)
        names = {path.name for path in TEST_DIRECTORY.glob("test_g2_*.py")}
        required = {
            "test_g2_evidence_audit.py",
            "test_g2_evidence_bundle_gate.py",
            "test_g2_signal_lineage_gate.py",
            "test_g2_cluster_bootstrap_gate.py",
            "test_g2_uci1081_signal_manifest.py",
            "test_g2_mq01_fileday_benchmark.py",
            "test_g2_mendeley_mq14_source_probe.py",
            "test_g2_zenodo_mq01_raw_inventory.py",
            "test_g2_workflow_test_coverage.py",
        }
        self.assertTrue(required.issubset(names), sorted(required - names))
        self.assertEqual(len(names), len(set(names)))

    def test_workflow_compiles_all_research_g2_sources(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn(
            "python -m compileall -q research/implementation_execution",
            workflow,
        )
        self.assertIn("research/g2/raw/**", workflow)
        self.assertIn("research/g2/evidence/**", workflow)


if __name__ == "__main__":
    unittest.main()
