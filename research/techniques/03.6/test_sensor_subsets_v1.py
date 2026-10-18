import unittest
from sensor_subsets_v1 import compare_subsets,select_subset
class SubsetTests(unittest.TestCase):
    def setUp(self):
        self.x=[[0,1],[.1,1],[10,1],[10.1,1]];self.y=["A","A","B","B"];self.ids=["MQ2","MQ3"]
    def test_number(self):self.assertEqual(len(compare_subsets(self.x,self.y,self.ids)),3)
    def test_single_only(self):self.assertEqual(len(compare_subsets(self.x,self.y,self.ids,max_size=1)),2)
    def test_two_only(self):self.assertEqual(len(compare_subsets(self.x,self.y,self.ids,min_size=2)),1)
    def test_best_nonambiguous(self):self.assertEqual(compare_subsets(self.x,self.y,self.ids)[0]["ambiguous_fraction"],0)
    def test_best_small(self):self.assertEqual(compare_subsets(self.x,self.y,self.ids)[0]["sensor_ids"],["MQ2"])
    def test_select(self):self.assertEqual(select_subset([[2,3]],compare_subsets(self.x,self.y,self.ids)[0]),[[2]])
    def test_bad_size(self):
        with self.assertRaises(ValueError):compare_subsets(self.x,self.y,self.ids,max_size=3)
    def test_bad_labels(self):
        with self.assertRaises(ValueError):compare_subsets(self.x,["A"]*4,self.ids)
    def test_short_class(self):
        with self.assertRaises(ValueError):compare_subsets(self.x,["A","A","A","B"],self.ids)
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):compare_subsets(self.x,self.y,["MQ2","MQ2"])
    def test_nan(self):
        with self.assertRaises(ValueError):compare_subsets([[0,1],[.1,1],[10,float("nan")],[10.1,1]],self.y,self.ids)
    def test_ragged(self):
        with self.assertRaises(ValueError):compare_subsets([[0],[.1,1],[10,1],[10.1,1]],self.y,self.ids)
    def test_empty(self):
        with self.assertRaises(ValueError):compare_subsets([],[],self.ids)
    def test_schema_mismatch(self):
        with self.assertRaises(ValueError):select_subset([[1]],{"columns":[1]})
    def test_constant_only(self):
        results=compare_subsets(self.x,self.y,self.ids,max_size=1)
        self.assertEqual(next(z for z in results if z["sensor_ids"]==["MQ3"])["ambiguous_fraction"],1)
    def test_deterministic(self):self.assertEqual(compare_subsets(self.x,self.y,self.ids),compare_subsets(self.x,self.y,self.ids))
if __name__=="__main__":unittest.main()
