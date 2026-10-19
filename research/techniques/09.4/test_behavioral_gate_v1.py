import unittest
from active_sampling_v1 import rank_waypoints
def w(x=0,y=0,u=1,r=0): return dict(x=x,y=y,uncertainty=u,risk=r)
class BehavioralGate(unittest.TestCase):
    def test_priority(self): self.assertEqual(rank_waypoints([w(u=.5),w(u=.9)])[0]["index"],1)
    def test_risk_dampens(self): self.assertEqual(rank_waypoints([w(u=.9,r=1),w(u=.5,r=0)])[0]["index"],1)
    def test_zero_risk(self): self.assertEqual(rank_waypoints([w(u=.5)])[0]["priority"],.5)
    def test_full_risk(self): self.assertEqual(rank_waypoints([w(u=1,r=1)])[0]["priority"],0)
    def test_tie_order(self): self.assertEqual([v["index"] for v in rank_waypoints([w(),w()])],[0,1])
    def test_empty(self): self.assertEqual(rank_waypoints([]),[])
    def test_xy_preserved(self): self.assertEqual(rank_waypoints([w(x=2,y=3)])[0]["x"],2)
    def test_missing_field(self):
        with self.assertRaises(ValueError): rank_waypoints([{"x":1}])
    def test_bad_uncertainty(self):
        with self.assertRaises(ValueError): rank_waypoints([w(u=-.1)])
    def test_bad_risk(self):
        with self.assertRaises(ValueError): rank_waypoints([w(r=1.1)])
    def test_nan(self):
        with self.assertRaises(ValueError): rank_waypoints([w(u=float("nan"))])
    def test_bool_coordinate(self):
        with self.assertRaises(ValueError): rank_waypoints([w(x=True)])
    def test_bool_risk(self):
        with self.assertRaises(ValueError): rank_waypoints([w(r=True)])
    def test_only_ranked(self): self.assertNotIn("motor_command",rank_waypoints([w()])[0])
if __name__=="__main__":unittest.main()
