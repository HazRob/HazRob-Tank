"""03.8: rank and local conditioning of a physical mixture response Jacobian.

This checks a necessary mathematical condition; no chemical identification.
"""
from math import isfinite,sqrt

def rank_mixture_candidates(jacobian, sensor_ids, gas_ids, *, source_id,max_condition=100.):
    x=[tuple(r) for r in jacobian];s=tuple(sensor_ids);g=tuple(gas_ids)
    if not source_id or not s or not g or len(g)>len(s) or len(x)!=len(s):
        raise ValueError("source and Jacobian dimensionality required")
    if len(set(s))!=len(s) or len(set(g))!=len(g) or any(not isinstance(z,str) or not z for z in s+g):
        raise ValueError("unique sensor and gas labels required")
    if any(len(row)!=len(g) or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in row) for row in x):
        raise ValueError("finite sensitivity Jacobian required")
    if isinstance(max_condition,bool) or not isinstance(max_condition,(int,float)) or not isfinite(max_condition) or max_condition<1:
        raise ValueError("invalid bound")
    cols=[[row[j] for row in x] for j in range(len(g))]
    scale=max(sqrt(sum(v*v for v in c)) for c in cols)
    if scale==0:
        return {"decision":"NON_IDENTIFIABLE","rank":0,"condition":float("inf"),"Tank_G3":False}
    q=[]
    for c in cols:
        residual=c[:]
        for unit in q:
            projection=sum(a*b for a,b in zip(residual,unit))
            residual=[v-projection*b for v,b in zip(residual,unit)]
        norm=sqrt(sum(v*v for v in residual))
        if norm<=scale*1e-10:
            return {"decision":"NON_IDENTIFIABLE","rank":len(q),"condition":float("inf"),"Tank_G3":False}
        q.append([v/norm for v in residual])
    cond=None
    if len(g)==1:
        cond=1.
    elif len(g)==2:
        a=sum(v*v for v in cols[0]);b=sum(v*v for v in cols[1])
        cross=sum(v*w for v,w in zip(cols[0],cols[1]))
        total=a+b;delta=sqrt(max(0.,(a-b)**2+4*cross*cross))
        low=(total-delta)/2;high=(total+delta)/2
        cond=sqrt(high/low) if low>0 else float("inf")
    decision=("NON_IDENTIFIABLE" if cond is not None and cond>max_condition else
              "RANK_ONLY_INCONCLUSIVE" if cond is None else "LOCALLY_RESOLVABLE_MATHEMATICALLY")
    return {"decision":decision,"rank":len(q),"condition":cond,
            "sensor_ids":s,"gas_ids":g,"source_id":source_id,"Tank_G3":False,
            "warning":"Rank and condition are necessary, never sufficient to infer a chemical mixture."}
