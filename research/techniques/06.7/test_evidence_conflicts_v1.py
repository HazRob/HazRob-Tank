import unittest
import evidence_conflicts_v1 as m

class NegativeEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.ref={"ethanol":{"MQ2":{"detection_power":.99},"MQ3":{"detection_power":.1}}}
    def test_high_power_nondetection_conflict(self):
        r=m.assess("ethanol",{"MQ2":"NOT_DETECTED"},self.ref)
        self.assertEqual(r["conflicting_sensors"],["MQ2"])
        self.assertEqual(r["status"],"NEGATIVE_EVIDENCE_CONFLICT")
    def test_never_hard_exclude(self):
        self.assertFalse(m.assess("ethanol",{"MQ2":"NOT_DETECTED"},self.ref)["hard_exclusion"])
    def test_low_power_is_uninformative(self):
        self.assertEqual(m.assess("ethanol",{"MQ3":"NOT_DETECTED"},self.ref)["uninformative_sensors"],["MQ3"])
    def test_detected_is_observed_support(self):
        self.assertEqual(m.assess("ethanol",{"MQ2":"DETECTED"},self.ref)["supporting_sensors"],["MQ2"])
    def test_fault_ignored(self):
        self.assertEqual(m.assess("ethanol",{"MQ2":"FAULT"},self.ref)["uninformative_sensors"],["MQ2"])
    def test_not_measured_ignored(self):
        self.assertEqual(m.assess("ethanol",{"MQ2":"NOT_MEASURED"},self.ref)["uninformative_sensors"],["MQ2"])
    def test_missing_reference_ignored(self):
        self.assertEqual(m.assess("ethanol",{"other":"NOT_DETECTED"},self.ref)["uninformative_sensors"],["other"])
    def test_unknown_candidate_ignored(self):
        self.assertEqual(m.assess("absent",{"MQ2":"NOT_DETECTED"},self.ref)["conflicting_sensors"],[])
    def test_empty_observations(self):
        self.assertEqual(m.assess("ethanol",{},self.ref)["status"],"NO_STRONG_NEGATIVE_CONFLICT")
    def test_adjustable_threshold(self):
        self.assertEqual(m.assess("ethanol",{"MQ3":"NOT_DETECTED"},self.ref,minimum_detection_power=.1)["conflicting_sensors"],["MQ3"])
    def test_invalid_observation(self):
        with self.assertRaises(ValueError):m.assess("ethanol",{"MQ2":"UNKNOWN"},self.ref)
    def test_invalid_detection_power(self):
        with self.assertRaises(ValueError):m.assess("ethanol",{"MQ2":"DETECTED"},{"ethanol":{"MQ2":{"detection_power":2}}})
    def test_invalid_threshold(self):
        with self.assertRaises(ValueError):m.assess("ethanol",{},self.ref,minimum_detection_power=0)
    def test_invalid_candidate(self):
        with self.assertRaises(ValueError):m.assess("",{},self.ref)
    def test_nonmapping_inputs(self):
        with self.assertRaises(ValueError):m.assess("ethanol",[],self.ref)
    def test_malformed_candidate_reference(self):
        with self.assertRaises(ValueError):m.assess("ethanol",{"MQ2":"DETECTED"},{"ethanol":["wrong"]})

if __name__=="__main__":unittest.main()
