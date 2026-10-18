import unittest
from resource_profiling_v1 import estimate
class BehavioralGate(unittest.TestCase):
    def test_weights(self): self.assertEqual(estimate(parameters=10)["weights_bytes"],40)
    def test_activation(self): self.assertEqual(estimate(parameters=10,activations_bytes=100)["total_estimated_bytes"],140)
    def test_bytes_per_weight(self): self.assertEqual(estimate(parameters=10,bytes_per_parameter=2)["weights_bytes"],20)
    def test_throughput(self): self.assertEqual(estimate(parameters=10,operations_per_inference=120,frequency_hz=5)["operations_per_second"],600)
    def test_noninteger_hertz(self): self.assertEqual(estimate(parameters=1,operations_per_inference=10,frequency_hz=.5)["operations_per_second"],5)
    def test_zero_parameters(self):
        with self.assertRaises(ValueError): estimate(parameters=0)
    def test_boolean_parameters(self):
        with self.assertRaises(ValueError): estimate(parameters=True)
    def test_negative_activation(self):
        with self.assertRaises(ValueError): estimate(parameters=10,activations_bytes=-1)
    def test_fraction_bytes(self):
        with self.assertRaises(ValueError): estimate(parameters=10,bytes_per_parameter=.1)
    def test_zero_frequency(self):
        with self.assertRaises(ValueError): estimate(parameters=10,frequency_hz=0)
    def test_nan_frequency(self):
        with self.assertRaises(ValueError): estimate(parameters=10,frequency_hz=float("nan"))
    def test_bool_frequency(self):
        with self.assertRaises(ValueError): estimate(parameters=10,frequency_hz=True)
    def test_unknown_power(self): self.assertIn("power",estimate(parameters=10)["warning"])
if __name__=="__main__": unittest.main()
