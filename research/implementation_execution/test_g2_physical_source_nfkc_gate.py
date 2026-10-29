"""Synthetic regression controls for G2 physical-source NFKC identity.

No generated fixture is evidence of gas concentration, calibration or G2.
"""
import csv
import json
import tempfile
import unittest
from pathlib import Path

from g2_cross_artifact_gate import (
    _canonical_physical_source_value, audit_cross_artifact,
)


class PhysicalSourceIdentityTests(unittest.TestCase):
    def test_canonical_multilingual_source_identifiers_are_preserved(self):
        for value in ("device-01", "جلسة-١", "café", "site 2", "d1"):
            with self.subTest(value=value):
                self.assertTrue(_canonical_physical_source_value(value))

    def test_compatibility_and_invisible_aliases_fail_closed(self):
        for value in ("ｄ1", "①", "ﬃ", "d\u200b1", "d\u00a01",
                      "d1 ", " d1", "e\u0301"):
            with self.subTest(value=value):
                self.assertFalse(_canonical_physical_source_value(value))

    def _audit_synthetic_group_manifest(self, test_device, train_exposure="e1",
                                        test_exposure="e3", prediction_exposure=None):
        with tempfile.TemporaryDirectory() as temp:
            base = (Path(temp) / "research" / "g2" / "evidence" /
                    "01.1" / "synthetic" / "run1")
            base.mkdir(parents=True)
            docs = (
                "raw_manifest.json", "source_qualification.json",
                "signal_lineage.json", "split_manifest.json",
                "protocol_locked.json", "calibration_manifest.json",
                "metrics_summary.json", "uncertainty_grouped.json",
                "review_decision.json", "ci_run.json",
            )
            for name in docs:
                payload = {}
                if name == "protocol_locked.json":
                    payload = {"baseline_model": {
                        "algorithm_id": "majority_train",
                        "prediction_column": "baseline_predicted_reference",
                        "fitted_group_ids": ["train"],
                        "fit_policy": "TRAIN_ONLY",
                        "selected_on_test": False,
                    }}
                (base / name).write_text(json.dumps(payload), encoding="utf-8")
            csv_files = {
                "group_manifest.csv": (
                    ["exposure_id", "group_id", "device_id", "session_id",
                     "gas_reference_id"],
                    [
                        [train_exposure, "train", "d1", "s1", "gasA"],
                        ["e2", "cal", "d2", "s2", "gasA"],
                        [test_exposure, "test", test_device, "s1", "gasA"],
                    ],
                ),
                "predictions_blind.csv": (
                    ["exposure_id", "group_id"],
                    [[prediction_exposure if prediction_exposure is not None
                      else test_exposure, "test"]],
                ),
                "metrics_per_group.csv": (
                    ["group_id", "sample_count"],
                    [["test", "1"]],
                ),
            }
            for name, (header, rows) in csv_files.items():
                with (base / name).open("w", encoding="utf-8", newline="") as f:
                    writer = csv.writer(f)
                    writer.writerow(header)
                    writer.writerows(rows)
            record = {
                "id": "01.1", "dataset_id": "synthetic", "run_id": "run1",
                "grouping_key": "device_session_id",
                "grouping_source_field": "device_id,session_id",
                "train_group_ids": ["train"],
                "calibration_group_ids": ["cal"],
                "test_group_ids": ["test"],
                "metric_name": "rmse", "metric_value": 1.0,
                "baseline_metric_value": 1.5,
            }
            return audit_cross_artifact(record, repo_root=temp)

    def test_fullwidth_source_alias_cannot_fake_blind_device(self):
        # A different canonical group_id does not prove physical independence:
        # fullwidth 'ｄ1' is an NFKC alias of training device 'd1'.
        issues = self._audit_synthetic_group_manifest("ｄ1")
        self.assertIn("NONCANONICAL_PHYSICAL_SOURCE_ID", issues)

    def test_distinct_canonical_device_does_not_trigger_new_blocker(self):
        issues = self._audit_synthetic_group_manifest("d3")
        self.assertNotIn("NONCANONICAL_PHYSICAL_SOURCE_ID", issues)

    def test_fullwidth_exposure_alias_in_group_manifest_fails_closed(self):
        # NFKC(ｅ1) == e1, but bytewise equality would miss this duplicate.
        issues = self._audit_synthetic_group_manifest(
            "d3", test_exposure="ｅ1")
        self.assertIn("NONCANONICAL_EXPOSURE_ID:GROUP_MANIFEST", issues)

    def test_fullwidth_exposure_alias_in_predictions_fails_closed(self):
        issues = self._audit_synthetic_group_manifest(
            "d3", prediction_exposure="ｅ3")
        self.assertIn("NONCANONICAL_EXPOSURE_ID:PREDICTIONS_BLIND", issues)

    def test_multilingual_canonical_exposure_ids_remain_supported(self):
        issues = self._audit_synthetic_group_manifest(
            "d3", train_exposure="تعرض-١", test_exposure="café-2")
        self.assertFalse(any(issue.startswith("NONCANONICAL_EXPOSURE_ID:")
                             for issue in issues))

    def test_empty_exposure_id_is_rejected_at_identity_boundary(self):
        issues = self._audit_synthetic_group_manifest(
            "d3", test_exposure="")
        self.assertIn("NONCANONICAL_EXPOSURE_ID:GROUP_MANIFEST", issues)


if __name__ == "__main__":
    unittest.main()
