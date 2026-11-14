"""07.9: Controlled learning update authorization with independent replay metrics.

Does not change weights: returns a research-only gate decision and rollback ID.
"""
from math import isfinite

def assess_continual_update(*,candidate_model_id,rollback_model_id,
       source_id,training_session_ids,replay_records,reviewer_approved=False,
       maximum_allowed_mae_increase=0.):
    if not all(isinstance(v,str) and v.strip() for v in
               (candidate_model_id,rollback_model_id,source_id)):
        raise ValueError("versioned model/source identities required")
    if candidate_model_id==rollback_model_id:raise ValueError("different candidate/rollback model required")
    if reviewer_approved is not True:raise PermissionError("human review required before any learning update")
    train=set(training_session_ids)
    if not train:raise ValueError("training session lineage required")
    if isinstance(maximum_allowed_mae_increase,bool) or not isinstance(maximum_allowed_mae_increase,(int,float)) or not isfinite(maximum_allowed_mae_increase) or maximum_allowed_mae_increase<0:
        raise ValueError("invalid tolerance")
    rows=list(replay_records)
    if len(rows)<5:raise ValueError("at least five independent replay events required")
    expo=set();sess=set();err_old=[];err_new=[]
    for r in rows:
        if not isinstance(r,dict) or r.get("source_id")!=source_id or not r.get("reference_trace_id"):
            raise ValueError("provenance missing")
        if not r.get("session_id") or not r.get("exposure_id") or r["session_id"] in train or r["exposure_id"] in expo:
            raise ValueError("replay/train leakage")
        expo.add(r["exposure_id"]);sess.add(r["session_id"])
        vals=[r.get(x) for x in ("reference_value","rollback_prediction","candidate_prediction")]
        if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in vals):
            raise ValueError("nonfinite replay prediction")
        truth,old,new=vals;err_old.append(abs(old-truth));err_new.append(abs(new-truth))
    if len(sess)<3:raise ValueError("replay needs 3 independent sessions")
    baseline=sum(err_old)/len(rows);candidate=sum(err_new)/len(rows)
    ok=candidate<=baseline+maximum_allowed_mae_increase
    return {"status":"RESEARCH_UPDATE_CANDIDATE_ACCEPTABLE" if ok else "REJECT_CATASTROPHIC_FORGETTING",
            "rollback_model_id":rollback_model_id,"candidate_model_id":candidate_model_id,
            "heldout_mae_rollback":baseline,"heldout_mae_candidate":candidate,
            "model_was_updated":False,"production_deployment_authorized":False,"Tank_G3":False}
