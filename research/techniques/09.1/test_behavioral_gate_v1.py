import unittest
from robotic_olfaction_v1 import encounters
class BehavioralGate(unittest.TestCase):
    def test_no_event(self): self.assertEqual(encounters([1,2,3],0,threshold=4)["events"],[])
    def test_one_event(self): self.assertEqual(encounters([1,4,4,1],0,threshold=3)["events"],[{"start":1,"end_exclusive":3}])
    def test_two_events(self): self.assertEqual(len(encounters([4,1,4,1],0,threshold=3)["events"]),2)
    def test_event_at_start(self): self.assertEqual(encounters([4,1],0,threshold=3)["events"][0]["start"],0)
    def test_event_at_end(self): self.assertEqual(encounters([1,4],0,threshold=3)["events"][0]["end_exclusive"],2)
    def test_threshold_inclusive(self): self.assertEqual(len(encounters([3],0,threshold=3)["events"]),1)
    def test_nonzero_baseline(self): self.assertEqual(encounters([12,15],10,threshold=4)["events"],[{"start":1,"end_exclusive":2}])
    def test_empty(self):
        with self.assertRaises(ValueError): encounters([],0)
    def test_zero_threshold(self):
        with self.assertRaises(ValueError): encounters([1],0,threshold=0)
    def test_bool_threshold(self):
        with self.assertRaises(ValueError): encounters([1],0,threshold=True)
    def test_nan(self):
        with self.assertRaises(ValueError): encounters([float("nan")],0)
    def test_bool_value(self):
        with self.assertRaises(ValueError): encounters([True],0)
    def test_bad_baseline(self):
        with self.assertRaises(ValueError): encounters([1],float("inf"))
    def test_no_gas_claim(self): self.assertIn("not gas identification",encounters([1],0)["warning"])
if __name__=="__main__":unittest.main()
