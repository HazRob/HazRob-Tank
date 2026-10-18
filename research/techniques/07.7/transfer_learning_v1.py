"""Research-only frozen-feature linear-head transfer baseline."""
from math import isfinite
def fit_head(features, labels, *, ridge=1e-6):
    x=[list(v) for v in features]; y=list(labels)
    if len(x)<2 or len(y)!=len(x) or not 0<ridge<1 or not x[0]:raise ValueError("invalid transfer examples")
    n=len(x[0])
    if any(len(v)!=n or any(not isinstance(z,(int,float)) or isinstance(z,bool) or not isfinite(z) for z in v) for v in x):raise ValueError("invalid frozen features")
    if any(not isinstance(k,str) or not k for k in y) or len(set(y))<2:raise ValueError("at least two target labels required")
    classes=sorted(set(y))
    centers={k:[sum(row[j] for row,t in zip(x,y) if t==k)/sum(t==k for t in y) for j in range(n)] for k in classes}
    return {"centroids":centers,"n_features":n,"warning":"Frozen features only; nearest-centroid target head, not neural fine-tuning."}
def predict(model, features):
    x=list(features)
    if len(x)!=model["n_features"] or any(not isinstance(z,(int,float)) or isinstance(z,bool) or not isfinite(z) for z in x):raise ValueError("invalid features")
    return min(model["centroids"],key=lambda k:(sum((a-b)**2 for a,b in zip(x,model["centroids"][k])),k))
