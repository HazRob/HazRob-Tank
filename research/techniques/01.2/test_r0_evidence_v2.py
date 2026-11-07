import unittest
from r0_evidence_v2 import R0Evidence, estimate_r0


def reference(**kw):
    fields=dict(device_id="Tank-test-device", sensor_instance_id="MQ135-unit-001",
                sensor_model="MQ135",measured_rl_ohm=10000.,
                calibrant="CO2",reference_concentration_ppm=400.,
                reference_ratio_rs_over_r0=3.6,
                r0_definition_id="MQ135-air-reference-3.6",heater_phase="CONSTANT",
                reference_source_id="test-datasheet",source_sha256="a"*64)
    fields.update(kw)
    return R0Evidence(**fields)


class EvidenceTests(unittest.TestCase):
    def test_r0_ohm_to_kohm_and_status(self):
        r=estimate_r0(reference(),[36000.]*5)
        self.assertEqual(r["r0_ohm"],10000.)
        self.assertEqual(r["r0_kohm"],10.)
        self.assertEqual(r["sensor_instance_id"],"MQ135-unit-001")
        self.assertIn("NOT_DEVICE",r["status"])
    def test_no_shared_sensor_instance(self):
        a=estimate_r0(reference(sensor_instance_id="MQ135-a"),[36000]*5)
        b=estimate_r0(reference(sensor_instance_id="MQ135-b"),[72000]*5)
        self.assertNotEqual(a["r0_ohm"],b["r0_ohm"])
        self.assertNotEqual(a["sensor_instance_id"],b["sensor_instance_id"])
    def test_reference_gas_and_phase_are_retained(self):
        x=estimate_r0(reference(sensor_model="MQ7",heater_phase="LOW",calibrant="CO",
                                reference_concentration_ppm=100.),[36000]*5)
        self.assertEqual((x["calibrant"],x["heater_phase"]),("CO","LOW"))
    def test_outlier_does_not_distort_median(self):
        self.assertEqual(estimate_r0(reference(),[36000]*4+[360000])["r0_ohm"],10000)
    def test_small_and_unstable_windows(self):
        for a in ([36000]*4,[1000,2000,3000,4000,5000],[0]*5):
            with self.subTest(a=a),self.assertRaises(ValueError):
                estimate_r0(reference(),a)
    def test_reject_missing_reference(self):
        for kv in ({"sensor_model":"TGS2602"},{"sensor_instance_id":""},
                   {"calibrant":""},{"reference_ratio_rs_over_r0":0},
                   {"reference_concentration_ppm":0},{"heater_phase":"UNKNOWN"},
                   {"measured_rl_ohm":float("nan")},{"source_sha256":"abc"},
                   {"r0_definition_id":""}):
            with self.subTest(kv=kv),self.assertRaises(ValueError): reference(**kv)
    def test_boolean_and_nonfinite_samples(self):
        for bad in (True,float("inf"),float("nan"),-5):
            with self.subTest(bad=bad),self.assertRaises(ValueError):
                estimate_r0(reference(),[36000]*4+[bad])
    def test_gate_rejects_incorrect_inputs(self):
        for v in (0,4,5.1,True):
            with self.subTest(v=v),self.assertRaises(ValueError):
                estimate_r0(reference(),[36000]*5,minimum_count=v)
        with self.assertRaises(ValueError):
            estimate_r0(reference(),[36000]*5,maximum_relative_mad=-.1)
    def test_sha_is_claim_not_verified(self):
        r=estimate_r0(reference(),[36000]*5)
        self.assertEqual(len(r["claimed_source_sha256"]),64)
        self.assertNotIn("verified_source_sha256",r)


if __name__=="__main__":
    unittest.main()
