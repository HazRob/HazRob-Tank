"""Synthetic negative tests of the predeclared source-domain benchmark."""
import unittest
from uci1081_g2_multichannel_open_set import evaluate_one, SENSOR_SETS, DISTANCES

def row(i,gas,x):
    return {"exposure_id":str(i),"gas":gas,"features":tuple(x for _ in range(10))}

def parts():
    train=[row(0,"A",0.),row(1,"A",.1),row(2,"B",10.),row(3,"B",10.1)]
    cal=[row(i,"A" if i%2==0 else "B",.02 if i%2==0 else 10.02) for i in range(4,19)]
    known=[row(19,"A",.03),row(20,"B",10.03)]
    unknown=[row(21,"C",100.),row(22,"C",110.)]
    return train,cal,known,unknown

class Tests(unittest.TestCase):
    def test_preset_configurations(self):
        self.assertEqual(len(SENSOR_SETS)*len(DISTANCES),6)
    def test_known_calibration_without_unknown_tuning(self):
        r=evaluate_one(*parts(),(4,),"EUCLIDEAN")
        self.assertEqual(r["unknown_reject"],2)
        self.assertEqual(r["known_accept"],2)
    def test_source_exposure_leak(self):
        a,b,c,d=parts();d[0]["exposure_id"]=a[0]["exposure_id"]
        with self.assertRaises(ValueError):evaluate_one(a,b,c,d,(4,),"EUCLIDEAN")
    def test_unknown_class_leak(self):
        a,b,c,d=parts();d[0]["gas"]="A"
        with self.assertRaises(ValueError):evaluate_one(a,b,c,d,(4,),"EUCLIDEAN")
    def test_invalid_metric(self):
        with self.assertRaises(ValueError):evaluate_one(*parts(),(4,),"UNKNOWN")
    def test_nonfinite_sensor(self):
        a,b,c,d=parts();a[0]["features"]=(float("nan"),)*10
        with self.assertRaises(ValueError):evaluate_one(a,b,c,d,(4,),"EUCLIDEAN")
    def test_wrong_sensor_layout(self):
        a,b,c,d=parts();a[0]["features"]=(0.,)
        with self.assertRaises(ValueError):evaluate_one(a,b,c,d,(4,),"EUCLIDEAN")
    def test_duplicate_channels(self):
        with self.assertRaises(ValueError):evaluate_one(*parts(),(4,4),"EUCLIDEAN")

if __name__=="__main__":unittest.main()
