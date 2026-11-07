"""03.6: sensor-subset stability under heldout source-exposure groups."""
from itertools import combinations
from math import isfinite

def grouped_sensor_subsets(rows, labels, groups, sensor_ids, *, source_id, max_sensors=2):
    x=[tuple(r) for r in rows]; y=list(labels); g=list(groups); ids=tuple(sensor_ids)
    if not isinstance(source_id,str) or not source_id or len(x)<8 or len(x)!=len(y) or len(x)!=len(g):
        raise ValueError("independent labelled source exposure required")
    if not ids or len(ids)!=len(x[0]) or len(set(ids))!=len(ids):
        raise ValueError("sensor ID mismatch")
    if any(len(r)!=len(ids) or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in r) for r in x):
        raise ValueError("invalid source feature")
    if any(not isinstance(v,str) or not v for v in y+g) or len(set(y))<2 or len(set(g))<4:
        raise ValueError("group and classes required")
    if isinstance(max_sensors,bool) or not isinstance(max_sensors,int) or not 1<=max_sensors<=len(ids):
        raise ValueError("sensor count invalid")
    classes=sorted(set(y)); results=[]
    for k in range(1,max_sensors+1):
        for cols in combinations(range(len(ids)),k):
            n=correct=0;folds=[]
            for heldout in sorted(set(g)):
                tr=[i for i in range(len(x)) if g[i]!=heldout]
                te=[i for i in range(len(x)) if g[i]==heldout]
                if any(not any(y[i]==c for i in tr) for c in classes):
                    raise ValueError("missing training class in grouped fold")
                means={c:[sum(x[i][j] for i in tr if y[i]==c)/sum(y[i]==c for i in tr) for j in cols] for c in classes}
                score=sum(min(classes,key=lambda c:(sum((x[i][j]-means[c][z])**2 for z,j in enumerate(cols)),c))==y[i] for i in te)
                correct+=score;n+=len(te);folds.append({"group":heldout,"n":len(te),"correct":score})
            results.append({"sensor_ids":[ids[j] for j in cols],"accuracy":correct/n,"folds":folds,"size":k})
    results.sort(key=lambda v:(-v["accuracy"],v["size"],v["sensor_ids"]))
    return {"source_id":source_id,"candidates":results,"best_exploratory":results[0],
            "final_independent_test":False,"Tank_G3":False}
