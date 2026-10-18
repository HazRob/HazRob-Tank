import unittest
from negative_evidence import update

class TestNegativeEvidence(unittest.TestCase):
    def setUp(self):
        self.priors={'a':.5,'b':.5}
        self.caps={'a':{'mq':.9},'b':{'mq':.1}}
    def test_negative_downweights_detectable(self):
        self.assertAlmostEqual(update(self.priors,{'mq':False},self.caps)['a'],.1)
    def test_positive_upweights_detectable(self):
        self.assertAlmostEqual(update(self.priors,{'mq':True},self.caps)['a'],.9)
    def test_missing_evidence(self):
        self.assertAlmostEqual(update(self.priors,{'mq':None},self.caps)['a'],.5)
    def test_bad_quality_ignored(self):
        self.assertAlmostEqual(update(self.priors,{'mq':False},self.caps,{'mq':False})['a'],.5)
    def test_no_hard_elimination(self):
        self.assertGreater(update(self.priors,{'mq':False},{'a':{'mq':1},'b':{'mq':0}})['a'],0)
    def test_invalid_probability(self):
        with self.assertRaises(ValueError):
            update(self.priors,{'mq':True},{'a':{'mq':2}})
    def test_invalid_priors(self):
        with self.assertRaises(ValueError):
            update({'a':-1},{},self.caps)
    def test_unknown_sensor_ignored(self):
        self.assertAlmostEqual(update(self.priors,{'unknown':False},self.caps)['a'],.5)

if __name__=='__main__':
    unittest.main()
