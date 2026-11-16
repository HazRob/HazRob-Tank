"""Deterministic fail-closed tests for MQ4 hypothetical paired-error bounds."""
import hashlib
import itertools
import json
import unittest
from pathlib import Path

from g2_mq4_paired_discordance_identifiability import derive, disagreement_bounds
from g2_mq4_pairwise_frechet_bounds import SOURCE, EVIDENCE

ROOT = Path(__file__).resolve().parent
PIN = ROOT / "G2_MQ4_PAIRED_DISCORDANCE_IDENTIFIABILITY_2026-11-01.json"


class PairedDiscordanceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SOURCE.read_bytes()
        cls.evidence = EVIDENCE.read_bytes()
        cls.result = derive(cls.source, cls.evidence)

    def test_sha(self):
        self.assertEqual(hashlib.sha256(self.source).hexdigest(),
                         "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea")

    def test_pinned_output(self):
        self.assertEqual(self.result, json.loads(PIN.read_text(encoding="utf-8")))

    def test_ten_pairs(self):
        self.assertEqual(len(self.result["pairs"]), 10)
        self.assertEqual(len({tuple(p["models"]) for p in self.result["pairs"]}), 10)

    def test_exhaustive_sharpness_three_rows(self):
        for a in range(4):
            for b in range(4):
                sets_a = [set(c) for c in itertools.combinations(range(3), a)]
                sets_b = [set(c) for c in itertools.combinations(range(3), b)]
                values = [len(x ^ y) for x in sets_a for y in sets_b]
                self.assertEqual(disagreement_bounds(a, b, 3), [min(values), max(values)])

    def test_parity_and_nonnegative(self):
        for row in self.result["pairs"]:
            lo, hi = row["total_disagreement_range"]
            self.assertGreaterEqual(lo, 0)
            self.assertLessEqual(lo, hi)
            self.assertEqual((hi - lo) % 2, 0)
            self.assertEqual(row["b_minus_a_total_errors"],
                             row["a_correct_b_wrong_range"][0] - row["a_wrong_b_correct_range"][0])

    def test_baseline_disagreement_fixed(self):
        for row in self.result["pairs"][:4]:
            self.assertEqual(*row["total_disagreement_range"])

    def test_bounds_are_not_observed(self):
        self.assertFalse(self.result["common_row_identity_verified"])
        self.assertFalse(self.result["pairwise_prediction_rows_available"])
        self.assertFalse(self.result["mcnemar_test_performed"])
        self.assertFalse(self.result["threshold_is_safety_limit"])
        self.assertFalse(self.result["g2_qualified"])
        self.assertFalse(self.result["g3_qualified"])

    def test_invalid_boolean(self):
        with self.assertRaisesRegex(ValueError, "BAD_INTEGER_COUNTS"):
            disagreement_bounds(True, 1, 3)

    def test_invalid_negative(self):
        with self.assertRaisesRegex(ValueError, "BAD_ERROR_COUNTS"):
            disagreement_bounds(-1, 1, 3)

    def test_invalid_excess(self):
        with self.assertRaisesRegex(ValueError, "BAD_ERROR_COUNTS"):
            disagreement_bounds(4, 1, 3)

    def test_invalid_zero_population(self):
        with self.assertRaisesRegex(ValueError, "BAD_ERROR_COUNTS"):
            disagreement_bounds(0, 0, 0)

    def test_reject_source_mutation(self):
        with self.assertRaisesRegex(ValueError, "SOURCE_SHA_MISMATCH"):
            derive(self.source + b" ", self.evidence)

    def test_reject_duplicate_evidence_key(self):
        attack = b'{"schema":"hazrob.g2.independent-evidence.v1","records":[1],"records":[]}'
        with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
            derive(self.source, attack)

    def test_reject_nonfinite_evidence(self):
        attack = b'{"schema":"hazrob.g2.independent-evidence.v1","records":[],"x":1e9999}'
        with self.assertRaisesRegex(ValueError, "NONFINITE_JSON_NUMBER"):
            derive(self.source, attack)

    def test_reject_nonempty_evidence(self):
        attack = b'{"schema":"hazrob.g2.independent-evidence.v1","records":[{"unverified":true}]}'
        with self.assertRaisesRegex(ValueError, "UNVERIFIED_INDEPENDENT_EVIDENCE"):
            derive(self.source, attack)

    def test_reject_nonobject_evidence(self):
        with self.assertRaisesRegex(ValueError, "INVALID_JSON_OBJECT"):
            derive(self.source, b"[]")


if __name__ == "__main__":
    unittest.main()
