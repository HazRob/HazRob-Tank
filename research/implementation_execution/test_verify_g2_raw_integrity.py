"""Raw byte integrity regression tests (stdlib only)."""
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from verify_g2_raw_integrity import verify

class RawIntegrityTests(unittest.TestCase):
    def test_empty_manifest_is_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(verify({"records": []}, directory)["status"], "BLOCKED")

    def test_real_bytes_verified_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"raw,adc\\n1,200\\n"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest()}]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertFalse(result["scientifically_validated"])

    def test_modified_bytes_are_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "sample.csv").write_bytes(b"modified")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(b"original").hexdigest()}]}, directory)
            self.assertEqual(result["status"], "BLOCKED")

    def test_traversal_and_symlink_escape_are_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory, "root")
            root.mkdir()
            external = Path(directory, "outside.csv")
            external.write_bytes(b"raw")
            (root / "link.csv").symlink_to(external)
            for path in ("../outside.csv", "link.csv"):
                with self.subTest(path=path):
                    result = verify({"records": [{"raw_data_path": path,
                        "raw_sha256": hashlib.sha256(b"raw").hexdigest()}]}, root)
                    self.assertEqual(result["status"], "BLOCKED")


    def test_missing_file_blocks_batch_even_when_another_file_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"independent-raw-sample"
            Path(directory, "present.csv").write_bytes(data)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "present.csv", "raw_sha256": digest},
                {"raw_data_path": "missing.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertEqual(len(result["errors"]), 1)
            self.assertFalse(result["scientifically_validated"])

    def test_uppercase_sha256_matches_real_bytes_without_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"raw-data-case-check"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [{
                "raw_data_path": "sample.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest().upper()
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])

    def test_missing_root_is_blocked_without_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = Path(directory, "missing")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": "0" * 64}]}, missing)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])
            self.assertIn("invalid raw-data root", result["errors"][0])

    def test_file_root_is_blocked_without_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            root_file = Path(directory, "root.txt")
            root_file.write_text("not a directory")
            result = verify({"records": [{"raw_data_path": "sample.csv",
                "raw_sha256": "0" * 64}]}, root_file)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertFalse(result["scientifically_validated"])

    def test_duplicate_raw_path_blocks_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"independent-sample"
            Path(directory, "sample.csv").write_bytes(data)
            record = {"raw_data_path": "sample.csv",
                      "raw_sha256": hashlib.sha256(data).hexdigest()}
            result = verify({"records": [record, dict(record)]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("duplicate raw file path", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_distinct_raw_paths_are_integrity_checked_only(self):
        with tempfile.TemporaryDirectory() as directory:
            records = []
            for name, data in (("a.csv", b"a"), ("b.csv", b"b")):
                Path(directory, name).write_bytes(data)
                records.append({"raw_data_path": name,
                                "raw_sha256": hashlib.sha256(data).hexdigest()})
            result = verify({"records": records}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 2)
            self.assertFalse(result["scientifically_validated"])

    def test_hardlinked_files_cannot_count_as_independent_files(self):
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory, "first.csv")
            second = Path(directory, "second.csv")
            data = b"same-inode"
            first.write_bytes(data)
            import os
            os.link(first, second)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "first.csv", "raw_sha256": digest},
                {"raw_data_path": "second.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("hard link", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_internal_symlink_alias_cannot_inflate_raw_file_count(self):
        with tempfile.TemporaryDirectory() as directory:
            original = Path(directory, "original.csv")
            alias = Path(directory, "alias.csv")
            data = b"single-source"
            original.write_bytes(data)
            alias.symlink_to(original.name)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "original.csv", "raw_sha256": digest},
                {"raw_data_path": "alias.csv", "raw_sha256": digest}
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("duplicate raw file path", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])


    def test_nested_raw_file_is_verified_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            nested = Path(directory, "session-001", "raw")
            nested.mkdir(parents=True)
            data = b"adc,time\\n1024,0\\n"
            (nested / "sample.csv").write_bytes(data)
            result = verify({"records": [{
                "raw_data_path": "session-001/raw/sample.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest()
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])

    def test_directory_as_raw_file_is_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "sample.csv").mkdir()
            result = verify({"records": [{
                "raw_data_path": "sample.csv", "raw_sha256": "0" * 64
            }]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_malformed_records_and_digests_are_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"measured-raw"
            Path(directory, "sample.csv").write_bytes(data)
            cases = [
                None, [], "not-a-record",
                {"raw_data_path": "sample.csv", "raw_sha256": "0" * 63},
                {"raw_data_path": "sample.csv", "raw_sha256": "g" * 64},
                {"raw_data_path": 123, "raw_sha256": hashlib.sha256(data).hexdigest()},
            ]
            for record in cases:
                with self.subTest(record=record):
                    result = verify({"records": [record]}, directory)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])

    def test_mixed_valid_and_invalid_records_cannot_pass_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"measured-raw"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [
                {"raw_data_path": "sample.csv", "raw_sha256": hashlib.sha256(data).hexdigest()},
                {"raw_data_path": "missing.csv", "raw_sha256": "not-a-digest"},
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertEqual(len(result["errors"]), 1)
            self.assertFalse(result["scientifically_validated"])


    def test_distinct_files_with_identical_bytes_are_not_independent_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"same-payload-not-independent-observations"
            digest = hashlib.sha256(data).hexdigest()
            records = []
            for name in ("replica-a.csv", "replica-b.csv"):
                Path(directory, name).write_bytes(data)
                records.append({"raw_data_path": name, "raw_sha256": digest})
            result = verify({"records": records}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 2)
            self.assertFalse(result["scientifically_validated"])

    def test_invalid_top_level_manifests_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            for manifest in (None, [], "invalid", {"records": None},
                             {"records": {}}, {"records": "sample.csv"}, {}):
                with self.subTest(manifest=manifest):
                    result = verify(manifest, directory)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])

    def test_failed_digest_does_not_reserve_file_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"valid-raw-payload"
            Path(directory, "sample.csv").write_bytes(data)
            result = verify({"records": [
                {"raw_data_path": "sample.csv", "raw_sha256": "0" * 64},
                {"raw_data_path": "sample.csv", "raw_sha256": hashlib.sha256(data).hexdigest()},
            ]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertEqual(len(result["errors"]), 1)
            self.assertIn("SHA-256 mismatch", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_unsafe_path_forms_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            data = b"raw-path-policy"
            Path(directory, "sample.csv").write_bytes(data)
            digest = hashlib.sha256(data).hexdigest()
            for candidate in ("", "/sample.csv", "../sample.csv", "./sample.csv",
                              "sub//sample.csv", "sub/../sample.csv",
                              "~/sample.csv", "C:/sample.csv",
                              "sub\\\\sample.csv"):
                with self.subTest(path=candidate):
                    result = verify({"records": [{
                        "raw_data_path": candidate, "raw_sha256": digest
                    }]}, directory)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])


    def test_empty_raw_file_is_hash_verified_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "empty.csv").write_bytes(b"")
            result = verify({"records": [{
                "raw_data_path": "empty.csv",
                "raw_sha256": hashlib.sha256(b"").hexdigest(),
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])

    def test_unicode_filename_is_verified_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            filename = "قياس-001.csv"
            payload = b"adc,time\\n200,1\\n"
            Path(directory, filename).write_bytes(payload)
            result = verify({"records": [{
                "raw_data_path": filename,
                "raw_sha256": hashlib.sha256(payload).hexdigest(),
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])


    def test_streaming_sha256_across_multiple_read_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            payload = (bytes(range(256)) * 8193) + b"\\x00\\xff\\x00"
            Path(directory, "binary.bin").write_bytes(payload)
            result = verify({"records": [{
                "raw_data_path": "binary.bin",
                "raw_sha256": hashlib.sha256(payload).hexdigest(),
            }]}, directory)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertEqual(result["verified_files"], 1)
            self.assertFalse(result["scientifically_validated"])

    def test_streaming_sha256_detects_mutation_after_first_block(self):
        with tempfile.TemporaryDirectory() as directory:
            original = bytes(range(256)) * 8193
            modified = original[:-1] + bytes([original[-1] ^ 1])
            Path(directory, "binary.bin").write_bytes(modified)
            result = verify({"records": [{
                "raw_data_path": "binary.bin",
                "raw_sha256": hashlib.sha256(original).hexdigest(),
            }]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertIn("SHA-256 mismatch", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])


    def test_dangling_symlink_is_blocked_without_crashing(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "missing-link.csv").symlink_to("nonexistent.csv")
            result = verify({"records": [{
                "raw_data_path": "missing-link.csv", "raw_sha256": "0" * 64,
            }]}, directory)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_nested_symlink_escape_is_blocked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory, "root")
            nested = root / "nested"
            nested.mkdir(parents=True)
            outside = Path(directory, "outside.csv")
            data = b"independent-source-boundary"
            outside.write_bytes(data)
            (nested / "alias.csv").symlink_to(outside)
            result = verify({"records": [{
                "raw_data_path": "nested/alias.csv",
                "raw_sha256": hashlib.sha256(data).hexdigest(),
            }]}, root)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])


    def test_nested_directory_symlink_alias_cannot_double_count_raw_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            nested = root / "session" / "raw"
            nested.mkdir(parents=True)
            data = b"single-acquisition-through-directory-alias"
            (nested / "sample.bin").write_bytes(data)
            (root / "alias").symlink_to(nested, target_is_directory=True)
            digest = hashlib.sha256(data).hexdigest()
            result = verify({"records": [
                {"raw_data_path": "session/raw/sample.bin", "raw_sha256": digest},
                {"raw_data_path": "alias/sample.bin", "raw_sha256": digest},
            ]}, root)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertIn("duplicate raw file path", result["errors"][0])
            self.assertFalse(result["scientifically_validated"])

    def test_symlink_cycle_is_blocked_without_crashing(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "cycle-a").symlink_to("cycle-b")
            (root / "cycle-b").symlink_to("cycle-a")
            result = verify({"records": [
                {"raw_data_path": "cycle-a", "raw_sha256": "0" * 64},
            ]}, root)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])


    def test_cli_valid_raw_bytes_exit_zero_without_scientific_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"raw-cli-check"
            (root / "sample.bin").write_bytes(payload)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [{
                "raw_data_path": "sample.bin",
                "raw_sha256": hashlib.sha256(payload).hexdigest()
            }]}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 0, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "RAW_INTEGRITY_VERIFIED_ONLY")
            self.assertFalse(result["scientifically_validated"])

    def test_cli_malformed_manifest_exits_nonzero_and_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            manifest.write_text('{"records": [', encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])


    def test_cli_missing_raw_file_exits_two_and_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [{
                "raw_data_path": "absent.bin", "raw_sha256": "0" * 64
            }]}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertEqual(len(result["errors"]), 1)
            self.assertFalse(result["scientifically_validated"])

    def test_cli_mixed_batch_exits_two_even_with_verified_raw_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"real-raw-bytes"
            (root / "present.bin").write_bytes(payload)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [
                {"raw_data_path": "present.bin",
                 "raw_sha256": hashlib.sha256(payload).hexdigest()},
                {"raw_data_path": "absent.bin", "raw_sha256": "0" * 64}
            ]}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertEqual(len(result["errors"]), 1)
            self.assertFalse(result["scientifically_validated"])


    def test_cli_symlink_escape_exits_two_and_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            root = base / "root"
            root.mkdir()
            external = base / "outside.bin"
            payload = b"external-not-independent"
            external.write_bytes(payload)
            (root / "alias.bin").symlink_to(external)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [{
                "raw_data_path": "alias.bin",
                "raw_sha256": hashlib.sha256(payload).hexdigest()
            }]}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_cli_duplicate_hardlink_exits_two_and_blocks(self):
        import os
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"one-acquisition-not-two"
            (root / "first.bin").write_bytes(payload)
            os.link(root / "first.bin", root / "second.bin")
            digest = hashlib.sha256(payload).hexdigest()
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": [
                {"raw_data_path": "first.bin", "raw_sha256": digest},
                {"raw_data_path": "second.bin", "raw_sha256": digest}
            ]}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 1)
            self.assertTrue(any("hard link" in e for e in result["errors"]))
            self.assertFalse(result["scientifically_validated"])


    def test_cli_empty_evidence_manifest_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"records": []}), encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_cli_missing_manifest_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(root / "missing.json"), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])


    def test_cli_invalid_utf8_manifest_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            manifest.write_bytes(b"\\xff\\xfe\\x00")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_cli_scalar_manifest_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            manifest.write_text("null", encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                 "--manifest", str(manifest), "--root", str(root)],
                capture_output=True, text=True, check=False)
            self.assertEqual(process.returncode, 2, process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual(result["status"], "BLOCKED")
            self.assertEqual(result["verified_files"], 0)
            self.assertFalse(result["scientifically_validated"])

    def test_cli_rejects_duplicate_json_keys_at_all_levels(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            payload = b"independent-test"
            (root / "sample.bin").write_bytes(payload)
            digest = hashlib.sha256(payload).hexdigest()
            cases = [
                '{"records":[],"records":[{"raw_data_path":"sample.bin","raw_sha256":"'
                + digest + '"}]}',
                '{"records":[{"raw_data_path":"missing.bin","raw_data_path":"sample.bin",'
                '"raw_sha256":"' + digest + '"}]}',
                '{"records":[{"raw_data_path":"sample.bin","raw_sha256":"'
                + "0" * 64 + '","raw_sha256":"' + digest + '"}]}',
            ]
            for content in cases:
                with self.subTest(content=content):
                    manifest.write_text(content, encoding="utf-8")
                    process = subprocess.run(
                        [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                         "--manifest", str(manifest), "--root", str(root)],
                        capture_output=True, text=True, check=False)
                    self.assertEqual(process.returncode, 2, process.stderr)
                    result = json.loads(process.stdout)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])
                    self.assertIn("duplicate JSON key", result["errors"][0])

    def test_cli_rejects_nonfinite_json_constants(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            for token in ("NaN", "Infinity", "-Infinity"):
                for content in ('{"records":[],"extra":' + token + '}',
                                '{"records":[{"raw_data_path":"sample.bin",'
                                '"raw_sha256":"' + "0" * 64 + '","extra":' + token + '}]}'):
                    with self.subTest(token=token, content=content):
                        manifest.write_text(content, encoding="utf-8")
                        process = subprocess.run(
                            [sys.executable, str(Path(__file__).with_name("verify_g2_raw_integrity.py")),
                             "--manifest", str(manifest), "--root", str(root)],
                            capture_output=True, text=True, check=False)
                        self.assertEqual(process.returncode, 2, process.stderr)
                        result = json.loads(process.stdout)
                        self.assertEqual(result["status"], "BLOCKED")
                        self.assertEqual(result["verified_files"], 0)
                        self.assertFalse(result["scientifically_validated"])
                        self.assertIn("non-finite JSON constant", result["errors"][0])

    def test_deterministic_single_byte_mutations_across_block_boundaries(self):
        """Seeded corruption matrix; all altered payloads must fail SHA-256."""
        import random
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            rng = random.Random(20261112)
            original = bytes(rng.getrandbits(8) for _ in range(1024 * 1024 + 17))
            digest = hashlib.sha256(original).hexdigest()
            sample = root / "sample.bin"
            offsets = [0, 1, 1024 * 1024 - 1, 1024 * 1024,
                       1024 * 1024 + 1, len(original) - 1]
            offsets.extend(rng.sample(range(len(original)), 12))
            for offset in offsets:
                with self.subTest(offset=offset):
                    mutated = bytearray(original)
                    mutated[offset] ^= 0x01
                    sample.write_bytes(mutated)
                    result = verify({"records": [{
                        "raw_data_path": "sample.bin", "raw_sha256": digest
                    }]}, root)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])
                    self.assertIn("SHA-256 mismatch", result["errors"][0])


    def test_truncation_and_append_corruption_fail_closed(self):
        """Length changes must invalidate a known SHA-256 across streaming boundaries."""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            original = bytes(range(256)) * 4097 + b"tail"
            digest = hashlib.sha256(original).hexdigest()
            sample = root / "sample.bin"
            for label, changed in (
                ("truncate_one", original[:-1]),
                ("truncate_block", original[:1024 * 1024]),
                ("append_one", original + bytes([0])),
                ("append_block", original + bytes(1024 * 1024)),
            ):
                with self.subTest(case=label):
                    sample.write_bytes(changed)
                    result = verify({"records": [{
                        "raw_data_path": "sample.bin", "raw_sha256": digest
                    }]}, root)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])
                    self.assertIn("SHA-256 mismatch", result["errors"][0])

    def test_cli_rejects_actual_invalid_utf8_bytes(self):
        """Exercise real invalid byte sequences, not backslash-escaped ASCII text."""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "manifest.json"
            for label, payload in (
                ("invalid_lead", bytes([0xff, 0xfe, 0x00])),
                ("invalid_continuation", b'{"records":[]}' + bytes([0x80])),
                ("truncated_multibyte", b'{"records":[]}' + bytes([0xe2, 0x82])),
            ):
                with self.subTest(case=label):
                    manifest.write_bytes(payload)
                    process = subprocess.run(
                        [sys.executable, str(Path(__file__).with_name(
                            "verify_g2_raw_integrity.py")),
                         "--manifest", str(manifest), "--root", str(root)],
                        capture_output=True, text=True, check=False)
                    self.assertEqual(process.returncode, 2, process.stderr)
                    result = json.loads(process.stdout)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertEqual(result["verified_files"], 0)
                    self.assertFalse(result["scientifically_validated"])

if __name__ == "__main__":
    unittest.main()
