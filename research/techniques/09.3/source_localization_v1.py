"""Research-only weighted spatial centroid of positive sensor responses."""
from math import isfinite
def weighted_centroid(samples):
    rows=list(samples)
    if len(rows)<2:raise ValueError("at least two spatial samples required")
    for r in rows:
        if not isinstance(r,(list,tuple)) or len(r)!=3 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in r) or r[2]<0:raise ValueError("finite coordinates and nonnegative signals required")
    total=sum(r[2] for r in rows)
    if total<=0:raise ValueError("positive total signal required")
    return {"x":sum(r[0]*r[2] for r in rows)/total,"y":sum(r[1]*r[2] for r in rows)/total,
            "warning":"Signal centroid is not leak-source location; wind, turbulence, sensor lag and obstacles can bias it."}
