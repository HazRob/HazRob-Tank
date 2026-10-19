"""G2-03 deterministic tests of source eligibility and causal filtering metrics."""
import math
import unittest
import uci1081_g2_batch03_v1 as m

class G203ReferenceTests(unittest.TestCase):
    def test_ten_unique_ids(self):
        self.assertEqual(len(m.TARGET_IDS),10)
        self.assertEqual(len(set(m.TARGET_IDS)),10)

    def test_only_generic_filter_eligible(self):
        x=m.audit_compatibility()
        self.assertEqual([ident for ident,s in x.items()
                          if not s["status"].startswith("BLOCKED")],["02.1"])

    def test_adc_not_fabricated(self):
        x=m.audit_compatibility()
        self.assertIn("ADC",x["01.1"]["status"])
        self.assertIn("raw_adc_codes",x["01.6"]["required_fields_missing"])

    def test_r0_and_heater_blocked(self):
        x=m.audit_compatibility()
        self.assertIn("R0",x["01.2"]["status"])
        self.assertIn("heater_phase",x["01.4"]["required_fields_missing"])

    def test_bme_and_concentration_blocked(self):
        x=m.audit_compatibility()
        self.assertIn("temperature_C",x["01.5"]["required_fields_missing"])
        self.assertIn("measured_R0_ohm",x["05.1"]["required_fields_missing"])

    def test_physical_ohm_features_blocked(self):
        x=m.audit_compatibility()
        self.assertIn("baseline_Rs_ohm",x["02.3"]["required_fields_missing"])
        self.assertIn("paired_Rs_ohm",x["01.7"]["required_fields_missing"])

    def test_mad_zero(self):
        self.assertEqual(m.mad([1.,1.,1.,1.]),0.0)

    def test_mad_positive(self):
        self.assertEqual(m.mad([1.,2.,3.,10.]),1.0)

    def test_onset_up(self):
        xs=[1.]*300+[1.,1.,3.]+[3.]*597
        self.assertEqual(m.onset_half_crossing(xs,1.,3.),2)

    def test_onset_down(self):
        xs=[3.]*300+[3.,1.]+[1.]*598
        self.assertEqual(m.onset_half_crossing(xs,3.,1.),1)

    def test_zero_response_not_identifiable(self):
        self.assertIsNone(m.onset_half_crossing([1.]*900,1.,1.))

    def test_end_without_crossing(self):
        self.assertIsNone(m.onset_half_crossing([1.]*900,1.,3.))

    def test_source_not_equated_tank(self):
        x=m.audit_compatibility()
        self.assertEqual(x["02.1"]["physical_signal_unit"],"UNVERIFIED")
        self.assertEqual(x["02.1"]["physical_gas_calibration"],"NOT_SUPPORTED")

if __name__=="__main__":
    unittest.main()
