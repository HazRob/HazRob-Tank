"""Research-only unsupervised featurewise affine domain alignment."""
from math import sqrt,isfinite
def _stats(rows):
    data=[list(r) for r in rows]
    if len(data)<3:raise ValueError("at least three observations per domain")
    n=len(data[0])
    if not n or any(len(r)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in r) for r in data):raise ValueError("finite aligned domain features required")
    means=[sum(r[j] for r in data)/len(data) for j in range(n)]
    std=[sqrt(sum((r[j]-means[j])**2 for r in data)/(len(data)-1)) for j in range(n)]
    return means,std,n
def fit_alignment(source,target):
    sm,ss,n=_stats(source);tm,ts,m=_stats(target)
    if n!=m or any(v<=1e-12 for v in ss+ts):raise ValueError("same feature count and nonconstant dimensions required")
    return {"source_mean":sm,"source_std":ss,"target_mean":tm,"target_std":ts,"n":n,
            "warning":"Unsupervised marginal alignment may remove gas signal or cause negative transfer."}
def target_to_source(model,row):
    x=list(row)
    if len(x)!=model["n"] or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("invalid query")
    return [(v-t)/ts*ss+s for v,t,ts,ss,s in zip(x,model["target_mean"],model["target_std"],model["source_std"],model["source_mean"])]
