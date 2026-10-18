import unittest
from continual_learning_v1 import new_state,update
class BehavioralGate(unittest.TestCase):
    def test_empty_state(self): self.assertEqual(new_state(2)["counts"],{})
    def test_approval_required(self):
        with self.assertRaises(PermissionError): update(new_state(2),"a",[1,2])
    def test_first_update(self):
        s=update(new_state(2),"a",[1.,3.],approved=True);self.assertEqual(s["means"]["a"],[1.,3.])
    def test_second_mean(self):
        s=update(new_state(1),"a",[1.],approved=True);s=update(s,"a",[3.],approved=True)
        self.assertEqual(s["means"]["a"],[2.])
    def test_count(self):
        s=update(new_state(1),"a",[1.],approved=True);s=update(s,"a",[3.],approved=True)
        self.assertEqual(s["counts"]["a"],2)
    def test_multiple_labels(self):
        s=update(new_state(1),"a",[1.],approved=True);s=update(s,"b",[3.],approved=True)
        self.assertEqual(s["counts"],{"a":1,"b":1})
    def test_audit(self):
        s=update(new_state(1),"a",[1.],approved=True);self.assertEqual(s["audit"][-1]["label"],"a")
    def test_nonmutation(self):
        old=new_state(1);new=update(old,"a",[1.],approved=True)
        self.assertEqual(old["counts"],{});self.assertIsNot(old,new)
    def test_bad_dim(self):
        with self.assertRaises(ValueError): new_state(0)
    def test_boolean_dim(self):
        with self.assertRaises(ValueError): new_state(True)
    def test_missing_label(self):
        with self.assertRaises(ValueError): update(new_state(1),"",[1],approved=True)
    def test_wrong_features(self):
        with self.assertRaises(ValueError): update(new_state(2),"a",[1],approved=True)
    def test_nonfinite_features(self):
        with self.assertRaises(ValueError): update(new_state(1),"a",[float("inf")],approved=True)
    def test_bool_features(self):
        with self.assertRaises(ValueError): update(new_state(1),"a",[True],approved=True)
if __name__=="__main__":unittest.main()
