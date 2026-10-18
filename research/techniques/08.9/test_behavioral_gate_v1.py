import unittest
from unmixing_v1 import unmix_two
A=[1.,0.]
B=[0.,1.]
class BehavioralGate(unittest.TestCase):
    def test_pure_a(self): self.assertAlmostEqual(unmix_two(A,A,B)["coefficient_a"],1.)
    def test_pure_b(self): self.assertAlmostEqual(unmix_two(B,A,B)["coefficient_b"],1.)
    def test_mixture(self):
        out=unmix_two([.6,.4],A,B,step=.2)
        self.assertAlmostEqual(out["coefficient_a"],.6);self.assertAlmostEqual(out["coefficient_b"],.4)
    def test_exact_error(self): self.assertAlmostEqual(unmix_two([.6,.4],A,B,step=.2)["squared_error"],0.)
    def test_nondivisor_endpoint(self): self.assertEqual(unmix_two(A,A,B,step=.3)["coefficient_a"],1.)
    def test_nonnegative(self):
        out=unmix_two([.3,.7],A,B)
        self.assertGreaterEqual(out["coefficient_a"],0);self.assertGreaterEqual(out["coefficient_b"],0)
    def test_zero_signal(self):
        out=unmix_two([0.,0.],A,B);self.assertEqual((out["coefficient_a"],out["coefficient_b"]),(0.,0.))
    def test_empty(self):
        with self.assertRaises(ValueError): unmix_two([],[],[])
    def test_mismatched(self):
        with self.assertRaises(ValueError): unmix_two([1.,1.],[1.],[1.,1.])
    def test_nan(self):
        with self.assertRaises(ValueError): unmix_two([float("nan"),1.],A,B)
    def test_zero_step(self):
        with self.assertRaises(ValueError): unmix_two(A,A,B,step=0)
    def test_bool_step(self):
        with self.assertRaises(ValueError): unmix_two(A,A,B,step=True)
    def test_oversmall_step_budget(self):
        with self.assertRaises(ValueError): unmix_two(A,A,B,step=.001)
    def test_negative_step(self):
        with self.assertRaises(ValueError): unmix_two(A,A,B,step=-.1)
    def test_large_step(self):
        with self.assertRaises(ValueError): unmix_two(A,A,B,step=2)
if __name__=="__main__": unittest.main()
