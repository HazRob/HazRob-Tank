"""03.4: actually fit binary LDA and PLS1-DA, hold out whole exposure groups.

Original UCI multi-class trained benchmarks remain separate and are not Tank G3.
"""
from math import isfinite
from discriminant_v1 import fit_binary_lda,fit_binary_plsda,project
def grouped_binary_discriminant(rows,labels,groups,*,source_id,method="LDA"):
    xs=[tuple(r) for r in rows];ys=list(labels);gs=list(groups)
    if not source_id or len(xs)<6 or len(xs)!=len(ys) or len(xs)!=len(gs):
        raise ValueError("binary labelled source exposures and group IDs required")
    if len(set(ys))!=2 or any(not isinstance(z,str) or not z for z in ys+gs):
        raise ValueError("two known classes and physical group IDs required")
    if any(isinstance(z,bool) or not isinstance(z,(int,float)) or not isfinite(z) for row in xs for z in row):
        raise ValueError("invalid sensor features")
    if method not in ("LDA","PLS1DA"):raise ValueError("unsupported method")
    groups_set=sorted(set(gs))
    folds=[]
    for group in groups_set:
        train=[i for i,x in enumerate(gs) if x!=group]
        test=[i for i,x in enumerate(gs) if x==group]
        if any(sum(ys[i]==c for i in train)<2 for c in set(ys)):
            raise ValueError("insufficient independent training observations per class")
        trainx=[xs[i] for i in train];trainy=[ys[i] for i in train]
        model=fit_binary_lda(trainx,trainy) if method=="LDA" else fit_binary_plsda(trainx,trainy)
        proj=project(trainx,model)
        c0,c1=model["classes"];m0=sum(v for v,c in zip(proj,trainy) if c==c0)/trainy.count(c0)
        m1=sum(v for v,c in zip(proj,trainy) if c==c1)/trainy.count(c1)
        predictions=[c0 if abs(v-m0)<=abs(v-m1) else c1 for v in project([xs[i] for i in test],model)]
        correct=sum(pred==ys[i] for pred,i in zip(predictions,test))
        folds.append({"heldout_group":group,"n":len(test),"correct":correct})
    count=sum(f["n"] for f in folds)
    return {"source_id":source_id,"method":method,"group_folds":folds,
            "exploratory_group_heldout_accuracy":sum(f["correct"] for f in folds)/count,
            "n_heldout_predictions":count,
            "multiclass_model":False,"Tank_G3":False}
