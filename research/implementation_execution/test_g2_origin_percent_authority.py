"""Synthetic source-origin controls; never evidence of physical G2 qualification."""
import hashlib
import tempfile
import unittest
from pathlib import Path

from g2_reference_catalog_readiness import (
    REQUIRED_SAFETY_POLICY, EXPECTED_AUTHORITY, EXPECTED_SCOPE,
    EXPECTED_SCHEMA_VERSION, audit_source_catalog,
)


class PercentAuthoritySourceProvenanceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        raw_dir = self.root / "research/g2/raw"
        raw_dir.mkdir(parents=True)
        rows = []
        for idx in range(2):
            raw = ("synthetic nonphysical origin fixture " + str(idx)).encode("ascii")
            rel = "research/g2/raw/origin-" + str(idx) + ".bin"
            (self.root / rel).write_bytes(raw)
            rows.append({
                "id": "MQ-EXT-0" + str(idx + 1),
                "category": "EXTERNAL_MEASUREMENT_DATASET",
                "title": "Synthetic provenance control",
                "primary_url": "https://example.org/archive-" + str(idx),
                "measurement_qualification": True,
                "experiment_independence": "PHYSICAL_GROUPS_VERIFIED",
                "raw_file_status": "RAW_BYTES_SHA256_VERIFIED",
                "raw_sha256": hashlib.sha256(raw).hexdigest(),
                "raw_size_bytes": len(raw),
                "raw_local_path": rel,
                "physical_reference_traceability": True,
            })
        self.catalog = {
            "schema_version": EXPECTED_SCHEMA_VERSION,
            "authority": EXPECTED_AUTHORITY,
            "scope": EXPECTED_SCOPE,
            "qualification_policy": {key: True for key in REQUIRED_SAFETY_POLICY},
            "record_count": 2,
            "counts_by_type": {"EXTERNAL_MEASUREMENT_DATASET": 2},
            "primary_catalog_entries": rows,
        }

    def matrix(self):
        return audit_source_catalog(
            self.catalog, expected_total=2, expected_datasets=2,
            repo_root=self.root,
        )["datasets"]

    def test_encoded_hostname_is_blocked_not_counted_as_independent(self):
        self.catalog["primary_catalog_entries"][0]["primary_url"] = (
            "https://exa%6Dple.org/archive-0"
        )
        first, second = self.matrix()
        self.assertIn("ORIGINAL_SOURCE_ORIGIN_MALFORMED", first["blockers"])
        self.assertFalse(first["catalog_source_review_ready"])
        self.assertTrue(second["catalog_source_review_ready"])

    def test_invalid_percent_triplets_fail_closed_for_url_and_doi(self):
        bad = (
            "https://example.org/archive%",
            "https://example.org/archive%G1",
            "https://example.org/archive%1",
            "https://example.org/archive?x=%u0041",
        )
        for url in bad:
            with self.subTest(url=url):
                self.catalog["primary_catalog_entries"][0]["primary_url"] = url
                first, second = self.matrix()
                self.assertIn("ORIGINAL_SOURCE_ORIGIN_MALFORMED", first["blockers"])
                self.assertTrue(second["catalog_source_review_ready"])
        self.catalog["primary_catalog_entries"][0]["primary_url"] = (
            "https://example.org/archive-0"
        )
        for doi in ("10.9999/bad%", "10.9999/bad%GG", "10.9999/bad%1"):
            with self.subTest(doi=doi):
                self.catalog["primary_catalog_entries"][0]["doi"] = doi
                self.assertIn("ORIGINAL_SOURCE_ORIGIN_MALFORMED",
                              self.matrix()[0]["blockers"])

    def test_valid_percent_encoding_is_still_accepted(self):
        self.catalog["primary_catalog_entries"][0]["primary_url"] = (
            "https://example.org/%7Earchive%25"
        )
        first, second = self.matrix()
        self.assertTrue(first["catalog_source_review_ready"])
        self.assertTrue(second["catalog_source_review_ready"])

    def test_equivalent_unreserved_escape_is_not_a_second_source(self):
        self.catalog["primary_catalog_entries"][0]["primary_url"] = (
            "https://example.org/%7Earchive"
        )
        self.catalog["primary_catalog_entries"][1]["primary_url"] = (
            "https://example.org/~archive"
        )
        first, second = self.matrix()
        for row in (first, second):
            self.assertIn("DUPLICATE_ORIGINAL_DATASET_ORIGIN", row["blockers"])
            self.assertFalse(row["catalog_source_review_ready"])


if __name__ == "__main__":
    unittest.main()
