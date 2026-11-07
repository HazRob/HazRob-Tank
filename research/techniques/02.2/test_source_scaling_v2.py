import unittest
from source_scaling_v2 import fit_train_scaler, transform_heldout


def fitted():
    return fit_train_scaler("MQ-EXT-03",("MQ2","MQ135"),[(1,10),(3,30)],["expoA","expoB"])


class GroupedSourceScalingTests(unittest.TestCase):
    def test_center_scale_training_only(self):
        f=fitted()
        self.assertEqual(f.center,(2.,20.))
        self.assertEqual(f.scale,(1.,10.))
        r=transform_heldout(f,[(4,40)],["expoC"],sensor_ids=("MQ2","MQ135"),source_id="MQ-EXT-03")
        self.assertEqual(r,[[2.,2.]])
    def test_multirow_same_heldout_group_allowed(self):
        f=fitted()
        xs=transform_heldout(f,[(4,40),(5,50)],["expoC","expoC"],
            sensor_ids=("MQ2","MQ135"),source_id="MQ-EXT-03")
        self.assertEqual(len(xs),2)
    def test_no_train_test_exposure_overlap(self):
        with self.assertRaises(ValueError):
            transform_heldout(fitted(),[(2,20)],["expoA"],sensor_ids=("MQ2","MQ135"),source_id="MQ-EXT-03")
    def test_column_order_mismatch_rejected(self):
        with self.assertRaises(ValueError):
            transform_heldout(fitted(),[(2,20)],["expoC"],sensor_ids=("MQ135","MQ2"),source_id="MQ-EXT-03")
    def test_source_mismatch_rejected(self):
        with self.assertRaises(ValueError):
            transform_heldout(fitted(),[(2,20)],["expoC"],sensor_ids=("MQ2","MQ135"),source_id="other")
    def test_constant_channel_safe(self):
        f=fit_train_scaler("x",("MQ2",),[(7,),(7,)],["a","b"])
        self.assertEqual(transform_heldout(f,[(7,)],["c"],sensor_ids=("MQ2",),source_id="x"),[[0.]])
    def test_missing_group_rejected(self):
        with self.assertRaises(ValueError):
            fit_train_scaler("x",("MQ2",),[(1,),(2,)],["a"])
    def test_bad_training_shape(self):
        with self.assertRaises(ValueError):
            fit_train_scaler("x",("MQ2",),[(1,),(1,2)],["a","b"])
    def test_nan_bool_rejected(self):
        for v in (True,float("nan"),float("inf")):
            with self.subTest(v=v), self.assertRaises(ValueError):
                fit_train_scaler("x",("MQ2",),[(v,)],["a"])
    def test_bad_channels(self):
        with self.assertRaises(ValueError):
            fit_train_scaler("x",("MQ2","MQ2"),[(1,2)],["a"])
    def test_no_ppm_claim(self):
        self.assertTrue(fitted().no_ppm_approved)
    def test_center_only(self):
        f=fit_train_scaler("x",("MQ2",),[(1,),(3,)],["a","b"],method="center_only")
        self.assertEqual(transform_heldout(f,[(5,)],["c"],sensor_ids=("MQ2",),source_id="x"),[[3.]])
    def test_bad_method(self):
        with self.assertRaises(ValueError):
            fit_train_scaler("x",("MQ2",),[(1,)],["a"],method="unknown")


if __name__=="__main__":
    unittest.main()
