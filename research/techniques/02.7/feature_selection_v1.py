"""Research-only supervised univariate feature ranking; fit on TRAINING data only."""
from math import isfinite

def rank_features(train_rows,train_labels,feature_names,*,min_class_samples=2):
    """Fisher-like between/within-class score; not a classifier or validation."""
    x=[list(row) for row in train_rows]; y=list(train_labels);names=list(feature_names)
    if len(x)!=len(y) or not x or not names or any(len(row)!=len(names) for row in x):
        raise ValueError("shape mismatch")
    if len(set(names))!=len(names) or any(not isinstance(n,str) or not n for n in names):
        raise ValueError("invalid feature names")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid numeric feature")
    try: classes=list(dict.fromkeys(y))
    except TypeError: raise ValueError("unhashable label")
    if len(classes)<2 or any(y.count(c)<min_class_samples for c in classes):
        raise ValueError("requires >=2 classes with sufficient samples")
    result=[]
    for j,name in enumerate(names):
        vals=[row[j] for row in x]; grand=sum(vals)/len(vals)
        between=0.;within=0.
        for c in classes:
            group=[row[j] for row,label in zip(x,y) if label==c]
            avg=sum(group)/len(group)
            between+=len(group)*(avg-grand)**2
            within+=sum((v-avg)**2 for v in group)
        # Constant features are not informative; perfectly separated training classes
        # receive infinity, explicitly not a generalization claim.
        score=between/within if within>0 else (float("inf") if between>0 else 0.)
        result.append({"feature":name,"score":score,"index":j,"constant":max(vals)==min(vals)})
    return sorted(result,key=lambda z:(-z["score"],z["index"]))

def select_indices(ranking,k):
    if not isinstance(k,int) or isinstance(k,bool) or k<1 or k>len(ranking):
        raise ValueError("invalid k")
    return tuple(item["index"] for item in ranking[:k])

def project(rows,indices):
    idx=tuple(indices)
    if not idx or len(set(idx))!=len(idx) or any(not isinstance(i,int) or isinstance(i,bool) or i<0 for i in idx):
        raise ValueError("invalid indices")
    result=[]
    for row in rows:
        v=list(row)
        if max(idx)>=len(v): raise ValueError("feature mismatch")
        result.append([v[i] for i in idx])
    return result
