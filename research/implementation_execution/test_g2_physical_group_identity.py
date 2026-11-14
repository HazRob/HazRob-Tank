"""Research-only negative controls for canonical physical holdout group IDs.

No synthetic fixture in this module constitutes independent measured evidence.
"""
import unittest

from g2_evidence_audit import audit_record


class PhysicalGroupIdentityTests(unittest.TestCase):
    @staticmethod
    def record():
        return {
            "id": "01.1",
            "train_group_ids": ["device A / session 1"],
            "calibration_group_ids": ["device A / session 2"],
            "test_group_ids": ["device B / session 1", "device B / session 2"],
        }

    def test_normal_nfc_multilingual_and_interior_ascii_space_allowed(self):
        row = self.record()
        row["train_group_ids"] = ["جهاز ١ / جلسة ١"]
        self.assertNotIn("NONCANONICAL_PHYSICAL_GROUP_ID",
                         audit_record(row)["issues"])

    def test_visually_ambiguous_group_ids_fail_closed(self):
        variants = (
            "device A / session 1 ",
            " device A / session 1",
            "device A / session 1\u200b",
            "device A / session 1\u00a0",
            "device A / session 1\t",
            "cafe\u0301",
        )
        for bad in variants:
            with self.subTest(bad=repr(bad)):
                row = self.record()
                row["train_group_ids"] = [bad]
                self.assertIn("NONCANONICAL_PHYSICAL_GROUP_ID",
                              audit_record(row)["issues"])

    def test_nfkc_compatibility_aliases_fail_closed_in_all_partitions(self):
        # Fullwidth ASCII, circled digits, ligatures and superscript digits
        # can be visually/semantically confused with other physical IDs.
        # These are software-only negative controls, not gas measurements.
        aliases = ("device Ａ", "session ①", "session ²", "oﬃce 1")
        for key in ("train_group_ids", "calibration_group_ids", "test_group_ids"):
            for alias in aliases:
                with self.subTest(partition=key, alias=alias):
                    row = self.record()
                    row[key] = [alias]
                    self.assertIn("NONCANONICAL_PHYSICAL_GROUP_ID",
                                  audit_record(row)["issues"])

    def test_canonical_ascii_and_multilingual_physical_ids_remain_valid(self):
        row = self.record()
        row["train_group_ids"] = ["جهاز ١ / جلسة ١"]
        row["calibration_group_ids"] = ["café / session 2"]
        row["test_group_ids"] = ["device B / session 1", "device B / session 2"]
        self.assertNotIn("NONCANONICAL_PHYSICAL_GROUP_ID",
                         audit_record(row)["issues"])

    def test_split_alias_does_not_masquerade_as_independent_test_group(self):
        row = self.record()
        row["test_group_ids"] = ["device B / session 1",
                                 "device B / session 1\u200b"]
        issues = audit_record(row)["issues"]
        self.assertIn("NONCANONICAL_PHYSICAL_GROUP_ID", issues)

    def test_non_string_group_id_fails_closed_without_crashing(self):
        row = self.record()
        row["calibration_group_ids"] = [123]
        issues = audit_record(row)["issues"]
        self.assertIn("NONCANONICAL_PHYSICAL_GROUP_ID", issues)
        self.assertIn("INVALID_OR_DUPLICATE_GROUP_IDS", issues)


if __name__ == "__main__":
    unittest.main()
