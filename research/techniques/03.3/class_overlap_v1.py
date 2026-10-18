"""Research-only class overlap analysis on held-out or training feature matrices."""
from math import isfinite, sqrt

def _validated(rows, labels):
    x=[tuple(row) for row in rows]; y=list(labels)
    if not x or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid feature matrix")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("features must be finite numeric")
    if any(not isinstance(label,str) or not label for label in y):
        raise ValueError("labels must be nonempty strings")
    classes=sorted(set(y))
    if len(classes)<2 or any(y.count(c)<2 for c in classes):
        raise ValueError("requires at least two samples in each of two classes")
    return x,y,classes

def _distance(a,b,scales):
    return sqrt(sum(((u-v)/s)**2 for u,v,s in zip(a,b,scales)))

def overlap_report(rows,labels,*,scales=None):
    """Leave-one-out nearest same/other-class distances and ambiguity fractions.
    Descriptive only: not a calibrated probability or out-of-sample estimate."""
    x,y,classes=_validated(rows,labels)
    d=len(x[0])
    s=(1.,)*d if scales is None else tuple(scales)
    if len(s)!=d or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0 for v in s):
        raise ValueError("invalid scales")
    details=[]
    for i,(row,label) in enumerate(zip(x,y)):
        same=min(_distance(row,other,s) for j,other in enumerate(x) if i!=j and y[j]==label)
        other=min(_distance(row,other_row,s) for j,other_row in enumerate(x) if y[j]!=label)
        details.append({"index":i,"class":label,"nearest_same":same,
                        "nearest_other":other,"ambiguous":other<=same,
                        "margin":other-same})
    by_class={c:{"n":sum(z["class"]==c for z in details),
                 "ambiguous_fraction":sum(z["ambiguous"] for z in details if z["class"]==c)/sum(z["class"]==c for z in details)}
              for c in classes}
    return {"n_samples":len(x),"classes":classes,
            "ambiguous_fraction":sum(z["ambiguous"] for z in details)/len(details),
            "by_class":by_class,"details":details}
