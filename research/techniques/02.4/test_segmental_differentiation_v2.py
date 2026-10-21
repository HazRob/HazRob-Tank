import unittest
from segmental_differentiation_v2 import segmental_difference as f
def run(**kwargs):
    d=dict(times_s=list(range(8)),rs_ohm=[10000,9900,9000,8900,8000,7900,7000,6900],
        baseline_rs_ohm=10000.,sensor_instance_id="MQ135-A",exposure_group_id="exp01",
        source_id="MQ-EXT",heater_phase="CONSTANT")
    d.update(kwargs)
    return f(**d)
class SegmentedScientificTests(unittest.TestCase):
    def test_monotone_response(self):
        result=run()
        self.assertEqual(len(result["segment_difference_per_second"]),3)
        self.assertTrue(all(x<0 for x in result["segment_difference_per_second"]))
        self.assertFalse(result["ppm_validated"])
    def test_time_scale_changes_derivative(self):
        a=run();b=run(times_s=[x*2 for x in range(8)])
        self.assertAlmostEqual(b["segment_difference_per_second"][0],a["segment_difference_per_second"][0]/2)
    def test_refuses_mixed_or_missing_timing(self):
        for times in ([0,1,2,3,3,5,6,7],[0,1,2,3,4,20,21,22]):
            with self.subTest(times=times),self.assertRaises(ValueError):run(times_s=times)
    def test_invalid_reference_or_heater(self):
        for kw in (dict(baseline_rs_ohm=0),dict(heater_phase="UNKNOWN"),
                   dict(sensor_instance_id=""),dict(segments=5),dict(rs_ohm=[True]*8)):
            with self.subTest(kw=kw),self.assertRaises(ValueError):run(**kw)
    def test_source_identity_preserved(self):
        self.assertEqual(run()["exposure_group_id"],"exp01")
if __name__=="__main__": unittest.main()
