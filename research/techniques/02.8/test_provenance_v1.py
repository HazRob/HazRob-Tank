import unittest
from provenance_v1 import validate_provenance,provenance_sha256
def record():
    return dict(schema_version="1.0",record_id="r1",device_id="tank1",session_id="s1",
        sensor_id="MQ2",timestamp_utc="2026-10-18T03:00:00Z",firmware_version="dev",
        calibration_id="c1",heater_phase="steady",measurement_kind="rs",
        measurement_unit="ohm",sample_count=10,processing_steps=["adc_to_rs"])
class ProvenanceTests(unittest.TestCase):
    def test_valid(self): self.assertTrue(validate_provenance(record()))
    def test_digest(self): self.assertEqual(provenance_sha256(record()),provenance_sha256(dict(reversed(list(record().items())))))
    def test_digest_change(self):
        r=record();r["calibration_id"]="c2";self.assertNotEqual(provenance_sha256(r),provenance_sha256(record()))
    def test_missing(self):
        r=record();del r["sensor_id"]
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_empty(self):
        r=record();r["device_id"]=""
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_version(self):
        r=record();r["schema_version"]="2.0"
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_naive_time(self):
        r=record();r["timestamp_utc"]="2026-10-18T03:00:00"
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_offset_time(self):
        r=record();r["timestamp_utc"]="2026-10-18T06:00:00+03:00"
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_count(self):
        r=record();r["sample_count"]=0
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_steps(self):
        r=record();r["processing_steps"]=[""]
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_environment(self):
        r=record();r["environment"]={"temperature_c":float("nan")}
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_parent(self):
        r=record();r["parent_record_ids"]=[""]
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_kind(self):
        r=record();r["measurement_kind"]="ppm"
        with self.assertRaises(ValueError): validate_provenance(r)
    def test_extra(self):
        r=record();r["experiment_protocol_id"]="p1"
        self.assertTrue(validate_provenance(r))
if __name__=="__main__": unittest.main()
