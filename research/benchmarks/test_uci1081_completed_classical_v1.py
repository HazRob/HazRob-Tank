"""No source download needed for algorithm-contract tests."""
import unittest
import numpy as np
from uci1081_completed_classical_v1 import fit_predict,METHODS

def fixture():
    # 6 classes, three independent features and fixed noise, no test-label fit.
    x=[];y=[]
    for label in range(6):
        for rep in range(8):
            x.append([label*3.+rep*.015, (label%3)*5.+rep*.02,
                      (label//3)*4.-rep*.01,rep*.025])
            y.append("gas_"+str(label))
    q=[[label*3.+.04,(label%3)*5.+.04,(label//3)*4.-.01,.05] for label in range(6)]
    return x,y,q

class ClassicalTests(unittest.TestCase):
    def test_seven_methods(self):self.assertEqual(len(METHODS),7)
    def test_all_trainable_methods_predict_every_query(self):
        x,y,q=fixture()
        for method in METHODS:
            with self.subTest(method=method):
                labels=fit_predict(method,x,y,q)
                self.assertEqual(len(labels),6)
                self.assertTrue(all(s in set(y) for s in labels))
    def test_genuine_multiclass_lda(self):
        x,y,q=fixture()
        self.assertGreaterEqual(sum(a==b for a,b in zip(
            fit_predict("lda_multiclass",x,y,q),sorted(set(y)))),4)
    def test_plsda_multicomponent(self):
        x,y,q=fixture()
        self.assertEqual(len(fit_predict("plsda_multicomponent",x,y,q)),6)
    def test_svm_kernels(self):
        x,y,q=fixture()
        self.assertNotEqual(fit_predict("svm_linear",x,y,q),[])
        self.assertNotEqual(fit_predict("svm_rbf",x,y,q),[])
    def test_actual_xgboost_output(self):
        x,y,q=fixture()
        result=fit_predict("xgboost_multiclass",x,y,q)
        self.assertEqual(len(result),6)
    def test_majority_tie(self):
        x,y,q=fixture()
        self.assertEqual(fit_predict("majority",x,y,q),["gas_0"]*6)
    def test_unknown_method_rejected(self):
        x,y,q=fixture()
        with self.assertRaises(ValueError):
            fit_predict("not_a_model",x,y,q)
    def test_wrong_shape_should_fail(self):
        x,y,q=fixture()
        with self.assertRaises(Exception):
            fit_predict("svm_linear",x,y,[[1,2,3]])
    def test_training_classes_only(self):
        x,y,q=fixture()
        labels=fit_predict("xgboost_multiclass",x,y,q)
        self.assertTrue(set(labels)<=set(y))

if __name__=="__main__":unittest.main()
