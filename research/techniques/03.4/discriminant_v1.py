"""Binary LDA and one-component PLS-DA research baselines. Not calibrated classifiers."""
from math import isfinite,sqrt

def _check(rows,labels):
    x=[list(row) for row in rows];y=list(labels)
    if len(x)!=len(y) or len(x)<4 or not x[0] or any(len(z)!=len(x[0]) for z in x):
        raise ValueError("invalid matrix")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("invalid features")
    if any(not isinstance(v,str) or not v for v in y):raise ValueError("invalid labels")
    classes=sorted(set(y))
    if len(classes)!=2 or any(y.count(c)<2 for c in classes):
        raise ValueError("binary classes with at least two samples each required")
    return x,y,classes

def _solve(a,b):
    n=len(b);a=[list(row)+[b[i]] for i,row in enumerate(a)]
    for j in range(n):
        pivot=max(range(j,n),key=lambda i:abs(a[i][j]))
        if abs(a[pivot][j])<1e-14:raise ValueError("singular system")
        a[j],a[pivot]=a[pivot],a[j]
        p=a[j][j];a[j]=[v/p for v in a[j]]
        for i in range(n):
            if i!=j:
                f=a[i][j];a[i]=[v-f*w for v,w in zip(a[i],a[j])]
    return [row[-1] for row in a]

def fit_binary_lda(train_rows,train_labels,*,ridge=1e-4):
    x,y,classes=_check(train_rows,train_labels);d=len(x[0])
    if not isinstance(ridge,(int,float)) or isinstance(ridge,bool) or not isfinite(ridge) or ridge<=0:
        raise ValueError("ridge must be positive")
    groups=[[row for row,label in zip(x,y) if label==c] for c in classes]
    means=[[sum(row[j] for row in group)/len(group) for j in range(d)] for group in groups]
    scatter=[[sum((row[i]-m[i])*(row[j]-m[j]) for group,m in zip(groups,means) for row in group) for j in range(d)] for i in range(d)]
    trace=sum(scatter[i][i] for i in range(d))
    lam=ridge*(trace/d if trace>0 else 1.)
    for i in range(d):scatter[i][i]+=lam
    direction=_solve(scatter,[means[1][i]-means[0][i] for i in range(d)])
    norm=sqrt(sum(v*v for v in direction))
    if norm==0:raise ValueError("identical class centroids")
    direction=[v/norm for v in direction]
    midpoint=[(a+b)/2 for a,b in zip(*means)]
    return {"method":"binary_regularized_lda","classes":classes,"direction":direction,
            "center":midpoint,"features":d,"ridge":ridge}

def fit_binary_plsda(train_rows,train_labels):
    """One-component PLS1-DA: normalized X^T y covariance direction."""
    x,y,classes=_check(train_rows,train_labels);n=len(x);d=len(x[0])
    mean=[sum(row[j] for row in x)/n for j in range(d)]
    yc=[(-1. if label==classes[0] else 1.) for label in y]
    w=[sum((row[j]-mean[j])*v for row,v in zip(x,yc)) for j in range(d)]
    norm=sqrt(sum(v*v for v in w))
    if norm==0:raise ValueError("no covariance with class labels")
    return {"method":"binary_pls1da_one_component","classes":classes,"direction":[v/norm for v in w],
            "center":mean,"features":d}

def project(rows,model):
    x=[list(row) for row in rows];d=model["features"]
    if any(len(row)!=d or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in row) for row in x):
        raise ValueError("feature mismatch")
    return [sum((row[j]-model["center"][j])*model["direction"][j] for j in range(d)) for row in x]
