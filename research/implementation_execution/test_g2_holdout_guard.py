import json,unittest,subprocess,sys,tempfile
from pathlib import Path
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
 def test_each_group_key_leakage(self):
  for key in ("source_id","device_id","session_id","exposure_series_id","calibration_episode_id"):
   with self.subTest(key=key):
    train=row("a","train")
    test=row("b","test")
    test[key]=train[key]
    result=check(doc([train,test]))
    self.assertEqual(result["status"],"REJECTED")
    self.assertTrue(any(e.startswith("leakage "+key+"=") for e in result["errors"]))
 def test_whitespace_alias_cannot_bypass_group_leakage(self):
  train=row("a","train")
  test=row("b","test")
  test["device_id"]=" "+train["device_id"]+" "
  result=check(doc([train,test]))
  self.assertEqual(result["status"],"REJECTED")
  self.assertTrue(any(e.startswith("leakage device_id=") for e in result["errors"]))
 def test_whitespace_group_identifier_rejected_even_without_leakage(self):
  train=row("a","train")
  train["source_id"]=" source "
  result=check(doc([train,row("b","test")]))
  self.assertEqual(result["status"],"REJECTED")
  self.assertIn("no train records",result["errors"])
 def test_control_characters_in_group_identifiers_rejected(self):
  for char in ("\x00","\n","\t","\x7f"):
   for key in ("source_id","device_id","session_id","exposure_series_id","calibration_episode_id"):
    with self.subTest(char=repr(char),key=key):
     train=row("a","train")
     train[key]="prefix"+char+"suffix"
     result=check(doc([train,row("b","test")]))
     self.assertEqual(result["status"],"REJECTED")
     self.assertTrue(any(e.startswith("control character "+key) for e in result["errors"]))
     self.assertIn("no train records",result["errors"])
 def test_invisible_unicode_format_characters_rejected(self):
  for char in ("\u200b","\u200d","\u2060","\ufeff"):
   for key in ("source_id","device_id","session_id","exposure_series_id","calibration_episode_id"):
    with self.subTest(char=repr(char),key=key):
     train=row("a","train")
     train[key]="source"+char+"identifier"
     result=check(doc([train,row("b","test")]))
     self.assertEqual(result["status"],"REJECTED")
     self.assertTrue(any(e.startswith("control character "+key) for e in result["errors"]))
     self.assertIn("no train records",result["errors"])
 def test_same_source_within_train_is_allowed(self):
  a=row("a","train")
  b=row("b","train")
  b["source_id"]=a["source_id"]
  result=check(doc([a,b,row("c","test")]))
  self.assertEqual(result["status"],"GROUP_PREFLIGHT_ONLY")
 def test_invalid_sha256_rejected(self):
  bad=row("a","train")
  bad["raw_sha256"]="A"*64
  result=check(doc([bad,row("b","test")]))
  self.assertEqual(result["status"],"REJECTED")
  self.assertIn("no train records",result["errors"])
 def test_nonobject_row_rejected(self):
  result=check(doc([row("a","train"),None,row("b","test")]))
  self.assertEqual(result["status"],"REJECTED")
 def test_invalid_schema_never_approves(self):
  for payload in (None,{},{"schema":"other","rows":[]},{"schema":"hazrob.g2.holdout-split.v1","rows":{}}):
   with self.subTest(payload=payload):
    result=check(payload)
    self.assertEqual(result["status"],"INVALID_SCHEMA")
    self.assertFalse(result["g2_approved"])
 def test_duplicate(self):
  with self.assertRaises(ValueError): json.loads('{"x":1,"x":2}',object_pairs_hook=unique)
class CliContractTests(unittest.TestCase):
 def run_cli(self, payload):
  with tempfile.TemporaryDirectory() as d:
   path=Path(d)/"split.json"
   path.write_text(payload,encoding="utf-8")
   proc=subprocess.run([sys.executable,str(Path(__file__).with_name("g2_holdout_guard.py")),str(path)],capture_output=True,text=True,check=False)
   return proc,json.loads(proc.stdout)
 def test_cli_disjoint_preflight_only(self):
  proc,result=self.run_cli(json.dumps(doc([row("a","train"),row("b","test")])))
  self.assertEqual(proc.returncode,0)
  self.assertEqual(result["status"],"GROUP_PREFLIGHT_ONLY")
  self.assertIs(result["g2_approved"],False)
 def test_cli_leakage_fails_closed(self):
  proc,result=self.run_cli(json.dumps(doc([row("a","train"),row("a","test")])))
  self.assertEqual(proc.returncode,2)
  self.assertEqual(result["status"],"REJECTED")
  self.assertIs(result["g2_approved"],False)
 def test_cli_duplicate_json_keys_fails_closed(self):
  proc,result=self.run_cli('{"schema":"hazrob.g2.holdout-split.v1","rows":[],"rows":[]}')
  self.assertEqual(proc.returncode,2)
  self.assertEqual(result["status"],"INPUT_ERROR")
  self.assertIs(result["g2_approved"],False)
 def test_cli_nonfinite_json_fails_closed(self):
  proc,result=self.run_cli('{"schema":"hazrob.g2.holdout-split.v1","rows":[NaN]}')
  self.assertEqual(proc.returncode,2)
  self.assertEqual(result["status"],"INPUT_ERROR")
  self.assertIs(result["g2_approved"],False)
if __name__=="__main__": unittest.main()
