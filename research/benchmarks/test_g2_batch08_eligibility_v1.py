import unittest
from g2_batch08_eligibility_v1 import IDS,BLOCKS,screening,baseline_summary
class Protocol(unittest.TestCase):
 def rows(self):
  rows=[]
  for level in (50.,100.,200.):
   for i in range(30):
    rows.append({'ppb':level,'trace':[[float(i+1)]*300+[float(i+10)]*600 for _ in range(10)]})
  return rows
 def test_ten_unique(self):self.assertEqual(len(set(IDS)),10)
 def test_executed_and_blocked_partition(self):self.assertEqual(set(IDS),set(BLOCKS)|{'05.8'})
 def test_physical_blockers_explicit(self):self.assertTrue(all(isinstance(v,list) and all(v) for v in BLOCKS.values()))
 def test_one_summary_per_exposure(self):self.assertEqual(baseline_summary(self.rows()[0],0),(1.,10.))
 def test_screening_counts(self):
  r=screening(self.rows());self.assertEqual(len(r),30);self.assertTrue(all(x['train_exposures']==60 and x['test_exposures']==30 for x in r))
 def test_not_quantifiable(self):self.assertTrue(all(x['quantifiable']==0 for x in screening(self.rows())))
 def test_zero_variance_not_invented(self):
  rows=self.rows()
  for r in rows:r['trace']=[[1.]*900 for _ in range(10)]
  self.assertTrue(all(x['status']=='UNESTIMABLE_ZERO_BASELINE_VARIANCE' for x in screening(rows)))
 def test_test_values_do_not_change_training_threshold(self):
  rows=self.rows();a=screening(rows)[:10]
  for r in rows:
   if r['ppb']==50.:r['trace']=[[1000.]*900 for _ in range(10)]
  b=screening(rows)[:10]
  self.assertEqual([r['threshold_source_units'] for r in a],[r['threshold_source_units'] for r in b])
 def test_incomplete_inventory_rejected(self):
  with self.assertRaises(ValueError):screening(self.rows()[:-1])
if __name__=='__main__':unittest.main()
