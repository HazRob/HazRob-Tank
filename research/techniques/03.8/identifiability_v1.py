"""Research-only identifiability diagnostics for labelled sensor fingerprints."""
from math import isfinite,sqrt

def identifiability_report(rows, labels, *, scales=None, threshold=0.5):
    """Flag cross-class near-collisions using fixed feature scales and threshold.
    Threshold is a research input, NOT a calibrated confidence or detection limit.
    """
    x=[tuple(row) for row in rows]; y=list(labels)
    if len(x)<2 or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid dataset")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in x for v in row):
        raise ValueError("nonfinite feature")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("need two labelled classes")
    d=len(x[0]);s=(1.,)*d if scales is None else tuple(scales)
    if len(s)!=d or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0 for v in s):
        raise ValueError("invalid scales")
    if not isinstance(threshold,(int,float)) or isinstance(threshold,bool) or not isfinite(threshold) or threshold<0:
        raise ValueError("invalid threshold")
    pairs=[];nearest=[None]*len(x)
    for i in range(len(x)):
        for j in range(i+1,len(x)):
            if y[i]==y[j]:continue
            distance=sqrt(sum(((a-b)/scale)**2 for a,b,scale in zip(x[i],x[j],s)))
            if nearest[i] is None or distance<nearest[i]:nearest[i]=distance
            if nearest[j] is None or distance<nearest[j]:nearest[j]=distance
            if distance<=threshold:
                pairs.append({"left_index":i,"right_index":j,"left_class":y[i],
                              "right_class":y[j],"distance":distance})
    return {"threshold":threshold,"cross_class_collision_pairs":pairs,
            "collision_pair_count":len(pairs),"nearest_other_class_distances":nearest,
            "ambiguous_sample_indices":[i for i,v in enumerate(nearest) if v is not None and v<=threshold],
            "warning":"Threshold-based empirical ambiguity, not a physical identifiability bound or posterior probability."}

def screen_query(query, references, labels, *, scales=None, threshold=0.5):
    """Conservative abstention when more than one class lies inside fixed radius."""
    q=tuple(query); refs=[tuple(r) for r in references];labs=list(labels)
    if not refs or len(refs)!=len(labs) or not q or any(len(r)!=len(q) for r in refs):
        raise ValueError("invalid reference data")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for row in [q]+refs for v in row):
        raise ValueError("nonfinite features")
    if any(not isinstance(v,str) or not v for v in labs):raise ValueError("invalid labels")
    s=(1.,)*len(q) if scales is None else tuple(scales)
    if len(s)!=len(q) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0 for v in s):
        raise ValueError("invalid scales")
    if not isinstance(threshold,(int,float)) or isinstance(threshold,bool) or not isfinite(threshold) or threshold<0:
        raise ValueError("invalid threshold")
    hits=sorted(set(label for r,label in zip(refs,labs) if sqrt(sum(((a-b)/scale)**2 for a,b,scale in zip(q,r,s)))<=threshold))
    return {"decision":"AMBIGUOUS" if len(hits)>1 else ("INSUFFICIENT_EVIDENCE" if not hits else "SINGLE_REFERENCE_CLASS"),
            "candidate_classes":hits,"warning":"SINGLE_REFERENCE_CLASS is not a validated gas identification."}
