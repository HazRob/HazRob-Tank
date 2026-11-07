#!/usr/bin/env python3
"""Research-only holdout group leakage preflight; never approves G2."""
import json,re,sys,unicodedata
from collections import defaultdict
KEYS=("raw_sha256","source_id","device_id","session_id","exposure_series_id","calibration_episode_id")
def unique(pairs):
 d={}
 for k,v in pairs:
  if k in d: raise ValueError("duplicate key "+k)
  d[k]=v
 return d
def check(doc):
 if not isinstance(doc,dict) or doc.get("schema")!="hazrob.g2.holdout-split.v1" or not isinstance(doc.get("rows"),list):
  return dict(status="INVALID_SCHEMA",g2_approved=False)
 errors=[]; groups=defaultdict(lambda:defaultdict(set)); seen_raw=set(); valid_partitions=set()
 for i,row in enumerate(doc["rows"]):
  if not isinstance(row,dict): errors.append("nonobject row "+str(i));continue
  p=row.get("partition")
  if not isinstance(p,str) or p not in ("train","validation","test"): errors.append("invalid partition "+str(i));continue
  row_valid=True
  for k in KEYS:
   v=row.get(k)
   if not isinstance(v,str) or not v.strip() or (k=="raw_sha256" and not re.fullmatch("[0-9a-f]{64}",v)):
    errors.append("missing/invalid "+k+" row "+str(i));row_valid=False;continue
   if v != v.strip():
    errors.append("noncanonical whitespace "+k+" row "+str(i));row_valid=False
   if unicodedata.normalize("NFKC",v) != v:
    errors.append("noncanonical unicode "+k+" row "+str(i));row_valid=False
   if any(unicodedata.category(c) in ('Cc','Cf') for c in v):
    errors.append("control character "+k+" row "+str(i));row_valid=False
   if k=="raw_sha256":
    if v in seen_raw: errors.append("duplicate raw_sha256 row "+str(i))
    seen_raw.add(v)
   groups[k][v.strip()].add(p)
  if row_valid: valid_partitions.add(p)
 for k in KEYS:
  for v,ps in sorted(groups[k].items()):
   if len(ps)>1: errors.append("leakage "+k+"="+v)
 if not doc["rows"]: errors.append("no records")
 if "train" not in valid_partitions: errors.append("no train records")
 if "test" not in valid_partitions: errors.append("no sealed test")
 return dict(status="REJECTED" if errors else "GROUP_PREFLIGHT_ONLY",errors=errors,g2_approved=False)
if __name__=="__main__":
 try:
  with open(sys.argv[1],encoding="utf-8") as f: data=json.load(f,object_pairs_hook=unique,parse_constant=lambda x: (_ for _ in ()).throw(ValueError(x)))
  result=check(data)
 except (OSError,ValueError,IndexError) as e: result=dict(status="INPUT_ERROR",errors=[str(e)],g2_approved=False)
 print(json.dumps(result,sort_keys=True))
 sys.exit(0 if result["status"]=="GROUP_PREFLIGHT_ONLY" else 2)
