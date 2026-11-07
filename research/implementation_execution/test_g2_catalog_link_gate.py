"""Offline negative controls for source-to-technique evidence reconciliation.

All records are synthetic. Passing a source gate does not establish ppm,
gas identity, independent physical exposures or G2/G3 scientific approval.
"""
import json
import tempfile
import unittest
from pathlib import Path

from g2_catalog_link_gate import (
    PINNED_SOURCE_MATRIX_BLOB, load_pinned_source_matrix,
    reconcile_catalog_source_evidence,
)
from g2_reference_catalog_readiness import git_blob_sha

ROOT = Path(__file__).resolve().parent
MATRIX = ROOT / "G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json"


def candidate():
    return [{"id": "01.1", "status": "G2_PASS_CANDIDATE_REQUIRES_REVIEW",
             "issues": []}]


class CentralSourceBindingTests(unittest.TestCase):
    def setUp(self):
        self.matrix = load_pinned_source_matrix(MATRIX)

    def check(self, record):
        return reconcile_catalog_source_evidence(
            candidate(), [{"id": "01.1", **record}], self.matrix)[0]

    def test_pinned_source_matrix_bytes_are_replayable(self):
        self.assertEqual(git_blob_sha(MATRIX.read_bytes()),
                         PINNED_SOURCE_MATRIX_BLOB)
        self.assertEqual(len(self.matrix), 14)
        self.assertEqual(sum(row["catalog_source_review_ready"]
                             for row in self.matrix.values()), 0)

    def test_known_blocked_catalog_source_cannot_be_review_candidate(self):
        result = self.check({"dataset_id": "MQ-EXT-01"})
        self.assertEqual(result["status"], "G2_BLOCKED")
        self.assertIn("CATALOG_SOURCE_NOT_REVIEW_READY:MQ-EXT-01",
                      result["issues"])

    def test_explicit_binding_blocks_alias_dataset_name(self):
        result = self.check({"dataset_id": "alternate-archive-name",
                             "catalog_source_id": "MQ-EXT-15"})
        self.assertIn("CATALOG_SOURCE_NOT_REVIEW_READY:MQ-EXT-15",
                      result["issues"])

    def test_mismatched_explicit_binding_cannot_override_dataset_id(self):
        result = self.check({"dataset_id": "MQ-EXT-01",
                             "catalog_source_id": "MQ-EXT-15"})
        self.assertIn("CATALOG_SOURCE_ID_CONFLICT", result["issues"])

    def test_unknown_or_malformed_central_source_fails_closed(self):
        for record, expected in (
            ({"dataset_id": "MQ-EXT-99"},
             "CATALOG_SOURCE_NOT_IN_PINNED_MATRIX:MQ-EXT-99"),
            ({"dataset_id": "MQ-EXT-01-copy"},
             "CATALOG_SOURCE_BINDING_INVALID"),
            ({"dataset_id": "MQ-EXT-01", "catalog_source_id": ""},
             "CATALOG_SOURCE_BINDING_INVALID"),
            ({"dataset_id": "tank-exposure", "catalog_source_id": True},
             "CATALOG_SOURCE_BINDING_INVALID"),
        ):
            with self.subTest(record=record):
                self.assertIn(expected, self.check(record)["issues"])

    def test_non_catalogue_original_experiment_is_not_mislabeled(self):
        result = self.check({"dataset_id": "tank-original-session-001"})
        self.assertEqual(result["status"], "G2_PASS_CANDIDATE_REQUIRES_REVIEW")
        self.assertEqual(result["issues"], [])

    def test_source_ready_never_upgrades_blocked_technique(self):
        # A future, separately audited source may become catalogue-ready,
        # but the source layer must never promote an already blocked technique.
        matrix = {"MQ-EXT-01": {"catalog_source_review_ready": True}}
        original = [{"id": "01.1", "status": "G2_BLOCKED",
                     "issues": ["NO_BLIND_HOLDOUT"]}]
        result = reconcile_catalog_source_evidence(
            original, [{"id": "01.1", "dataset_id": "MQ-EXT-01"}], matrix)[0]
        self.assertEqual(result["status"], "G2_BLOCKED")
        self.assertEqual(result["issues"], ["NO_BLIND_HOLDOUT"])

    def test_duplicate_records_cannot_hide_blocked_catalog_source(self):
        result = reconcile_catalog_source_evidence(
            candidate(), [
                {"id": "01.1", "dataset_id": "tank-original-session"},
                {"id": "01.1", "dataset_id": "MQ-EXT-01"},
            ], self.matrix)[0]
        self.assertIn("CATALOG_SOURCE_NOT_REVIEW_READY:MQ-EXT-01",
                      result["issues"])

    def test_tampered_matrix_rejected_even_if_counts_look_plausible(self):
        raw = MATRIX.read_bytes()
        modified = raw.replace(b'"source_review_ready": 0',
                               b'"source_review_ready": 1', 1)
        self.assertNotEqual(raw, modified)
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "matrix.json"
            path.write_bytes(modified)
            with self.assertRaisesRegex(
                    ValueError, "G2_SOURCE_MATRIX_GIT_BLOB_MISMATCH"):
                load_pinned_source_matrix(path)


if __name__ == "__main__":
    unittest.main()
