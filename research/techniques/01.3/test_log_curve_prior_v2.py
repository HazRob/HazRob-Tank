import unittest
from log_curve_prior_v2 import LogCurvePrior


def curve(**params):
    x=dict(sensor_model="MQ3",target_gas="Ethanol",a=2.0,b=-.5,
           output_scale_ppm=50.,lower_ppm=5.15,upper_ppm=500.,
           r0_definition_id="MQ3-ethanol-curve-r0",heater_phase="CONSTANT",
           source_id="MQDataScience-mfg-plot")
    x.update(params)
    return LogCurvePrior(**x)


class CurveEvidenceTests(unittest.TestCase):
    def test_scaled_internal_concentration(self):
        self.assertAlmostEqual(curve().forward_rs_r0(50),2)
        self.assertAlmostEqual(curve().inverse_ppm_prior(2),50)
        self.assertNotAlmostEqual(curve().forward_rs_r0(50),2*(50**(-.5)))
    def test_inverse_in_valid_interval(self):
        for ppm in (5.15,10,25,50,100,500):
            with self.subTest(ppm=ppm):
                self.assertAlmostEqual(curve().inverse_ppm_prior(curve().forward_rs_r0(ppm)),ppm)
    def test_no_forward_extrapolation(self):
        for ppm in (0,5,501,float("nan"),True):
            with self.subTest(ppm=ppm),self.assertRaises(ValueError):
                curve().forward_rs_r0(ppm)
    def test_no_inverse_extrapolation(self):
        for ratio in (0,-2,100,1e-15):
            with self.subTest(ratio=ratio),self.assertRaises(ValueError):
                curve().inverse_ppm_prior(ratio)
    def test_sensor_definition_crosscheck(self):
        self.assertTrue(curve().check_sensor_r0_compatibility(
            "MQ3","MQ3-ethanol-curve-r0","CONSTANT"))
        for vals in (("MQ135","MQ3-ethanol-curve-r0","CONSTANT"),
                     ("MQ3","unknown","CONSTANT"),("MQ3","MQ3-ethanol-curve-r0","LOW")):
            with self.subTest(vals=vals),self.assertRaises(ValueError):
                curve().check_sensor_r0_compatibility(*vals)
    def test_bad_coefficient(self):
        for kw in ({"b":0},{"b":float("inf")},{"a":0},{"output_scale_ppm":0},
                   {"lower_ppm":500},{"sensor_model":"TGS2600"},
                   {"heater_phase":"UNKNOWN"},{"source_id":""},
                   {"evidence_tier":"APPROVED_PPM"}):
            with self.subTest(kw=kw),self.assertRaises(ValueError):curve(**kw)
    def test_not_an_experimental_calibration(self):
        self.assertEqual(curve().evidence_tier,"DIGITIZED_MANUFACTURER_PRIOR")


if __name__=="__main__":
    unittest.main()
