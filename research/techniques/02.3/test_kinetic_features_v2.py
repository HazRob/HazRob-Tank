import unittest
from math import log
from kinetic_features_v2 import transient_features

def trace():
    b=[{"time_s":t,"rs_ohm":100} for t in (0,.5)]
    e=[{"time_s":t,"rs_ohm":v} for t,v in ((1,100),(2,50),(3,25))]
    r=[{"time_s":t,"rs_ohm":v} for t,v in ((4,50),(5,100),(6,100))]
    return b,e,r

def extract(parts=None):
    return transient_features("MQ135-physical-A","session-S1",*(parts or trace()))

class KineticTests(unittest.TestCase):
    def test_peak_and_orientation(self):
        d=extract()
        self.assertAlmostEqual(d["peak_log_response"],log(.25))
        self.assertEqual(d["peak_time_s"],3)
        self.assertEqual(d["peak_direction"],"RS_DECREASE")

    def test_recovery_half_time(self):
        d=extract()
        self.assertAlmostEqual(d["half_recovery_after_exposure_s"],1)

    def test_t50(self):
        self.assertAlmostEqual(extract()["time_to_half_peak_s"],1)

    def test_auc_nonzero(self):
        self.assertGreater(extract()["integrated_abs_log_response_s"],0)

    def test_no_ppm(self):
        d=extract()
        self.assertFalse(d["gas_identity_proven"])
        self.assertFalse(d["ppm_approved"])

    def test_no_response(self):
        b,e,r=trace()
        for stage in (e,r):
            for point in stage:point["rs_ohm"]=100
        d=extract((b,e,r))
        self.assertIsNone(d["time_to_half_peak_s"])
        self.assertIsNone(d["half_recovery_after_exposure_s"])
        self.assertEqual(d["peak_direction"],"NO_RESPONSE")

    def test_upgoing_peak(self):
        b,e,r=trace()
        for x in e:x["rs_ohm"]=200
        for x in r:x["rs_ohm"]=100
        self.assertEqual(extract((b,e,r))["peak_direction"],"RS_INCREASE")

    def test_no_recovery(self):
        b,e,r=trace()
        for x in r:x["rs_ohm"]=25
        self.assertIsNone(extract((b,e,r))["half_recovery_after_exposure_s"])

    def test_bad_sensor(self):
        with self.assertRaises(ValueError):
            transient_features("","s",*trace())

    def test_bad_session(self):
        with self.assertRaises(ValueError):
            transient_features("MQ","",*trace())

    def test_overlapping_stages(self):
        b,e,r=trace()
        e[0]["time_s"]=.5
        with self.assertRaises(ValueError):extract((b,e,r))

    def test_duplicate_time(self):
        b,e,r=trace()
        e[1]["time_s"]=e[0]["time_s"]
        with self.assertRaises(ValueError):extract((b,e,r))

    def test_nonfinite_measurement(self):
        b,e,r=trace()
        e[0]["rs_ohm"]=float("nan")
        with self.assertRaises(ValueError):extract((b,e,r))

    def test_zero_measurement(self):
        b,e,r=trace()
        e[0]["rs_ohm"]=0
        with self.assertRaises(ValueError):extract((b,e,r))

    def test_missing_stage(self):
        b,e,r=trace()
        with self.assertRaises(ValueError):extract((b,e,[]))

    def test_bool_rs(self):
        b,e,r=trace()
        e[0]["rs_ohm"]=True
        with self.assertRaises(ValueError):extract((b,e,r))

    def test_unordered_stage(self):
        b,e,r=trace()
        r[0]["time_s"]=100
        with self.assertRaises(ValueError):extract((b,e,r))

if __name__=="__main__":unittest.main()
