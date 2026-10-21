"""03.3: necessary linear rank/conditioning check for two candidate gas fingerprints.

A rank pass does NOT establish physical mixture separability or chemical identity.
"""
from math import isfinite,sqrt
def two_component_identifiability(a,b,*,sensor_ids,threshold_condition=30.):
    x=tuple(a);y=tuple(b);ids=tuple(sensor_ids)
    if len(ids)<2 or len(set(ids))!=len(ids) or len(x)!=len(ids) or len(y)!=len(ids):
        raise ValueError("need two independent channels with aligned candidate fingerprints")
    if any(isinstance(z,bool) or not isinstance(z,(int,float)) or not isfinite(z) for z in x+y):
        raise ValueError("invalid response")
    if isinstance(threshold_condition,bool) or not isinstance(threshold_condition,(int,float)) or not isfinite(threshold_condition) or threshold_condition<=1:
        raise ValueError("invalid conditioning limit")
    xx=sum(z*z for z in x);yy=sum(z*z for z in y);xy=sum(z*w for z,w in zip(x,y))
    if not xx or not yy:
        return {"status":"NON_IDENTIFIABLE_ZERO_RESPONSE","condition_number":None,
                "linear_rank":1 if max(xx,yy)>0 else 0,"ppm_approved":False}
    trace=xx+yy;det=xx*yy-xy*xy
    if det<0 and abs(det)<=1e-10*xx*yy:det=0
    if det<0:raise ValueError("numerical Gram instability")
    delta=sqrt(max(0.,trace*trace-4*det))
    high=(trace+delta)/2.;low=det/high if high else 0
    condition=sqrt(high/low) if low>0 else float("inf")
    ok=condition<=threshold_condition
    return {"status":"LINEAR_IDENTIFIABILITY_PLAUSIBLE_NOT_PROVEN" if ok else "NON_IDENTIFIABLE_OR_ILL_CONDITIONED",
            "linear_rank":2 if low>0 else 1,"condition_number":condition,
            "threshold_condition":threshold_condition,"ppm_approved":False,
            "note":"Necessary 2-column linear mixture matrix diagnostic, no real mixture proof"}
