"""07.5: Correct a *matched* blank shift, never subtract an unknown gas response."""
from math import isfinite

def _finite(x):
    if isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x):
        raise ValueError("finite reference signal required")
    return float(x)

def matched_blank_correction(before,after,query,*,source_id,sensor_ids,reference_id):
    channels=tuple(sensor_ids)
    if not channels or len(set(channels))!=len(channels):
        raise ValueError("unambiguous channel order required")
    meta=(source_id,channels,reference_id)
    for r in (before,after,query):
        if not isinstance(r,dict) or (r.get("source_id"),tuple(r.get("sensor_ids",())),r.get("reference_id"))!=meta:
            raise ValueError("source/channel/reference mismatch")
        if not r.get("session_id") or not r.get("heater_phase") or not r.get("sensor_array_id"):
            raise ValueError("complete physical context required")
        if r["heater_phase"] not in ("LOW","HIGH","CONSTANT"):
            raise ValueError("undefined heater phase")
    if before["heater_phase"]!=after["heater_phase"] or after["heater_phase"]!=query["heater_phase"]:
        raise ValueError("heater phase changed")
    if len({r["session_id"] for r in (before,after,query)})!=3:
        raise ValueError("independent session identity required")
    if any(before.get(k)!=after.get(k) or before.get(k)!=query.get(k)
           for k in ("sensor_array_id","environment_bin","gas_baseline_label")):
        raise ValueError("blank gas and environmental state not matched")
    if before.get("gas_baseline_label")!="CLEAN_BLANK" or after.get("gas_baseline_label")!="CLEAN_BLANK":
        raise ValueError("only clean blank shifts can be subtracted")
    ts=[_finite(r.get("timestamp_s")) for r in (before,after,query)]
    if not ts[0]<ts[1]<=ts[2]:
        raise ValueError("reference measurement order invalid")
    vectors=[]
    for r in (before,after,query):
        v=r.get("features")
        if not isinstance(v,(tuple,list)) or len(v)!=len(channels):raise ValueError("channel count mismatch")
        vectors.append([_finite(x) for x in v])
    start,end,measured=vectors
    offsets=[y-x for x,y in zip(start,end)]
    return {"research_corrected_features":[z-d for z,d in zip(measured,offsets)],
            "matched_blank_offsets":offsets,"source_id":source_id,
            "method":"additive_matched_blank_v2","status":"UNVALIDATED_CORRECTION_PROPOSAL",
            "auto_apply_to_live_sensor":False,"Tank_G3":False}
