"""Research-only causal dilated temporal convolution primitive."""
from math import isfinite
def causal_conv(signal,kernel,dilation=1):
    x=list(signal);k=list(kernel)
    if not x or not k or not isinstance(dilation,int) or isinstance(dilation,bool) or dilation<1 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x+k):raise ValueError("finite signals, kernel and positive dilation required")
    return [sum(k[j]*x[t-j*dilation] for j in range(len(k)) if t-j*dilation>=0) for t in range(len(x))]
def residual_block(signal,kernel,dilation=1):
    x=list(signal);y=causal_conv(x,kernel,dilation)
    return [a+max(0,b) for a,b in zip(x,y)]
