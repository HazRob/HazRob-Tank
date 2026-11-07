import unittest
from hashlib import sha256
from r0_provenance_v2 import CalibrationSpec,derive_r0,verify_sha256


RAW=b"unverified scientific example only"
DIGEST=sha256(RAW).hexdigest()


def spec(**kw):
    d=dict(sensor_model="MQ7",sensor_instance_id="tank-mq7-instance-01",
           calibration_id="cal-01",reference_id="source-reference-17",
           reference_type="gas_reference",reference_label="CO",
           concentration_ppm=100.0,ratio_rs_to_r0=2.0,
           ratio_origin="measured_reference",heater_phase="LOW",
           rl_ohm=10000.0,vc_volts=5.0,reference_temperature_c=24.0,
           reference_humidity_pct=49.0,source_sha256=DIGEST)
    d.update(kw)
    return CalibrationSpec(**d)


def samples(n=5,rs=20000):
    return [dict(sensor_instance_id="tank-mq7-instance-01",
                 heater_phase="LOW",reference_id="source-reference-17",
                 time_s=i*0.5,rs_ohm=rs) for i in range(n)]


class ProvenanceTests(unittest.TestCase):
    def test_r0_ohm_and_kohm(self):
        v=derive_r0(spec(),samples())
        self.assertEqual(v["r0_ohm"],10000)
        self.assertEqual(v["r0_kohm"],10)
        self.assertEqual(v["reference_concentration_ppm"],100)
        self.assertEqual(v["sensor_instance_id"],"tank-mq7-instance-01")
        self.assertFalse(v["tank_G3_validated"])
        self.assertFalse(v["device_ppm_approved"])

    def test_independent_gas_metadata_is_not_physical_proof(self):
        self.assertEqual(derive_r0(spec(),samples())["evidence_grade"],
                         "REFERENCE_METADATA_REQUIRES_INDEPENDENT_VERIFICATION")

    def test_air_factor_prior(self):
        x=spec(reference_type="clean_air_factor",reference_label="CLEAN_AIR",
               concentration_ppm=0,ratio_origin="manufacturer_curve_prior")
        y=derive_r0(x,samples())
        self.assertEqual(y["evidence_grade"],"RESEARCH_CURVE_PRIOR")
        self.assertFalse(y["device_ppm_approved"])

    def test_air_factor_not_measured_gas(self):
        with self.assertRaises(ValueError):
            spec(reference_type="clean_air_factor",reference_label="CLEAN_AIR",
                 concentration_ppm=0,ratio_origin="measured_reference")

    def test_air_factor_cannot_claim_concentration(self):
        with self.assertRaises(ValueError):
            spec(reference_type="clean_air_factor",reference_label="CLEAN_AIR",
                 concentration_ppm=100,ratio_origin="manufacturer_curve_prior")

    def test_unknown_gas_rejected(self):
        with self.assertRaises(ValueError): spec(reference_label="UNKNOWN")

    def test_phase_unknown_rejected(self):
        with self.assertRaises(ValueError): spec(heater_phase="UNKNOWN")

    def test_zero_reference_ratio_rejected(self):
        with self.assertRaises(ValueError): spec(ratio_rs_to_r0=0)

    def test_bad_RL(self):
        with self.assertRaises(ValueError): spec(rl_ohm=0)

    def test_bad_RH(self):
        with self.assertRaises(ValueError): spec(reference_humidity_pct=150)

    def test_bad_hash_shape(self):
        with self.assertRaises(ValueError): spec(source_sha256="abc")

    def test_real_hash_matches(self):
        self.assertTrue(verify_sha256(RAW,DIGEST))
        self.assertFalse(verify_sha256(b"tampered",DIGEST))

    def test_source_bytes_not_automatically_verified(self):
        self.assertFalse(derive_r0(spec(),samples())["source_bytes_sha256_verified_here"])

    def test_cross_physical_instance_rejected(self):
        rows=samples();rows[3]["sensor_instance_id"]="otherMQ"
        with self.assertRaises(ValueError): derive_r0(spec(),rows)

    def test_wrong_heater_phase_rejected(self):
        rows=samples();rows[3]["heater_phase"]="HIGH"
        with self.assertRaises(ValueError): derive_r0(spec(),rows)

    def test_wrong_reference_rejected(self):
        rows=samples();rows[2]["reference_id"]="another"
        with self.assertRaises(ValueError): derive_r0(spec(),rows)

    def test_mixed_or_reordered_time_rejected(self):
        rows=samples();rows[3]["time_s"]=rows[2]["time_s"]
        with self.assertRaises(ValueError): derive_r0(spec(),rows)

    def test_negative_resistance_rejected(self):
        rows=samples();rows[0]["rs_ohm"]=-5
        with self.assertRaises(ValueError): derive_r0(spec(),rows)

    def test_outlier_median(self):
        rows=samples();rows[-1]["rs_ohm"]=100000
        self.assertEqual(derive_r0(spec(),rows)["r0_ohm"],10000)

    def test_unstable_window_rejected(self):
        rows=samples()
        for i,x in enumerate(rows):x["rs_ohm"]=10000*(i+1)
        with self.assertRaises(ValueError):derive_r0(spec(),rows)

    def test_insufficient_samples(self):
        with self.assertRaises(ValueError):derive_r0(spec(),samples(n=4))

    def test_bool_sample_not_real(self):
        rows=samples();rows[0]["rs_ohm"]=True
        with self.assertRaises(ValueError):derive_r0(spec(),rows)

    def test_invalid_min_samples(self):
        with self.assertRaises(ValueError):derive_r0(spec(),samples(),min_samples=True)

    def test_threshold_bounds(self):
        with self.assertRaises(ValueError):derive_r0(spec(),samples(),max_relative_mad=1.1)

    def test_zero_gas_concentration_rejected(self):
        with self.assertRaises(ValueError):spec(concentration_ppm=0)

    def test_nan_rejected(self):
        with self.assertRaises(ValueError):spec(ratio_rs_to_r0=float("nan"))

if __name__=="__main__":unittest.main()
