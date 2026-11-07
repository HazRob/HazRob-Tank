"""07.8: Prioritize laboratory *requests* for reference experiments, never execute."""
from math import isfinite

def prioritize_reference_experiments(candidates,*,approved_sensor_models,
                                     upper_safe_reference_ppm=None):
    sensors=set(approved_sensor_models)
    if not sensors:raise ValueError("approved physical sensor models required")
    if upper_safe_reference_ppm is not None and (
        isinstance(upper_safe_reference_ppm,bool) or not isinstance(upper_safe_reference_ppm,(int,float))
        or not isfinite(upper_safe_reference_ppm) or upper_safe_reference_ppm<=0):
        raise ValueError("invalid independently specified upper gas reference")
    results=[];seen=set()
    for c in candidates:
        if not isinstance(c,dict):raise ValueError("experiment request must be dict")
        for key in ("candidate_id","gas_label","sensor_model","reference_trace_id"):
            if not isinstance(c.get(key),str) or not c[key]:
                raise ValueError(key+" required")
        if c["candidate_id"] in seen:raise ValueError("duplicate experiment requests")
        seen.add(c["candidate_id"])
        if c["sensor_model"] not in sensors:raise ValueError("sensor model missing")
        ppm=c.get("reference_ppm");humidity=c.get("rh_percent");uncert=c.get("uncertainty")
        if any(isinstance(x,bool) or not isinstance(x,(float,int)) or not isfinite(x) for x in (ppm,humidity,uncert)):
            raise ValueError("numeric source values required")
        if ppm<=0 or not 0<=humidity<=100 or not 0<=uncert<=1:
            raise ValueError("physical constraints exceeded")
        if upper_safe_reference_ppm is not None and ppm>upper_safe_reference_ppm:
            raise ValueError("out of independently approved lab research range")
        if c.get("certified_lab_plan") is not True or c.get("gas_handling_safety_review") is not True:
            raise PermissionError("physical gas work requires approved lab plan and hazard review")
        score=uncert+(1 if c.get("not_yet_measured") is True else 0)
        results.append({"candidate_id":c["candidate_id"],"priority_score":score,
                        "reference_trace_id":c["reference_trace_id"]})
    return {"proposals":sorted(results,key=lambda x:(-x["priority_score"],x["candidate_id"])),
            "experiment_executed":False,"robot_actuation":False,"Tank_G3":False}
