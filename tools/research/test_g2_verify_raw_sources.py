#!/usr/bin/env python3
"""Regression tests for G2 raw-source integrity verification (stdlib only).

Run: python -m unittest discover -s tools/research -p 'test_g2_verify_raw_sources.py' -v
"""
import hashlib
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

from g2_verify_raw_sources import verify

SCRIPT = pathlib.Path(__file__).with_name("g2_verify_raw_sources.py")


class RawSourceVerificationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name) / "raw"
        self.root.mkdir()
        self.payload = b"reproducible G2 source fixture\n"
        (self.root / "sample.bin").write_bytes(self.payload)
        self.digest = hashlib.sha256(self.payload).hexdigest()

    def entry(self, path="sample.bin", sha=None):
        return {"source_id": "fixture", "path": path, "sha256": sha or self.digest}

    def test_verified_bytes_do_not_promote_science(self):
        result = verify({"sources": [self.entry()]}, self.root)
        self.assertTrue(result["all_verified"])
        self.assertEqual(result["results"][0]["status"], "verified")
        self.assertFalse(result["scientific_qualification"])
        self.assertEqual(result["results"][0]["size_bytes"], len(self.payload))

    def test_empty_manifest_does_not_pass(self):
        self.assertFalse(verify({"sources": []}, self.root)["all_verified"])

    def test_duplicate_source_identity_blocked(self):
        result = verify({"sources": [self.entry(), self.entry()]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][1]["error"], "duplicate_source_id")

    def test_whitespace_variant_source_identity_is_duplicate(self):
        entry = self.entry()
        padded = dict(entry, source_id=" fixture ")
        result = verify({"sources": [entry, padded]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][1]["error"], "duplicate_source_id")
        self.assertEqual(result["results"][1]["source_id"], "fixture")

    def test_duplicate_raw_path_across_distinct_ids_blocked(self):
        first = self.entry()
        second = dict(first, source_id="another-source")
        result = verify({"sources": [first, second]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][1]["error"], "duplicate_source_path")
        self.assertFalse(result["scientific_qualification"])

    def test_distinct_raw_paths_are_allowed(self):
        (self.root / "second.bin").write_bytes(self.payload)
        second = self.entry("second.bin")
        second["source_id"] = "another-source"
        result = verify({"sources": [self.entry(), second]}, self.root)
        self.assertTrue(result["all_verified"])
        self.assertFalse(result["scientific_qualification"])

    def test_alias_path_is_detected_as_duplicate(self):
        alias = dict(self.entry(), source_id="alias", path="sub/../sample.bin")
        result = verify({"sources": [self.entry(), alias]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][1]["error"], "unsafe_path")

    def test_repeated_separator_path_is_detected_as_duplicate(self):
        alias = dict(self.entry(), source_id="alias", path="sub//../sample.bin")
        result = verify({"sources": [self.entry(), alias]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][1]["error"], "unsafe_path")

    def test_non_string_source_identity_blocked(self):
        entry = dict(self.entry(), source_id=42)
        result = verify({"sources": [entry]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][0]["error"], "invalid_source_id")

    def test_non_string_hash_blocked(self):
        entry = dict(self.entry(), sha256=123)
        result = verify({"sources": [entry]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][0]["error"], "invalid_sha256")

    def test_empty_source_identity_blocked(self):
        entry = self.entry()
        entry["source_id"] = " "
        result = verify({"sources": [entry]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][0]["error"], "invalid_source_id")

    def test_missing_source(self):
        result = verify({"sources": [self.entry("missing.bin")]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][0]["error"], "missing_raw_bytes")

    def test_hash_mismatch(self):
        result = verify({"sources": [self.entry(sha="0" * 64)]}, self.root)
        self.assertEqual(result["results"][0]["status"], "hash_mismatch")
        self.assertEqual(result["results"][0]["size_bytes"], len(self.payload))

    def test_parent_traversal(self):
        result = verify({"sources": [self.entry("../sample.bin")]}, self.root)
        self.assertEqual(result["results"][0]["error"], "unsafe_path")

    def test_absolute_path(self):
        result = verify({"sources": [self.entry(str(self.root / "sample.bin"))]}, self.root)
        self.assertEqual(result["results"][0]["error"], "unsafe_path")

    def test_single_backslash_rejected(self):
        result = verify({"sources": [self.entry("sub\\sample.bin")]}, self.root)
        self.assertEqual(result["results"][0]["error"], "unsafe_path")

    def test_manifest_rejects_non_object_entry(self):
        result = verify({"sources": [None]}, self.root)
        self.assertFalse(result["all_verified"])
        self.assertEqual(result["results"][0]["error"], "invalid_source_entry")

    def test_rejects_duplicate_identity_even_with_different_path(self):
        entry = self.entry("missing.bin")
        result = verify({"sources": [self.entry(), entry]}, self.root)
        self.assertEqual(result["results"][1]["error"], "duplicate_source_id")

    def test_invalid_hash(self):
        result = verify({"sources": [self.entry(sha="invalid")]}, self.root)
        self.assertEqual(result["results"][0]["error"], "invalid_sha256")

    def test_direct_symlink(self):
        link = self.root / "alias.bin"
        try:
            link.symlink_to(self.root / "sample.bin")
        except (OSError, NotImplementedError):
            self.skipTest("symlinks unsupported")
        result = verify({"sources": [self.entry("alias.bin")]}, self.root)
        self.assertEqual(result["results"][0]["error"], "unsafe_path")

    def test_nested_symlink(self):
        directory = self.root / "alias"
        try:
            directory.symlink_to(self.root, target_is_directory=True)
        except (OSError, NotImplementedError):
            self.skipTest("symlinks unsupported")
        result = verify({"sources": [self.entry("alias/sample.bin")]}, self.root)
        self.assertEqual(result["results"][0]["error"], "unsafe_path")

    def test_cli_success_and_failure_exit_codes(self):
        manifest = pathlib.Path(self.tmp.name) / "manifest.json"
        manifest.write_text(json.dumps({"sources": [self.entry()]}), encoding="utf-8")
        command = [sys.executable, str(SCRIPT), str(manifest), "--root", str(self.root)]
        success = subprocess.run(command, capture_output=True, text=True, check=False)
        self.assertEqual(success.returncode, 0, success.stderr)
        self.assertTrue(json.loads(success.stdout)["all_verified"])
        manifest.write_text(json.dumps({"sources": [self.entry(sha="0" * 64)]}), encoding="utf-8")
        failure = subprocess.run(command, capture_output=True, text=True, check=False)
        self.assertEqual(failure.returncode, 1)
        self.assertFalse(json.loads(failure.stdout)["all_verified"])


if __name__ == "__main__":
    unittest.main()
