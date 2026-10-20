"""Research-only split conformal multiclass prediction sets."""
from math import ceil,isfinite

def _row(p,n=None):
    if not isinstance(p,(list,tuple)) or len(p)<2 or (n is not None and len(p)!=n):
        raise ValueError("fixed multiclass probability vector required")
    if any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x)
           or x<0 or x>1 for x in p) or abs(sum(p)-1)>1e-6:
        raise ValueError("finite normalized probabilities required")
    return len(p)

def fit_split_conformal(calibration_probabilities, true_labels, *, alpha=0.1):
    """Split conformal nonconformity score = 1 - p(true class).
    Marginal coverage under exchangeability, not per-class or OOD guarantees.
    """
    rows=list(calibration_probabilities);labels=list(true_labels)
    if not isinstance(alpha,(int,float)) or isinstance(alpha,bool) or not isfinite(alpha) or not 0<alpha<1:
        raise ValueError("0 < alpha < 1 required")
    if not rows or len(rows)!=len(labels):
        raise ValueError("aligned nonempty calibration rows required")
    n=_row(rows[0])
    scores=[]
    for p,y in zip(rows,labels):
        _row(p,n)
        if not isinstance(y,int) or isinstance(y,bool) or not 0<=y<n:
            raise ValueError("invalid class index")
        scores.append(1-p[y])
    scores.sort()
    k=ceil((len(scores)+1)*(1-alpha))
    threshold=scores[k-1] if k<=len(scores) else float("inf")
    return {"threshold":threshold,"alpha":alpha,"n_calibration":len(rows),
            "n_classes":n,"quantile_rank":k,
            "warning":"Marginal exchangeable-data coverage only; no conditional, classwise, unknown-gas or mixture guarantee."}

def predict_set(probabilities, model):
    _row(probabilities,model["n_classes"])
    threshold=model["threshold"]
    included=[i for i,p in enumerate(probabilities) if 1-p<=threshold]
    return {"candidate_indices":included,"set_size":len(included),
            "status":"CONFORMAL_SET" if included else "EMPTY_SET_ABSTAIN",
            "warning":"Set size and membership do not prove gas identity."}

def evaluate_sets(prediction_sets, true_labels):
    sets=list(prediction_sets);labels=list(true_labels)
    if not sets or len(sets)!=len(labels):raise ValueError("aligned evaluation data required")
    if any(not isinstance(s,(list,tuple,set)) or
           any(not isinstance(v,int) or isinstance(v,bool) or v<0 for v in s)
           for s in sets):raise ValueError("integer-index sets required (empty prediction sets are valid abstentions)")
    if any(not isinstance(y,int) or isinstance(y,bool) or y<0 for y in labels):
        raise ValueError("invalid labels")
    return {"n":len(sets),"empirical_coverage":sum(y in s for s,y in zip(sets,labels))/len(sets),
            "mean_set_size":sum(len(s) for s in sets)/len(sets)}
