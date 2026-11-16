import unittest
from causal_pose_fusion_v2 import fuse_causal
S=[dict(time=i,pose_time=i,x=i,y=0,response=float(i+1)) for i in range(3)]
class FusionTests(unittest.TestCase):
 def test_causal(self):self.assertEqual([p['fused_response'] for p in fuse_causal(S)['points']],[1,1.5,2])
 def test_never_claim_ppm(self):self.assertFalse(fuse_causal(S)['ppm_approved'])
 def test_gap_reset(self):
  a=[S[0],dict(time=10,pose_time=10,x=0,y=0,response=9.)]
  self.assertEqual(fuse_causal(a)['points'][-1]['fused_response'],9.)
 def test_stale_pose(self):
  with self.assertRaises(ValueError):fuse_causal([S[0]|{'pose_time':-5}])
 def test_future_pose(self):
  with self.assertRaises(ValueError):fuse_causal([S[0]|{'pose_time':1}])
 def test_reordered(self):
  with self.assertRaises(ValueError):fuse_causal(S[::-1])
 def test_nan(self):
  with self.assertRaises(ValueError):fuse_causal([S[0]|{'response':float('nan')}])
 def test_boolean(self):
  with self.assertRaises(ValueError):fuse_causal([S[0]|{'time':True}])
 def test_delay_without_pose(self):
  with self.assertRaises(ValueError):fuse_causal(S,sensor_delay_seconds=0.5)
 def test_lag_misassignment_rejected(self):
  with self.assertRaises(ValueError):fuse_causal([S[0]|{'pose_time':-1}],sensor_delay_seconds=.5)
 def test_empty(self):
  with self.assertRaises(ValueError):fuse_causal([])
if __name__=='__main__':unittest.main()
