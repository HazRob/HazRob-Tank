"""Research-only sim-to-real distribution-gap diagnostic."""
from math import sqrt,isfinite
def compare(simulated,observed):
    a=[list(x) for x in simulated];b=[list(x) for x in observed]
    if len(a)<2 or len(b)<2 or not a[0]:raise ValueError("at least two samples per domain")
    n=len(a[0])
    if any(len(x)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x) for x in a+b):raise ValueError("aligned finite features required")
    ma=[sum(x[j] for x in a)/len(a) for j in range(n)]
    mb=[sum(x[j] for x in b)/len(b) for j in range(n)]
    rms=sqrt(sum((x-y)**2 for x,y in zip(ma,mb))/n)
    return {"mean_sim":ma,"mean_real":mb,"mean_gap_rmse":rms,
            "warning":"Descriptive mean gap is not proof of sim-to-real transfer; validate against real held-out sessions."}
