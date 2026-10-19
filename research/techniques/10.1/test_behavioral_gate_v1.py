import unittest
from negative_evidence import update
PR={"ethanol":.5,"acetone":.5}
C={"ethanol":{"mq":.9},"acetone":{"mq":.1}}
class BehavioralGate(unittest.TestCase):
    def test_detected(self): self.assertAlmostEqual(update(PR,{"mq":True},C)["ethanol"],.9)
    def test_not_detected(self): self.assertAlmostEqual(update(PR,{"mq":False},C)["ethanol"],.1)
    def test_no_detection_observation(self): self.assertAlmostEqual(update(PR,{"mq":None},C)["ethanol"],.5)
    def test_quality_false(self): self.assertAlmostEqual(update(PR,{"mq":True},C,quality={"mq":False})["ethanol"],.5)
    def test_unknown_power_ignored(self): self.assertAlmostEqual(update(PR,{"unknown":True},C)["ethanol"],.5)
    def test_nonzero_both(self):
        out=update(PR,{"mq":False},{"ethanol":{"mq":1.},"acetone":{"mq":0.}})
        self.assertGreater(out["ethanol"],0)
        self.assertGreater(out["acetone"],0)
    def test_sum_one(self): self.assertAlmostEqual(sum(update(PR,{"mq":True},C).values()),1)
    def test_nonuniform_prior(self): self.assertAlmostEqual(update({"a":3.,"b":1.},{},{})["a"],.75)
    def test_negative_prior(self):
        with self.assertRaises(ValueError): update({"a":-1,"b":2},{},{})
    def test_boolean_prior(self):
        with self.assertRaises(ValueError): update({"a":True,"b":1},{},{})
    def test_bad_detect_state(self):
        with self.assertRaises(ValueError): update(PR,{"mq":"no"},C)
    def test_bad_cap(self):
        with self.assertRaises(ValueError): update(PR,{"mq":True},{"ethanol":{"mq":1.1}})
    def test_bool_cap(self):
        with self.assertRaises(ValueError): update(PR,{"mq":True},{"ethanol":{"mq":True}})
    def test_bad_quality(self):
        with self.assertRaises(ValueError): update(PR,{"mq":True},C,quality={"mq":1})
    def test_bad_floor(self):
        with self.assertRaises(ValueError): update(PR,{},C,floor=True)
    def test_missing_mapping(self):
        with self.assertRaises(ValueError): update(PR,{"mq":True},{"ethanol":[]})
    def test_nonfinite_cap(self):
        with self.assertRaises(ValueError): update(PR,{"mq":True},{"ethanol":{"mq":float("nan")}})
if __name__=="__main__":unittest.main()
