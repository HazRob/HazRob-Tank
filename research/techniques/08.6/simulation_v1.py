"""Research-only synthetic sensor response simulator, not a validated physical gas model."""
from math import exp,isfinite
def simulate(exposures, sensitivity, *, baseline=1., tau_seconds=5., step_seconds=1.):
    x=list(exposures)
    if not x or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for v in x+[sensitivity,baseline,tau_seconds,step_seconds]) or tau_seconds<=0 or step_seconds<=0:raise ValueError("finite nonnegative exposure and positive time constants required")
    a=1-exp(-step_seconds/tau_seconds);state=float(baseline);out=[]
    for concentration in x:
        target=baseline+sensitivity*concentration
        state+=a*(target-state)
        out.append(state)
    return {"responses":out,"warning":"First-order toy model only; no MQ chemistry, selectivity, temperature, humidity or cross-sensitivity validation."}
