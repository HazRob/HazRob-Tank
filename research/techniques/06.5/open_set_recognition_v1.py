"""Research-only nearest-prototype open-set rejection with held-out unknown evaluation."""
from math import sqrt,isfinite

def _vector(v,n=None):
    if not isinstance(v,(list,tuple)) or not v or (n is not None and len(v)!=n) or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) for x in v):
        raise ValueError("finite fixed-length feature vector required")
    return tuple(float(x) for x in v)

def _distance(a,b):
    return sqrt(sum((x-y)**2 for x,y in zip(a,b)))

def fit_open_set(known_examples, *, rejection_quantile=0.95):
    """Known examples: (class_label, feature_vector). Prototype/threshold are descriptive.
    No guaranteed unknown detection or statistical coverage.
    """
    if not isinstance(rejection_quantile,(int,float)) or isinstance(rejection_quantile,bool) or not isfinite(rejection_quantile) or not 0<rejection_quantile<1:
        raise ValueError("quantile in (0,1) required")
    rows=list(known_examples)
    if not rows:raise ValueError("known training data required")
    groups={};n=None
    for label,vec in rows:
        if not isinstance(label,str) or not label:raise ValueError("nonempty class label required")
        v=_vector(vec,n)
        if n is None:n=len(v)
        groups.setdefault(label,[]).append(v)
    if len(groups)<2 or any(len(v)<2 for v in groups.values()):
        raise ValueError("at least two known classes with >=2 examples each required")
    centers={k:tuple(sum(row[i] for row in samples)/len(samples) for i in range(n)) for k,samples in groups.items()}
    distances=sorted(_distance(vec,centers[label]) for label,vecs in groups.items() for vec in vecs)
    # In-sample threshold is only a research baseline and optimistically biased.
    index=min(len(distances)-1,max(0,int(rejection_quantile*len(distances))))
    return {"prototypes":centers,"threshold":distances[index],"n_features":n,
            "known_classes":sorted(groups),"status":"UNVALIDATED_OPEN_SET_BASELINE",
            "warning":"Training-distance threshold is optimistic; tune on disjoint known validation and test with held-out unknown gas classes."}

def predict_open_set(model, features):
    x=_vector(features,model["n_features"])
    distances={k:_distance(x,v) for k,v in model["prototypes"].items()}
    nearest=min(distances,key=lambda k:(distances[k],k))
    reject=distances[nearest]>model["threshold"]
    return {"status":"UNKNOWN_OR_UNSUPPORTED" if reject else "KNOWN_CANDIDATE",
            "candidate":None if reject else nearest,"nearest_known":nearest,
            "distance":distances[nearest],"threshold":model["threshold"],
            "warning":"Rejection does not prove a novel gas; may reflect concentration, drift, mixtures or sensor faults."}

def evaluate_open_set(model, known_rows, unknown_rows):
    known=list(known_rows);unknown=list(unknown_rows)
    if not known or not unknown:raise ValueError("known and unknown test examples required")
    kr=[predict_open_set(model,x) for x in known]
    ur=[predict_open_set(model,x) for x in unknown]
    return {"known_accept_rate":sum(v["status"]=="KNOWN_CANDIDATE" for v in kr)/len(kr),
            "unknown_reject_rate":sum(v["status"]=="UNKNOWN_OR_UNSUPPORTED" for v in ur)/len(ur),
            "n_known":len(kr),"n_unknown":len(ur)}
