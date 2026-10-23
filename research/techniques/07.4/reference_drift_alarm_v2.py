"""07.4: Chronological matched-blank sensor drift *flag*, not true drift proof."""
from math import isfinite
from statistics import median

def _reading(r,source,sensor,heater,calibrant):
    if not isinstance(r,dict) or any(r.get(k)!=v for k,v in
        (("source_id",source),("sensor_instance_id",sensor),
         ("heater_phase",heater),("blank_reference_id",calibrant))):
        raise ValueError("physical reference context changed")
    if not r.get("day_id") or not r.get("session_id"):raise ValueError("independent days and sessions required")
    ts=r.get("timestamp_s");val=r.get("rs_ohm")
    for x in (ts,val):
        if isinstance(x,bool) or not isinstance(x,(float,int)) or not isfinite(x):
            raise ValueError("finite timestamp and Rs required")
    if val<=0:raise ValueError("positive Rs required")
    return float(ts),float(val)

def matched_blank_drift(baseline,later,*,threshold_fraction=.15):
    a=list(baseline);b=list(later)
    if len(a)<3 or len(b)<3:raise ValueError("at least three independent sessions in each period")
    if isinstance(threshold_fraction,bool) or not isinstance(threshold_fraction,(int,float)) or not isfinite(threshold_fraction) or threshold_fraction<=0:
        raise ValueError("threshold invalid")
    first=a[0]
    ctx=[first.get(x) for x in ("source_id","sensor_instance_id","heater_phase","blank_reference_id")]
    if any(not v for v in ctx):raise ValueError("missing source reference")
    aa=[_reading(x,*ctx) for x in a];bb=[_reading(x,*ctx) for x in b]
    if max(t for t,_ in aa)>=min(t for t,_ in bb):raise ValueError("time periods overlap")
    if {x["session_id"] for x in a}&{x["session_id"] for x in b}:
        raise ValueError("baseline/later session overlap")
    if len({x["session_id"] for x in a})!=len(a) or len({x["session_id"] for x in b})!=len(b):
        raise ValueError("repeated samples from session counted as independent")
    if len({x["day_id"] for x in a+b})<2:
        raise ValueError("single day cannot establish longitudinal stability")
    old=median(x[1] for x in aa);new=median(x[1] for x in bb)
    delta=new/old-1.
    return {"relative_blank_shift":delta,
            "status":"SENSOR_REFERENCE_SHIFT_FLAG" if abs(delta)>threshold_fraction else "NO_FLAG",
            "note":"Shift may be ageing, chamber contamination or environmental confounder",
            "auto_recalibration":False,"Tank_G3":False}
