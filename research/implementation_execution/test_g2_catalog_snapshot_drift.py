"""Fail-closed regressions for the pinned G2 source snapshot audit."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

SOURCE = Path(__file__).with_name("g2_catalog_snapshot_drift.py")
spec = importlib.util.spec_from_file_location("g2_catalog_snapshot_drift", SOURCE)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def fixtures():
    left = {"schema": "hazrob.g2.central-catalog-readiness.v1",
            "scope": "SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL",
            "source_git_blob_sha1": "a" * 40, "external_measurement_datasets": 14,
            "datasets": [{"id": f"MQ-EXT-{i:02}",
                          "catalog_source_review_ready": False,
                          "source_status": "archive_listed_not_downloaded",
                          "raw_file_status": "SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING"}
                         for i in range(1, 15)]}
    right = {"schema": "hazrob.g2.source-candidates.v1",
             "catalog_blob_sha": "a" * 40,
             "candidates": [{"id": f"MQ-EXT-{i:02}",
                             "category": "EXTERNAL_MEASUREMENT_DATASET",
                             "measurement_qualification": False,
                             "raw_file_status": "SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING"}
                            for i in range(1, 15)]}
    return left, right


class TestG2CatalogSnapshotDrift(unittest.TestCase):
    def test_identical_unqualified_snapshots(self):
        a, b = fixtures()
        x = audit.reconcile(a, b)
        self.assertEqual(x["discrepancy_count"], 0)
        self.assertEqual(x["g2_qualified_by_this_audit"], 0)

    def test_claimed_receipt_is_diagnostic_only(self):
        a, b = fixtures()
        b["candidates"][0].update({
            "raw_file_status": "ORIGINAL_ARCHIVE_SHA256_AND_PUBLISHER_MD5_VERIFIED",
            "source_sha256": "b" * 64, "verified_raw_source_ci_run_id": 123})
        x = audit.reconcile(a, b)
        self.assertEqual([r["dataset_id"] for r in x["discrepancies"]], ["MQ-EXT-01"])
        self.assertEqual(x["g2_qualified_by_this_audit"], 0)
        self.assertEqual(x["g3_qualified_by_this_audit"], 0)
        self.assertFalse(a["datasets"][0]["catalog_source_review_ready"])

    def test_wrong_catalog_or_missing_id_rejected(self):
        a, b = fixtures()
        b["catalog_blob_sha"] = "c" * 40
        with self.assertRaisesRegex(ValueError, "SNAPSHOT_SCHEMA_OR_CATALOG_IDENTITY_MISMATCH"):
            audit.reconcile(a, b)
        a, b = fixtures()
        b["candidates"].pop()
        with self.assertRaisesRegex(ValueError, "EXTERNAL_DATASET_ID_SET_MISMATCH"):
            audit.reconcile(a, b)

    def test_invalid_digest_and_approval_rejected(self):
        a, b = fixtures()
        b["candidates"][0]["source_sha256"] = "not-a-digest"
        with self.assertRaisesRegex(ValueError, "CANDIDATE_SOURCE_DIGEST_INVALID"):
            audit.reconcile(a, b)
        a, b = fixtures()
        a["datasets"][0]["catalog_source_review_ready"] = True
        with self.assertRaisesRegex(ValueError, "SOURCE_QUALIFICATION_CHANGE_REQUIRES_INDEPENDENT_AUDIT"):
            audit.reconcile(a, b)

    def test_claim_without_digest_and_duplicate_ids_rejected(self):
        a, b = fixtures()
        b["candidates"][0]["raw_file_status"] = "ORIGINAL_ARCHIVE_SHA256_AND_PUBLISHER_MD5_VERIFIED"
        with self.assertRaisesRegex(ValueError, "CLAIMED_ORIGINAL_ARCHIVE_WITHOUT_SHA256"):
            audit.reconcile(a, b)
        a, b = fixtures()
        b["candidates"][1]["id"] = b["candidates"][0]["id"]
        with self.assertRaisesRegex(ValueError, "CANDIDATES_ROW_OR_ID_INVALID"):
            audit.reconcile(a, b)

    def test_duplicate_json_key_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "duplicate.json"
            path.write_text('{"id":"A","id":"B"}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
                audit.strict_json(path)

    def test_actual_pinned_snapshots_still_blocked(self):
        root = Path(__file__).resolve().parent
        a = audit.strict_json(root / "G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json")
        b = audit.strict_json(root / "G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json")
        x = audit.reconcile(a, b)
        self.assertEqual(x["external_datasets_reconciled"], 14)
        self.assertEqual([r["dataset_id"] for r in x["discrepancies"]], ["MQ-EXT-01"])
        self.assertEqual(x["g2_qualified_by_this_audit"], 0)


if __name__ == "__main__":
    unittest.main()
