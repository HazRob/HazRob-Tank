"""02.7: rank features on TRAIN folds only; grouped holdout accuracy not final test performance."""
from math import isfinite
from feature_selection_v1 import rank_features
def grouped_feature_study(rows,labels,groups,features,*,max_features=None):
    x=[tuple(r) for r in rows];y=list(labels);g=list(groups);names=tuple(features)
    if not x or len(x)!=len(y) or len(x)!=len(g) or not names or any(len(r)!=len(names) for r in x):
        raise ValueError("feature study shape error")
    if any(not isinstance(z,str) or not z.strip() for z in y+g):
        raise ValueError("known class and experimental group IDs required")
    if len(set(names))!=len(names) or any(not isinstance(z,str) or not z for z in names):
        raise ValueError("unique feature IDs required")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for row in x for v in row):
        raise ValueError("bad signal feature")
    kmax=len(names) if max_features is None else max_features
    if isinstance(kmax,bool) or not isinstance(kmax,int) or not 1<=kmax<=len(names):
        raise ValueError("invalid feature count")
    unique=sorted(set(g));result={k:[] for k in range(1,kmax+1)}
    if len(unique)<4:
        raise ValueError("enough distinct exposure groups required for holdout")
    for group in unique:
        train=[i for i,grp in enumerate(g) if grp!=group]
        test=[i for i,grp in enumerate(g) if grp==group]
        train_x=[x[i] for i in train];train_y=[y[i] for i in train]
        ranks=rank_features(train_x,train_y,names,min_class_samples=2)
        classes=sorted(set(train_y))
        if set(y[i] for i in test)-set(classes):
            raise ValueError("holdout contains unseen class, cannot score closed-set study")
        for k in result:
            indices=[z["index"] for z in ranks[:k]]
            means={c:tuple(sum(x[i][j] for i in train if y[i]==c)/sum(y[i]==c for i in train)
                           for j in indices) for c in classes}
            correct=0
            for i in test:
                pred=min(classes,key=lambda c:(sum((x[i][j]-means[c][z])**2
                                      for z,j in enumerate(indices)),c))
                correct+=pred==y[i]
            result[k].append({"heldout_group":group,"correct":correct,
                              "count":len(test),"indices":indices})
    summary=[{"k":k,"correct":sum(z["correct"] for z in folds),
              "total":sum(z["count"] for z in folds),
              "accuracy_grouped_exploratory":sum(z["correct"] for z in folds)/sum(z["count"] for z in folds),
              "folds":folds} for k,folds in result.items()]
    best=max(summary,key=lambda z:(z["accuracy_grouped_exploratory"],-z["k"]))
    return {"source_group_ids":unique,"candidate_results":summary,
            "best_feature_count_exploratory":best["k"],
            "nested_outer_holdout_performed":False,"ppm_approved":False,
            "note":"Exploratory k was selected on same grouped folds; independent outer test still required"}
