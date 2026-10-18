import unittest
from simulation_v1 import simulate
class BehavioralGate(unittest.TestCase):
    def test_length(self): self.assertEqual(len(simulate([0,1,2],1)["responses"]),3)
    def test_no_exposure(self): self.assertEqual(simulate([0,0,0],1)["responses"],[1.,1.,1.])
    def test_first_order_step(self):
        from math import exp
        self.assertAlmostEqual(simulate([1],2,baseline=1,tau_seconds=1,step_seconds=1)["responses"][0],1+2*(1-exp(-1)))
    def test_monotonic_rise(self):
        out=simulate([1]*5,1)["responses"];self.assertEqual(out,sorted(out))
    def test_decay(self):
        out=simulate([1,1,0,0,0],1)["responses"];self.assertGreater(out[2],out[-1])
    def test_zero_sensitivity(self): self.assertEqual(simulate([1,2,3],0)["responses"],[1.,1.,1.])
    def test_step_limit(self): self.assertAlmostEqual(simulate([1],1,tau_seconds=.01,step_seconds=1)["responses"][0],2.)
    def test_empty(self):
        with self.assertRaises(ValueError): simulate([],1)
    def test_negative_exposure(self):
        with self.assertRaises(ValueError): simulate([-1],1)
    def test_nan_exposure(self):
        with self.assertRaises(ValueError): simulate([float("nan")],1)
    def test_bad_tau(self):
        with self.assertRaises(ValueError): simulate([1],1,tau_seconds=0)
    def test_bad_step(self):
        with self.assertRaises(ValueError): simulate([1],1,step_seconds=0)
    def test_bool_sensitivity(self):
        with self.assertRaises(ValueError): simulate([1],True)
    def test_negative_baseline(self):
        with self.assertRaises(ValueError): simulate([1],1,baseline=-1)
if __name__=="__main__": unittest.main()
