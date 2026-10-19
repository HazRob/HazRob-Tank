import unittest
from multi_analyte_regression_v1 import estimate
A=[1.,0.];B=[0.,1.]
class BehavioralGate(unittest.TestCase):
    def test_one_component(self):
        r=estimate(A,[A,B],iterations=100,step_size=.1)
        self.assertAlmostEqual(r["coefficients"][0],1.,places=4)
    def test_second_component(self):
        r=estimate(B,[A,B],iterations=100,step_size=.1)
        self.assertAlmostEqual(r["coefficients"][1],1.,places=4)
    def test_two_components(self):
        r=estimate([.3,.6],[A,B],iterations=150,step_size=.1)
        self.assertAlmostEqual(r["coefficients"][0],.3,places=3)
        self.assertAlmostEqual(r["coefficients"][1],.6,places=3)
    def test_nonnegative(self):
        r=estimate([-1.,0.],[A,B],iterations=10,step_size=.01)
        self.assertGreaterEqual(min(r["coefficients"]),0.)
    def test_dimensions(self): self.assertEqual(len(estimate(A,[A,B])["coefficients"]),2)
    def test_empty(self):
        with self.assertRaises(ValueError): estimate([],[])
    def test_no_columns(self):
        with self.assertRaises(ValueError): estimate(A,[])
    def test_misaligned(self):
        with self.assertRaises(ValueError): estimate(A,[[1.]])
    def test_bool_iter(self):
        with self.assertRaises(ValueError): estimate(A,[A],iterations=True)
    def test_zero_iterations(self):
        with self.assertRaises(ValueError): estimate(A,[A],iterations=0)
    def test_excessive_iterations(self):
        with self.assertRaises(ValueError): estimate(A,[A],iterations=10001)
    def test_bool_step(self):
        with self.assertRaises(ValueError): estimate(A,[A],step_size=True)
    def test_negative_step(self):
        with self.assertRaises(ValueError): estimate(A,[A],step_size=-.01)
    def test_nan(self):
        with self.assertRaises(ValueError): estimate([1,float("nan")],[A])
    def test_boolean_observed(self):
        with self.assertRaises(ValueError): estimate([True,1],[A])
if __name__=="__main__":unittest.main()
