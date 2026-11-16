import unittest
from quality_context_v2 import WindowProtocol,inspect_window


def protocol(**kw):
    d=dict(sensor_instance_id="tank-mq9-B",session_id="S1",
           expected_sampling_s=.150,max_gap_multiplier=1.5,adc_max_code=4095)
    d.update(kw);return WindowProtocol(**d)

def rows(**kw):
    arr=[{"time_s":i*.15,"adc_code":200+i,"fan_state":"ON","heater_phase":"LOW"} for i in range(6)]
    for k,v in kw.items():
        field,ix=k.rsplit("_",1);arr[int(ix)][field]=v
    return arr

class QCTests(unittest.TestCase):
    def test_nominal(self):
        x=inspect_window(protocol(),rows())
        self.assertEqual(x["flags"],[])
        self.assertTrue(x["eligible_for_features"])
        self.assertFalse(x["chemical_blank_proven"])
    def test_missing(self):
        a=rows();a[1]["adc_code"]=None
        self.assertIn("MISSING_READINGS",inspect_window(protocol(),a)["flags"])
    def test_saturated(self):
        a=rows();a[2]["adc_code"]=4095
        self.assertIn("ADC_SATURATION",inspect_window(protocol(),a)["flags"])
    def test_gap(self):
        a=rows()
        for i in range(3,6):a[i]["time_s"]+=1
        self.assertIn("SAMPLING_GAP",inspect_window(protocol(),a)["flags"])
    def test_fan_change(self):
        a=rows();a[3]["fan_state"]="OFF"
        self.assertIn("FAN_STATE_CHANGED",inspect_window(protocol(),a)["flags"])
    def test_heater_change(self):
        a=rows();a[4]["heater_phase"]="HIGH"
        self.assertIn("HEATER_PHASE_INCONSISTENT",inspect_window(protocol(),a)["flags"])
    def test_unknown_heater(self):
        a=rows();a[4]["heater_phase"]="UNKNOWN"
        self.assertIn("HEATER_PHASE_INCONSISTENT",inspect_window(protocol(),a)["flags"])
    def test_time_duplicate(self):
        a=rows();a[1]["time_s"]=a[0]["time_s"]
        with self.assertRaises(ValueError):inspect_window(protocol(),a)
    def test_bad_adc(self):
        a=rows();a[1]["adc_code"]=5000
        with self.assertRaises(ValueError):inspect_window(protocol(),a)
    def test_bad_fan(self):
        a=rows();a[0]["fan_state"]="MAYBE"
        with self.assertRaises(ValueError):inspect_window(protocol(),a)
    def test_bad_phase(self):
        a=rows();a[0]["heater_phase"]="unknown"
        with self.assertRaises(ValueError):inspect_window(protocol(),a)
    def test_insufficient(self):
        with self.assertRaises(ValueError):inspect_window(protocol(),rows()[:2])
    def test_missing_identity(self):
        with self.assertRaises(ValueError):protocol(sensor_instance_id="")
    def test_invalid_gap_multiplier(self):
        with self.assertRaises(ValueError):protocol(max_gap_multiplier=0.5)
    def test_bad_threshold(self):
        with self.assertRaises(ValueError):protocol(max_missing_fraction=-.2)
    def test_bool_code(self):
        a=rows();a[3]["adc_code"]=True
        with self.assertRaises(ValueError):inspect_window(protocol(),a)
    def test_no_gas_inference(self):
        self.assertFalse(inspect_window(protocol(),rows())["gas_concentration_approved"])

if __name__=="__main__":unittest.main()
