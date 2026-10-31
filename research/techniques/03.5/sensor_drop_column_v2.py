"""03.5: source-specific leave-one-sensor-out group-heldout centroid evaluation."""
from math import isfinite
def sensor_drop_column(rows,labels,groups,sensor_ids,*,source_id):
    x=[tuple(row) for row in rows];y=list(labels);g=list(groups);ids=tuple(sensor_ids)
    if not source_id or not x or len(x)!=len(y) or len(x)!=len(g) or len(ids)<2 or len(set(ids))!=len(ids):
        raise ValueError("source/shape/independent groups required")
    if any(len(row)!=len(ids) for row in x) or any(isinstance(v,bool) or not isinstance(v,(float,int)) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid numeric features")
    if len(set(y))<2 or any(not isinstance(s,str) or not s for s in y+g):
        raise ValueError("known class and exposure group labels required")
    folds=sorted(set(g))
    if len(folds)<4:raise ValueError("independent test groups insufficient")
    classes=sorted(set(y))
    def heldout_score(excluded=None):
        tested=0;correct=0
        idx=[j for j in range(len(ids)) if j!=excluded]
        for group in folds:
            train=[i for i in range(len(x)) if g[i]!=group]
            tests=[i for i in range(len(x)) if g[i]==group]
            if set(classes)-set(y[i] for i in train):
                raise ValueError("holdout isolates entire class, cannot compare")
            mean={c:[sum(x[i][j] for i in train if y[i]==c)/sum(y[i]==c for i in train) for j in idx]
                  for c in classes}
            for i in tests:
                p=min(classes,key=lambda c:(sum((x[i][j]-mean[c][a])**2 for a,j in enumerate(idx)),c))
                correct+=p==y[i];tested+=1
        return correct/tested
    full=heldout_score()
    dropped=[{"sensor":s,"accuracy_without":heldout_score(j),
              "accuracy_loss":full-heldout_score(j)} for j,s in enumerate(ids)]
    return {"source_id":source_id,"accuracy_all_sensors":full,"drop_one":dropped,
            "independent_final_test":False,"gas_selectivity_proven":False,
            "note":"Exploratory leave-group-out feature attribution only"}
