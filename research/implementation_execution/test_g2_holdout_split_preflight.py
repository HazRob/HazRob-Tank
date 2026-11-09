"""Regression tests for conservative G2 physical holdout partition audit."""
import importlib.util
import pathlib
import unittest
import tempfile
from unittest.mock import patch

path = pathlib.Path(__file__).with_name("g2_holdout_split_preflight.py")
spec = importlib.util.spec_from_file_location("g2_holdout_split_preflight", path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

SHA = "a" * 64
def row(i, p):
    return dict(sample_id=str(i), device_id=f"dev{i}", day_id=f"day{i}",
                session_id=f"session{i}", partition=p, raw_sha256=f"{i:064x}")

class SplitAuditTests(unittest.TestCase):
    def test_valid_disjoint_split_not_scientific_approval(self):
        r = module.audit([row(1,"train"),row(2,"validation"),row(3,"holdout")])
        self.assertEqual(r["status"], "PASS_SPLIT_INTEGRITY_ONLY")
        self.assertFalse(r["scientific_g2_approved"])

    def test_session_leakage_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        c["session_id"] = a["session_id"]
        self.assertEqual(module.audit([a,b,c])["status"],"FAIL")

    def test_device_and_day_leakage_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["device_id"] = a["device_id"]
        c["day_id"] = a["day_id"]
        errors = module.audit([a,b,c])["errors"]
        self.assertTrue(any("device_id" in x for x in errors))
        self.assertTrue(any("day_id" in x for x in errors))

    def test_identical_raw_payload_cross_partition_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        c["raw_sha256"] = a["raw_sha256"].upper()
        errors = module.audit([a,b,c])["errors"]
        self.assertTrue(any("raw_sha256 leaks" in x for x in errors))

    def test_same_raw_payload_within_partition_allowed_by_split_gate(self):
        a,b,c,d = row(1,"train"),row(2,"train"),row(3,"validation"),row(4,"holdout")
        b["raw_sha256"] = a["raw_sha256"]
        self.assertEqual(module.audit([a,b,c,d])["status"],"PASS_SPLIT_INTEGRITY_ONLY")

    def test_missing_holdout_rejected(self):
        self.assertEqual(module.audit([row(1,"train"),row(2,"validation")])["status"],"FAIL")

    def test_duplicate_and_invalid_hash_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        c["sample_id"] = a["sample_id"]
        c["raw_sha256"] = "xyz"
        self.assertGreaterEqual(len(module.audit([a,b,c])["errors"]),2)

    def test_null_and_missing_fields_rejected_without_exception(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["device_id"] = None
        c.pop("raw_sha256")
        errors = module.audit([a,b,c])["errors"]
        self.assertTrue(any("missing device_id" in e for e in errors))
        self.assertTrue(any("missing raw_sha256" in e for e in errors))

    def test_whitespace_cannot_hide_group_leakage(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["device_id"] = "  " + a["device_id"] + " "
        self.assertTrue(any("device_id" in e and "leaks" in e
                            for e in module.audit([a,b,c])["errors"]))

    def test_whitespace_cannot_hide_duplicate_sample(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["sample_id"] = " " + a["sample_id"] + " "
        self.assertTrue(any("duplicate sample_id" in e
                            for e in module.audit([a,b,c])["errors"]))

    def test_nfkc_device_alias_cannot_cross_partitions(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["device_id"] = "ｄｅｖ１"
        self.assertTrue(any("device_id" in e and "leaks" in e
                            for e in module.audit([a,b,c])["errors"]))

    def test_nfkc_sample_alias_is_duplicate(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["sample_id"] = "１"
        self.assertTrue(any("duplicate sample_id" in e
                            for e in module.audit([a,b,c])["errors"]))

    def test_nfkc_whitespace_only_identifier_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        b["device_id"] = "\u3000"
        errors = module.audit([a,b,c])["errors"]
        self.assertEqual(module.audit([a,b,c])["status"], "FAIL")
        self.assertTrue(any("missing device_id" in e or "invalid normalized identifiers device_id" in e for e in errors))

    def test_invisible_format_character_identifier_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        c["session_id"] = "session3\u200b"
        errors = module.audit([a,b,c])["errors"]
        self.assertTrue(any("invalid normalized identifiers session_id" in e for e in errors))

    def test_header_whitespace_alias_rejected(self):
        # Verify the actual CSV parser error, not the numeric SystemExit code.
        with tempfile.TemporaryDirectory() as tmp:
            manifest = pathlib.Path(tmp) / "manifest.csv"
            manifest.write_text(
                "sample_id,device_id,session_id,day_id,partition,raw_sha256, sample_id \n"
                "1,dev1,s1,d1,train," + "a" * 64 + ",shadow\n",
                encoding="utf-8",
            )
            with patch("sys.argv", ["g2_holdout_split_preflight.py", str(manifest)]):
                with self.assertRaisesRegex(SystemExit, "duplicate CSV columns"):
                    module.main()


    def test_csv_missing_cell_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = pathlib.Path(tmp) / "manifest.csv"
            manifest.write_text(
                "sample_id,device_id,session_id,day_id,partition,raw_sha256\n"
                "1,dev1,s1,d1,train," + "a" * 64 + "\n"
                "2,dev2,s2,d2,validation\n"
                "3,dev3,s3,d3,holdout," + "c" * 64 + "\n",
                encoding="utf-8",
            )
            with patch("sys.argv", ["g2_holdout_split_preflight.py", str(manifest)]):
                with self.assertRaises(SystemExit) as raised:
                    module.main()
            self.assertEqual(raised.exception.code, 1)

    def test_csv_extra_cell_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = pathlib.Path(tmp) / "manifest.csv"
            manifest.write_text(
                "sample_id,device_id,session_id,day_id,partition,raw_sha256\n"
                "1,dev1,s1,d1,train," + "a" * 64 + ",unexpected\n",
                encoding="utf-8",
            )
            with patch("sys.argv", ["g2_holdout_split_preflight.py", str(manifest)]):
                with self.assertRaisesRegex(SystemExit, "CSV rows contain extra fields"):
                    module.main()


    def test_unterminated_quoted_csv_field_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = pathlib.Path(tmp) / "manifest.csv"
            manifest.write_text(
                "sample_id,device_id,session_id,day_id,partition,raw_sha256\n"
                '1,"unterminated,s1,d1,train,' + "a" * 64 + "\n",
                encoding="utf-8",
            )
            with patch("sys.argv", ["g2_holdout_split_preflight.py", str(manifest)]):
                with self.assertRaisesRegex(SystemExit, "malformed CSV"):
                    module.main()

if __name__ == "__main__":
    unittest.main()
