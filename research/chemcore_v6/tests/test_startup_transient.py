"""Research-only first-minute MQ transient quality checks."""
import math
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import startup_transient as t

def record(model="MQ2",heater="VENDOR_NOMINAL_FIXED"):
    return dict(device_id="TEST_DEVICE",sensor_id=model,
                power_event_id="relay-01",phase="POWER_ON_TRANSIENT",
                heater_state=heater,
                samples=[dict(t_ms=i*1000,rs=10000-1000*(1-math.exp(-i/9)))
                         for i in range(61)])

class StartupTransientTests(unittest.TestCase):
    def test_research_only(self):
        z=t.extract(record())
        self.assertEqual(z["status"],"FEATURES_RESEARCH_ONLY")
        self.assertEqual(z["sample_count"],61)
        self.assertIsNone(z["gas_identity"])
        self.assertIsNone(z["ppm_estimate"])
        self.assertIsNone(z["R0_calibration"])
        self.assertFalse(z["warmup_may_be_skipped"])
        self.assertFalse(z["Tank_G3_qualification"])
    def test_first_seconds_missing(self):
        r=record()
        r["samples"]=r["samples"][5:]
        self.assertEqual(t.extract(r)["reason"],"FIRST_LAST_COVERAGE_INSUFFICIENT")
    def test_late_samples_missing(self):
        r=record()
        r["samples"]=r["samples"][:45]
        self.assertEqual(t.extract(r)["reason"],"FIRST_LAST_COVERAGE_INSUFFICIENT")
    def test_large_sample_gap(self):
        r=record()
        r["samples"]=[s for s in r["samples"] if not 13000<=s["t_ms"]<=21000]
        self.assertEqual(t.extract(r)["reason"],"GAP_EXCEEDS_5_SECONDS")
    def test_duplicate_timestamp(self):
        r=record()
        r["samples"][10]["t_ms"]=r["samples"][9]["t_ms"]
        self.assertEqual(t.extract(r)["reason"],"OUT_OF_ORDER_OR_OUTSIDE_CAPTURE_WINDOW")
    def test_restart_event(self):
        r=record()
        r["relay_restarted"]=True
        self.assertEqual(t.extract(r)["reason"],"POWER_EVENT_CHANGED")
    def test_mq7_mq9_need_heater_proof(self):
        for key in ["MQ7","MQ9"]:
            self.assertEqual(t.extract(record(model=key))["reason"],
                             "MQ7_MQ9_HEATER_VARIANT_CYCLE_NOT_VALIDATED")
    def test_heater_unknown(self):
        self.assertEqual(t.extract(record(heater="HEATER_UNVERIFIED"))["reason"],
                         "HEATER_STATE_UNVERIFIED")
    def test_no_power_event(self):
        r=record()
        del r["power_event_id"]
        self.assertEqual(t.extract(r)["reason"],"MISSING_POWER_EVENT_ID")
    def test_bme280_is_not_mq(self):
        self.assertEqual(t.extract(record(model="BME280"))["reason"],
                         "UNRECOGNIZED_MQ_CHANNEL")
    def test_sparse_samples(self):
        r=record()
        r["samples"]=r["samples"][::4]
        self.assertEqual(t.extract(r)["reason"],"INSUFFICIENT_SAMPLE_COUNT")
    def test_nan_rs(self):
        r=record()
        r["samples"][20]["rs"]=float("nan")
        self.assertEqual(t.extract(r)["reason"],"INVALID_TIME_OR_RESISTANCE")
    def test_bosch_heater_not_inferred(self):
        z=t.extract(record(model="MQ7",heater="VENDOR_VALIDATED_CYCLIC"))
        self.assertEqual(z["status"],"FEATURES_RESEARCH_ONLY")
        self.assertFalse(z["heater_profile_is_bosch_scan"])
    def test_simulation_provenance(self):
        z=t.demo()
        self.assertTrue(z["synthetic"])
        self.assertIsNone(z["ppm_estimate"])

if __name__=="__main__":
    unittest.main()
