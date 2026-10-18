"""Research-only random forest with bootstrap and random feature subsets."""
from collections import Counter
from math import isfinite,log2,sqrt
from random import Random

def _gini(labels):
    n=len(labels)
    return 1-sum((v/n)**2 for v in Counter(labels).values())

def _grow(x,y,indices,depth,max_depth,min_leaf,mtry,rng):
    counts=Counter(y[i] for i in indices)
    label=sorted(counts,key=lambda z:(-counts[z],z))[0]
    if depth>=max_depth or len(counts)==1 or len(indices)<2*min_leaf:
        return {"label":label}
    d=len(x[0]);features=rng.sample(range(d),min(mtry,d))
    best=None;parent=_gini([y[i] for i in indices])
    for j in sorted(features):
        values=sorted(set(x[i][j] for i in indices))
        for a,b in zip(values,values[1:]):
            cut=(a+b)/2
            left=[i for i in indices if x[i][j]<=cut]
            right=[i for i in indices if x[i][j]>cut]
            if min(len(left),len(right))<min_leaf:continue
            impurity=(len(left)*_gini([y[i] for i in left])+len(right)*_gini([y[i] for i in right]))/len(indices)
            candidate=(impurity,j,cut,left,right)
            if best is None or candidate[:3]<best[:3]:best=candidate
    if best is None or best[0]>=parent-1e-12:return {"label":label}
    _,j,cut,left,right=best
    return {"feature":j,"threshold":cut,"left":_grow(x,y,left,depth+1,max_depth,min_leaf,mtry,rng),
            "right":_grow(x,y,right,depth+1,max_depth,min_leaf,mtry,rng)}

def fit_forest(train_rows,train_labels,*,n_trees=25,max_depth=5,min_leaf=2,max_features=None,seed=42):
    x=[tuple(row) for row in train_rows];y=list(train_labels)
    if len(x)<4 or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid training matrix")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid feature")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("need two labelled classes")
    if any(not isinstance(v,int) or isinstance(v,bool) or v<1 for v in (n_trees,max_depth,min_leaf)):
        raise ValueError("invalid tree hyperparameter")
    d=len(x[0]);mtry=max(1,int(sqrt(d))) if max_features is None else max_features
    if not isinstance(mtry,int) or isinstance(mtry,bool) or not 1<=mtry<=d:
        raise ValueError("invalid max_features")
    if not isinstance(seed,int) or isinstance(seed,bool):raise ValueError("invalid seed")
    rng=Random(seed);n=len(x)
    trees=[_grow(x,y,[rng.randrange(n) for _ in range(n)],0,max_depth,min_leaf,mtry,rng)
           for _ in range(n_trees)]
    return {"trees":trees,"n_features":d,"classes":sorted(set(y)),"seed":seed}

def _tree_predict(row,node):
    while "feature" in node:
        node=node["left"] if row[node["feature"]]<=node["threshold"] else node["right"]
    return node["label"]

def predict_forest(rows,model):
    results=[]
    for row in rows:
        a=tuple(row)
        if len(a)!=model["n_features"] or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in a):
            raise ValueError("invalid query vector")
        votes=Counter(_tree_predict(a,tree) for tree in model["trees"])
        winner=sorted(votes,key=lambda z:(-votes[z],z))[0]
        results.append({"predicted_label":winner,"vote_fractions":{c:votes[c]/len(model["trees"]) for c in model["classes"]}})
    return results
