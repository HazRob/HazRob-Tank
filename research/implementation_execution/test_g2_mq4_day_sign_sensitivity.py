"""Deterministic fail-closed checks of observational MQ4 day-sign diagnostic."""
import copy
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_day_sign_sensitivity import SOURCE, evaluate, exact_positive_sign_tail


class DaySignSensitivityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = json.loads(SOURCE.read_text(encoding="utf-8"))

    def test_exact_sign_tail_5_of_6(self):
        self.assertEqual(exact_positive_sign_tail(5, 1), Fraction(7, 64))

    def test_exact_sign_tail_4_of_6(self):
        self.assertEqual(exact_positive_sign_tail(4, 2), Fraction(22, 64))

    def test_extreme_and_all_fail(self):
        self.assertEqual(exact_positive_sign_tail(6, 0), Fraction(1, 64))
        self.assertEqual(exact_positive_sign_tail(0, 6), Fraction(1, 1))

    def test_invalid_count_types_and_ranges(self):
        for a, b in [(True, 2), (1.5, 3), (-1, 7), (0, 0), (1, "2")]:
            with self.subTest(a=a, b=b), self.assertRaises(ValueError):
                exact_positive_sign_tail(a, b)

    def test_observational_summary_all_nine_not_significant(self):
        out = evaluate(self.source)
        self.assertEqual(len(out["cases"]), 9)
        self.assertTrue(all(not x["conditional_nominal_5pct_reject"] for x in out["cases"]))
        self.assertTrue(all(not x["conditional_80_claim_bonferroni_reject"] for x in out["cases"]))
        self.assertTrue(all(x["mean_day_skill_negative"] for x in out["cases"]))

    def test_pinned_result_and_replay(self):
        pinned = json.loads(SOURCE.with_name("G2_MQ4_DAY_SIGN_SENSITIVITY_2026-10-31.json").read_text(encoding="utf-8"))
        self.assertEqual(evaluate(self.source), pinned)
        self.assertEqual(evaluate(self.source), evaluate(self.source))

    def test_no_promotion(self):
        out = evaluate(self.source)
        self.assertFalse(out["g2_qualified"])
        self.assertFalse(out["g3_qualified"])
        self.assertEqual(out["independent_physical_exposure_records"], 0)
        self.assertTrue(all(not x["g2_qualified"] for x in out["cases"]))

    def test_mutated_source_sha_rejected(self):
        s = copy.deepcopy(self.source)
        s["raw_member_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "LINEAGE"):
            evaluate(s)

    def test_mutated_scientific_flag_rejected(self):
        s = copy.deepcopy(self.source)
        s["scientific_state"]["g2_qualified"] = True
        with self.assertRaisesRegex(ValueError, "PROMOTION"):
            evaluate(s)

    def test_missing_or_forged_day_count_rejected(self):
        for value in (5, True, 7):
            s = copy.deepcopy(self.source)
            s["channels"]["MQ4_1"]["0"]["positive_skill_days"] = value
            with self.subTest(value=value), self.assertRaises(ValueError):
                evaluate(s)

    def test_missing_channel_and_gap_rejected(self):
        for field in ("channel", "gap"):
            s = copy.deepcopy(self.source)
            if field == "channel":
                del s["channels"]["MQ4_1"]
            else:
                del s["channels"]["MQ4_1"]["1"]
            with self.subTest(field=field), self.assertRaises(ValueError):
                evaluate(s)

    def test_invalid_day_skill_rejected(self):
        s = copy.deepcopy(self.source)
        s["channels"]["MQ4_1"]["0"]["mean_defined_day_skill"] = float("nan")
        with self.assertRaisesRegex(ValueError, "SKILL"):
            evaluate(s)


if __name__ == "__main__":
    unittest.main()
