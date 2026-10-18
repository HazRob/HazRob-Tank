"""Research-only causal MQ signal filters. No production integration."""
from collections import deque
from math import isfinite
from statistics import median

def _validate(samples):
    values=list(samples)
    if not values or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in values):
        raise ValueError("nonempty finite numeric signal required")
    return values

def causal_median(samples,window=3):
    if not isinstance(window,int) or isinstance(window,bool) or window<1: raise ValueError("invalid window")
    q=deque(maxlen=window);out=[]
    for v in _validate(samples):
        q.append(v);out.append(float(median(q)))
    return out

def causal_ema(samples,alpha=.25):
    if not isinstance(alpha,(int,float)) or isinstance(alpha,bool) or not isfinite(alpha) or not 0<alpha<=1:
        raise ValueError("invalid alpha")
    xs=_validate(samples);out=[float(xs[0])]
    for x in xs[1:]:out.append(alpha*x+(1-alpha)*out[-1])
    return out

def causal_moving_average(samples,window=3):
    if not isinstance(window,int) or isinstance(window,bool) or window<1: raise ValueError("invalid window")
    q=deque(maxlen=window);out=[]
    for v in _validate(samples):
        q.append(v);out.append(sum(q)/len(q))
    return out
