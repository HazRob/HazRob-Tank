import json,unittest
from g2_holdout_guard import check,unique
def row(v,p):
 return dict(raw_sha256=v*64,source_id=v,device_id=v,session_id=v,exposure_series_id=v,calibration_episode_id=v,partition=p)
def doc(rows): return dict(schema="hazrob.g2.holdout-split.v1",rows=rows)
class Tests(unittest.TestCase):
 def test_disjoint(self): self.assertEqual(check(doc([row("a","train"),row("b","test")]))["status"],"GROUP_PREFLIGHT_ONLY")
 def test_test_only_rejected(self): self.assertEqual(check(doc([row("a","test")]))["status"],"REJECTED")
 def test_train_only_rejected(self): self.assertEqual(check(doc([row("a","train")]))["status"],"REJECTED")
 def test_leakage(self): self.assertEqual(check(doc([row("a","train"),row("a","test")]))["status"],"REJECTED")
 def test_duplicate_raw_same_partition(self):
  self.assertEqual(check(doc([row("a","train"),row("a","train"),row("b","test")]))["status"],"REJECTED")
 def test_duplicate_raw_cross_partition(self):
  self.assertEqual(check(doc([row("a","train"),row("a","test")]))["status"],"REJECTED")
 def test_empty(self): self.assertEqual(check(doc([]))["status"],"REJECTED")
 def test_missing(self): self.assertEqual(check(doc([dict(row("a","test"),source_id="")]))["status"],"REJECTED")
 def test_bad_partition(self): self.assertEqual(check(doc([row("a",[])]))["status"],"REJECTED")
 def test_invalid_train_does_not_count_as_train(self):
  result=check(doc([dict(row("a","train"),device_id=""),row("b","test")]))
  self.assertEqual(result["status"],"REJECTED")
  self.assertIn("no train records",result["errors"])
 def test_invalid_test_does_not_count_as_test(self):
  result=check(doc([row("a","train"),dict(row("b","test"),session_id="")]))
  self.assertEqual(result["status"],"REJECTED")
  self.assertIn("no sealed test",result["errors"])
 def test_duplicate(self):
  with self.assertRaises(ValueError): json.loads('{"x":1,"x":2}',object_pairs_hook=unique)
if __name__=="__main__": unittest.main()
