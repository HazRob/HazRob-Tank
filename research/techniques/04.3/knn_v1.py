"""Research-only KNN classification on aligned sensor fingerprints."""
from math import sqrt, isfinite

def _vec(row, n):
    try: v=tuple(row)
    except TypeError: raise ValueError("invalid feature vector")
    if len(v)!=n or any(isinstance(z,bool) or not isinstance(z,(int,float)) or not isfinite(z) for z in v):
        raise ValueError("invalid numeric feature vector")
    return v

def fit_knn(train_rows, train_labels, sensor_ids, *, scales=None):
    ids=tuple(sensor_ids)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(z,str) or not z for z in ids):
        raise ValueError("invalid sensor IDs")
    x=[_vec(row,len(ids)) for row in train_rows]
    y=tuple(train_labels)
    if not x or len(x)!=len(y) or any(not isinstance(z,str) or not z for z in y):
        raise ValueError("invalid labelled training records")
    s=(1.,)*len(ids) if scales is None else tuple(scales)
    if len(s)!=len(ids) or any(isinstance(z,bool) or not isinstance(z,(int,float)) or not isfinite(z) or z<=0 for z in s):
        raise ValueError("invalid feature scales")
    return {"sensor_ids":ids,"scales":s,"rows":tuple(x),"labels":y}

def predict_knn(query, model, *, k=3, weighting="uniform"):
    if not isinstance(k,int) or isinstance(k,bool) or not 1<=k<=len(model["rows"]):
        raise ValueError("invalid k")
    if weighting not in ("uniform","inverse_distance"):
        raise ValueError("invalid weighting")
    q=_vec(query,len(model["sensor_ids"]))
    s=model["scales"]
    neighbors=sorted(
        ({"index":i,"label":label,"distance":sqrt(sum(((a-b)/scale)**2 for a,b,scale in zip(q,row,s)))}
         for i,(row,label) in enumerate(zip(model["rows"],model["labels"]))),
        key=lambda z:(z["distance"],z["index"]))[:k]
    # When there are exact matches, ignore nonzero-distance neighbors for weighted voting.
    if weighting=="inverse_distance":
        zero=[z for z in neighbors if z["distance"]==0]
        if zero: neighbors_for_vote=zero
        else: neighbors_for_vote=neighbors
    else:neighbors_for_vote=neighbors
    votes={}
    for z in neighbors_for_vote:
        weight=1. if weighting=="uniform" or z["distance"]==0 else 1./z["distance"]
        votes[z["label"]]=votes.get(z["label"],0.)+weight
    ranked=sorted(votes.items(),key=lambda item:(-item[1],item[0]))
    return {"predicted_label":ranked[0][0],"votes":dict(ranked),"neighbors":neighbors,
            "tie":len(ranked)>1 and abs(ranked[0][1]-ranked[1][1])<1e-12,
            "warning":"KNN vote is not calibrated gas probability or safety confirmation."}
