import unittest
from feature_selection_v1 import rank_features,select_indices,project
class SelectionTests(unittest.TestCase):
    def setUp(self):
        self.x=[[0,1,5],[0,2,5],[10,1,5],[10,2,5]]
        self.y=["A","A","B","B"]
        self.names=["signal","noise","constant"]
    def test_best(self): self.assertEqual(rank_features(self.x,self.y,self.names)[0]["feature"],"signal")
    def test_constant(self):
        r=rank_features(self.x,self.y,self.names)
        self.assertEqual(next(z for z in r if z["feature"]=="constant")["score"],0)
    def test_inf(self): self.assertEqual(rank_features(self.x,self.y,self.names)[0]["score"],float("inf"))
    def test_indices(self): self.assertEqual(select_indices(rank_features(self.x,self.y,self.names),1),(0,))
    def test_project(self): self.assertEqual(project(self.x,[0]),[[0],[0],[10],[10]])
    def test_train_only(self):
        rank=rank_features(self.x,self.y,self.names)
        self.assertEqual(project([[99,0,0]],select_indices(rank,1)),[[99]])
    def test_missing_class(self):
        with self.assertRaises(ValueError): rank_features(self.x,["A"]*4,self.names)
    def test_one_sample(self):
        with self.assertRaises(ValueError): rank_features(self.x[:3],self.y[:3],self.names)
    def test_nan(self):
        with self.assertRaises(ValueError): rank_features([[0,float("nan")],[0,1],[1,1],[1,2]],self.y,["a","b"])
    def test_ragged(self):
        with self.assertRaises(ValueError): rank_features([[0],[0,1],[1],[1]],self.y,["a"])
    def test_duplicate_names(self):
        with self.assertRaises(ValueError): rank_features(self.x,self.y,["x","x","z"])
    def test_invalid_k(self):
        with self.assertRaises(ValueError): select_indices(rank_features(self.x,self.y,self.names),0)
    def test_invalid_index(self):
        with self.assertRaises(ValueError): project(self.x,[99])
    def test_tie_order(self):
        x=[[0,0],[0,0],[1,1],[1,1]]
        self.assertEqual([z["index"] for z in rank_features(x,self.y,["a","b"])],[0,1])
if __name__=="__main__": unittest.main()
