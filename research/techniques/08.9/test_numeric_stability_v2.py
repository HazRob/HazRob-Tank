import unittest
from nonnegative_rank_guard_v2 import nnls_two_component

def run(y,a,b,**kw):
    return nnls_two_component(y,a,b,source_id='MQexternal',
        sensor_ids=['MQ2','MQ3','MQ135'], reference_r0_id='R0-paper',
        heater_phase='CONSTANT',**kw)

class TestRank(unittest.TestCase):
    def test_exact(self):
        x=run([2,3,5],[1,0,1],[0,1,1])
        self.assertEqual(x['status'],'NUMERICAL_NNLS_SOURCE_PRIOR_ONLY')
        self.assertAlmostEqual(x['coefficients']['a'],2)
        self.assertAlmostEqual(x['coefficients']['b'],3)
        self.assertAlmostEqual(x['residual_rmse'],0)
        self.assertIsNone(x['gas_ppm'])
    def test_collinear(self):
        self.assertEqual(run([1,2,3],[1,2,3],[2,4,6])['status'],'NOT_IDENTIFIABLE_SOURCE_COLLINEAR')
    def test_negative_boundary(self):
        x=run([1,0,1],[1,0,1],[0,1,1])
        self.assertTrue(all(v>=0 for v in x['coefficients'].values()))
    def test_tiny(self):
        x=run([2e-200,3e-200,5e-200],[1e-200,0,1e-200],[0,1e-200,1e-200])
        self.assertAlmostEqual(x['coefficients']['a'],2)
        self.assertAlmostEqual(x['coefficients']['b'],3)
    def test_huge(self):
        x=run([2e200,3e200,5e200],[1e200,0,1e200],[0,1e200,1e200])
        self.assertAlmostEqual(x['coefficients']['a'],2)
        self.assertAlmostEqual(x['coefficients']['b'],3)
    def test_near(self):
        x=run([1,1,1],[1,1,1],[1,1.00001,1],max_condition=100)
        self.assertIn(x['status'],('NOT_IDENTIFIABLE_ILL_CONDITIONED','NOT_IDENTIFIABLE_SOURCE_COLLINEAR'))
    def test_nan(self):
        with self.assertRaises(ValueError):run([1,2,float('nan')],[1,0,1],[0,1,1])
    def test_bad_channels(self):
        with self.assertRaises(ValueError):
            nnls_two_component([1,2],[1,0],[0,1],source_id='x',sensor_ids=['MQ2','MQ2'],reference_r0_id='x',heater_phase='CONSTANT')

if __name__=='__main__':unittest.main()
