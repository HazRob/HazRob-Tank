import unittest,math
from uci1081_g2_batch07_v1 import IDS,partitions,fit_binary_likelihood,repo_module,probability_rows

class Protocol(unittest.TestCase):
 def rows(self):return [dict(ppb=c,gas=g,replicate_ordinal=r) for g in range(6) for c in (50.,100.,200.) for r in range(5)]
 def test_ten_unique_ids(self):self.assertEqual((len(IDS),len(set(IDS))),(10,10))
 def test_disjoint_exposures(self):
  for c in (50.,100.,200.):
   a,b,d=partitions(self.rows(),c);self.assertFalse(set(a)&set(b)|set(a)&set(d)|set(b)&set(d))
 def test_no_concentration_overlap(self):
  rows=self.rows();a,b,d=partitions(rows,50.);self.assertTrue(all(rows[i]['ppb']!=50 for i in a+b))
 def test_holdouts_cover_once(self):self.assertEqual(sorted(i for c in (50.,100.,200.) for i in partitions(self.rows(),c)[2]),list(range(90)))
 def test_calibration_not_training(self):
  rows=self.rows();a,b,_=partitions(rows,50.);self.assertTrue(all(rows[i]['replicate_ordinal']==4 for i in b));self.assertTrue(all(rows[i]['replicate_ordinal']!=4 for i in a))
 def test_corrupt_inventory_rejected(self):
  with self.assertRaises(ValueError):partitions(self.rows()[:-1],50.)
 def test_probability_column_alignment(self):self.assertEqual(probability_rows([{'softmax_scores':{'b':.7,'a':.3}}],['a','b']),[[.3,.7]])
 def test_single_event_bayes_training_likelihood(self):
  t,l=fit_binary_likelihood([[0],[1],[10],[11]],['a','a','b','b'],['a','b']);self.assertEqual(t,5.5);self.assertEqual(l,{'a':.25,'b':.75})
 def test_dependent_models_do_not_make_independent_sources(self):
  m=repo_module('06.1','evidence_fusion_v1.py');s={'group':'same','probabilities':{'a':.8,'b':.2}};self.assertEqual(m.fuse_evidence([s,s],prior={'a':.5,'b':.5})['status'],'ABSTAIN')
 def test_empty_conformal_set_counts_as_miss(self):
  m=repo_module('06.4','conformal_prediction_v1.py');r=m.evaluate_sets([[],[1]],[0,1]);self.assertEqual(r['empirical_coverage'],.5);self.assertEqual(r['mean_set_size'],.5)
 def test_all_empty_conformal_sets(self):self.assertEqual(repo_module('06.4','conformal_prediction_v1.py').evaluate_sets([[],[]],[0,1])['empirical_coverage'],0)
 def test_exact_shapley_linear_oracle(self):
  r=repo_module('06.8','shap_v1.py').explain(lambda x:2*x[0]-x[1],[0,0],[3,2]);self.assertEqual(r['attributions'],[6.,-2.]);self.assertAlmostEqual(r['residual'],0)
 def test_ood_overrides_high_score(self):self.assertEqual(repo_module('06.9','abstention_v1.py').decide({'a':.99,'b':.01},ood=True)['status'],'ABSTAIN')
if __name__=='__main__':unittest.main()
