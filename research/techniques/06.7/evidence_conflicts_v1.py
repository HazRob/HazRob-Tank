"""Research-only contradiction and negative-evidence screening."""
from math import isfinite
def assess(candidate, readings, response_matrix, *, minimum_detection_power=0.95):
    if not isinstance(candidate,str) or not candidate or not 0<minimum_detection_power<=1:raise ValueError("invalid candidate/threshold")
    if not isinstance(readings,dict) or not isinstance(response_matrix,dict):raise ValueError("mapping required")
    contradictions=[];unknown=[];supports=[]
    for sensor,observed in readings.items():
        if observed not in ("DETECTED","NOT_DETECTED","FAULT","NOT_MEASURED"):raise ValueError("invalid observation state")
        rule=response_matrix.get(candidate,{}).get(sensor)
        if observed in ("FAULT","NOT_MEASURED") or rule is None:
            unknown.append(sensor);continue
        power=rule.get("detection_power")
        if not isinstance(power,(int,float)) or isinstance(power,bool) or not isfinite(power) or not 0<=power<=1:raise ValueError("invalid empirical detection power")
        if observed=="NOT_DETECTED" and power>=minimum_detection_power:contradictions.append(sensor)
        elif observed=="DETECTED":supports.append(sensor)
        else:unknown.append(sensor)
    return {"candidate":candidate,"status":"NEGATIVE_EVIDENCE_CONFLICT" if contradictions else "NO_STRONG_NEGATIVE_CONFLICT",
            "conflicting_sensors":contradictions,"supporting_sensors":supports,"uninformative_sensors":unknown,
            "hard_exclusion":False}
