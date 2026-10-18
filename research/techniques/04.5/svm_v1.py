"""Research-only linear one-vs-rest SVM via primal hinge-loss subgradient descent."""
from math import isfinite
def _check(rows,labels):
    x=[tuple(z) for z in rows];y=list(labels)
    if len(x)<4 or len(x)!=len(y) or not x[0] or any(len(z)!=len(x[0]) for z in x):
        raise ValueError("invalid matrix")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for z in x for v in z):
        raise ValueError("nonfinite feature")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("need two labelled classes")
    return x,y,sorted(set(y))
def fit_linear_ovr_svm(train_rows,train_labels,*,epochs=500,learning_rate=.03,c=1.):
    x,y,classes=_check(train_rows,train_labels)
    if not isinstance(epochs,int) or isinstance(epochs,bool) or not 1<=epochs<=100000:
        raise ValueError("invalid epochs")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0 for v in (learning_rate,c)):
        raise ValueError("invalid optimization settings")
    n=len(x);d=len(x[0])
    mean=[sum(z[j] for z in x)/n for j in range(d)]
    std=[(sum((z[j]-mean[j])**2 for z in x)/n)**.5 for j in range(d)]
    std=[s if s else 1. for s in std]
    z=[[(v-mean[j])/std[j] for j,v in enumerate(row)] for row in x]
    weights=[];biases=[]
    for label in classes:
        w=[0.]*d;b=0.
        for epoch in range(epochs):
            # Diminishing learning rate reduces oscillation on non-smooth hinge loss.
            step=learning_rate/(1+epoch*.01)
            gw=w[:];gb=0.
            for row,actual in zip(z,y):
                target=1. if actual==label else -1.
                if target*(sum(a*v for a,v in zip(w,row))+b)<1:
                    for j in range(d):gw[j]-=c*target*row[j]/n
                    gb-=c*target/n
            w=[a-step*g for a,g in zip(w,gw)]
            b-=step*gb
            if not all(isfinite(v) for v in w) or not isfinite(b):
                raise ValueError("optimization diverged")
        weights.append(w);biases.append(b)
    return {"classes":classes,"mean":mean,"scale":std,"weights":weights,
            "biases":biases,"features":d,"c":c,"epochs":epochs}
def decision_function(rows,model):
    d=model["features"];result=[]
    for row in rows:
        a=tuple(row)
        if len(a)!=d or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in a):
            raise ValueError("invalid query")
        z=[(a[j]-model["mean"][j])/model["scale"][j] for j in range(d)]
        scores=[sum(w[j]*z[j] for j in range(d))+b for w,b in zip(model["weights"],model["biases"])]
        best=max(range(len(scores)),key=lambda i:scores[i])
        result.append({"predicted_label":model["classes"][best],
                       "decision_scores":dict(zip(model["classes"],scores))})
    return result
