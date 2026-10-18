import unittest
from abstention_v1 import decide
P={"ethanol":0.85,"acetone":0.15}
class BehavioralGate(unittest.TestCase):
    def test_accept(self): self.assertEqual(decide(P)["candidate"],"ethanol")
    def test_status(self): self.assertEqual(decide(P)["status"],"TENTATIVE_CANDIDATE")
    def test_ood(self): self.assertIn("OOD",decide(P,ood=True)["reasons"])
    def test_conflict(self): self.assertIn("EVIDENCE_CONFLICT",decide(P,conflict=True)["reasons"])
    def test_invalid_measurement(self): self.assertIn("INVALID_MEASUREMENT",decide(P,valid=False)["reasons"])
    def test_unknown_candidate_abstention(self): self.assertIsNone(decide(P,ood=True)["candidate"])
    def test_low_probability(self): self.assertIn("LOW_SUPPORT",decide(P,minimum_top=.9)["reasons"])
    def test_small_margin(self): self.assertIn("LOW_MARGIN",decide(P,minimum_margin=.8)["reasons"])
    def test_high_confidence_tie_break(self): self.assertEqual(decide({"a":.5,"b":.5},minimum_top=.5,minimum_margin=0)["candidate"],"a")
    def test_multiple_flags(self): self.assertGreaterEqual(len(decide(P,ood=True,conflict=True,valid=False)["reasons"]),3)
    def test_empty(self):
        with self.assertRaises(ValueError): decide({})
    def test_negative(self):
        with self.assertRaises(ValueError): decide({"a":1.1,"b":-.1})
    def test_not_normalized(self):
        with self.assertRaises(ValueError): decide({"a":.8,"b":.3})
    def test_nonfinite(self):
        with self.assertRaises(ValueError): decide({"a":float("nan"),"b":.1})
    def test_boolean_score(self):
        with self.assertRaises(ValueError): decide({"a":True,"b":0})
    def test_invalid_threshold(self):
        with self.assertRaises(ValueError): decide(P,minimum_margin=-.2)
if __name__=="__main__": unittest.main()
