import unittest
from gnn_v1 import aggregate
X=[[1.],[3.],[5.]]
class BehavioralGate(unittest.TestCase):
    def test_self_aggregation(self): self.assertEqual(aggregate(X,[]),X)
    def test_edge_mean(self): self.assertEqual(aggregate(X,[(0,1)]),[[2.],[2.],[5.]])
    def test_symmetric(self): self.assertEqual(aggregate(X,[(1,0)]),aggregate(X,[(0,1)]))
    def test_duplicate_no_double(self): self.assertEqual(aggregate(X,[(0,1),(0,1)]),aggregate(X,[(0,1)]))
    def test_no_self(self): self.assertEqual(aggregate(X,[(0,1)],include_self=False),[[3.],[1.],[0.]])
    def test_full_graph(self): self.assertEqual(aggregate(X,[(0,1),(0,2),(1,2)]),[[3.],[3.],[3.]])
    def test_multifeature(self): self.assertEqual(aggregate([[1,3],[5,7]],[(0,1)]),[[3.,5.],[3.,5.]])
    def test_empty_nodes(self):
        with self.assertRaises(ValueError): aggregate([],[])
    def test_ragged(self):
        with self.assertRaises(ValueError): aggregate([[1],[1,2]],[])
    def test_nan(self):
        with self.assertRaises(ValueError): aggregate([[1],[float("nan")]],[])
    def test_bool_feature(self):
        with self.assertRaises(ValueError): aggregate([[True]],[])
    def test_bad_edge(self):
        with self.assertRaises(ValueError): aggregate(X,[(0,3)])
    def test_bool_vertex(self):
        with self.assertRaises(ValueError): aggregate(X,[(True,1)])
    def test_bad_edge_shape(self):
        with self.assertRaises(ValueError): aggregate(X,[(0,)])
if __name__=="__main__": unittest.main()
