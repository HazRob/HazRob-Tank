"""UCI1081 original file sensor-block layout regression, no raw ZIP required in CI.

Original 90x9002 CSV stored in external user research library; benchmark only
runs when exact original SHA256 archive is explicitly supplied. These tests
ensure the correct source-specific flattening rule and pin proof of its parity
to independently published MQ135 temporal fingerprint statistics.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
SCRIPT=ROOT/"research/chemcore_v6/benchmarks/uci1081_v6_benchmark.py"
RESULTS=ROOT/"research/chemcore_v6/results/uci1081_corrected_sensor_blocked_benchmark_2026-10-29.json"
REFERENCE=ROOT/"android/app/src/main/assets/chemcore_v6/reference/uci_mq135_lowconcentration_transfer_fingerprints_v1.json"

def sensor_blocks(flat, channel_count, seconds_per_channel):
    if len(flat)!=channel_count*seconds_per_channel:
        raise ValueError("invalid original array shape")
    return [[flat[ch*seconds_per_channel+t] for ch in range(channel_count)]
            for t in range(seconds_per_channel)]

class UciLayoutRegression(unittest.TestCase):
    def test_no_interleaving_of_original_sensor_blocks(self):
        # 3 sec of sensor 0, then 3 sec of sensor 1, then sensor 2.
        source=[10,11,12,20,21,22,30,31,32]
        self.assertEqual(sensor_blocks(source,3,3),
                         [[10,20,30],[11,21,31],[12,22,32]])
        with self.assertRaises(ValueError):
            sensor_blocks(source,3,4)

    def test_benchmark_requires_correct_shape_and_source_parity(self):
        code=SCRIPT.read_text()
        self.assertIn(".reshape(10,900).T",code)
        self.assertNotIn("reshape(900,10)",code.split("values = np.asarray")[-1])
        self.assertIn("abs(float(np.mean(ratios))-0.9471726656594102)>1e-9",code)
        self.assertIn("813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab",code)

    def test_corrected_endpoint_and_research_scores_match_evidence_file(self):
        data=json.loads(RESULTS.read_text())
        source=json.loads(REFERENCE.read_text())
        original=next(x for x in source["records"]
                      if x["gas"]=="acetone" and x["concentration_ppb"]==50)
        expected=original["features"]["endpoint_over_baseline"]["mean"]
        self.assertAlmostEqual(data["proof"]["corrected_raw_endpoint_over_baseline_mean"],
                               expected,places=12)
        self.assertTrue(data["previous_results_invalid"])
        self.assertEqual(data["raw_rows"],90)
        self.assertEqual(len(data["models"]),4)
        self.assertAlmostEqual(data["models"][0]["mean_balanced_accuracy"],0.288888888888889)
        self.assertAlmostEqual(data["models"][-1]["mean_balanced_accuracy"],0.788888888888889)
        self.assertTrue(all(len(x["folds"])==3 for x in data["models"]))
        self.assertTrue(all(sum(f["withheld_ppb"]==p for f in x["folds"])==1
                            for x in data["models"] for p in (50,100,200)))

if __name__=="__main__":
    unittest.main()
