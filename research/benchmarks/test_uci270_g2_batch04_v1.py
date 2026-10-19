"""G2-04 invariant tests; independent source download+SHA in Actions."""
import math
import unittest
import uci270_g2_batch04_v1 as benchmark
from uci270_source_adapter_v1 import (BATCH_COUNTS, LABELS, N_FEATURES,
                                      select_sensor_features)

class Uci270G204Invariants(unittest.TestCase):
    def test_explicit_10_ids(self):
        self.assertEqual(len(benchmark.IDS),10)
        self.assertEqual(len(set(benchmark.IDS)),10)
    def test_13910_references(self):
        self.assertEqual(sum(BATCH_COUNTS.values()),13910)
    def test_six_label_map(self):
        self.assertEqual(list(LABELS.values()),
                         ["ethanol","ethylene","ammonia","acetaldehyde","acetone","toluene"])
    def test_128_to_16_is_descriptor_selection(self):
        f=select_sensor_features({"features":list(range(128))})
        self.assertEqual(f,[8*i+1 for i in range(16)])
    def test_labelled_device_gate(self):
        self.assertEqual(benchmark.device_gate([])["status"],"BLOCKED_ONE_PHYSICAL_DEVICE")
    def test_mean_and_distance(self):
        self.assertEqual(benchmark.mean_vector([[1,2],[3,4]]),[2,3])
        self.assertEqual(benchmark.distance([0,0],[3,4]),5)
    def test_zero_distance(self):
        self.assertEqual(benchmark.distance([1,2],[1,2]),0)
    def test_nearest_centroid_tie_deterministic(self):
        m={"centroids":{"ethanol":[0.],"acetone":[2.]}}
        self.assertEqual(benchmark.centroid_predict(m,[1.]),"acetone")
    def test_resource_budget_is_not_real_device_measurement(self):
        result=benchmark.resource_budget()
        self.assertEqual(result["status"],"EXECUTED_ANALYTICAL_ONLY_NOT_DEVICE_PROFILE")
        self.assertEqual(result["head_parameters"],96)
    def test_group_holds_all_rows(self):
        rows=[{"batch":i} for i in range(1,11)]
        report=benchmark.chronological_batch_gate(rows)
        self.assertEqual(report["n_folds"],10)
        self.assertEqual(report["n_holdout_exposures_per_temporal_batch"],[1]*10)
    def test_no_matched_reference_blocks_without_fake_reading(self):
        rows=[{"batch":1,"gas":"a","ppm":10},{"batch":10,"gas":"b","ppm":10}]
        self.assertIsNone(benchmark.matched_standard(rows))
    def test_canonical_source_sha(self):
        self.assertEqual(len(benchmark.ZIP_SHA256),64)

if __name__=="__main__":
    unittest.main()
