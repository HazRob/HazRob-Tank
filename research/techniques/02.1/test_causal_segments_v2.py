import unittest
from causal_segments_v2 import CausalFilterSpec,filter_exposure_stream


def rows(n=5):
    return [dict(time_s=.15*i,rs_ohm=10000+(i*1000),physical_sensor_id="MQ2-A",
                 session_id="s01",stage="EXPOSURE",heater_phase="CONSTANT") for i in range(n)]


class CausalSegmentTests(unittest.TestCase):
    def test_causal_formula(self):
        o=filter_exposure_stream(CausalFilterSpec(.5,1),rows())
        self.assertEqual(o[0]["filtered_rs_ohm"],10000)
        self.assertEqual(o[1]["filtered_rs_ohm"],10500)
        self.assertFalse(o[1]["reset"])
        self.assertFalse(o[1]["ppm_approved"])

    def test_exposure_purge_not_blended(self):
        a=rows()
        a[3]["stage"]="PURGE";a[4]["stage"]="PURGE"
        b=filter_exposure_stream(CausalFilterSpec(.5,1),a)
        self.assertTrue(b[3]["reset"])
        self.assertEqual(b[3]["filtered_rs_ohm"],13000)

    def test_phase_change_reset(self):
        a=rows();a[2]["heater_phase"]="LOW"
        self.assertTrue(filter_exposure_stream(CausalFilterSpec(.5,1),a)[2]["reset"])

    def test_device_change_reset(self):
        a=rows();a[2]["physical_sensor_id"]="MQ2-B"
        self.assertTrue(filter_exposure_stream(CausalFilterSpec(.5,1),a)[2]["reset"])

    def test_session_change_reset(self):
        a=rows();a[2]["session_id"]="s02"
        self.assertTrue(filter_exposure_stream(CausalFilterSpec(.5,1),a)[2]["reset"])

    def test_gap_reset(self):
        a=rows()
        for i in range(3,5):a[i]["time_s"]+=10
        self.assertTrue(filter_exposure_stream(CausalFilterSpec(.5,1),a)[3]["reset"])

    def test_no_future_peek(self):
        a=rows();b=rows();b[-1]["rs_ohm"]=1000000
        x=filter_exposure_stream(CausalFilterSpec(.5,1),a)
        y=filter_exposure_stream(CausalFilterSpec(.5,1),b)
        self.assertEqual([v["filtered_rs_ohm"] for v in x[:-1]],
                         [v["filtered_rs_ohm"] for v in y[:-1]])

    def test_one_sample(self):
        self.assertEqual(len(filter_exposure_stream(CausalFilterSpec(.5,1),rows(1))),1)

    def test_empty(self):
        with self.assertRaises(ValueError):filter_exposure_stream(CausalFilterSpec(.5,1),[])

    def test_backward_time(self):
        a=rows();a[2]["time_s"]=0
        with self.assertRaises(ValueError):filter_exposure_stream(CausalFilterSpec(.5,1),a)

    def test_nan_rs(self):
        a=rows();a[0]["rs_ohm"]=float("nan")
        with self.assertRaises(ValueError):filter_exposure_stream(CausalFilterSpec(.5,1),a)

    def test_zero_rs(self):
        a=rows();a[0]["rs_ohm"]=0
        with self.assertRaises(ValueError):filter_exposure_stream(CausalFilterSpec(.5,1),a)

    def test_wrong_stage(self):
        a=rows();a[0]["stage"]="UNKNOWN"
        with self.assertRaises(ValueError):filter_exposure_stream(CausalFilterSpec(.5,1),a)

    def test_bad_alpha(self):
        with self.assertRaises(ValueError):CausalFilterSpec(2,1)

    def test_bad_gap(self):
        with self.assertRaises(ValueError):CausalFilterSpec(.5,0)

    def test_output_no_ppm(self):
        self.assertFalse(filter_exposure_stream(CausalFilterSpec(.5,1),rows())[0]["ppm_approved"])

if __name__=="__main__":unittest.main()
