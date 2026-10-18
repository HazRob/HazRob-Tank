import unittest
from sensitivity_curves_v1 import ratio_at_ppm, ppm_from_ratio

P=[(10,4),(100,2),(1000,1)]
class CurveTests(unittest.TestCase):
    def test_node(self): self.assertEqual(ratio_at_ppm(P,100),2)
    def test_midpoint(self): self.assertAlmostEqual(ratio_at_ppm(P,100**.5*10**.5),8**.5)
    def test_inverse_node(self): self.assertEqual(ppm_from_ratio(P,2),100)
    def test_inverse_roundtrip(self):
        for x in (10,30,100,300,1000):
            self.assertAlmostEqual(ppm_from_ratio(P,ratio_at_ppm(P,x)),x)
    def test_low_extrapolation_rejected(self):
        with self.assertRaises(ValueError): ratio_at_ppm(P,1)
    def test_high_extrapolation_rejected(self):
        with self.assertRaises(ValueError): ppm_from_ratio(P,0.1)
    def test_explicit_extrapolation(self): self.assertGreater(ratio_at_ppm(P,1,allow_extrapolation=True),4)
    def test_nonmonotonic_inverse(self):
        with self.assertRaises(ValueError): ppm_from_ratio([(10,2),(100,3),(1000,2)],2.5)
    def test_unsorted(self):
        with self.assertRaises(ValueError): ratio_at_ppm([(100,2),(10,4)],50)
    def test_duplicate_concentration(self):
        with self.assertRaises(ValueError): ratio_at_ppm([(10,2),(10,1)],10)
    def test_nonpositive(self):
        with self.assertRaises(ValueError): ratio_at_ppm([(10,1),(100,0)],20)
    def test_nonfinite(self):
        with self.assertRaises(ValueError): ratio_at_ppm(P,float("nan"))
    def test_increasing_curve(self):
        self.assertAlmostEqual(ppm_from_ratio([(10,1),(100,10)],5),50)

    def test_inverse_extrapolation_uses_first_segment_decreasing(self):
        nonlinear=[(10,8),(100,4),(1000,1)]
        self.assertAlmostEqual(ppm_from_ratio(nonlinear,16,allow_extrapolation=True),1)
    def test_inverse_extrapolation_uses_final_segment_decreasing(self):
        nonlinear=[(10,8),(100,4),(1000,1)]
        self.assertAlmostEqual(ppm_from_ratio(nonlinear,.25,allow_extrapolation=True),2000)
    def test_inverse_extrapolation_uses_first_segment_increasing(self):
        nonlinear=[(10,1),(100,2),(1000,8)]
        self.assertAlmostEqual(ppm_from_ratio(nonlinear,.5,allow_extrapolation=True),1)
    def test_inverse_extrapolation_uses_final_segment_increasing(self):
        nonlinear=[(10,1),(100,2),(1000,8)]
        self.assertAlmostEqual(ppm_from_ratio(nonlinear,32,allow_extrapolation=True),4000)

if __name__=="__main__": unittest.main()
