"""Synthetic G2 controls for blind target diversity; never physical evidence."""
import hashlib
import tempfile
import unittest

from g2_evidence_audit import audit_record
from g2_cross_artifact_gate import audit_cross_artifact
from test_g2_evidence_bundle_gate import synthetic_bundle


def change_csv_and_rehash(record, base, before, after):
    path = base / "group_manifest.csv"
    old = path.read_text(encoding="utf-8")
    if before not in old:
        raise AssertionError("synthetic fixture anchor not found")
    updated = old.replace(before, after)
    path.write_text(updated, encoding="utf-8")
    record["evidence_files"]["group_manifest.csv"] = hashlib.sha256(
        updated.encode("utf-8")).hexdigest()


class G2BlindTargetDiversityTests(unittest.TestCase):
    def test_two_distinct_blind_classes_are_not_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertNotIn("BLIND_TEST_SINGLE_REFERENCE_CLASS",
                             audit_cross_artifact(record, temp))
            self.assertEqual(audit_record(record, temp)["status"],
                             "G2_PASS_CANDIDATE_REQUIRES_REVIEW")

    def test_one_blind_class_fails_even_with_two_independent_groups(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,GAS_A,d2,s2")
            result = audit_record(record, temp)
            self.assertIn("BLIND_TEST_SINGLE_REFERENCE_CLASS", result["issues"])
            self.assertEqual(result["status"], "G2_BLOCKED")

    def test_training_and_calibration_classes_do_not_count_as_blind(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,GAS_A,d2,s2")
            # The original training/calibration labels still include A and B.
            self.assertIn("BLIND_TEST_SINGLE_REFERENCE_CLASS",
                          audit_cross_artifact(record, temp))

    def test_missing_blind_truth_is_not_treated_as_another_class(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,,d2,s2")
            result = audit_record(record, temp)
            self.assertIn("BLIND_TEST_TARGET_LABELS_UNAVAILABLE", result["issues"])
            self.assertEqual(result["status"], "G2_BLOCKED")

    def test_fullwidth_alias_cannot_fake_second_blind_class(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,ＧＡＳ＿Ａ,d2,s2")
            issues = audit_cross_artifact(record, temp)
            self.assertIn("BLIND_TEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertIn("BLIND_TEST_SINGLE_REFERENCE_CLASS", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_distinct_ascii_blind_labels_remain_distinct(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            issues = audit_cross_artifact(record, temp)
            self.assertNotIn("BLIND_TEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertNotIn("BLIND_TEST_SINGLE_REFERENCE_CLASS", issues)

    def test_non_accuracy_metric_does_not_inherit_alias_discrimination_gate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["metric_name"] = "rmse"
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,ＧＡＳ＿Ａ,d2,s2")
            issues = audit_cross_artifact(record, temp)
            self.assertNotIn("BLIND_TEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertIn("METRIC_FAMILY_NOT_RECOMPUTABLE", issues)

    def test_non_accuracy_metrics_do_not_inherit_accuracy_class_gate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["metric_name"] = "rmse"
            change_csv_and_rehash(record, base,
                                  "te2,d2-s2,GAS_B,d2,s2",
                                  "te2,d2-s2,GAS_A,d2,s2")
            issues = audit_cross_artifact(record, temp)
            self.assertNotIn("BLIND_TEST_SINGLE_REFERENCE_CLASS", issues)
            self.assertIn("METRIC_FAMILY_NOT_RECOMPUTABLE", issues)


    def test_training_to_blind_compatibility_alias_blocks_candidate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            # Blind diversity remains two distinct classes; the prior
            # blind-only alias check did not see the training spelling.
            change_csv_and_rehash(record, base,
                                  "tr,d1-s1,GAS_A,d1,s1",
                                  "tr,d1-s1,ＧＡＳ＿Ａ,d1,s1")
            issues = audit_cross_artifact(record, temp)
            self.assertIn("GROUP_MANIFEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertNotIn("BLIND_TEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertNotIn("BLIND_TEST_SINGLE_REFERENCE_CLASS", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_calibration_to_blind_compatibility_alias_blocks_candidate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            change_csv_and_rehash(record, base,
                                  "ca,d1-s2,GAS_B,d1,s2",
                                  "ca,d1-s2,ＧＡＳ＿Ｂ,d1,s2")
            issues = audit_cross_artifact(record, temp)
            self.assertIn("GROUP_MANIFEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertNotIn("BLIND_TEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertEqual(audit_record(record, temp)["status"], "G2_BLOCKED")

    def test_consistent_reference_labels_have_no_manifest_alias_issue(self):
        with tempfile.TemporaryDirectory() as temp:
            record, _ = synthetic_bundle(temp)
            self.assertNotIn("GROUP_MANIFEST_REFERENCE_LABEL_NFKC_ALIAS",
                             audit_cross_artifact(record, temp))

    def test_non_accuracy_metric_does_not_gain_new_classification_gate(self):
        with tempfile.TemporaryDirectory() as temp:
            record, base = synthetic_bundle(temp)
            record["metric_name"] = "rmse"
            change_csv_and_rehash(record, base,
                                  "tr,d1-s1,GAS_A,d1,s1",
                                  "tr,d1-s1,ＧＡＳ＿Ａ,d1,s1")
            issues = audit_cross_artifact(record, temp)
            self.assertNotIn("GROUP_MANIFEST_REFERENCE_LABEL_NFKC_ALIAS", issues)
            self.assertIn("METRIC_FAMILY_NOT_RECOMPUTABLE", issues)


if __name__ == "__main__":
    unittest.main()
