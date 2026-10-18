"""Research-only class-conditional running prototype updates with audit trail."""
from math import isfinite
def new_state(n_features):
    if not isinstance(n_features,int) or isinstance(n_features,bool) or n_features<1:raise ValueError("invalid dimensions")
    return {"n_features":n_features,"counts":{},"means":{},"audit":[]}
def update(state,label,features,*,approved=False):
    if not approved:raise PermissionError("human-approved verified label required")
    if not isinstance(label,str) or not label:raise ValueError("label required")
    x=list(features)
    if len(x)!=state["n_features"] or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("finite aligned features required")
    counts=dict(state["counts"]);means={k:list(v) for k,v in state["means"].items()}
    n=counts.get(label,0)+1;old=means.get(label,[0.]*len(x))
    means[label]=[old[j]+(x[j]-old[j])/n for j in range(len(x))];counts[label]=n
    return {"n_features":state["n_features"],"counts":counts,"means":means,
            "audit":list(state["audit"])+[{"label":label,"count":n}],
            "warning":"No automatic pseudo-labeling; replay and catastrophic forgetting evaluation pending."}
