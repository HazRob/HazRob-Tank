"""Fail-closed static checks for GitHub Actions G2 source/document trigger coverage.

This verifies YAML path intent, not whether GitHub actually ran a workflow
or whether any scientific technique has passed G2.
"""
from fnmatch import fnmatchcase
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/g2-signal-lineage-v4.yml"
RESEARCH_BRANCH = "research/chemcore80-central-evidence-gap-analysis-20261021"
WILDCARD = "research/implementation_execution/**"


def event_block(yaml_text, event):
    lines = yaml_text.splitlines()
    header = f"  {event}:"
    if lines.count(header) != 1:
        raise ValueError("G2_TRIGGER_EVENT_NOT_UNIQUE:" + event)
    start = lines.index(header) + 1
    stop = next((i for i in range(start, len(lines))
                 if lines[i].startswith("  ") and not lines[i].startswith("    ")
                 and lines[i].strip()), len(lines))
    return lines[start:stop]


def paths_for_event(yaml_text, event):
    block = event_block(yaml_text, event)
    if block.count("    paths:") != 1:
        raise ValueError("G2_TRIGGER_PATHS_NOT_UNIQUE:" + event)
    start = block.index("    paths:") + 1
    patterns = []
    for line in block[start:]:
        if line.startswith("      - "):
            patterns.append(line[8:].strip().strip("'\""))
        elif line.startswith("    ") and not line.startswith("      "):
            break
        elif line.strip() and not line.startswith("      "):
            break
    if not patterns:
        raise ValueError("G2_TRIGGER_PATHS_EMPTY:" + event)
    return patterns


def covered(patterns, path):
    return any(fnmatchcase(path, pattern) for pattern in patterns)


class G2WorkflowTriggerCoverageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.yaml_text = WORKFLOW.read_text(encoding="utf-8")

    def test_all_research_artifacts_covered_on_push_and_pr(self):
        critical = [
            "research/implementation_execution/RESUME.md",
            "research/implementation_execution/G2_MQ4_FORWARD_4PPM_2026-11-01.json",
            "research/implementation_execution/G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json",
            "research/implementation_execution/G2_MQ4_FORWARD_DAY_RANKING_2026-11-01.json",
            "research/implementation_execution/G2_MQ4_FORWARD_DAY_SUPPORT_STRATA_2026-11-01_AR.md",
            "research/implementation_execution/test_g2_workflow_trigger_coverage.py",
            "research/implementation_execution/g2_evidence_audit.py",
            "research/implementation_execution/new_nested/source.csv",
        ]
        for event in ("push", "pull_request"):
            patterns = paths_for_event(self.yaml_text, event)
            with self.subTest(event=event):
                self.assertIn(WILDCARD, patterns)
            for path in critical:
                with self.subTest(event=event, path=path):
                    self.assertTrue(covered(patterns, path))

    def test_missing_wildcard_exposes_prior_receipt_blind_spot(self):
        path = "research/implementation_execution/G2_MQ4_FORWARD_DAY_RANKING_2026-11-01.json"
        for event in ("push", "pull_request"):
            patterns = paths_for_event(self.yaml_text, event)
            with self.subTest(event=event):
                self.assertTrue(covered(patterns, path))
                self.assertFalse(covered([p for p in patterns if p != WILDCARD], path))

    def test_raw_evidence_and_registry_still_trigger(self):
        paths = (
            "research/g2/raw/MQ4/raw.csv",
            "research/g2/evidence/01.1/MQ4/run1/protocol_locked.json",
            "research/PROGRESS_80.json",
            "research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json",
        )
        for event in ("push", "pull_request"):
            patterns = paths_for_event(self.yaml_text, event)
            for path in paths:
                with self.subTest(event=event, path=path):
                    self.assertTrue(covered(patterns, path))

    def test_push_branch_is_research_only(self):
        block = event_block(self.yaml_text, "push")
        self.assertIn("      - " + RESEARCH_BRANCH, block)
        self.assertNotIn("      - main", block)
        self.assertNotIn("      - develop", block)

    def test_full_audit_and_publication_guard_remain(self):
        self.assertIn("python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v", self.yaml_text)
        self.assertIn("python research/implementation_execution/g2_evidence_audit.py", self.yaml_text)
        self.assertIn("python research/implementation_execution/g2_artifact_publish_gate.py", self.yaml_text)
        self.assertIn("  workflow_dispatch:", self.yaml_text)


if __name__ == "__main__":
    unittest.main()
