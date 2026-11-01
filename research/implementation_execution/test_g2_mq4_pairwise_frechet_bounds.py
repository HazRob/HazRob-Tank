"""Deterministic fail-closed tests for sharp MQ4 pairwise bounds."""
import hashlib,itertools,json,sys,unittest
from fractions import Fraction
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from g2_mq4_pairwise_frechet_bounds import SOURCE,EVIDENCE,SHA,NAMES,P,N,bounds,project,rational
class PairBoundsTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
    cls.raw=SOURCE.read_bytes();cls.ev=EVIDENCE.read_bytes();cls.out=project(cls.raw,cls.ev)
 def test_sha(self): self.assertEqual(hashlib.sha256(self.raw).hexdigest(),SHA)
 def test_pinned(self):
    self.assertEqual(self.out,json.loads(SOURCE.with_name("G2_MQ4_PAIRWISE_FRECHET_BOUNDS_2026-11-01.json").read_text()))
 def test_ten_pairs(self): self.assertEqual(len(self.out["pairs"]),10)
 def test_pairs_unique(self):
    self.assertEqual([x["models"] for x in self.out["pairs"]],[list(x) for x in itertools.combinations(NAMES,2)])
 def test_ranges(self):
    for row in self.out["pairs"]:
     for mode in ("OR","AND"):
      x=row[mode]
      for k in ("tp_range","fp_range","fn_range","tn_range"): self.assertLessEqual(x[k][0],x[k][1])
      self.assertEqual(x["fn_range"],[P-x["tp_range"][1],P-x["tp_range"][0]])
      self.assertEqual(x["tn_range"],[N-x["fp_range"][1],N-x["fp_range"][0]])
      self.assertEqual(x["sensitivity_range"],[rational(v,P) for v in x["tp_range"]])
      self.assertEqual(x["false_positive_rate_range"],[rational(v,N) for v in x["fp_range"]])
 def test_or_not_less_than_best_tp(self):
    for row in self.out["pairs"]:
     ms=[next(m for m in json.loads(self.raw)["models"] if m["model"]==n) for n in row["models"]]
     self.assertEqual(row["OR"]["tp_range"][0],max(m["total_4ppm_true_positive"] for m in ms))
 def test_and_not_more_than_min_fp(self):
    for row in self.out["pairs"]:
     ms=[next(m for m in json.loads(self.raw)["models"] if m["model"]==n) for n in row["models"]]
     self.assertEqual(row["AND"]["fp_range"][1],min(m["total_4ppm_false_positive"] for m in ms))
 def test_ridge_hgb_or(self):
    row=self.out["pairs"][-1];self.assertEqual(row["models"],["RIDGE_MQ4_3_T_RH","HGB_MQ4_3_T_RH"])
    self.assertEqual(row["OR"]["fn_range"],[511,1044])
    self.assertEqual(row["OR"]["fp_range"],[1044,1831])
 def test_ridge_hgb_and(self):
    row=self.out["pairs"][-1]
    self.assertEqual(row["AND"]["tp_range"],[0,533])
    self.assertEqual(row["AND"]["fp_range"],[0,787])
 def test_baseline_or(self):
    for row in self.out["pairs"][:4]:
     m=next(m for m in json.loads(self.raw)["models"] if m["model"]==row["models"][1])
     self.assertEqual(row["OR"]["tp_range"],[m["total_4ppm_true_positive"]]*2)
 def test_baseline_and(self):
    for row in self.out["pairs"][:4]:
     self.assertEqual(row["AND"]["tp_range"],[0,0])
     self.assertEqual(row["AND"]["fp_range"],[0,0])
 def test_exhaustive_sharpness(self):
    for a in range(4):
     for b in range(4):
      sa=[set(x) for x in itertools.combinations(range(3),a)]
      sb=[set(x) for x in itertools.combinations(range(3),b)]
      for mode in ("OR","AND"):
       vals=[len((x|y) if mode=="OR" else (x&y)) for x in sa for y in sb]
       out=bounds((a,3-a,a,3-a),(b,3-b,b,3-b),3,3,mode)
       self.assertEqual(out["tp_range"],[min(vals),max(vals)])
       self.assertEqual(out["fp_range"],[min(vals),max(vals)])
 def test_reject_boolean(self):
    with self.assertRaisesRegex(ValueError,"BAD_COUNTS"):
     bounds((True,2,0,3),(1,2,0,3),3,3,"OR")
 def test_reject_bad_denominator(self):
    with self.assertRaisesRegex(ValueError,"BAD_DENOMINATORS"):
     bounds((1,1,0,3),(1,2,0,3),3,3,"OR")
 def test_reject_bad_mode(self):
    with self.assertRaisesRegex(ValueError,"BAD_MODE"):
     bounds((1,2,0,3),(1,2,0,3),3,3,"XOR")
 def test_reject_tamper(self):
    with self.assertRaisesRegex(ValueError,"SOURCE_SHA_MISMATCH"):
     project(self.raw+b" ",self.ev)
 def test_reject_unverified_evidence(self):
    e=json.loads(self.ev);e["records"]=[{"unverified":True}]
    with self.assertRaisesRegex(ValueError,"UNVERIFIED_INDEPENDENT_EVIDENCE"):
     project(self.raw,json.dumps(e).encode())
 def test_duplicate_evidence_records_fail_closed(self):
    malicious=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[{"unverified":true}],"records":[]}'
    with self.assertRaisesRegex(ValueError,"DUPLICATE_JSON_KEY"):
     project(self.raw,malicious)
 def test_nested_duplicate_evidence_key_fail_closed(self):
    malicious=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[],"meta":{"source":"a","source":"b"}}'
    with self.assertRaisesRegex(ValueError,"DUPLICATE_JSON_KEY"):
     project(self.raw,malicious)
 def test_nonfinite_nan_evidence_fail_closed(self):
    malicious=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[],"meta":NaN}'
    with self.assertRaisesRegex(ValueError,"NONFINITE_JSON_NUMBER"):
     project(self.raw,malicious)
 def test_nonfinite_infinity_evidence_fail_closed(self):
    malicious=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[],"meta":Infinity}'
    with self.assertRaisesRegex(ValueError,"NONFINITE_JSON_NUMBER"):
     project(self.raw,malicious)
 def test_nonfinite_exponent_evidence_fail_closed(self):
    malicious=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[],"meta":1e10000}'
    with self.assertRaisesRegex(ValueError,"NONFINITE_JSON_NUMBER"):
     project(self.raw,malicious)
 def test_reordered_valid_evidence_same_result(self):
    valid=b'{"records":[],"schema":"hazrob.g2.independent-evidence.v1"}'
    self.assertEqual(project(self.raw,valid),self.out)
 def test_nonobject_evidence_fail_closed(self):
    with self.assertRaisesRegex(ValueError,"INVALID_JSON_OBJECT"):
     project(self.raw,b'[]')
 def test_nonempty_evidence_never_qualified(self):
    valid=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[{"source":"unverified"}]}'
    with self.assertRaisesRegex(ValueError,"UNVERIFIED_INDEPENDENT_EVIDENCE"):
     project(self.raw,valid)
 def test_no_promotion(self):
    self.assertFalse(self.out["g2_qualified"]);self.assertFalse(self.out["g3_qualified"])
    self.assertFalse(self.out["prospective_or_tank_validation"])
    self.assertFalse(self.out["threshold_is_safety_limit"])
    self.assertFalse(self.out["joint_ensemble_performance_measured"])
    self.assertEqual(self.out["independent_physical_evidence_records"],0)
if __name__=="__main__":unittest.main()
