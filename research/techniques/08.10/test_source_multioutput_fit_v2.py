import unittest
from source_multioutput_fit_v2 import fit_multianalyte_source, predict_source_heldout


def rows():
    result=[]
    for x in (1.,2.,3.):
        for y in (1.,2.,3.):
            result.append({'source_id':'external','sensor_array_id':'SENS-1',
                'heater_phase':'CONSTANT','r0_reference_id':'R0-1',
                'physical_session_id':'S'+str(int(x)),
                'exposure_id':'E'+str(len(result)),
                'reference_trace_id':'UNVERIFIED-TRACE',
                'features':{'MQ2':x+2*y,'MQ135':2*x+y},
                'concentration_ppm_by_gas':{'A':x,'B':y}})
    return result


def fit(data=None,**kw):
    opts=dict(source_id='external',sensor_ids=['MQ2','MQ135'],gas_labels=['A','B'],
              sensor_array_id='SENS-1',heater_phase='CONSTANT',r0_reference_id='R0-1')
    opts.update(kw)
    return fit_multianalyte_source(data if data is not None else rows(),**opts)


def predict(model,features=None,**kw):
    p=dict(source_id='external',sensor_array_id='SENS-1',heater_phase='CONSTANT',
           r0_reference_id='R0-1',physical_session_id='S4',exposure_id='heldout')
    p.update(kw)
    return predict_source_heldout(model,features or {'MQ2':5.8,'MQ135':6.5},**p)

class TestMultianalyte(unittest.TestCase):
    def test_true_multioutput_linear_synthetic(self):
        m=fit(ridge=1e-10)
        x=predict(m)
        self.assertAlmostEqual(x['estimated_component_ppm_research_only']['A'],2.4,places=5)
        self.assertAlmostEqual(x['estimated_component_ppm_research_only']['B'],1.7,places=5)
        self.assertFalse(x['gas_ppm_approved'])
        self.assertFalse(m['independent_reference_certificate_verified'])
    def test_group_leakage_blocks(self):
        with self.assertRaises(ValueError):predict(fit(),physical_session_id='S1')
    def test_no_extrapolation(self):
        x=predict(fit(),features={'MQ2':999.,'MQ135':999.})
        self.assertEqual(x['status'],'ABSTAIN_OUT_OF_SOURCE_RANGE')
    def test_no_duplicate_exposures(self):
        r=rows();r[1]['exposure_id']=r[0]['exposure_id']
        with self.assertRaises(ValueError):fit(r)
    def test_collinearity_refused(self):
        r=rows()
        for q in r:q['features']['MQ135']=2*q['features']['MQ2']
        with self.assertRaises(ValueError):fit(r)
    def test_mismatched_source_refused(self):
        with self.assertRaises(ValueError):predict(fit(),source_id='different')
    def test_bad_ppm_label(self):
        r=rows();r[0]['concentration_ppm_by_gas']['A']=-1
        with self.assertRaises(ValueError):fit(r)
    def test_missing_reference_id(self):
        r=rows();r[0]['reference_trace_id']=''
        with self.assertRaises(ValueError):fit(r)
    def test_too_few_sessions(self):
        r=rows()
        for q in r:q['physical_session_id']='S1'
        with self.assertRaises(ValueError):fit(r)
    def test_not_fake_production(self):
        x=predict(fit())
        self.assertFalse(x['Tank_G3'])
        self.assertNotIn('certified_ppm',x)

if __name__=='__main__':unittest.main()
