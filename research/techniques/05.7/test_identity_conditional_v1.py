import unittest
from identity_conditional_v1 import estimate_conditional
class IdentityConditionalTests(unittest.TestCase):
    def setUp(self):
        self.cal={"ethanol":lambda _:{"ppm":50,"status":"WITHIN_CALIBRATED_RANGE"}}
    def call(self,probs,cal=None,**kw):
        return estimate_conditional(probs,self.cal if cal is None else cal,{"rs":10},**kw)
    def test_success(self):self.assertEqual(self.call({"ethanol":.95,"other":.05})["ppm"],50)
    def test_label(self):self.assertEqual(self.call({"ethanol":.95,"other":.05})["gas_label"],"ethanol")
    def test_confidence(self):self.assertEqual(self.call({"ethanol":.95,"other":.05})["identity_probability"],.95)
    def test_low_confidence(self):self.assertEqual(self.call({"ethanol":.6,"other":.4})["reason"],"LOW_IDENTITY_CONFIDENCE")
    def test_margin(self):self.assertEqual(self.call({"ethanol":.55,"other":.45},min_identity_probability=.5)["reason"],"AMBIGUOUS_IDENTITY")
    def test_missing(self):self.assertEqual(self.call({"ethanol":1.0},cal={})["reason"],"MISSING_GAS_CALIBRATOR")
    def test_rejected(self):self.assertEqual(self.call({"ethanol":1.0},cal={"ethanol":lambda _:{"ppm":None,"status":"OUTSIDE_CALIBRATED_RANGE"}})["reason"],"CALIBRATOR_ABSTAINED")
    def test_invalid_ppm(self):self.assertEqual(self.call({"ethanol":1.0},cal={"ethanol":lambda _:{"ppm":-2,"status":"WITHIN_CALIBRATED_RANGE"}})["reason"],"INVALID_CONCENTRATION")
    def test_exception(self):
        def fail(_):raise ValueError("bad sensor")
        self.assertEqual(self.call({"ethanol":1.0},cal={"ethanol":fail})["reason"],"CALIBRATOR_INPUT_REJECTED")
    def test_bad_sum(self):
        with self.assertRaises(ValueError):self.call({"ethanol":.8,"other":.1})
    def test_negative(self):
        with self.assertRaises(ValueError):self.call({"ethanol":1.1,"other":-.1})
    def test_nan(self):
        with self.assertRaises(ValueError):self.call({"ethanol":float("nan")})
    def test_bad_threshold(self):
        with self.assertRaises(ValueError):self.call({"ethanol":1.0},min_identity_probability=1.1)
    def test_empty(self):
        with self.assertRaises(ValueError):self.call({})
    def test_invalid_result(self):self.assertEqual(self.call({"ethanol":1.0},cal={"ethanol":lambda _:5})["reason"],"INVALID_CALIBRATOR_RESULT")
    def test_single(self):self.assertEqual(self.call({"ethanol":1.0})["status"],"CONDITIONAL_ESTIMATE")
if __name__=="__main__":unittest.main()
