import unittest
from spatiotemporal_fusion_v1 import fuse
def row(t,response=1.,pose_t=None):
    return dict(time=t,pose_time=t if pose_t is None else pose_t,x=float(t),y=0.,response=response)
class BehavioralGate(unittest.TestCase):
    def test_count(self): self.assertEqual(len(fuse([row(0),row(1)])["points"]),2)
    def test_causal_first(self):
        p=fuse([row(0,2),row(1,4)])["points"]
        self.assertEqual(p[0]["fused_response"],2)
    def test_second_average(self): self.assertEqual(fuse([row(0,2),row(1,4)])["points"][-1]["fused_response"],3)
    def test_old_outside_window(self):
        p=fuse([row(0,2),row(3,6)],window_seconds=1)["points"]
        self.assertEqual(p[-1]["fused_response"],6)
    def test_sorting(self): self.assertEqual([p["time"] for p in fuse([row(3),row(0)])["points"]],[0,3])
    def test_pose_accepted(self): self.assertEqual(fuse([row(3,pose_t=2)])["points"][0]["x"],3)
    def test_stale_pose(self):
        with self.assertRaises(ValueError): fuse([row(3,pose_t=1)],max_pose_age_seconds=1)
    def test_future_pose(self):
        with self.assertRaises(ValueError): fuse([row(3,pose_t=4)])
    def test_empty(self):
        with self.assertRaises(ValueError): fuse([])
    def test_missing_field(self):
        with self.assertRaises(ValueError): fuse([{"time":0}])
    def test_bad_window(self):
        with self.assertRaises(ValueError): fuse([row(0)],window_seconds=0)
    def test_bool_window(self):
        with self.assertRaises(ValueError): fuse([row(0)],window_seconds=True)
    def test_bool_pose_age(self):
        with self.assertRaises(ValueError): fuse([row(0)],max_pose_age_seconds=True)
    def test_nonfinite_response(self):
        with self.assertRaises(ValueError): fuse([row(0,float("nan"))])
    def test_dont_claim_physical_lag(self): self.assertIn("does not correct sensor lag",fuse([row(0)])["warning"])
if __name__=="__main__":unittest.main()
