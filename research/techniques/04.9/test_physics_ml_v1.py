import unittest
from physics_ml_v1 import augment_physics_features,rank_reference_residuals
class PhysicsMLTests(unittest.TestCase):
    def setUp(self):
        self.curves={"A":{"MQ2":[(10,10),(100,1)],"MQ3":[(10,10),(100,1)]},
                     "B":{"MQ2":[(10,1),(100,.1)],"MQ3":[(10,1),(100,.1)]}}
        self.data=augment_physics_features({"MQ2":1,"MQ3":1},self.curves,100,["MQ2","MQ3"])
    def test_ranking(self):self.assertEqual(rank_reference_residuals(self.data)[0]["gas_label"],"A")
    def test_zero_residual(self):self.assertAlmostEqual(self.data["reference_residuals"]["A"]["log10_residuals"][0],0)
    def test_two_channels(self):self.assertEqual(self.data["reference_residuals"]["A"]["valid_channels"],2)
    def test_log_observed(self):self.assertEqual(self.data["log10_observed"],[0.,0.])
    def test_missing_channel(self):
        c={"A":{"MQ2":[(10,10),(100,1)]}}
        z=augment_physics_features({"MQ2":1,"MQ3":1},c,100,["MQ2","MQ3"])
        self.assertEqual(z["reference_residuals"]["A"]["valid_mask"],[True,False])
    def test_no_extrapolation(self):
        z=augment_physics_features({"MQ2":1,"MQ3":1},self.curves,1000,["MQ2","MQ3"])
        self.assertEqual(rank_reference_residuals(z),[])
    def test_min_channels(self):self.assertEqual(rank_reference_residuals(self.data,min_channels=3),[])
    def test_bad_min_channels(self):
        with self.assertRaises(ValueError):rank_reference_residuals(self.data,min_channels=0)
    def test_bad_ppm(self):
        with self.assertRaises(ValueError):augment_physics_features({"MQ2":1,"MQ3":1},self.curves,0,["MQ2","MQ3"])
    def test_bad_observed(self):
        with self.assertRaises(ValueError):augment_physics_features({"MQ2":0,"MQ3":1},self.curves,100,["MQ2","MQ3"])
    def test_missing_observed(self):
        with self.assertRaises(ValueError):augment_physics_features({"MQ2":1},self.curves,100,["MQ2","MQ3"])
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):augment_physics_features({"MQ2":1},self.curves,100,["MQ2","MQ2"])
    def test_invalid_curve(self):
        with self.assertRaises(ValueError):augment_physics_features({"MQ2":1,"MQ3":1},{"A":{"MQ2":[(100,1),(10,10)]}},100,["MQ2","MQ3"])
    def test_deterministic(self):
        self.assertEqual(self.data,augment_physics_features({"MQ2":1,"MQ3":1},self.curves,100,["MQ2","MQ3"]))
    def test_reference_order(self):self.assertEqual(list(self.data["reference_residuals"]),["A","B"])
    def test_candidate_specific(self):self.assertNotEqual(self.data["reference_residuals"]["A"]["log10_residuals"],self.data["reference_residuals"]["B"]["log10_residuals"])
if __name__=="__main__":unittest.main()
