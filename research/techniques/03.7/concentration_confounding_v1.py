"""Research-only concentration confounding diagnostic; no causal claims."""
from math import isfinite, sqrt

def _corr(a,b):
    ma=sum(a)/len(a);mb=sum(b)/len(b)
    aa=[v-ma for v in a];bb=[v-mb for v in b]
    den=sqrt(sum(v*v for v in aa)*sum(v*v for v in bb))
    return None if den==0 else sum(u*v for u,v in zip(aa,bb))/den

def concentration_report(rows, gas_labels, concentrations, *, sensor_ids=None):
    """Within-gas Pearson correlation of each feature with log10 concentration.

    A diagnostic of association only. No pooled cross-gas concentration fit.
    """
    x=[tuple(row) for row in rows]; labels=list(gas_labels); ppm=list(concentrations)
    if not x or len(x)!=len(labels) or len(x)!=len(ppm) or not x[0]:
        raise ValueError("invalid dataset")
    d=len(x[0])
    if any(len(row)!=d or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in row) for row in x):
        raise ValueError("invalid features")
    if any(not isinstance(g,str) or not g for g in labels):
        raise ValueError("invalid gas labels")
    if any(isinstance(c,bool) or not isinstance(c,(int,float)) or not isfinite(c) or c<=0 for c in ppm):
        raise ValueError("positive finite concentration required")
    ids=tuple(sensor_ids) if sensor_ids is not None else tuple("feature_"+str(j) for j in range(d))
    if len(ids)!=d or len(set(ids))!=d or any(not isinstance(s,str) or not s for s in ids):
        raise ValueError("invalid feature ids")
    from math import log10
    groups={}
    for i,g in enumerate(labels):groups.setdefault(g,[]).append(i)
    result={}
    for gas,idx in sorted(groups.items()):
        distinct=len(set(ppm[i] for i in idx))
        if len(idx)<3 or distinct<2:
            result[gas]={"n":len(idx),"n_concentrations":distinct,"status":"INSUFFICIENT",
                         "correlations":None}
            continue
        c=[log10(ppm[i]) for i in idx]
        result[gas]={"n":len(idx),"n_concentrations":distinct,"status":"DESCRIPTIVE",
                     "correlations":{ids[j]:_corr(c,[x[i][j] for i in idx]) for j in range(d)}}
    return {"unit":"ppm","concentration_transform":"log10","by_gas":result,
            "warning":"Within-gas association only; concentration, time, humidity and session may be confounded."}
