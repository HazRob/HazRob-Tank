"""Research-only pure-Python single-channel valid 1D convolution primitive."""
from math import isfinite
def convolve_valid(signal,kernel,bias=0.0,activation="relu"):
    x=list(signal);k=list(kernel)
    if not k or len(x)<len(k) or activation not in ("relu","linear") or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x+k+[bias]):raise ValueError("invalid convolution input")
    out=[sum(x[i+j]*w for j,w in enumerate(k))+bias for i in range(len(x)-len(k)+1)]
    return [max(0,v) for v in out] if activation=="relu" else out
def global_average_pool(values):
    x=list(values)
    if not x or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("invalid feature map")
    return sum(x)/len(x)
