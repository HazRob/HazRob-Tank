"""Regression tests for conservative G2 physical holdout partition audit."""
import importlib.util
import pathlib
import unittest

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

if __name__ == "__main__":
    unittest.main()
