import unittest
from filtering_v1 import causal_median,causal_ema,causal_moving_average

class FilteringTests(unittest.TestCase):
    def test_median_spike(self): self.assertEqual(causal_median([1,1,100,1,1],3),[1,1,1,1,1])
    def test_median_causal(self): self.assertEqual(causal_median([1,3,5,7],3),[1,2,3,5])
    def test_median_window_one(self): self.assertEqual(causal_median([1,3],1),[1,3])
    def test_ema_identity(self): self.assertEqual(causal_ema([1,2,3],1),[1,2,3])
    def test_ema_step(self): self.assertEqual(causal_ema([0,10,10],.5),[0,5,7.5])
    def test_average(self): self.assertEqual(causal_moving_average([1,3,5,7],3),[1,2,3,5])
    def test_average_window_one(self): self.assertEqual(causal_moving_average([2,4],1),[2,4])
    def test_short_window(self): self.assertEqual(causal_median([5],3),[5])
    def test_empty(self):
        for fn in (causal_median,causal_ema,causal_moving_average):
            with self.subTest(fn=fn),self.assertRaises(ValueError): fn([])
    def test_nan(self):
        with self.assertRaises(ValueError): causal_ema([1,float("nan")])
    def test_inf(self):
        with self.assertRaises(ValueError): causal_median([1,float("inf")])
    def test_bool(self):
        with self.assertRaises(ValueError): causal_moving_average([True,2])
    def test_invalid_window(self):
        with self.assertRaises(ValueError): causal_median([1,2],0)
    def test_invalid_alpha(self):
        with self.assertRaises(ValueError): causal_ema([1,2],0)
    def test_future_independence(self):
        a=causal_ema([1,2,3]);b=causal_ema([1,2,3,100]);self.assertEqual(a,b[:3])
if __name__=="__main__": unittest.main()
