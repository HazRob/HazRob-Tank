"""Research-only PLS1 (NIPALS-style) for known-gas log-ppm regression."""
from math import log10,sqrt,isfinite

def _positive(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and v>0

def fit_pls(rows, concentrations, *, sensor_ids, gas_label, n_components=1):
    ids=tuple(sensor_ids);x=[tuple(row) for row in rows];ppm=list(concentrations)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(s,str) or not s for s in ids):
        raise ValueError("unique sensor IDs required")
    if not isinstance(gas_label,str) or not gas_label:raise ValueError("known gas required")
    d=len(ids);n=len(x)
    if n<max(5,d+2) or n!=len(ppm) or any(len(row)!=d for row in x):
        raise ValueError("invalid or insufficient aligned data")
    if any(not _positive(v) for row in x for v in row) or any(not _positive(v) for v in ppm):
        raise ValueError("positive finite Rs/R0 and ppm required")
    if len(set(ppm))<3:raise ValueError("at least three reference concentrations required")
    if not isinstance(n_components,int) or isinstance(n_components,bool) or not 1<=n_components<=min(d,n-1):
        raise ValueError("invalid number of latent components")
    a=[[log10(v) for v in row] for row in x];y=[log10(v) for v in ppm]
    mean=[sum(row[j] for row in a)/n for j in range(d)]
    scale=[sqrt(sum((row[j]-mean[j])**2 for row in a)/n) or 1. for j in range(d)]
    z=[[(row[j]-mean[j])/scale[j] for j in range(d)] for row in a]
    ymean=sum(y)/n;res=[v-ymean for v in y]
    weights=[];loadings=[];q_values=[]
    for _ in range(n_components):
        w=[sum(z[i][j]*res[i] for i in range(n)) for j in range(d)]
        norm=sqrt(sum(v*v for v in w))
        if norm<1e-12:break
        w=[v/norm for v in w]
        t=[sum(z[i][j]*w[j] for j in range(d)) for i in range(n)]
        tt=sum(v*v for v in t)
        if tt<1e-12:break
        p=[sum(z[i][j]*t[i] for i in range(n))/tt for j in range(d)]
        q=sum(res[i]*t[i] for i in range(n))/tt
        weights.append(w);loadings.append(p);q_values.append(q)
        z=[[z[i][j]-t[i]*p[j] for j in range(d)] for i in range(n)]
        res=[res[i]-q*t[i] for i in range(n)]
    if not weights:raise ValueError("no informative latent component")
    # Sequential deflation coefficients are applied to incoming standardized rows.
    return {"sensor_ids":ids,"gas_label":gas_label,"means":mean,"scales":scale,
            "ymean":ymean,"weights":weights,"loadings":loadings,"q":q_values,
            "n_components":len(weights),"min_ppm":min(ppm),"max_ppm":max(ppm),"n":n}

def estimate_pls(rows,model):
    d=len(model["sensor_ids"]);out=[]
    for item in rows:
        row=tuple(item)
        if len(row)!=d or any(not _positive(v) for v in row):
            raise ValueError("invalid sensor ratios")
        z=[(log10(row[j])-model["means"][j])/model["scales"][j] for j in range(d)]
        logppm=model["ymean"]
        for w,p,q in zip(model["weights"],model["loadings"],model["q"]):
            t=sum(z[j]*w[j] for j in range(d))
            logppm+=q*t
            z=[z[j]-t*p[j] for j in range(d)]
        if not isfinite(logppm):raise ValueError("nonfinite prediction")
        if logppm<log10(model["min_ppm"])-1e-10 or logppm>log10(model["max_ppm"])+1e-10:
            out.append({"ppm":None,"status":"OUTSIDE_CALIBRATED_CONCENTRATION_RANGE"})
        else:
            out.append({"ppm":10**min(log10(model["max_ppm"]),max(log10(model["min_ppm"]),logppm)),
                        "status":"WITHIN_CALIBRATED_CONCENTRATION_RANGE"})
    return out
