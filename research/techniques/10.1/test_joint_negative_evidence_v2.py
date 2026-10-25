import unittest
from joint_negative_evidence_v2 import compare_joint_nondetection


def run(**extra):
    p=dict(priors={'ethanol':.5,'CO':.5}, source_id='lab',
           physical_session_id='S1',sensor_array_id='TankMQ8',
           detections={'MQ2':False,'MQ135':False},
           sensor_quality={'MQ2':True,'MQ135':True})
    p.update(extra)
    return compare_joint_nondetection(**p)

class JointEvidenceTests(unittest.TestCase):
    def test_unverified_no_evidence_update(self):
        x=run(joint_pattern_likelihood_by_gas={'ethanol':.01,'CO':.95})
        self.assertEqual(x['posterior_research_only'], {'ethanol':.5,'CO':.5})
        self.assertEqual(x['status'],'UNVERIFIED_JOINT_LIKELIHOOD_NO_UPDATE')
    def test_calibrated_joint_pattern_once_not_twice(self):
        x=run(joint_pattern_likelihood_by_gas={'ethanol':.2,'CO':.8},
              joint_reference_independently_verified=True,
              joint_reference_source_sha256='a'*64)
        self.assertAlmostEqual(x['posterior_research_only']['CO'], .8)
        self.assertEqual(x['hard_exclusions'], [])
        self.assertFalse(x['joint_model_certification_verified_by_function'])
    def test_no_data(self):
        x=run(sensor_quality={'MQ2':False,'MQ135':False})
        self.assertEqual(x['status'],'NO_VALID_MEASUREMENT')
    def test_missing_quality(self):
        with self.assertRaises(ValueError):run(sensor_quality={'MQ2':True})
    def test_missing_sha_claim(self):
        with self.assertRaises(ValueError):run(joint_reference_independently_verified=True,
              joint_pattern_likelihood_by_gas={'ethanol':.2,'CO':.8})
    def test_no_bogus_zeros(self):
        with self.assertRaises(ValueError):run(joint_reference_independently_verified=True,
            joint_pattern_likelihood_by_gas={'ethanol':0,'CO':1},
            joint_reference_source_sha256='a'*64)
    def test_sensor_unknown(self):
        x=run(detections={'MQ2':None,'MQ135':None})
        self.assertEqual(x['status'],'NO_VALID_MEASUREMENT')
    def test_bad_prior(self):
        with self.assertRaises(ValueError):run(priors={'CO':0,'ethanol':0})
    def test_no_hard_exclusion(self):
        x=run(joint_pattern_likelihood_by_gas={'ethanol':1e-8,'CO':1.},
              joint_reference_independently_verified=True,
              joint_reference_source_sha256='b'*64)
        self.assertGreater(x['posterior_research_only']['ethanol'],0)
        self.assertFalse(x['gas_identity_verified'])

if __name__=='__main__':unittest.main()
