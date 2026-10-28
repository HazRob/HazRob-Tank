"""Reproducibility and fail-closed tests for G2 acquisition triage."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from g2_acquisition_triage import make_triage, read_json


def fixture():
    audit = {
        "schema": "hazrob.g2.audit.v1", "g2_blocked": 80, "g2_pass_candidates": 0,
        "records": [{"id": "T%02d" % i, "status": "G2_BLOCKED",
                     "issues": ["MISSING_RAW_SHA256"]} for i in range(80)]
    }
    matrix = {
        "schema": "hazrob.g2.central-catalog-readiness.v1",
        "source_review_ready": 0, "source_review_blocked": 14,
        "datasets": [{"id": "MQ-EXT-%02d" % i,
                      "catalog_source_review_ready": False,
                      "blockers": ["ORIGINAL_RAW_SHA256_NOT_DOCUMENTED"]}
                     for i in range(1, 15)]
    }
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

    def test_duplicate_json_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "forged.json"
            path.write_text('{"records":[],"records":[1]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
                read_json(path)


if __name__ == "__main__":
    unittest.main()
