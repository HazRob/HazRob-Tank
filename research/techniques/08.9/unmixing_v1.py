"""Research-only nonnegative two-component grid-search unmixing."""
from math import isfinite
def unmix_two(observed,a,b,step=0.05):
    y=list(observed);x=list(a);z=list(b)
    if not y or len(y)!=len(x) or len(y)!=len(z) or not isinstance(step,(int,float)) or not isfinite(step) or not 0<step<=1:raise ValueError("invalid inputs")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in y+x+z):raise ValueError("finite features required")
    count=int(1/step);best=None
    for i in range(count+1):
        for j in range(count+1):
            u=i*step;v=j*step
            error=sum((target-u*p-v*q)**2 for target,p,q in zip(y,x,z))
            if best is None or error<best[0]:best=(error,u,v)
    return {"squared_error":best[0],"coefficient_a":best[1],"coefficient_b":best[2],
            "warning":"Nonnegative linear-response toy model; MQ mixture responses can be nonlinear and nonidentifiable."}
