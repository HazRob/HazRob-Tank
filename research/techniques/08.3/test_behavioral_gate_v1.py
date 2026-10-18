import unittest
from math import tanh
from lstm_gru_v1 import sigmoid,gru_scalar_step
W={k:0. for k in ("wz","uz","bz","wr","ur","br","wh","uh","bh")}
class BehavioralGate(unittest.TestCase):
    def test_sigmoid_zero(self): self.assertEqual(sigmoid(0),.5)
    def test_sigmoid_symmetry(self): self.assertAlmostEqual(sigmoid(2)+sigmoid(-2),1.)
    def test_sigmoid_large(self): self.assertEqual(sigmoid(1000),1.)
    def test_sigmoid_negative_large(self): self.assertEqual(sigmoid(-1000),0.)
    def test_zero_hidden(self): self.assertEqual(gru_scalar_step(1.,0.,W),0)
    def test_decay_hidden(self): self.assertEqual(gru_scalar_step(1.,2.,W),1)
    def test_nonlinear_candidate(self):
        w=dict(W);w["wh"]=1.
        self.assertAlmostEqual(gru_scalar_step(1.,0.,w),.5*tanh(1.))
    def test_missing_weights(self):
        with self.assertRaises(ValueError): gru_scalar_step(0.,0.,{})
    def test_extra_weight(self):
        with self.assertRaises(ValueError): gru_scalar_step(0.,0.,dict(W,other=0))
    def test_wrong_weight_type(self):
        with self.assertRaises(ValueError): gru_scalar_step(0.,0.,[])
    def test_nan_weight(self):
        w=dict(W);w["wz"]=float("nan")
        with self.assertRaises(ValueError): gru_scalar_step(0.,0.,w)
    def test_bool_weight(self):
        w=dict(W);w["wz"]=True
        with self.assertRaises(ValueError): gru_scalar_step(0.,0.,w)
    def test_bool_input(self):
        with self.assertRaises(ValueError): gru_scalar_step(True,0.,W)
    def test_infinite_hidden(self):
        with self.assertRaises(ValueError): gru_scalar_step(0.,float("inf"),W)
if __name__=="__main__": unittest.main()
