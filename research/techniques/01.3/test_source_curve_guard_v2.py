import unittest
from source_curve_guard_v2 import SourceCurve,predicted_ratio,inverse_candidate


def curve(**kw):
    c=dict(sensor_model="MQ3",gas_label="ETHANOL",source_id="TOOL-MQ-01",
           reference_type="source_graph",reference_definition="Rs/R0 normalized in datasheet",
           curve_a=3.0,curve_b=-0.5,ppm_per_internal_unit=50.0,
           min_internal=.1,max_internal=10.0,
           model_kind="MANUFACTURER_PRIOR",reference_heater_phase="CONSTANT")
    c.update(kw);return SourceCurve(**c)


class CurveGuards(unittest.TestCase):
    def test_50x_scaled_nominal_range(self):
        c=curve()
        self.assertEqual(c.ppm_min,5)
        self.assertEqual(c.ppm_max,500)
        self.assertEqual(predicted_ratio(c,50)["rs_over_r0_prior"],3)

    def test_forward_inverse_nominal_roundtrip(self):
        c=curve()
        for ppm in (5,50,250,500):
            with self.subTest(ppm=ppm):
                y=predicted_ratio(c,ppm)["rs_over_r0_prior"]
                x=inverse_candidate(c,y,r0_calibration_id="cal-18",source_sensor_instance_id="mq3-A")
                self.assertAlmostEqual(x["candidate_nominal_ppm"],ppm,places=8)
                self.assertFalse(x["device_ppm_approved"])
                self.assertFalse(x["mixture_components_quantified"])
                self.assertFalse(x["tank_G3_validated"])

    def test_ppm_scale_not_silently_one(self):
        c=curve()
        self.assertAlmostEqual(predicted_ratio(c,500)["rs_over_r0_prior"],3/(10**.5))

    def test_forward_out_of_domain(self):
        for ppm in (0,4.9,501):
            with self.subTest(ppm=ppm),self.assertRaises(ValueError):predicted_ratio(curve(),ppm)

    def test_inverse_beyond_domain(self):
        with self.assertRaises(ValueError):
            inverse_candidate(curve(),1e-8,r0_calibration_id="c",source_sensor_instance_id="mq3-A")

    def test_missing_r0_id(self):
        with self.assertRaises(ValueError):
            inverse_candidate(curve(),3,source_sensor_instance_id="mq3-A")

    def test_missing_physical_sensor_instance(self):
        with self.assertRaises(ValueError):
            inverse_candidate(curve(),3,r0_calibration_id="cal")

    def test_curve_locked_to_specific_sensor(self):
        c=curve(physical_sensor_instance_id="mq3-real-1")
        with self.assertRaises(ValueError):
            inverse_candidate(c,3,r0_calibration_id="cal",source_sensor_instance_id="other")

    def test_bad_curve_coefficient(self):
        for value in (0,-1,float("nan"),True):
            with self.subTest(value=value),self.assertRaises(ValueError):curve(curve_a=value)

    def test_b_zero(self):
        with self.assertRaises(ValueError):curve(curve_b=0)

    def test_b_nonfinite(self):
        with self.assertRaises(ValueError):curve(curve_b=float("inf"))

    def test_invalid_scale(self):
        with self.assertRaises(ValueError):curve(ppm_per_internal_unit=0)

    def test_bad_domain(self):
        with self.assertRaises(ValueError):curve(min_internal=10,max_internal=10)

    def test_unknown_heater(self):
        with self.assertRaises(ValueError):curve(reference_heater_phase="UNKNOWN")

    def test_explicit_source_required(self):
        with self.assertRaises(ValueError):curve(source_id="")

    def test_explicit_reference_definition_required(self):
        with self.assertRaises(ValueError):curve(reference_definition=" ")

    def test_ratio_nonpositive(self):
        with self.assertRaises(ValueError):
            inverse_candidate(curve(),0,r0_calibration_id="cal",source_sensor_instance_id="mq3-A")

    def test_external_record_not_verified_by_label(self):
        x=inverse_candidate(curve(model_kind="EXTERNAL_EMPIRICAL_RESEARCH"),3,
                            r0_calibration_id="cal",source_sensor_instance_id="mq3-A")
        self.assertFalse(x["device_ppm_approved"])
        self.assertEqual(x["evidence_kind"],"EXTERNAL_EMPIRICAL_RESEARCH")

if __name__=="__main__": unittest.main()
