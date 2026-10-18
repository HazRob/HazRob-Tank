import unittest
from heater_cycles_v1 import HeaterProfile,classify_phase,tag_samples

class HeaterTests(unittest.TestCase):
    def setUp(self): self.p=HeaterProfile("synthetic",60,90,5)
    def test_start(self): self.assertEqual(classify_phase(self.p,0)["phase"],"HIGH")
    def test_transition(self): self.assertEqual(classify_phase(self.p,60)["phase"],"LOW")
    def test_wrap(self): self.assertEqual(classify_phase(self.p,150)["phase"],"HIGH")
    def test_settling(self): self.assertFalse(classify_phase(self.p,62)["eligible"])
    def test_eligible(self): self.assertTrue(classify_phase(self.p,65)["eligible"])
    def test_high_eligible(self): self.assertTrue(classify_phase(self.p,5)["eligible"])
    def test_negative_time(self):
        with self.assertRaises(ValueError): classify_phase(self.p,-1)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): classify_phase(self.p,float("nan"))
    def test_invalid_period(self):
        with self.assertRaises(ValueError): HeaterProfile("bad",0,90)
    def test_invalid_settle(self):
        with self.assertRaises(ValueError): HeaterProfile("bad",60,90,60)
    def test_preserves(self):
        tagged=tag_samples(self.p,[(0,10),(60,20)])
        self.assertEqual([x["value"] for x in tagged],[10,20])
    def test_long_elapsed(self): self.assertEqual(classify_phase(self.p,210)["phase"],"LOW")

if __name__=="__main__": unittest.main()
