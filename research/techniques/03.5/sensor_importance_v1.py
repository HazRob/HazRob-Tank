"""Research-only per-sensor discriminative ranking; no causal/selectivity claims."""
from math import isfinite

def rank_sensor_importance(train_rows, train_labels, sensor_ids, *, min_per_class=2):
    """Univariate between/within-class Fisher scores fitted to training records only.
    Rows are aligned sensor response features (one scalar per sensor).
    """
    x=[tuple(row) for row in train_rows];y=list(train_labels);ids=tuple(sensor_ids)
    if not x or len(x)!=len(y) or not ids or len(set(ids))!=len(ids) or any(not isinstance(s,str) or not s for s in ids):
        raise ValueError("invalid input or sensor ids")
    if any(len(row)!=len(ids) for row in x):raise ValueError("sensor columns mismatch")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid sensor readings")
    if any(not isinstance(v,str) or not v for v in y):raise ValueError("invalid labels")
    classes=sorted(set(y))
    if len(classes)<2 or any(y.count(c)<min_per_class for c in classes):
        raise ValueError("need >=2 classes and enough samples per class")
    out=[]
    for j,sensor in enumerate(ids):
        values=[row[j] for row in x];grand=sum(values)/len(values)
        between=0.;within=0.
        for c in classes:
            group=[row[j] for row,label in zip(x,y) if label==c]
            mu=sum(group)/len(group)
            between+=len(group)*(mu-grand)**2
            within+=sum((v-mu)**2 for v in group)
        score=between/within if within>0 else (float("inf") if between>0 else 0.)
        out.append({"sensor_id":sensor,"column":j,"fisher_score":score,
                    "constant":max(values)==min(values)})
    return sorted(out,key=lambda item:(-item["fisher_score"],item["column"]))

def select_sensor_columns(rows,ranking,k):
    if not isinstance(k,int) or isinstance(k,bool) or not 1<=k<=len(ranking):
        raise ValueError("invalid sensor count")
    indices=[item["column"] for item in ranking[:k]]
    result=[]
    for row in rows:
        a=list(row)
        if max(indices)>=len(a):raise ValueError("sensor schema mismatch")
        result.append([a[i] for i in indices])
    return result
