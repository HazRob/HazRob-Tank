"""Ordered 8-sensor MQ response fingerprint; research only."""
from math import isfinite,log,sqrt
SENSORS=("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135")

def build_fingerprint(baseline_rs, exposure_rs, *, sensor_ids=SENSORS):
    """Log-fold Rs response, positive or negative; no chemical ID inferred."""
    if tuple(sensor_ids)!=SENSORS: raise ValueError("Tank sensor order/schema mismatch")
    b=list(baseline_rs);e=list(exposure_rs)
    if len(b)!=8 or len(e)!=8: raise ValueError("eight sensors required")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0 for v in b+e):
        raise ValueError("invalid Rs")
    values=tuple(log(x/y) for x,y in zip(e,b))
    return {"sensor_ids":SENSORS,"features":values,"feature_type":"log(Rs_exposure/Rs_baseline)"}

def cosine_similarity(a,b):
    """Signed cosine; returns None for zero-norm fingerprints."""
    x=tuple(a);y=tuple(b)
    if len(x)!=8 or len(y)!=8 or any(not isfinite(v) for v in x+y):
        raise ValueError("invalid fingerprints")
    nx=sqrt(sum(v*v for v in x));ny=sqrt(sum(v*v for v in y))
    if nx==0 or ny==0:return None
    return max(-1.,min(1.,sum(v*w for v,w in zip(x,y))/(nx*ny)))
