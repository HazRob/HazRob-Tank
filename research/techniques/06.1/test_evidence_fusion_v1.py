import unittest
from evidence_fusion_v1 import fuse_evidence
P={"a":.5,"b":.5}
def s(a,group="g",weight=1):return {"group":group,"weight":weight,"probabilities":{"a":a,"b":1-a}}
class EvidenceTests(unittest.TestCase):
    def test_empty(self):self.assertEqual(fuse_evidence([],prior=P)["status"],"ABSTAIN")
    def test_single_group(self):self.assertEqual(fuse_evidence([s(.9)],prior=P)["reason"],"INSUFFICIENT_INDEPENDENT_GROUPS")
    def test_two_groups(self):self.assertEqual(fuse_evidence([s(.9),s(.8,"h")],prior=P)["ranking"][0],"a")
    def test_normalized(self):self.assertAlmostEqual(sum(fuse_evidence([s(.9),s(.8,"h")],prior=P)["scores"].values()),1)
    def test_conflict(self):self.assertAlmostEqual(fuse_evidence([s(.9),s(.1,"h")],prior=P)["scores"]["a"],.5)
    def test_duplicate_group_cap(self):
        a=fuse_evidence([s(.9),s(.9),s(.8,"h")],prior=P)
        b=fuse_evidence([s(.9),s(.8,"h")],prior=P)
        self.assertAlmostEqual(a["scores"]["a"],b["scores"]["a"])
    def test_count(self):self.assertEqual(fuse_evidence([s(.9),s(.8,"h")],prior=P)["n_groups"],2)
    def test_deterministic(self):self.assertEqual(fuse_evidence([s(.9),s(.8,"h")],prior=P),fuse_evidence([s(.9),s(.8,"h")],prior=P))
    def test_missing_candidate(self):
        with self.assertRaises(ValueError):fuse_evidence([{"group":"g","probabilities":{"a":1}},s(.8,"h")],prior=P)
    def test_zero_probability(self):
        with self.assertRaises(ValueError):fuse_evidence([s(1),s(.8,"h")],prior=P)
    def test_bad_prior(self):
        with self.assertRaises(ValueError):fuse_evidence([],prior={"a":.2,"b":.2})
    def test_bad_group(self):
        with self.assertRaises(ValueError):fuse_evidence([s(.9,group=""),s(.8,"h")],prior=P)
    def test_bad_weight(self):
        with self.assertRaises(ValueError):fuse_evidence([s(.9,weight=0),s(.8,"h")],prior=P)
    def test_nan(self):
        with self.assertRaises(ValueError):fuse_evidence([s(float("nan")),s(.8,"h")],prior=P)
    def test_bad_cap(self):
        with self.assertRaises(ValueError):fuse_evidence([s(.9),s(.8,"h")],prior=P,max_group_weight=0)
    def test_three_groups(self):self.assertEqual(fuse_evidence([s(.8),s(.8,"h"),s(.8,"j")],prior=P)["n_groups"],3)
if __name__=="__main__":unittest.main()
