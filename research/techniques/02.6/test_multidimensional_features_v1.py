import unittest
from multidimensional_features_v1 import build_trajectory,trajectory_path_length
class MultidimensionalTests(unittest.TestCase):
    def setUp(self):
        self.t=[0,1,2];self.x=[[0,.1,.2],[0,-.1,-.2]];self.ids=["MQ2","MQ3"]
    def test_3d(self): self.assertEqual(build_trajectory(self.t,self.x,self.ids)["dimensions"],3)
    def test_4d(self): self.assertEqual(build_trajectory(self.t,self.x,self.ids,environment=[20,21,22])["dimensions"],4)
    def test_point(self): self.assertEqual(build_trajectory(self.t,self.x,self.ids)["points"][1],(1,.1,-.1))
    def test_order(self): self.assertEqual(build_trajectory(self.t,self.x,self.ids)["sensor_ids"],tuple(self.ids))
    def test_env_point(self): self.assertEqual(build_trajectory(self.t,self.x,self.ids,environment=[20,21,22])["points"][0],(0,0,0,20))
    def test_duplicate_id(self):
        with self.assertRaises(ValueError): build_trajectory(self.t,self.x,["MQ2","MQ2"])
    def test_short(self):
        with self.assertRaises(ValueError): build_trajectory([0],[[0],[0]],self.ids)
    def test_nonmonotonic(self):
        with self.assertRaises(ValueError): build_trajectory([0,1,1],self.x,self.ids)
    def test_ragged(self):
        with self.assertRaises(ValueError): build_trajectory(self.t,[[0],[0,1,2]],self.ids)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): build_trajectory(self.t,[[0,float("nan"),2],[0,1,2]],self.ids)
    def test_bad_environment(self):
        with self.assertRaises(ValueError): build_trajectory(self.t,self.x,self.ids,environment=[20])
    def test_length(self): self.assertAlmostEqual(trajectory_path_length([(0,0,0),(3,4,0)],scales=[1,1,1]),5)
    def test_scaled_length(self): self.assertAlmostEqual(trajectory_path_length([(0,0),(2,4)],scales=[2,4]),2**.5)
    def test_zero_scale(self):
        with self.assertRaises(ValueError): trajectory_path_length([(0,0),(1,1)],scales=[1,0])
    def test_shape(self):
        with self.assertRaises(ValueError): trajectory_path_length([(0,0),(1,1)],scales=[1])
    def test_unchanged(self): self.assertEqual(trajectory_path_length([(0,0),(0,0)],scales=[1,1]),0)
if __name__=="__main__": unittest.main()
