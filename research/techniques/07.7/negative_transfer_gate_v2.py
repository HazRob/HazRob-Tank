"""07.7: Compare frozen baseline vs proposed transferred model on disjoint labels."""
from math import isfinite

def reject_negative_transfer(rows, *, training_session_ids, expected_source_id,
                             allowed_mean_error_increase=0.):
    data=list(rows);train=set(training_session_ids)
    if len(data)<5 or not train:raise ValueError("independent heldout test cohort needed")
    if isinstance(allowed_mean_error_increase,bool) or not isinstance(allowed_mean_error_increase,(float,int)) or not isfinite(allowed_mean_error_increase) or allowed_mean_error_increase<0:
        raise ValueError("invalid rejection tolerance")
    ids=set(); sessions=set(); baseline=[];transferred=[]
    for x in data:
        if not isinstance(x,dict) or x.get("source_id")!=expected_source_id:
            raise ValueError("source mismatch")
        if not x.get("session_id") or not x.get("exposure_id") or not x.get("reference_trace_id"):
            raise ValueError("traceable source/labels required")
        if x["session_id"] in train or x["exposure_id"] in ids:
            raise ValueError("train/test leakage")
        ids.add(x["exposure_id"]);sessions.add(x["session_id"])
        vals=[x.get(k) for k in ("reference_value","baseline_prediction","transfer_prediction")]
        if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in vals):
            raise ValueError("finite comparable predictions/reference required")
        ref,base,new=vals
        baseline.append(abs(base-ref));transferred.append(abs(new-ref))
    if len(sessions)<3:raise ValueError("needs >=3 independent test sessions")
    e0=sum(baseline)/len(baseline);e1=sum(transferred)/len(transferred)
    return {"baseline_mae":e0,"transfer_mae":e1,"status":"REJECT_NEGATIVE_TRANSFER" if e1>e0+allowed_mean_error_increase else "NO_WORSE_ON_THIS_HELDOUT_SOURCE",
            "permission_for_auto_deployment":False,"Tank_G3":False}
