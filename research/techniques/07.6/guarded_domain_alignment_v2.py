"""07.6: Source/target covariate-shift proposal with channel/physical-domain audit.
Computes target mean shift for a research diagnostic, NOT gas concentration.
"""
from math import isfinite
from statistics import mean

def _metadata(rows,source_id,sensor_ids):
    data=list(rows); seen=set();vectors=[]
    if len(data)<3:raise ValueError("at least 3 independent exposures")
    for x in data:
        if not isinstance(x,dict) or x.get("source_id")!=source_id:
            raise ValueError("source identity invalid")
        if tuple(x.get("sensor_ids",()))!=tuple(sensor_ids):
            raise ValueError("channel order/domain mismatch")
        for k in ("physical_device_id","session_id","exposure_id","sensor_family","heater_phase"):
            if not x.get(k):raise ValueError(k+" required")
        if x["exposure_id"] in seen:raise ValueError("no multiple rows as independent exposures")
        seen.add(x["exposure_id"])
        v=x.get("features")
        if not isinstance(v,(tuple,list)) or len(v)!=len(sensor_ids) or any(
          isinstance(z,bool) or not isinstance(z,(float,int)) or not isfinite(z) for z in v):
            raise ValueError("invalid feature vector")
        vectors.append(tuple(float(z) for z in v))
    return data,vectors

def propose_domain_mean_alignment(source,target,*,source_id,target_source_id,sensor_ids):
    channels=tuple(sensor_ids)
    if not channels or len(set(channels))!=len(channels):
        raise ValueError("unique exact source channels required")
    a,av=_metadata(source,source_id,channels)
    b,bv=_metadata(target,target_source_id,channels)
    if {r["exposure_id"] for r in a}&{r["exposure_id"] for r in b}:
        raise ValueError("same exposure on source and target")
    if {r["session_id"] for r in a}&{r["session_id"] for r in b}:
        raise ValueError("session leakage")
    if {r["physical_device_id"] for r in a}&{r["physical_device_id"] for r in b}:
        raise ValueError("target is not independently held out device")
    if {r["sensor_family"] for r in a+b}!={"MQ"}:
        raise ValueError("cross-sensor chemistry transfer (TGS/MQ) not supported")
    if len({r["heater_phase"] for r in a+b})!=1:
        raise ValueError("heater mismatch")
    ma=[mean(x[j] for x in av) for j in range(len(channels))]
    mb=[mean(x[j] for x in bv) for j in range(len(channels))]
    return {"source_mean":ma,"target_mean":mb,"candidate_shift":[x-y for x,y in zip(ma,mb)],
            "status":"RESEARCH_UNLABELED_COVARIATE_SHIFT_CANDIDATE",
            "safe_to_transfer_ppm":False,"Tank_G3":False,
            "note":"Mean alignment may destroy actual analyte composition differences"}
