"""08.8: compare single vs binary convex fingerprints as *hypotheses* only.
A low mixture residual can ALSO result from unknown gas, drift and correlations.
"""
from math import isfinite,sqrt
def propose_binary_mixture_hypothesis(observed,component_a,component_b,*,
                                     source_id,sensor_ids,reference_r0_id,heater_phase,steps=20):
    if not source_id or not reference_r0_id or heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("source/R0/heater context required")
    a=list(component_a);b=list(component_b);y=list(observed)
    n=len(y)
    if n<2 or len(a)!=n or len(b)!=n or len(sensor_ids)!=n or len(set(sensor_ids))!=n:
        raise ValueError("matched MQ array required")
    if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in y+a+b):
        raise ValueError("finite features needed")
    if isinstance(steps,bool) or not isinstance(steps,int) or steps<10 or steps>1000:
        raise ValueError("research resolution must be in range 10..1000")
    def norm(xs):return sum((u-v)**2 for u,v in zip(y,xs))
    best_single=min(norm(a),norm(b))
    solutions=[(norm([fraction*a[i]+(1-fraction)*b[i] for i in range(n)]),fraction)
               for fraction in (k/steps for k in range(steps+1))]
    min_loss,fraction=min(solutions)
    return {"pure_best_rmse":sqrt(best_single/n),"convex_binary_rmse":sqrt(min_loss/n),
            "proposed_fraction_a":fraction,
            "mixture_is_verified":False,"unknown_gas_excluded":False,
            "mixture_reference_ppm":None,"status":"MIXTURE_HYPOTHESIS_ONLY",
            "Tank_G3":False}
