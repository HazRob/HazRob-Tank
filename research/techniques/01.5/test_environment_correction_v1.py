import unittest
from math import exp
from environment_correction_v1 import normalize_rs,fit_coefficients

class EnvironmentTests(unittest.TestCase):
    def test_zero_coefficients(self): self.assertAlmostEqual(normalize_rs(10000,25,50,1013.25),10000)
    def test_at_reference(self): self.assertAlmostEqual(normalize_rs(10000,25,50,1013.25,coefficients=(.1,.2,.3)),10000)
    def test_temperature(self): self.assertAlmostEqual(normalize_rs(10000,26,50,1013.25,coefficients=(.1,0,0)),10000*exp(-.1))
    def test_humidity(self): self.assertAlmostEqual(normalize_rs(10000,25,60,1013.25,coefficients=(0,.01,0)),10000*exp(-.1))
    def test_pressure(self): self.assertAlmostEqual(normalize_rs(10000,25,50,1023.25,coefficients=(0,0,.01)),10000*exp(-.1))
    def test_zero_rs(self):
        with self.assertRaises(ValueError): normalize_rs(0,25,50,1013.25)
    def test_bad_humidity(self):
        with self.assertRaises(ValueError): normalize_rs(10000,25,101,1013.25)
    def test_bad_pressure(self):
        with self.assertRaises(ValueError): normalize_rs(10000,25,50,0)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): normalize_rs(10000,float("nan"),50,1013.25)
    def test_extreme_coefficient(self):
        with self.assertRaises(ValueError): normalize_rs(10000,30,50,1013.25,coefficients=(100,0,0))
    def test_fit_synthetic(self):
        rows=[]
        for dt,dh,dp in [(1,0,0),(0,1,0),(0,0,1),(1,1,1),(2,3,4),(3,2,1)]:
            rs=10000*exp(.01*dt+.02*dh+.03*dp)
            rows.append((rs,25+dt,50+dh,1013.25+dp,10000))
        b=fit_coefficients(rows)
        for got,want in zip(b,(.01,.02,.03)): self.assertAlmostEqual(got,want,places=8)
    def test_rank_deficient(self):
        with self.assertRaises(ValueError): fit_coefficients([(10000,25,50,1013.25,10000)]*5)

if __name__=="__main__": unittest.main()
