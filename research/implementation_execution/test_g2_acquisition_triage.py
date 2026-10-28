"""Reproducibility and fail-closed tests for G2 acquisition triage."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from g2_acquisition_triage import (CANONICAL_REGISTRY, PINNED_MATRIX,
                                   build_triage, make_triage, read_json)
from g2_evidence_audit import load_canonical_technique_ids


def fixture():
    canonical_ids = load_canonical_technique_ids(CANONICAL_REGISTRY)
    audit = {
        "schema": "hazrob.g2.audit.v1",
        "scope": "new V4 independent source qualification",
        "g2_blocked": 80, "g2_pass_candidates": 0,
        "records": [{"id": tid, "status": "G2_BLOCKED",
                     "issues": ["MISSING_RAW_SHA256"]} for tid in canonical_ids],
    }
    matrix = read_json(PINNED_MATRIX)
    return audit, matrix

class AcquisitionTriageTests(unittest.TestCase):
    def test_deterministic_80_by_14_and_no_promotion(self):
        audit, matrix = fixture()
        a = make_triage(audit, matrix)
        b = make_triage(copy.deepcopy(audit), copy.deepcopy(matrix))
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertEqual(a["g2_blocked"], 80)
        self.assertEqual(a["external_sources_blocked"], 14)
        self.assertEqual(a["source_blocker_counts"]["ORIGINAL_RAW_SHA256_NOT_DOCUMENTED"], 14)
        self.assertEqual(a["g2_pass_candidates_requiring_review"], 0)
        self.assertEqual(len(a["techniques"]), 80)

    def test_forged_counts_and_duplicate_identifiers_fail_closed(self):
        for mode in ("counts", "duplicate_source", "duplicate_technique", "ready_with_blockers"):
            with self.subTest(mode=mode):
                audit, matrix = fixture()
                if mode == "counts":
                    audit["g2_pass_candidates"] = True
                elif mode == "duplicate_source":
                    matrix["datasets"][1]["id"] = matrix["datasets"][0]["id"]
                elif mode == "duplicate_technique":
                    audit["records"][1]["id"] = audit["records"][0]["id"]
                else:
                    matrix["datasets"][0]["catalog_source_review_ready"] = True
                with self.assertRaises(ValueError):
                    make_triage(audit, matrix)

    def test_canonical_80_and_pinned_14_cannot_be_substituted(self):
        for mode in ("technique", "source", "source_blockers"):
            with self.subTest(mode=mode):
                audit, matrix = fixture()
                if mode == "technique":
                    audit["records"][0]["id"] = "99.99"
                elif mode == "source":
                    matrix["datasets"][0]["id"] = "MQ-EXT-99"
                else:
                    matrix["datasets"][0]["blockers"] = ["UNREVIEWED_UNKNOWN_REASON"]
                with self.assertRaises(ValueError):
                    make_triage(audit, matrix)

    def test_status_and_issue_consistency_is_required(self):
        for mode in ("blocked_without_issues", "candidate_with_issues", "blank_issue"):
            with self.subTest(mode=mode):
                audit, matrix = fixture()
                row = audit["records"][0]
                if mode == "blocked_without_issues":
                    row["issues"] = []
                elif mode == "candidate_with_issues":
                    row["status"] = "G2_PASS_CANDIDATE_REQUIRES_REVIEW"
                else:
                    row["issues"] = ["   "]
                with self.assertRaisesRegex(ValueError, "G2_TRIAGE_RECORDS_INVALID"):
                    make_triage(audit, matrix)

    def test_cli_rejects_rehashed_but_unpinned_source_matrix(self):
        audit, matrix = fixture()
        with tempfile.TemporaryDirectory() as temp:
            audit_path = Path(temp) / "audit.json"
            matrix_path = Path(temp) / "matrix.json"
            audit_path.write_text(json.dumps(audit), encoding="utf-8")
            matrix["datasets"][0]["notes"] = "changed while preserving 14 source IDs"
            matrix_path.write_text(json.dumps(matrix), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "G2_SOURCE_MATRIX_GIT_BLOB_MISMATCH"):
                build_triage(audit_path, matrix_path)

    def test_nonfinite_or_overflow_json_numbers_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "invalid.json"
            for data in ('{"n":1e999}', '{"n":' + "9" * 400 + "}"):
                with self.subTest(data=data[:20]):
                    path.write_text(data, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "NONFINITE_JSON_NUMBER"):
                        read_json(path)

    def test_duplicate_json_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "forged.json"
            path.write_text('{"records":[],"records":[1]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
                read_json(path)


if __name__ == "__main__":
    unittest.main()
