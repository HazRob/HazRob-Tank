"""Numerical ridge fit for source-labelled MQ log response QSPR research.

No publisher descriptors invented and no physical Tank calibration inferred.
"""
from math import isfinite, log10

def number(v):
    if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v):
        raise ValueError("finite numeric evidence required")
    return float(v)

def solve(matrix, targets):
    n=len(targets)
    a=[list(row)+[targets[i]] for i,row in enumerate(matrix)]
    for j in range(n):
        pivot=max(range(j,n),key=lambda i:abs(a[i][j]))
        if abs(a[pivot][j])<1e-12:
            raise ValueError("ill-conditioned source descriptors")
        a[j],a[pivot]=a[pivot],a[j]
        divisor=a[j][j]
        a[j]=[v/divisor for v in a[j]]
        for i in range(n):
            if i==j:continue
            ratio=a[i][j]
            a[i]=[v-ratio*t for v,t in zip(a[i],a[j])]
    return [row[-1] for row in a]

def fit_ridge(features, targets, ridge=.01):
    x=[tuple(number(v) for v in row) for row in features]
    y=[number(v) for v in targets]
    if not x or len(x)!=len(y) or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid source rows")
    if len(x)<len(x[0])+2 or ridge<=0:
        raise ValueError("insufficient independent source rows")
    k=len(x[0])
    gram=[[sum(row[i]*row[j] for row in x)+(ridge if i==j and i>0 else 0.)
           for j in range(k)] for i in range(k)]
    rhs=[sum(row[i]*v for row,v in zip(x,y)) for i in range(k)]
    return solve(gram,rhs)
