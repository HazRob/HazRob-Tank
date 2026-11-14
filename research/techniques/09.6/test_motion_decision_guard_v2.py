import unittest
from motion_decision_guard_v2 import advisory
BASE=dict(obstacle_cm=30,cliff_detected=False,communication_ok=True,pose_fresh=True,sensor_fresh=True,chemical_evidence_valid=True,operator_authorized=True)
class GateTests(unittest.TestCase):
    def test_advisory_never_motor(self):
        x=advisory(**BASE); self.assertIsNone(x['motor_command']);self.assertFalse(x['production_qualified'])
    def test_cliff(self): self.assertIn('CLIFF',advisory(**(BASE|{'cliff_detected':True}))['reasons'])
    def test_equal_clearance(self): self.assertIn('OBSTACLE_CLEARANCE',advisory(**(BASE|{'obstacle_cm':15}))['reasons'])
    def test_stale_pose(self): self.assertIn('STALE_POSE',advisory(**(BASE|{'pose_fresh':False}))['reasons'])
    def test_stale_sensor(self): self.assertIn('STALE_SENSOR',advisory(**(BASE|{'sensor_fresh':False}))['reasons'])
    def test_comms(self): self.assertIn('COMMUNICATION_LOSS',advisory(**(BASE|{'communication_ok':False}))['reasons'])
    def test_chemical(self): self.assertIn('UNVERIFIED_CHEMICAL_EVIDENCE',advisory(**(BASE|{'chemical_evidence_valid':False}))['reasons'])
    def test_operator(self): self.assertIn('OPERATOR_NOT_AUTHORIZED',advisory(**(BASE|{'operator_authorized':False}))['reasons'])
    def test_invalid_bool(self):
        with self.assertRaises(ValueError):advisory(**(BASE|{'cliff_detected':1}))
    def test_nan(self):
        with self.assertRaises(ValueError):advisory(**(BASE|{'obstacle_cm':float('nan')}))
    def test_confidence(self):
        with self.assertRaises(ValueError):advisory(**BASE,plume_confidence=1.2)
if __name__=='__main__':unittest.main()
