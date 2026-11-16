"""Fail-closed synthetic and pinned-source regressions for day-level sensitivity."""
import json
import math
import subprocess
import sys
import tempfile
import unittest
from datetime import date, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_forward_day_robustness import analyze, compact


def fixture():
    days = [(date(2021, 7, 1) + timedelta(days=i)).isoformat() for i in range(9)]
    tests = days[5:]
    out = {"schema": "hazrob.g2.mq4-forward-calendar-day.v1", "days": days,
           "common_test_days": tests, "gaps": [0, 1, 2], "min_train_days": 3,
           "raw_member_sha256": "a"*64,
           "scientific_state": {k: False for k in
                                ("g2_qualified", "g3_tank_qualified", "physical_independence_verified",
                                 "reference_calibration_verified", "blinded_holdout_verified")},
           "channels": {}}
    for c in range(1, 4):
        channel = {}
        for gap in (0, 1, 2):
            folds = {}
            ms_total = bs_total = 0.
            n_total = worse = 0
            for j, day in enumerate(tests):
                train = [d for d in days if (date.fromisoformat(day)-date.fromisoformat(d)).days > gap]
                n = j+3
                m = 1.5 + .2*c + .1*gap + (2 if j == 1 else 0)
                b = 2.5 + .1*gap
                folds[day] = {"train_days": train, "train_rows": 20,
                              "test_rows": n, "rmse_model_ppb": m,
                              "rmse_train_mean_baseline_ppb": b}
                ms_total += n*m*m
                bs_total += n*b*b
                n_total += n
                worse += m > b
            channel[str(gap)] = {
                "folds": folds, "pooled_rmse_model_ppb": math.sqrt(ms_total/n_total),
                "pooled_rmse_baseline_ppb": math.sqrt(bs_total/n_total),
                "pooled_skill_vs_train_mean": 1-ms_total/bs_total,
                "days_worse_than_training_mean": worse, "total_test_rows": n_total}
        out["channels"][f"MQ4_{c}"] = channel
    return out


class DayRobustnessTests(unittest.TestCase):
    def test_valid_jackknife_and_replay(self):
        a = analyze(fixture())
        b = analyze(fixture())
        self.assertEqual(a, b)
        self.assertEqual(a["calendar_day_count"], 4)
        x = a["channels"]["MQ4_1"]["0"]
        self.assertEqual(len(x["leave_one_test_day_out"]), 4)
        self.assertLessEqual(x["leave_one_test_day_out_skill_min"], x["pooled_skill"])
        self.assertGreaterEqual(x["leave_one_test_day_out_skill_max"], x["pooled_skill"])
        self.assertFalse(a["scientific_state"]["g2_qualified"])

    def test_compact_omits_fold_details_only(self):
        a = analyze(fixture())
        c = compact(a)
        self.assertNotIn("leave_one_test_day_out", c["channels"]["MQ4_1"]["0"])
        self.assertEqual(c["channels"]["MQ4_1"]["0"]["pooled_skill"],
                         a["channels"]["MQ4_1"]["0"]["pooled_skill"])

    def test_reject_tampered_pooled_rmse(self):
        f = fixture()
        f["channels"]["MQ4_1"]["1"]["pooled_rmse_model_ppb"] += .2
        with self.assertRaisesRegex(ValueError, "POOLED_RECONCILIATION"):
            analyze(f)

    def test_reject_tampered_skill(self):
        f = fixture()
        f["channels"]["MQ4_2"]["0"]["pooled_skill_vs_train_mean"] = 1
        with self.assertRaisesRegex(ValueError, "SKILL_RECONCILIATION"):
            analyze(f)

    def test_reject_missing_fold(self):
        f = fixture()
        f["channels"]["MQ4_3"]["2"]["folds"].pop("2021-07-09")
        with self.assertRaisesRegex(ValueError, "FOLD_DAYS"):
            analyze(f)

    def test_reject_future_train_and_unexpected_train_membership(self):
        f = fixture()
        f["channels"]["MQ4_1"]["0"]["folds"]["2021-07-06"]["train_days"].append("2021-07-07")
        with self.assertRaisesRegex(ValueError, "TRAIN_MEMBERSHIP"):
            analyze(f)
        f = fixture()
        f["channels"]["MQ4_1"]["2"]["folds"]["2021-07-06"]["train_days"].append("2021-07-05")
        with self.assertRaisesRegex(ValueError, "TRAIN_MEMBERSHIP"):
            analyze(f)

    def test_reject_population_shift_and_nonfinite(self):
        f = fixture()
        f["channels"]["MQ4_1"]["2"]["folds"]["2021-07-07"]["test_rows"] += 1
        with self.assertRaisesRegex(ValueError, "POOLED_RECONCILIATION"):
            analyze(f)
        f = fixture()
        f["channels"]["MQ4_1"]["2"]["folds"]["2021-07-07"]["rmse_model_ppb"] = float("nan")
        with self.assertRaisesRegex(ValueError, "FOLD_RMSE"):
            analyze(f)

    def test_no_scientific_promotion(self):
        f = fixture()
        f["scientific_state"]["reference_calibration_verified"] = True
        with self.assertRaisesRegex(ValueError, "PROMOTION"):
            analyze(f)

    def test_cli_synthetic_reproducibility(self):
        with tempfile.TemporaryDirectory() as td:
            source = Path(td)/"input.json"
            out = Path(td)/"output.json"
            source.write_text(json.dumps(fixture()))
            subprocess.run([sys.executable, str(Path(__file__).with_name("g2_mq4_forward_day_robustness.py")),
                            "--full-forward-report", str(source), "--output", str(out), "--compact"],
                           check=True, capture_output=True)
            self.assertEqual(json.loads(out.read_text()), compact(analyze(fixture())))

    def test_pinned_source_remains_observational(self):
        path = Path(__file__).with_name("G2_MQ4_FORWARD_DAY_ROBUSTNESS_2026-10-31.json")
        r = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(r["raw_member_sha256"],
                         "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d")
        self.assertEqual(r["calendar_day_count"], 6)
        self.assertEqual(r["channels"]["MQ4_1"]["0"]["test_rows"], 7889)
        self.assertAlmostEqual(r["channels"]["MQ4_1"]["0"]["pooled_skill"], .2739975382192923)
        self.assertGreater(r["channels"]["MQ4_1"]["0"]["negative_skill_days"], 0)
        self.assertFalse(r["scientific_state"]["g2_qualified"])
        self.assertFalse(r["scientific_state"]["physical_independence_verified"])


if __name__ == "__main__":
    unittest.main()
