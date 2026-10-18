"""Train-only per-sensor scaling for HazRob Tank research. No production integration."""
from math import isfinite,sqrt
from statistics import median

def _matrix(rows, expected=None):
    a=[list(r) for r in rows]
    if not a or not a[0]: raise ValueError("empty matrix")
    d=len(a[0])
    if expected is not None and d!=expected: raise ValueError("feature mismatch")
    if any(len(r)!=d or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) for x in r) for r in a):
        raise ValueError("invalid matrix")
    return a,d

def fit_scaler(train_rows, method="standard"):
    """Fit on training split ONLY. Constant features use scale=1, become zero."""
    a,d=_matrix(train_rows)
    if method not in ("standard","robust"): raise ValueError("unsupported method")
    center=[];scale=[];constant=[]
    for j in range(d):
        v=[r[j] for r in a]
        if method=="standard":
            c=sum(v)/len(v)
            s=sqrt(sum((x-c)**2 for x in v)/len(v))
        else:
            c=median(v)
            sv=sorted(v)
            def quantile(q):
                k=(len(sv)-1)*q;lo=int(k);hi=min(lo+1,len(sv)-1)
                return sv[lo]+(sv[hi]-sv[lo])*(k-lo)
            s=quantile(.75)-quantile(.25)
        center.append(c);constant.append(s==0);scale.append(s if s>0 else 1.0)
    return {"method":method,"center":center,"scale":scale,"constant":constant,"features":d}

def transform(rows, fitted):
    if not isinstance(fitted,dict) or not all(k in fitted for k in ("center","scale","features")):
        raise ValueError("invalid fitted scaler")
    a,d=_matrix(rows,expected=fitted["features"])
    if len(fitted["center"])!=d or len(fitted["scale"])!=d or any(not isfinite(s) or s<=0 for s in fitted["scale"]):
        raise ValueError("invalid scaler dimensions")
    return [[(x-fitted["center"][j])/fitted["scale"][j] for j,x in enumerate(r)] for r in a]
