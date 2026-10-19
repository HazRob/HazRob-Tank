import unittest
from motion_decisions_v1 import recommend
K=dict(obstacle_cm=50.,cliff_detected=False,gas_alarm=False,measurement_valid=True,communication_ok=True,localization_valid=True)
def decide(**kw): return recommend(**dict(K,**kw))
class BehavioralGate(unittest.TestCase):
    def test_operator_only(self): self.assertEqual(decide()["intent"],"ELIGIBLE_FOR_OPERATOR_REVIEW")
    def test_no_motor(self): self.assertIsNone(decide()["motor_command"])
    def test_cliff(self): self.assertIn("CLIFF",decide(cliff_detected=True)["reasons"])
    def test_obstacle(self): self.assertIn("OBSTACLE",decide(obstacle_cm=5)["reasons"])
    def test_gas_alarm(self): self.assertIn("GAS_ALARM_REVIEW",decide(gas_alarm=True)["reasons"])
    def test_invalid_measurement(self): self.assertIn("INVALID_SENSOR_DATA",decide(measurement_valid=False)["reasons"])
    def test_comm_loss(self): self.assertIn("COMMUNICATION_LOSS",decide(communication_ok=False)["reasons"])
    def test_bad_localization(self): self.assertIn("LOCALIZATION_UNRELIABLE",decide(localization_valid=False)["reasons"])
    def test_boundary_clearance(self): self.assertEqual(decide(obstacle_cm=15)["intent"],"ELIGIBLE_FOR_OPERATOR_REVIEW")
    def test_multiple_reasons(self): self.assertGreater(len(decide(cliff_detected=True,gas_alarm=True)["reasons"]),1)
    def test_negative_distance(self):
        with self.assertRaises(ValueError): decide(obstacle_cm=-1)
    def test_bool_distance(self):
        with self.assertRaises(ValueError): decide(obstacle_cm=True)
    def test_bool_clearance(self):
        with self.assertRaises(ValueError): decide(minimum_clearance_cm=True)
    def test_invalid_flag(self):
        with self.assertRaises(ValueError): decide(gas_alarm=1)
    def test_invalid_nan(self):
        with self.assertRaises(ValueError): decide(obstacle_cm=float("nan"))
    def test_not_authority(self): self.assertIn("No autonomous motion",decide()["warning"])
if __name__=="__main__":unittest.main()
