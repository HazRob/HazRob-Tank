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
                session_id=f"session{i}", partition=p, raw_sha256=SHA)

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

    def test_missing_holdout_rejected(self):
        self.assertEqual(module.audit([row(1,"train"),row(2,"validation")])["status"],"FAIL")

    def test_duplicate_and_invalid_hash_rejected(self):
        a,b,c = row(1,"train"),row(2,"validation"),row(3,"holdout")
        c["sample_id"] = a["sample_id"]
        c["raw_sha256"] = "xyz"
        self.assertGreaterEqual(len(module.audit([a,b,c])["errors"]),2)

if __name__ == "__main__":
    unittest.main()
