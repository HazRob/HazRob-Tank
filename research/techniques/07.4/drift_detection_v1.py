"""Research-only per-sensor standardized baseline shift detection."""
from math import sqrt,isfinite
def fit_baseline(rows, min_std=1e-6):
    data=[list(r) for r in rows]
    if len(data)<3 or not isinstance(min_std,(int,float)) or not isfinite(min_std) or min_std<=0:raise ValueError("invalid baseline")
    n=len(data[0])
    if not n or any(len(r)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in r) for r in data):raise ValueError("invalid features")
    means=[sum(r[j] for r in data)/len(data) for j in range(n)]
    scales=[max(min_std,sqrt(sum((r[j]-means[j])**2 for r in data)/(len(data)-1))) for j in range(n)]
    return {"means":means,"scales":scales,"n":n}
def detect(model,row,threshold=3):
    x=list(row)
    if len(x)!=model["n"] or not isinstance(threshold,(int,float)) or not isfinite(threshold) or threshold<=0 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("invalid query")
    z=[abs((v-m)/s) for v,m,s in zip(x,model["means"],model["scales"])]
    return {"status":"SHIFT_FLAG" if max(z)>threshold else "NO_SHIFT_FLAG","z_scores":z,
            "warning":"A shift is not necessarily sensor drift; gas/concentration/environment may have changed."}
