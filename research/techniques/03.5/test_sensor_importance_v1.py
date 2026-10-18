import unittest
from sensor_importance_v1 import rank_sensor_importance,select_sensor_columns
class SensorImportanceTests(unittest.TestCase):
    def setUp(self):
        self.x=[[0,1,5],[0,2,5],[10,1,5],[10,2,5]]
        self.y=["A","A","B","B"];self.ids=["MQ2","MQ3","MQ4"]
    def test_top(self):self.assertEqual(rank_sensor_importance(self.x,self.y,self.ids)[0]["sensor_id"],"MQ2")
    def test_constant(self):
        r=rank_sensor_importance(self.x,self.y,self.ids)
        self.assertEqual(r[-1]["fisher_score"],0)
    def test_inf(self):self.assertEqual(rank_sensor_importance(self.x,self.y,self.ids)[0]["fisher_score"],float("inf"))
    def test_order(self):self.assertEqual(rank_sensor_importance(self.x,self.y,self.ids)[0]["column"],0)
    def test_projection(self):self.assertEqual(select_sensor_columns(self.x,rank_sensor_importance(self.x,self.y,self.ids),1),[[0],[0],[10],[10]])
    def test_holdout(self):
        r=rank_sensor_importance(self.x,self.y,self.ids)
        self.assertEqual(select_sensor_columns([[99,3,5]],r,1),[[99]])
    def test_duplicate_ids(self):
        with self.assertRaises(ValueError):rank_sensor_importance(self.x,self.y,["MQ2","MQ2","MQ4"])
    def test_bad_class(self):
        with self.assertRaises(ValueError):rank_sensor_importance(self.x,["A"]*4,self.ids)
    def test_undersampled(self):
        with self.assertRaises(ValueError):rank_sensor_importance(self.x,["A","A","A","B"],self.ids)
    def test_nan(self):
        with self.assertRaises(ValueError):rank_sensor_importance([[0,1,5],[0,2,5],[10,float("nan"),5],[10,2,5]],self.y,self.ids)
    def test_ragged(self):
        with self.assertRaises(ValueError):rank_sensor_importance([[0,1],[0,2,5],[10,1,5],[10,2,5]],self.y,self.ids)
    def test_bad_k(self):
        with self.assertRaises(ValueError):select_sensor_columns(self.x,rank_sensor_importance(self.x,self.y,self.ids),0)
    def test_bad_schema(self):
        with self.assertRaises(ValueError):select_sensor_columns([[1]],rank_sensor_importance(self.x,self.y,self.ids),2)
    def test_nonstring_labels(self):
        with self.assertRaises(ValueError):rank_sensor_importance(self.x,[1,1,2,2],self.ids)
if __name__=="__main__":unittest.main()
