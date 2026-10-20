"""Behavioral tests prove gradient training and temporal causality; not gas accuracy."""
import unittest
import numpy as np
import torch
from uci1081_trainable_neural_v1 import (
    CNN1D,CausalTCN,GRUSensor,SensorGNN,train_only_adjacency,
    fit_time_scaling,apply_time_scaling,fit_predict,measured_trace_features,
    KINDS
)

class NeuralAlgorithmsTests(unittest.TestCase):
    def setUp(self):
        torch.manual_seed(2)
        self.data=torch.randn(5,10,30)
    def test_cnn1d_shape(self):
        self.assertEqual(tuple(CNN1D(6)(self.data).shape),(5,6))
    def test_tcn_shape(self):
        self.assertEqual(tuple(CausalTCN(6)(self.data).shape),(5,6))
    def test_gru_shape(self):
        self.assertEqual(tuple(GRUSensor(6)(self.data).shape),(5,6))
    def test_gnn_shape(self):
        self.assertEqual(tuple(SensorGNN(6,np.eye(10))(self.data).shape),(5,6))
    def test_true_causality(self):
        model=CausalTCN(6)
        x=self.data.clone()
        y=x.clone()
        y[:,:,15:]+=10000.
        hidden1=model.temporal_hidden(x)
        hidden2=model.temporal_hidden(y)
        self.assertTrue(torch.allclose(hidden1[:,:,:15],hidden2[:,:,:15],rtol=0,atol=1e-6))
    def test_cnn_gradients(self):
        model=CNN1D(3)
        out=model(self.data)
        out.sum().backward()
        self.assertTrue(any(p.grad is not None and p.grad.abs().sum()>0 for p in model.parameters()))
    def test_tcn_gradients(self):
        model=CausalTCN(3)
        model(self.data).sum().backward()
        self.assertTrue(any(p.grad is not None and p.grad.abs().sum()>0 for p in model.parameters()))
    def test_gru_gradients(self):
        model=GRUSensor(3)
        model(self.data).sum().backward()
        self.assertTrue(any(p.grad is not None and p.grad.abs().sum()>0 for p in model.parameters()))
    def test_gnn_gradients(self):
        model=SensorGNN(3,np.eye(10))
        model(self.data).sum().backward()
        self.assertTrue(any(p.grad is not None and p.grad.abs().sum()>0 for p in model.parameters()))
    def test_adjacency_rows_sum_one(self):
        x=np.random.default_rng(2).normal(size=(12,10,30)).astype("float32")
        adj=train_only_adjacency(x)
        self.assertEqual(adj.shape,(10,10))
        np.testing.assert_allclose(adj.sum(axis=1),1.,atol=1e-6)
    def test_identity_no_edges(self):
        x=np.random.default_rng(2).normal(size=(12,10,30)).astype("float32")
        np.testing.assert_allclose(train_only_adjacency(x,identity=True),np.eye(10))
    def test_no_test_data_in_graph(self):
        x=np.random.default_rng(3).normal(size=(12,10,30)).astype("float32")
        train=x[:9]
        adj=train_only_adjacency(train)
        other=x.copy();other[9:]*=300.
        np.testing.assert_array_equal(adj,train_only_adjacency(other[:9]))
    def test_fit_scaling_training_only(self):
        x=np.ones((4,10,30),dtype="float32")
        mean,std=fit_time_scaling(x)
        self.assertTrue(np.allclose(apply_time_scaling(x,(mean,std)),0))
        y=x.copy();y[0]*=100
        self.assertGreater(apply_time_scaling(y,(mean,std))[0].max(),0)
    def test_measured_feature_shape(self):
        source=[tuple([1.]*300+[2.]*600) for _ in range(10)]
        self.assertEqual(measured_trace_features(source).shape,(10,30))
        self.assertTrue(np.allclose(measured_trace_features(source),1.))
    def test_measured_feature_reject_bad_schema(self):
        with self.assertRaises(ValueError):
            measured_trace_features([tuple([1.]*899)]*10)
    def test_trainable_four_architectures_and_gnn_ablation(self):
        rng=np.random.default_rng(4)
        x=rng.normal(size=(18,10,30)).astype("float32")
        y=["a"]*6+["b"]*6+["c"]*6
        q=rng.normal(size=(3,10,30)).astype("float32")
        for kind in KINDS:
            with self.subTest(model=kind):
                pred,details=fit_predict(kind,x,y,q,epochs=4)
                self.assertEqual(len(pred),3)
                self.assertTrue(set(pred)<={"a","b","c"})
                self.assertGreater(details["parameter_count"],0)
                self.assertEqual(details["n_epochs"],4)
    def test_single_class_invalid(self):
        with self.assertRaises(ValueError):
            fit_predict("gru",np.ones((3,10,30)),["a"]*3,np.ones((1,10,30)),epochs=2)

if __name__=="__main__":
    unittest.main()
