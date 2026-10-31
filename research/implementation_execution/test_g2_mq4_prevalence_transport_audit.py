"""Deterministic fail-closed checks for source-only prevalence stress audit."""
import hashlib
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_prevalence_transport_audit import SOURCE, EVIDENCE, SOURCE_SHA256, project, fraction_pair


def value(pair):
    return Fraction(*pair) if pair is not None else None


class PrevalenceAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = SOURCE.read_bytes()
        cls.evidence = EVIDENCE.read_bytes()
        cls.result = project(cls.raw, cls.evidence)

    def test_original_snapshot_sha(self):
        self.assertEqual(hashlib.sha256(self.raw).hexdigest(), SOURCE_SHA256)

    def test_pinned_replay(self):
        pinned = json.loads(SOURCE.with_name("G2_MQ4_PREVALENCE_TRANSPORT_2026-11-01.json").read_text())
        self.assertEqual(self.result, pinned)

    def test_model_count(self):
        self.assertEqual(len(self.result["models"]), 5)

    def test_model_source_denominators(self):
        for m in self.result["models"]:
            tp, fn, fp, tn = m["source_counts_tp_fn_fp_tn"]
            self.assertEqual((tp + fn, fp + tn), (2346, 11840))

    def test_prevalence_cases(self):
        for m in self.result["models"]:
            self.assertEqual([x["prevalence"] for x in m["hypothetical_prevalence_cases"]],
                             [[1, 100], [1, 20], [1, 10], [1, 2]])

    def test_ridge_precision_at_one_percent_is_low(self):
        case = self.result["models"][3]["hypothetical_prevalence_cases"][0]
        self.assertLess(value(case["precision"]), Fraction(8, 100))
        self.assertGreater(value(case["precision"]), Fraction(7, 100))

    def test_ridge_false_alarms_per_thousand(self):
        case = self.result["models"][3]["hypothetical_prevalence_cases"][0]
        self.assertEqual(value(case["expected_false_alarms_per_1000"]),
                         Fraction(787, 11840) * Fraction(99, 100) * 1000)

    def test_precision_monotonic_under_fixed_rates(self):
        for m in self.result["models"][1:]:
            ppvs = [value(x["precision"]) for x in m["hypothetical_prevalence_cases"]]
            self.assertEqual(ppvs, sorted(ppvs))

    def test_baseline_has_no_positive_predictions(self):
        for case in self.result["models"][0]["hypothetical_prevalence_cases"]:
            self.assertIsNone(case["precision"])
            self.assertEqual(value(case["expected_alarms_per_1000"]), 0)

    def test_exact_reduced_fractions(self):
        for m in self.result["models"]:
            for case in m["hypothetical_prevalence_cases"]:
                for k in ("precision", "negative_predictive_value",
                          "expected_alarms_per_1000", "expected_false_alarms_per_1000"):
                    pair = case[k]
                    if pair is not None:
                        self.assertEqual(fraction_pair(value(pair)), pair)

    def test_no_safety_threshold_claim(self):
        self.assertFalse(self.result["threshold_is_safety_limit"])

    def test_no_promotion(self):
        self.assertFalse(self.result["g2_qualified"])
        self.assertFalse(self.result["g3_qualified"])
        self.assertEqual(self.result["independent_physical_evidence_records"], 0)

    def test_no_prospective_claim(self):
        self.assertFalse(self.result["prospective_or_tank_validation"])
        self.assertTrue(self.result["prevalence_scenarios_are_hypothetical"])
        self.assertTrue(self.result["conditional_on_fixed_sensitivity_specificity"])

    def test_tampered_snapshot_fails(self):
        with self.assertRaisesRegex(ValueError, "SHA_MISMATCH"):
            project(self.raw + b" ", self.evidence)

    def test_added_unverified_evidence_fails(self):
        ev = json.loads(self.evidence)
        ev["records"].append({"unverified": True})
        with self.assertRaisesRegex(ValueError, "UNVERIFIED_INDEPENDENT_EVIDENCE"):
            project(self.raw, json.dumps(ev).encode())

    def test_bad_evidence_schema_fails(self):
        ev = json.loads(self.evidence)
        ev["schema"] = "other"
        with self.assertRaisesRegex(ValueError, "UNVERIFIED_INDEPENDENT_EVIDENCE"):
            project(self.raw, json.dumps(ev).encode())


if __name__ == "__main__":
    unittest.main()
