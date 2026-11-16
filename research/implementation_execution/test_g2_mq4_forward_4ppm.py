"""Synthetic-only CI checks; original SHA-pinned ZIP is not stored in GitHub."""
import json,unittest
from pathlib import Path
from g2_mq4_forward_4ppm import DAYS,MODELS,plan,score
R=Path(__file__).with_name('G2_MQ4_FORWARD_4PPM_2026-11-01.json')
class Tests(unittest.TestCase):
 def test_no_future_fit(self):
  f=plan();self.assertEqual(len(f),7)
  for x in f:
   self.assertTrue(all(d<x['cal']<x['test'] for d in x['train']))
 def test_bad_days(self):
  for x in [DAYS[::-1],DAYS[:3]+DAYS[4:],DAYS[:-1]+[DAYS[-2]]]:
   with self.assertRaises(ValueError):plan(x)
 def test_zero_positive_not_perfect_sensitivity(self):
  x=score([2,3],[2,5],1);self.assertEqual((x['tp'],x['fn'],x['fp'],x['tn']),(0,0,1,1))
 def test_threshold_boundary(self):
  x=score([4,5,2],[4,3,4],1);self.assertEqual((x['tp'],x['fn'],x['fp'],x['tn']),(1,1,1,0))
 def test_bad_scores(self):
  for t,p,q in [([2],[float('nan')],1),([2],[2],-1),([2],[2,3],1)]:
   with self.assertRaises(ValueError):score(t,p,q)
 def test_original_receipt_and_no_promotion(self):
  r=json.loads(R.read_text());self.assertEqual(r['source_rows'],14186)
  self.assertEqual(r['zip_sha256'],'7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9')
  self.assertEqual(r['test_days'],DAYS[4:]);self.assertEqual(sum(r['positive_by_test_day'].values()),1425)
  for k in ['g2_qualified','g3_qualified','independent_tank_exposures','blinded_prospective','traceable_calibration','threshold_4ppm_is_safety_limit']:
   self.assertIs(r[k],False)
  self.assertEqual(set(r['models']),set(MODELS))
  for m in r['models'].values():
   a=m['aggregate'];f=m['days'];self.assertEqual(len(f),7)
   self.assertEqual(a['n'],sum(r['test_rows_by_day'].values()))
   self.assertEqual(a['pos'],sum(x['pos'] for x in f))
   self.assertEqual(a['fn'],sum(x['fn'] for x in f))
   self.assertEqual(a['fp'],sum(x['fp'] for x in f))
   self.assertEqual(a['tp']+a['fn'],a['pos'])
   self.assertEqual(a['n'],a['tp']+a['fn']+a['fp']+a['tn'])
if __name__=='__main__':unittest.main()
