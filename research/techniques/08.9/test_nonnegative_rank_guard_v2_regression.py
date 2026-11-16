"""Source-independent numerical regression for 08.9; synthetic inputs only."""
import unittest
from nonnegative_rank_guard_v2 import nnls_two_component

def fit(y,a,b,**kw):
    return nnls_two_component(y,a,b,source_id='synthetic',sensor_ids=['S1','S2','S3'],reference_r0_id='synthetic-R0',heater_phase='CONSTANT',**kw)

class RankRegression(unittest.TestCase):
    def test_exact_solution(self):
        r=fit([2,3,5],[1,0,1],[0,1,1]);self.assertAlmostEqual(r['coefficients']['a'],2);self.assertAlmostEqual(r['coefficients']['b'],3)
    def test_scale_robust(self):
        r=fit([2,3,5],[200,0,200],[0,.005,.005]);self.assertEqual(r['status'],'NUMERICAL_NNLS_SOURCE_PRIOR_ONLY');self.assertAlmostEqual(r['coefficients']['a'],.01);self.assertAlmostEqual(r['coefficients']['b'],600)
    def test_collinear(self):self.assertEqual(fit([1,2,3],[1,2,3],[2,4,6])['status'],'NOT_IDENTIFIABLE_SOURCE_COLLINEAR')
    def test_boundary(self):self.assertGreaterEqual(fit([1,0,1],[1,0,1],[0,1,1])['coefficients']['b'],0)
    def test_nan(self):
        with self.assertRaises(ValueError):fit([float('nan'),0,0],[1,0,0],[0,1,0])
    def test_duplicate_channel(self):
        with self.assertRaises(ValueError):nnls_two_component([1,2,3],[1,0,1],[0,1,1],source_id='s',sensor_ids=['S1','S1','S3'],reference_r0_id='r',heater_phase='HIGH')
    def test_no_ppm_claim(self):self.assertIsNone(fit([2,3,5],[1,0,1],[0,1,1])['gas_ppm'])
if __name__=='__main__':unittest.main()
