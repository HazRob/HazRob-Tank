import json,unittest
from g2_holdout_guard import check,unique
def row(v,p):
 return dict(raw_sha256=v*64,source_id=v,device_id=v,session_id=v,exposure_series_id=v,calibration_episode_id=v,partition=p)
def doc(rows): return dict(schema="hazrob.g2.holdout-split.v1",rows=rows)
class Tests(unittest.TestCase):
 def test_disjoint(self): self.assertEqual(check(doc([row("a","train"),row("b","test")]))["status"],"GROUP_PREFLIGHT_ONLY")
 def test_leakage(self): self.assertEqual(check(doc([row("a","train"),row("a","test")]))["status"],"REJECTED")
 def test_empty(self): self.assertEqual(check(doc([]))["status"],"REJECTED")
 def test_missing(self): self.assertEqual(check(doc([dict(row("a","test"),source_id="")]))["status"],"REJECTED")
 def test_bad_partition(self): self.assertEqual(check(doc([row("a",[])]))["status"],"REJECTED")
 def test_duplicate(self):
  with self.assertRaises(ValueError): json.loads('{"x":1,"x":2}',object_pairs_hook=unique)
if __name__=="__main__": unittest.main()
