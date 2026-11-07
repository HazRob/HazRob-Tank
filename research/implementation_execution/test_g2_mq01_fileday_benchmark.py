"""Synthetic algorithm controls only; not physical G2 evidence."""
import tempfile
import unittest
import zipfile
from pathlib import Path
from g2_mq01_fileday_benchmark import (ALL_MQ, TANK_MQ, evaluate, load_source,
    permutation_negative_control, paired_channel_comparison,
    prespecified_channel_sets, ablation_comparison)

def synthetic_records():
    return [{"file": f"c{c} D{day}.csv", "label": f"c{c}", "day": day,
             "rows": 100, "medians": {mq: 100.0 * c + 0.01 * day for mq in ALL_MQ}}
            for c in range(10) for day in range(1, 6)]

class SourceDayBenchmarkTests(unittest.TestCase):
    def test_each_day_holdout_and_no_file_overlap(self):
        r = evaluate(synthetic_records(), TANK_MQ)
        self.assertEqual(r["n_test_files"], 50)
        self.assertEqual(r["correct"], 50)
        self.assertEqual([x["total"] for x in r["per_day"]], [10] * 5)

    def test_all_source_channels(self):
        r = evaluate(synthetic_records(), ALL_MQ)
        self.assertEqual(r["correct"], 50)

    def test_prespecified_channel_sets_are_unique_and_complete(self):
        configs = prespecified_channel_sets()
        self.assertEqual(len(configs), 12)
        self.assertEqual(len({x["id"] for x in configs}), 12)
        self.assertEqual(configs[0]["channels"], TANK_MQ)
        self.assertEqual(configs[3]["channels"], ALL_MQ)
        self.assertEqual(set(configs[1]["channels"]), set(TANK_MQ) - {"MQ7", "MQ9"})
        self.assertEqual({x["id"] for x in configs[4:]},
                         {"DROP_" + c for c in TANK_MQ})

    def test_sensor_ablation_is_paired_not_independent_accuracy(self):
        rows = synthetic_records()
        for row in rows:
            for channel in ALL_MQ:
                row["medians"][channel] = 1.0
            row["medians"]["MQ7"] = 100.0 * int(row["label"][1:]) + .01 * row["day"]
        runs = [dict(config_id=x["id"], **evaluate(rows, x["channels"]))
                for x in prespecified_channel_sets()]
        self.assertEqual(runs[0]["correct"], 50)
        self.assertLess(runs[1]["correct"], 50)
        comparison = ablation_comparison(runs)
        self.assertEqual(len(comparison), 8)
        mq7 = next(x for x in comparison if x["removed_sensor"] == "MQ7")
        self.assertLess(mq7["delta_correct"], 0)
        self.assertEqual(sum(mq7["paired"][k] for k in
                             ("both_correct", "only_first_correct",
                              "only_second_correct", "both_wrong")), 50)

    def test_invalid_channel_selection_fails_closed(self):
        rows = synthetic_records()
        for channels in ([], ["MQ7", "MQ7"], ["MQ_UNKNOWN"]):
            with self.assertRaisesRegex(ValueError, "INVALID_CHANNEL_SELECTION"):
                evaluate(rows, channels)

    def test_permutation_control_is_reproducible_and_never_g2_evidence(self):
        rows = synthetic_records()
        one = permutation_negative_control(rows, TANK_MQ, n_permutations=99, seed=42)
        two = permutation_negative_control(rows, TANK_MQ, n_permutations=99, seed=42)
        self.assertEqual(one, two)
        self.assertEqual(one["observed_correct"], 50)
        self.assertEqual(one["source_files"], 50)
        self.assertFalse(one["independent_session_significance_claim"])
        self.assertGreater(one["observed_correct"], one["null_p95_correct"])

    def test_paired_comparison_counts_match(self):
        rows = synthetic_records()
        a, b = evaluate(rows, TANK_MQ), evaluate(rows, ALL_MQ)
        paired = paired_channel_comparison(a, b)
        self.assertEqual(paired["both_correct"], 50)
        self.assertEqual(paired["only_first_correct"], 0)
        self.assertEqual(paired["only_second_correct"], 0)
        self.assertEqual(paired["both_wrong"], 0)

    def test_permutation_requires_one_file_per_class_and_day(self):
        rows = synthetic_records()
        rows[0]["label"] = "c1"
        with self.assertRaisesRegex(ValueError, "UNBALANCED_FILEDAY_PERMUTATION_STRATA"):
            permutation_negative_control(rows, TANK_MQ, n_permutations=10)

    def test_duplicate_file_ids_cannot_enter_permutation(self):
        rows = synthetic_records()
        rows[-1]["file"] = rows[0]["file"]
        with self.assertRaisesRegex(ValueError, "DUPLICATE_SOURCE_FILE_ID"):
            permutation_negative_control(rows, TANK_MQ, n_permutations=10)

    def test_source_missing_day_is_not_silently_imputed(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "test.zip"
            with zipfile.ZipFile(p, "w") as zf:
                zf.writestr("Banana D1.csv", "Ticks," + ",".join(ALL_MQ) + "\n0," + ",".join(["1"] * 9) + "\n1," + ",".join(["2"] * 9) + "\n")
            with self.assertRaisesRegex(ValueError, "UNEXPECTED_SOURCE_FILE_DAY_GROUPING"):
                load_source(p)

if __name__ == "__main__":
    unittest.main()
