"""Research-only deterministic GRU cell forward primitive (no training)."""
from math import exp,isfinite,tanh
def sigmoid(x):
    return 1/(1+exp(-x)) if x>=0 else exp(x)/(1+exp(x))
def gru_scalar_step(x,h,weights):
    needed={"wz","uz","bz","wr","ur","br","wh","uh","bh"}
    if not isinstance(weights,dict) or set(weights)!=needed:
        raise ValueError("nine GRU weights required")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in [x,h]+list(weights.values())):
        raise ValueError("finite scalar parameters required")
    z=sigmoid(weights["wz"]*x+weights["uz"]*h+weights["bz"])
    r=sigmoid(weights["wr"]*x+weights["ur"]*h+weights["br"])
    candidate=tanh(weights["wh"]*x+weights["uh"]*(r*h)+weights["bh"])
    return (1-z)*h+z*candidate
