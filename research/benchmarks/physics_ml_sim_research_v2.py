"""Research-only actual algorithm primitives for technique IDs 04.9, 08.6, 08.7.

Physically explicit declared calibrations are REQUIRED. No UCI/TGS signal may be
silently relabelled Rs/R0 or ppm. Everything in this module remains UNVALIDATED
on the Tank; no reference certificate is authenticated by the software.
"""
from __future__ import annotations
import math
from dataclasses import dataclass
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler

@dataclass(frozen=True)
class GasCurve:
    """Sensor-specific log(Rs/R0) = intercept + slope * ln(ppm)."""
    intercept: float
    slope: float
    minimum_ppm: float
    maximum_ppm: float
    calibration_id: str
    def __post_init__(self):
        nums=(self.intercept,self.slope,self.minimum_ppm,self.maximum_ppm)
        if any(not isinstance(x,(float,int)) or isinstance(x,bool) or
               not math.isfinite(x) for x in nums):
            raise ValueError("finite physical calibration coefficients required")
        if self.minimum_ppm<=0 or self.maximum_ppm<=self.minimum_ppm:
            raise ValueError("positive nonempty calibrated ppm interval required")
        if not isinstance(self.calibration_id,str) or not self.calibration_id.strip():
            raise ValueError("physical calibration provenance required")
    def log_ratio(self,ppm):
        if not math.isfinite(ppm) or not self.minimum_ppm<=ppm<=self.maximum_ppm:
            raise ValueError("concentration outside measured calibration envelope")
        return self.intercept+self.slope*math.log(ppm)

class ReferenceProfile:
    """A declared physical curve for EVERY sensor and candidate gas."""
    def __init__(self,sensor_instance_ids,gas_curves):
        self.sensor_instance_ids=tuple(sensor_instance_ids)
        if (not self.sensor_instance_ids or
            any(not isinstance(s,str) or not s.strip() for s in self.sensor_instance_ids) or
            len(set(self.sensor_instance_ids))!=len(self.sensor_instance_ids)):
            raise ValueError("unique identified physical sensor instances required")
        if not isinstance(gas_curves,dict) or len(gas_curves)<2:
            raise ValueError("at least two declared candidate gases required")
        self.curves={}
        for gas,curves in gas_curves.items():
            if not isinstance(gas,str) or not gas.strip() or not isinstance(curves,dict):
                raise ValueError("invalid candidate reference profile")
            if set(curves)!=set(self.sensor_instance_ids):
                raise ValueError("incomplete or mismatched sensor reference")
            if not all(isinstance(c,GasCurve) for c in curves.values()):
                raise ValueError("GasCurve instances required")
            low=max(c.minimum_ppm for c in curves.values())
            high=min(c.maximum_ppm for c in curves.values())
            if not low<high:
                raise ValueError("candidate sensors have no common calibrated ppm interval")
            self.curves[gas]=dict(curves)
        self.gases=tuple(sorted(self.curves))

    def candidate_features(self,rs_over_r0,n_grid=21):
        """Test features use ALL gas hypotheses, never the true gas label.

        For every candidate, choose the minimum multi-sensor squared residual
        across its own valid concentration grid. The candidate's optimized
        concentration is NOT a certified ppm estimate.
        """
        values=np.asarray(rs_over_r0,dtype=float)
        if (values.ndim!=2 or values.shape[1]!=len(self.sensor_instance_ids)
                or len(values)<1 or not np.isfinite(values).all()
                or np.any(values<=0)):
            raise ValueError("positive actual Rs/R0 matrix [samples, sensors] required")
        if not isinstance(n_grid,int) or isinstance(n_grid,bool) or not 3<=n_grid<=200:
            raise ValueError("finite concentration grid resolution required")
        log_obs=np.log(values)
        per_gas=[]
        for gas in self.gases:
            curves=self.curves[gas]
            low=max(c.minimum_ppm for c in curves.values())
            high=min(c.maximum_ppm for c in curves.values())
            grid=np.exp(np.linspace(np.log(low),np.log(high),n_grid))
            # The grid was bounded before exp/ln; direct evaluation avoids
            # floating endpoint overshoot (e.g. 100.00000000000004).
            prediction=np.asarray([[curves[s].intercept+curves[s].slope*math.log(float(c))
                                    for s in self.sensor_instance_ids] for c in grid])
            distance=np.mean((log_obs[:,None,:]-prediction[None,:,:])**2,axis=2)
            per_gas.append(np.min(distance,axis=1))
        return np.column_stack([log_obs]+[np.asarray(per_gas).T])


class PhysicsResidualClassifier:
    """An actual trainable physics+ML classifier, NOT a fitted Tank gas model."""
    def __init__(self,profile):
        if not isinstance(profile,ReferenceProfile):
            raise TypeError("ReferenceProfile required")
        self.profile=profile
        self.scaler=None
        self.classifier=None
    def fit(self,rs_over_r0,labels):
        x=self.profile.candidate_features(rs_over_r0)
        y=np.asarray(labels)
        if y.ndim!=1 or len(y)!=len(x) or any(label not in self.profile.gases for label in y):
            raise ValueError("label/curve mismatch")
        if len(set(y))<2:
            raise ValueError("multiple gases needed to train classifier")
        self.scaler=StandardScaler().fit(x)  # TRAIN ONLY
        self.classifier=LogisticRegression(max_iter=1000,random_state=19).fit(
            self.scaler.transform(x),y)
        return self
    def predict(self,rs_over_r0):
        if self.classifier is None: raise ValueError("fit before predict")
        x=self.profile.candidate_features(rs_over_r0)
        return self.classifier.predict(self.scaler.transform(x))
    def predict_proba(self,rs_over_r0):
        if self.classifier is None: raise ValueError("fit before predict")
        x=self.profile.candidate_features(rs_over_r0)
        return self.classifier.predict_proba(self.scaler.transform(x))


def _times(t):
    a=np.asarray(t,dtype=float)
    if a.ndim!=1 or len(a)<2 or not np.isfinite(a).all() or np.any(np.diff(a)<=0) or a[0]<0:
        raise ValueError("finite strictly ascending nonnegative seconds required")
    return a


def simulate_first_order(profile,gas,ppm,time_s,exposure_start_s,exposure_stop_s,
                         baseline_rs_over_r0,tau_rise_s,tau_recovery_s):
    """First-order research simulator 08.6; parameters MUST be independently sourced.

    Piecewise constant stimulus, explicit rise/recovery constants and units.
    Sampling at transition time applies the target state to subsequent intervals.
    """
    if not isinstance(profile,ReferenceProfile) or gas not in profile.curves:
        raise ValueError("reference candidate absent")
    t=_times(time_s)
    if not 0<=exposure_start_s<exposure_stop_s<=t[-1]:
        raise ValueError("invalid injection/purge interval")
    n=len(profile.sensor_instance_ids)
    base=np.asarray(baseline_rs_over_r0,dtype=float)
    tr=np.asarray(tau_rise_s,dtype=float)
    tf=np.asarray(tau_recovery_s,dtype=float)
    if any(a.shape!=(n,) or not np.isfinite(a).all() for a in (base,tr,tf)):
        raise ValueError("one finite constant per physical instance required")
    if np.any(base<=0) or np.any(tr<=0) or np.any(tf<=0):
        raise ValueError("positive baseline and time constants required")
    target=np.array([math.exp(profile.curves[gas][s].log_ratio(ppm))
                     for s in profile.sensor_instance_ids])
    # Exact piecewise exponential integration even if events fall between samples.
    states=np.empty((len(t),n))
    state=base.copy()
    old=0.
    for i,now in enumerate(t):
        cuts=[old]+[v for v in (exposure_start_s,exposure_stop_s) if old<v<now]+[now]
        for a,b in zip(cuts[:-1],cuts[1:]):
            active=exposure_start_s<=a<exposure_stop_s
            goal=target if active else base
            tau=tr if active else tf
            state=goal+(state-goal)*np.exp(-(b-a)/tau)
        states[i]=state
        old=now
    return states


def compare_sim_to_real(time_s,measured_time_s,simulated_ratio,measured_rs_ohm,r0_ohm,
                        simulated_instance_ids,measured_instance_ids,
                        measurement_reference_id):
    """08.7: no fitting or rescaling of validation records, exact aligned times.

    Returns descriptive log-ratio residuals; cannot itself validate a physical
    simulator without separately audited calibration and independent sessions.
    """
    times=_times(time_s)
    measured_times=_times(measured_time_s)
    if measured_times.shape!=times.shape or not np.allclose(
            measured_times,times,rtol=0.,atol=1e-9):
        raise ValueError("independent measured and simulated timestamps are not aligned")
    if not isinstance(measurement_reference_id,str) or not measurement_reference_id.strip():
        raise ValueError("independent measurement reference ID required")
    ids1=tuple(simulated_instance_ids)
    ids2=tuple(measured_instance_ids)
    if not ids1 or ids1!=ids2 or len(set(ids1))!=len(ids1):
        raise ValueError("same ordered physical sensor instances required")
    a=np.asarray(simulated_ratio,dtype=float)
    b=np.asarray(measured_rs_ohm,dtype=float)
    r0=np.asarray(r0_ohm,dtype=float)
    if a.shape!=b.shape or a.shape!=(len(times),len(ids1)) or r0.shape!=(len(ids1),):
        raise ValueError("aligned time/sensor geometry required")
    if (not np.isfinite(a).all() or not np.isfinite(b).all() or not np.isfinite(r0).all()
            or np.any(a<=0) or np.any(b<=0) or np.any(r0<=0)):
        raise ValueError("positive finite simulated ratio, real Rs ohm and R0 ohm required")
    delta=np.log(a)-np.log(b/r0[None,:])
    per=np.sqrt(np.mean(delta*delta,axis=0))
    return {
        "log_ratio_rmse_per_sensor":dict(zip(ids1,map(float,per))),
        "pooled_log_ratio_rmse":float(np.sqrt(np.mean(delta*delta))),
        "n_timepoints":len(times),
        "measurement_reference_id":measurement_reference_id,
        "independent_source_authenticated":False,
        "real_tank_validation":False,
        "interpretation":"Numerical comparison only; no physical Tank accuracy or gas safety claim."
    }
