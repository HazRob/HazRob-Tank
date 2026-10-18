"""Research-only training-envelope OOD screen."""
from math import isfinite
def fit(rows, margin=0.1):
    data=[tuple(x) for x in rows]
    if len(data)<3 or not 0<=margin<10:raise ValueError("invalid training data or margin")
    n=len(data[0])
    if n<1 or any(len(x)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x) for x in data):raise ValueError("finite aligned features required")
    low=[min(x[j] for x in data) for j in range(n)]
    high=[max(x[j] for x in data) for j in range(n)]
    spans=[max(high[j]-low[j],1e-9) for j in range(n)]
    return {"low":[v-margin*s for v,s in zip(low,spans)],"high":[v+margin*s for v,s in zip(high,spans)],"n":n}
def screen(model, row):
    x=list(row)
    if len(x)!=model["n"] or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("invalid query")
    bad=[j for j,v in enumerate(x) if v<model["low"][j] or v>model["high"][j]]
    return {"status":"OUTSIDE_TRAINING_ENVELOPE" if bad else "INSIDE_ENVELOPE_NOT_PROOF_OF_ID","violating_features":bad}
