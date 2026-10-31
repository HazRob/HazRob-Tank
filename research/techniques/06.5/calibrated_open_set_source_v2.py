"""06.5: source-domain open-set threshold calibrated on heldout known exposures.

Unknown gas labels are *never* used for threshold selection. This is research
and does not establish calibrated field unknown-gas coverage or Tank safety.
"""
from math import ceil,isfinite
from open_set_recognition_v1 import fit_open_set,predict_open_set


def calibrated_open_set_source(train,known_calibrate,known_test,unknown_test,
                                *, source_id,sensor_ids,alpha=.1):
    sets=[list(x) for x in (train,known_calibrate,known_test,unknown_test)]
    ids=tuple(sensor_ids)
    if (not isinstance(source_id,str) or not source_id or not ids or
        len(set(ids))!=len(ids) or isinstance(alpha,bool) or
        not isinstance(alpha,(int,float)) or not isfinite(alpha) or not 0<alpha<1):
        raise ValueError("source, fixed sensor map, 0<alpha<1 required")
    seen=set();labels=[];rows=[]
    for part in sets:
        if not part:raise ValueError("all four data partitions required")
        part_rows=[];part_labels=set()
        for r in part:
            if r.get("source_id")!=source_id or r.get("scope")!="PURE_GAS":
                raise ValueError("MQ source or pure-gas identity missing")
            key=r.get("exposure_id")
            if not isinstance(key,str) or not key or key in seen:
                raise ValueError("exposure split contamination")
            seen.add(key)
            y=r.get("gas_label")
            if not isinstance(y,str) or not y:
                raise ValueError("label must be a known gas or declared withheld gas")
            vec=r.get("features")
            if not isinstance(vec,(list,tuple)) or len(vec)!=len(ids):
                raise ValueError("measured MQ feature layout mismatch")
            part_rows.append((y,tuple(vec)));part_labels.add(y)
        rows.append(part_rows);labels.append(part_labels)
    if len(labels[0])<2 or not labels[1].issubset(labels[0]) or not labels[2].issubset(labels[0]):
        raise ValueError("training known gases must cover validation and known test")
    if labels[3]&labels[0]:
        raise ValueError("novel gas heldout leaked into any known-training class")
    model=fit_open_set(rows[0])
    n=len(rows[1])
    rank=ceil((n+1)*(1-alpha))
    if rank>n:raise ValueError("insufficient independent known calibration exposures for alpha")
    distances=sorted(predict_open_set(model,v)["distance"] for _,v in rows[1])
    threshold=distances[rank-1]
    model["threshold"]=threshold
    scored_known=[predict_open_set(model,v) for _,v in rows[2]]
    scored_unknown=[predict_open_set(model,v) for _,v in rows[3]]
    return {"method":"INDEPENDENT_KNOWN_CALIBRATION_QUANTILE_DISTANCE",
        "source_id":source_id,"sensor_ids":ids,"threshold":threshold,
        "calibration_n":n,"alpha":alpha,"known_test_n":len(scored_known),
        "withheld_unknown_n":len(scored_unknown),
        "known_accept_rate":sum(x["status"]=="KNOWN_CANDIDATE" for x in scored_known)/len(scored_known),
        "unknown_reject_rate":sum(x["status"]=="UNKNOWN_OR_UNSUPPORTED" for x in scored_unknown)/len(scored_unknown),
        "source_session_and_device_independence_verified":False,
        "physical_gas_identity_certified":False,"Tank_G3":False,
        "limitation":"Heldout unknowns did not set threshold, but gas/source/device changes and threshold uncertainty remain."}
