"""Research-only nonnegative two-component grid-search unmixing."""
from math import isfinite
def unmix_two(observed,a,b,step=0.05):
    y=list(observed);x=list(a);z=list(b)
    if not y or len(y)!=len(x) or len(y)!=len(z) or not isinstance(step,(int,float)) or isinstance(step,bool) or not isfinite(step) or not 0<step<=1:raise ValueError("invalid inputs")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in y+x+z):raise ValueError("finite features required")
    count=int(1/step)
    if count>300:
        raise ValueError("grid resolution exceeds 301-step research compute budget")
    levels=[i*step for i in range(count+1)]
    if levels[-1]<1-1e-12:
        levels.append(1.)
    else:
        levels[-1]=1.
    best=None
    for u in levels:
        for v in levels:
            error=sum((target-u*p-v*q)**2 for target,p,q in zip(y,x,z))
            if best is None or error<best[0]:best=(error,u,v)
    return {"squared_error":best[0],"coefficient_a":best[1],"coefficient_b":best[2],
            "warning":"Nonnegative linear-response toy model; MQ mixture responses can be nonlinear and nonidentifiable."}
