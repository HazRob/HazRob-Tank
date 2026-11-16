import unittest
from group_scaler_v2 import fit_group_scaler,transform_heldout


def train():
    return [{"group_id":"s1","values":{"a":10,"b":100}},
            {"group_id":"s1","values":{"a":12,"b":100}},
            {"group_id":"s2","values":{"a":20,"b":100}}]

def fit():
    return fit_group_scaler("MQ-EXT-03",("a","b"),train())

def held(**kw):
    d={"group_id":"s3","values":{"a":16,"b":100}}
    d.update(kw);return d

class ScalingContractTests(unittest.TestCase):
    def test_equal_group_weight_not_unequal_rows(self):
        m=fit()
        self.assertAlmostEqual(m.centers[0],15.5)
        self.assertEqual(m.centers[1],100)

    def test_heldout_values(self):
        m=fit()
        x=transform_heldout(m,[held()],source_id="MQ-EXT-03")[0]
        self.assertAlmostEqual(x["values"]["a"],(16-m.centers[0])/m.scales[0])
        self.assertEqual(x["values"]["b"],0)
        self.assertFalse(x["ppm_approved"])

    def test_heldout_group_overlap(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[held(group_id="s1")],source_id="MQ-EXT-03")

    def test_source_domain_transfer_forbidden(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[held()],source_id="UCI1081")

    def test_missing_sensor(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[held(values={"a":1})],source_id="MQ-EXT-03")

    def test_unexpected_sensor(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[held(values={"a":1,"b":2,"c":3})],source_id="MQ-EXT-03")

    def test_missing_training_group(self):
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","b"),[{"values":{"a":1,"b":3}}])

    def test_one_training_group(self):
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","b"),train()[:2])

    def test_duplicate_sensor_id(self):
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","a"),train())

    def test_bad_training_channels(self):
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","z"),train())

    def test_nonfinite_training(self):
        a=train();a[1]["values"]["b"]=float("nan")
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","b"),a)

    def test_nonfinite_heldout(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[held(values={"a":float("inf"),"b":100})],source_id="MQ-EXT-03")

    def test_bool_not_observation(self):
        a=train();a[1]["values"]["a"]=True
        with self.assertRaises(ValueError):
            fit_group_scaler("MQ-EXT-03",("a","b"),a)

    def test_empty_heldout(self):
        with self.assertRaises(ValueError):
            transform_heldout(fit(),[],source_id="MQ-EXT-03")

    def test_frozen_repeated_prediction(self):
        a=transform_heldout(fit(),[held()],source_id="MQ-EXT-03")
        b=transform_heldout(fit(),[held()],source_id="MQ-EXT-03")
        self.assertEqual(a,b)

    def test_order_is_explicit(self):
        self.assertEqual(fit().sensor_order,("a","b"))

if __name__=="__main__":unittest.main()
