"""Research-only multichannel fingerprint matching with frozen reference library."""
from math import isfinite, sqrt

def _vector(v, n=None):
    if not isinstance(v,(list,tuple)) or not v or (n is not None and len(v)!=n):
        raise ValueError("invalid fingerprint length")
    if any(not isinstance(z,(int,float)) or isinstance(z,bool) or not isfinite(z) for z in v):
        raise ValueError("fingerprints must be finite numeric vectors")
    return tuple(v)

def fit_fingerprint_library(train_rows,train_labels,sensor_ids,*,scales=None):
    """Store training exemplars; no holdout data allowed in this function."""
    ids=tuple(sensor_ids)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(v,str) or not v for v in ids):
        raise ValueError("invalid sensor ids")
    x=[_vector(row,len(ids)) for row in train_rows];y=list(train_labels)
    if not x or len(x)!=len(y) or any(not isinstance(v,str) or not v for v in y):
        raise ValueError("invalid reference records")
    s=(1.,)*len(ids) if scales is None else tuple(scales)
    if len(s)!=len(ids) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0 for v in s):
        raise ValueError("invalid scales")
    return {"sensor_ids":ids,"scales":s,"references":tuple(x),"labels":tuple(y)}

def match_fingerprint(observation,library,*,top_k=5,abstention_distance=None):
    """Nearest exemplar per class; distances are not probabilities."""
    ids=library["sensor_ids"];x=_vector(observation,len(ids))
    if not isinstance(top_k,int) or isinstance(top_k,bool) or top_k<1:
        raise ValueError("invalid top_k")
    if abstention_distance is not None and (not isinstance(abstention_distance,(int,float)) or isinstance(abstention_distance,bool) or not isfinite(abstention_distance) or abstention_distance<0):
        raise ValueError("invalid abstention distance")
    s=library["scales"]
    candidates={}
    for label,ref in zip(library["labels"],library["references"]):
        dist=sqrt(sum(((a-b)/scale)**2 for a,b,scale in zip(x,ref,s)))
        if label not in candidates or dist<candidates[label]:candidates[label]=dist
    ranking=sorted(({"gas_label":label,"distance":dist} for label,dist in candidates.items()),
                   key=lambda item:(item["distance"],item["gas_label"]))[:top_k]
    decision="REFERENCE_MATCH_ONLY"
    if abstention_distance is not None and ranking[0]["distance"]>abstention_distance:
        decision="ABSTAIN_DISTANCE"
    return {"ranking":ranking,"decision":decision,"warning":"Reference proximity is not gas identification or calibrated confidence."}
