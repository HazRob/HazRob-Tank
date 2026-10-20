"""Only synthetic regression checks for 04.9/08.6/08.7, not Tank gas tests."""
import math
import unittest
import numpy as np
from physics_ml_sim_research_v2 import (
    GasCurve, ReferenceProfile, PhysicsResidualClassifier,
    simulate_first_order, compare_sim_to_real
)

IDS=("tank-X:mq-a:serial-1","tank-X:mq-b:serial-2")

def profile():
    return ReferenceProfile(IDS,{
        "gas-A":{IDS[0]:GasCurve(.5,-.4,1.,100.,"ref-a1"),
                 IDS[1]:GasCurve(.1,.2,1.,100.,"ref-a2")},
        "gas-B":{IDS[0]:GasCurve(-.8,.3,1.,100.,"ref-b1"),
                 IDS[1]:GasCurve(1.2,-.2,1.,100.,"ref-b2")},
    })

def synthetic_fit_data():
    p=profile()
    x=[];y=[]
    for gas in ("gas-A","gas-B"):
        for ppm in (2.,4.,8.,16.,32.):
            x.append([math.exp(p.curves[gas][s].log_ratio(ppm)) for s in IDS])
            y.append(gas)
    return p,np.asarray(x),y

class PhysicsModelsResearchTests(unittest.TestCase):
    def test_curve_rejects_invalid_bounds(self):
        with self.assertRaises(ValueError):GasCurve(0.,1.,0.,100.,"c")
    def test_curve_rejects_missing_reference(self):
        with self.assertRaises(ValueError):GasCurve(0.,1.,1.,100.,"")
    def test_curve_rejects_nan(self):
        with self.assertRaises(ValueError):GasCurve(float("nan"),1.,1.,100.,"c")
    def test_curve_finite_and_units(self):
        self.assertAlmostEqual(GasCurve(0.,1.,1.,100.,"c").log_ratio(math.e),1.)
    def test_curve_rejects_extrapolation(self):
        with self.assertRaises(ValueError):GasCurve(0.,1.,1.,100.,"c").log_ratio(101.)
    def test_profile_rejects_mismatched_sensor_types(self):
        with self.assertRaises(ValueError):
            ReferenceProfile(IDS,{"gas-A":{IDS[0]:GasCurve(0,1,1,10,"r")},
                                  "gas-B":{IDS[1]:GasCurve(0,1,1,10,"r")}})
    def test_profile_rejects_no_shared_concentration(self):
        a=GasCurve(0,1,1,10,"r")
        b=GasCurve(0,1,20,50,"r")
        with self.assertRaises(ValueError):
            ReferenceProfile(IDS,{"gas-A":{IDS[0]:a,IDS[1]:b},
                                  "gas-B":{IDS[0]:a,IDS[1]:a}})
    def test_candidates_do_not_take_true_label(self):
        p,x,y=synthetic_fit_data()
        out=p.candidate_features(x)
        self.assertEqual(out.shape,(10,4))
        self.assertTrue(np.isfinite(out).all())
        self.assertEqual(p.candidate_features(x[:2]).shape,(2,4))
    def test_zero_resistance_rejected(self):
        p,x,_=synthetic_fit_data()
        x[0,0]=0
        with self.assertRaises(ValueError):p.candidate_features(x)
    def test_classifier_uses_train_only_fit(self):
        p,x,y=synthetic_fit_data()
        model=PhysicsResidualClassifier(p).fit(x,y)
        q=model.predict(x)
        self.assertEqual(q.shape,(10,))
        self.assertTrue(set(q)<=set(p.gases))
        self.assertEqual(model.predict_proba(x).shape,(10,2))
    def test_classifier_unfitted_rejected(self):
        p,x,y=synthetic_fit_data()
        with self.assertRaises(ValueError):PhysicsResidualClassifier(p).predict(x)
    def test_classifier_rejects_label_not_in_reference(self):
        p,x,y=synthetic_fit_data();y[0]="gas-C"
        with self.assertRaises(ValueError):PhysicsResidualClassifier(p).fit(x,y)
    def test_simulator_baseline_rise_and_purge(self):
        p=profile();times=[0.,5.,10.,15.,20.,25.,30.,40.]
        a=simulate_first_order(p,"gas-A",10.,times,10.,20.,
                               [1.,1.],[2.,2.],[3.,3.])
        self.assertEqual(a.shape,(8,2))
        np.testing.assert_allclose(a[:3],1.,atol=1e-12)
        goal=math.exp(p.curves["gas-A"][IDS[0]].log_ratio(10.))
        self.assertGreater(abs(a[3,0]-1.),0)
        self.assertLess(abs(a[4,0]-goal),abs(a[3,0]-goal))
        self.assertLess(abs(a[-1,0]-1.),abs(a[4,0]-1.))
    def test_simulator_rejects_outside_calibration(self):
        p=profile()
        with self.assertRaises(ValueError):
            simulate_first_order(p,"gas-A",200.,[0.,1.,2.],0.5,1.5,
                                 [1.,1.],[1.,1.],[1.,1.])
    def test_simulator_rejects_negative_time_constant(self):
        p=profile()
        with self.assertRaises(ValueError):
            simulate_first_order(p,"gas-A",10.,[0.,1.,2.],0.5,1.5,
                                 [1.,1.],[-1.,1.],[1.,1.])
    def test_simulator_rejects_incorrect_time_order(self):
        p=profile()
        with self.assertRaises(ValueError):
            simulate_first_order(p,"gas-A",10.,[0.,2.,1.],0.5,1.5,
                                 [1.,1.],[1.,1.],[1.,1.])
    def test_sim_to_real_matched_units_error_zero(self):
        p=profile();t=[0.,5.,10.,15.,20.,25.]
        predicted=simulate_first_order(p,"gas-A",5.,t,5.,20.,
                                       [1.,1.],[2.,2.],[3.,3.])
        result=compare_sim_to_real(t,predicted,predicted*np.array([12000.,11000.]),
                                   [12000.,11000.],IDS,IDS,"synthetic-ref")
        self.assertAlmostEqual(result["pooled_log_ratio_rmse"],0.,places=10)
        self.assertFalse(result["real_tank_validation"])
    def test_sim_to_real_reports_nonzero_error(self):
        p=profile();t=[0.,5.,10.,15.,20.,25.]
        predicted=simulate_first_order(p,"gas-A",5.,t,5.,20.,
                                       [1.,1.],[2.,2.],[3.,3.])
        actual=predicted*np.array([12000.,11000.])*1.15
        result=compare_sim_to_real(t,predicted,actual,[12000.,11000.],
                                   IDS,IDS,"synthetic-ref")
        self.assertGreater(result["pooled_log_ratio_rmse"],.1)
    def test_sim_to_real_rejects_sensor_order_mismatch(self):
        with self.assertRaises(ValueError):
            compare_sim_to_real([0.,1.],[[1.,1.],[1.,1.]],[[1000.,1000.],[1000.,1000.]],
                                [1000.,1000.],IDS,IDS[::-1],"r")
    def test_sim_to_real_rejects_missing_r0(self):
        with self.assertRaises(ValueError):
            compare_sim_to_real([0.,1.],[[1.,1.],[1.,1.]],[[1000.,1000.],[1000.,1000.]],
                                [1000.],IDS,IDS,"r")
    def test_sim_to_real_rejects_unknown_reference(self):
        with self.assertRaises(ValueError):
            compare_sim_to_real([0.,1.],[[1.,1.],[1.,1.]],[[1000.,1000.],[1000.,1000.]],
                                [1000.,1000.],IDS,IDS,"")

if __name__=="__main__": unittest.main()
