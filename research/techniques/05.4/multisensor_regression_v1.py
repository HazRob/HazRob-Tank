"""Research-only ridge regression for known-gas multisensor log concentration."""
from math import isfinite,log10,sqrt

def positive(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and v>0

def _solve(a,b):
    n=len(b);m=[list(a[i])+[b[i]] for i in range(n)]
    for k in range(n):
        pivot=max(range(k,n),key=lambda i:abs(m[i][k]))
        if abs(m[pivot][k])<1e-12:raise ValueError("singular matrix; use positive ridge or more diverse training data")
        m[k],m[pivot]=m[pivot],m[k]
        v=m[k][k]
        for j in range(k,n+1):m[k][j]/=v
        for i in range(n):
            if i==k:continue
            v=m[i][k]
            for j in range(k,n+1):m[i][j]-=v*m[k][j]
    return [m[i][n] for i in range(n)]

def fit_multisensor(rows,concentrations,*,sensor_ids,gas_label,ridge=1.0):
    ids=tuple(sensor_ids);data=[tuple(row) for row in rows];target=list(concentrations)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(s,str) or not s for s in ids):
        raise ValueError("unique nonempty sensor IDs required")
    if not isinstance(gas_label,str) or not gas_label:raise ValueError("known gas label required")
    d=len(ids);n=len(data)
    if n<max(5,d+2) or n!=len(target) or any(len(row)!=d for row in data):
        raise ValueError("insufficient or misaligned training samples")
    if any(not positive(v) for row in data for v in row) or any(not positive(v) for v in target):
        raise ValueError("positive finite Rs/R0 ratios and ppm required")
    if len(set(target))<3:raise ValueError("at least three known concentration levels required")
    if isinstance(ridge,bool) or not isinstance(ridge,(int,float)) or not isfinite(ridge) or ridge<=0:
        raise ValueError("strictly positive ridge required")
    x=[[log10(v) for v in row] for row in data];y=[log10(v) for v in target]
    means=[sum(row[j] for row in x)/n for j in range(d)]
    scales=[sqrt(sum((row[j]-means[j])**2 for row in x)/n) or 1. for j in range(d)]
    z=[[1.]+[(row[j]-means[j])/scales[j] for j in range(d)] for row in x]
    gram=[[sum(row[i]*row[j] for row in z)+(ridge if i==j and i>0 else 0.) for j in range(d+1)] for i in range(d+1)]
    rhs=[sum(row[i]*yy for row,yy in zip(z,y)) for i in range(d+1)]
    coef=_solve(gram,rhs)
    residual=[yy-sum(a*b for a,b in zip(coef,row)) for yy,row in zip(y,z)]
    return {"sensor_ids":ids,"gas_label":gas_label,"coefficients":coef,"means":means,
            "scales":scales,"min_ppm":min(target),"max_ppm":max(target),
            "training_log10_rmse":sqrt(sum(v*v for v in residual)/n),"n":n,"ridge":ridge}

def estimate_multisensor(rows,model):
    ids=model["sensor_ids"];d=len(ids);out=[]
    for row in rows:
        values=tuple(row)
        if len(values)!=d or any(not positive(v) for v in values):
            raise ValueError("invalid or misaligned sensor ratios")
        z=[1.]+[(log10(values[j])-model["means"][j])/model["scales"][j] for j in range(d)]
        logppm=sum(a*b for a,b in zip(model["coefficients"],z))
        if not isfinite(logppm):raise ValueError("invalid prediction")
        if logppm<log10(model["min_ppm"])-1e-10 or logppm>log10(model["max_ppm"])+1e-10:
            out.append({"ppm":None,"status":"OUTSIDE_CALIBRATED_CONCENTRATION_RANGE"})
        else:
            out.append({"ppm":10**min(log10(model["max_ppm"]),max(log10(model["min_ppm"]),logppm)),
                        "status":"WITHIN_CALIBRATED_CONCENTRATION_RANGE",
                        "warning":"Known gas only; feature distribution may still be out of domain."})
    return out
