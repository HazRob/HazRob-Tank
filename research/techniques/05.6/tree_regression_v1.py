"""Research-only bagged CART regression for known-gas log10(ppm)."""
from math import log10,isfinite,sqrt
from random import Random

def _positive(x):
    return isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x) and x>0

def _tree(x,y,indices,depth,max_depth,min_leaf,mtry,rng):
    values=[y[i] for i in indices];avg=sum(values)/len(values)
    if depth>=max_depth or len(indices)<2*min_leaf:
        return {"value":avg}
    best=None
    for j in sorted(rng.sample(range(len(x[0])),mtry)):
        ordered=sorted(set(x[i][j] for i in indices))
        for a,b in zip(ordered,ordered[1:]):
            cut=a+(b-a)/2
            left=[i for i in indices if x[i][j]<=cut]
            right=[i for i in indices if x[i][j]>cut]
            if min(len(left),len(right))<min_leaf:continue
            lm=sum(y[i] for i in left)/len(left)
            rm=sum(y[i] for i in right)/len(right)
            loss=sum((y[i]-lm)**2 for i in left)+sum((y[i]-rm)**2 for i in right)
            if best is None or (loss,j,cut)<best[:3]:
                best=(loss,j,cut,left,right)
    parent=sum((v-avg)**2 for v in values)
    if best is None or best[0]>=parent-1e-12:return {"value":avg}
    _,j,cut,left,right=best
    return {"feature":j,"threshold":cut,
            "left":_tree(x,y,left,depth+1,max_depth,min_leaf,mtry,rng),
            "right":_tree(x,y,right,depth+1,max_depth,min_leaf,mtry,rng)}

def fit_tree_ensemble(rows,concentrations,*,sensor_ids,gas_label,n_trees=31,max_depth=5,min_leaf=2,max_features=None,seed=42):
    ids=tuple(sensor_ids);raw=[tuple(row) for row in rows];ppm=list(concentrations)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(v,str) or not v for v in ids):
        raise ValueError("unique sensor IDs required")
    if not isinstance(gas_label,str) or not gas_label:raise ValueError("known gas label required")
    d=len(ids);n=len(raw)
    if n<max(6,d+2) or n!=len(ppm) or any(len(row)!=d for row in raw):
        raise ValueError("invalid or insufficient aligned samples")
    if any(not _positive(v) for row in raw for v in row) or any(not _positive(v) for v in ppm):
        raise ValueError("positive finite ratios and ppm required")
    if len(set(ppm))<3:raise ValueError("at least three reference concentrations required")
    if any(not isinstance(v,int) or isinstance(v,bool) or v<1 for v in (n_trees,max_depth,min_leaf)):
        raise ValueError("invalid tree hyperparameters")
    mtry=max(1,int(sqrt(d))) if max_features is None else max_features
    if not isinstance(mtry,int) or isinstance(mtry,bool) or not 1<=mtry<=d:
        raise ValueError("invalid max_features")
    if not isinstance(seed,int) or isinstance(seed,bool):raise ValueError("invalid seed")
    x=[[log10(v) for v in row] for row in raw];y=[log10(v) for v in ppm]
    rng=Random(seed)
    trees=[_tree(x,y,[rng.randrange(n) for _ in range(n)],0,max_depth,min_leaf,mtry,rng) for _ in range(n_trees)]
    return {"sensor_ids":ids,"gas_label":gas_label,"trees":trees,"min_ppm":min(ppm),
            "max_ppm":max(ppm),"n":n,"seed":seed,"max_features":mtry}

def _predict_tree(row,node):
    while "feature" in node:
        node=node["left"] if row[node["feature"]]<=node["threshold"] else node["right"]
    return node["value"]

def estimate_tree_ensemble(rows,model):
    d=len(model["sensor_ids"]);out=[]
    for item in rows:
        row=tuple(item)
        if len(row)!=d or any(not _positive(v) for v in row):
            raise ValueError("invalid query sensor ratios")
        x=[log10(v) for v in row]
        logs=[_predict_tree(x,t) for t in model["trees"]]
        logppm=sum(logs)/len(logs)
        lo=log10(model["min_ppm"]);hi=log10(model["max_ppm"])
        if not isfinite(logppm):raise ValueError("nonfinite prediction")
        if logppm<lo-1e-10 or logppm>hi+1e-10:
            out.append({"ppm":None,"status":"OUTSIDE_CALIBRATED_CONCENTRATION_RANGE"})
        else:
            out.append({"ppm":10**min(hi,max(lo,logppm)),
                        "status":"WITHIN_TRAINING_CONCENTRATION_RANGE",
                        "tree_log10_std":sqrt(sum((v-logppm)**2 for v in logs)/len(logs)),
                        "warning":"Ensemble disagreement is not calibrated uncertainty; feature-space OOD untested."})
    return out
