"""Research-only exhaustive sensor-subset comparison using training-only Fisher ranking."""
from itertools import combinations
from math import isfinite

def compare_subsets(rows, labels, sensor_ids, *, min_size=1, max_size=None):
    x=[tuple(row) for row in rows]; y=list(labels); ids=tuple(sensor_ids)
    if not x or len(x)!=len(y) or not ids or len(set(ids))!=len(ids) or any(not isinstance(z,str) or not z for z in ids):
        raise ValueError("invalid rows, labels or sensor ids")
    if any(len(row)!=len(ids) for row in x) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid features")
    if any(not isinstance(z,str) or not z for z in y):raise ValueError("invalid labels")
    classes=sorted(set(y))
    if len(classes)<2 or any(y.count(c)<2 for c in classes):raise ValueError("at least two samples per class required")
    n=len(ids); upper=n if max_size is None else max_size
    if not isinstance(min_size,int) or isinstance(min_size,bool) or not isinstance(upper,int) or isinstance(upper,bool) or not 1<=min_size<=upper<=n:
        raise ValueError("invalid subset size")
    # Descriptive nearest-neighbor leave-one-out proxy, not cross-session accuracy.
    results=[]
    for k in range(min_size,upper+1):
        for cols in combinations(range(n),k):
            ambiguous=0; margins=[]
            for i,row in enumerate(x):
                same=min(sum((row[j]-other[j])**2 for j in cols) for h,other in enumerate(x) if h!=i and y[h]==y[i])
                other=min(sum((row[j]-other_row[j])**2 for j in cols) for h,other_row in enumerate(x) if y[h]!=y[i])
                if other<=same:ambiguous+=1
                margins.append(other-same)
            results.append({"sensor_ids":[ids[j] for j in cols],"columns":list(cols),
                            "n_sensors":k,"ambiguous_fraction":ambiguous/len(x),
                            "mean_squared_margin":sum(margins)/len(margins)})
    results.sort(key=lambda z:(z["ambiguous_fraction"],z["n_sensors"],-z["mean_squared_margin"],z["columns"]))
    return results

def select_subset(rows, subset):
    cols=subset["columns"]; out=[]
    for row in rows:
        a=list(row)
        if not cols or max(cols)>=len(a):raise ValueError("schema mismatch")
        out.append([a[j] for j in cols])
    return out
