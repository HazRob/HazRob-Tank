"""08.5: descriptive ensemble of dependent models, NOT Bayes evidence multiplication."""
from math import isfinite
def ensemble_same_array(outputs, *, source_id, physical_session_id, array_id):
    data=list(outputs)
    if len(data)<2:raise ValueError("at least two models required")
    model_ids=set();label_keys=None;probabilities=[]
    for x in data:
        if not isinstance(x,dict) or any(x.get(k)!=v for k,v in
            (("source_id",source_id),("physical_session_id",physical_session_id),("sensor_array_id",array_id))):
            raise ValueError("not aligned to same physical sensor event")
        model=x.get("model_id")
        if not isinstance(model,str) or not model or model in model_ids:
            raise ValueError("unique model versions required")
        model_ids.add(model)
        scores=x.get("probabilities")
        if not isinstance(scores,dict) or len(scores)<2:
            raise ValueError("complete candidate probabilities required")
        if label_keys is None:label_keys=set(scores)
        if set(scores)!=label_keys or any(isinstance(p,bool) or not isinstance(p,(int,float)) or not isfinite(p) or p<0 for p in scores.values()) or abs(sum(scores.values())-1)>1e-8:
            raise ValueError("aligned normalized probabilities required")
        probabilities.append(scores)
    averaged={name:sum(r[name] for r in probabilities)/len(data) for name in sorted(label_keys)}
    return {"candidate_scores":averaged,"same_physical_array_models":len(data),
            "evidence_groups_independent":False,"calibrated_posterior":False,
            "gas_identity_verified":False,"Tank_G3":False}
