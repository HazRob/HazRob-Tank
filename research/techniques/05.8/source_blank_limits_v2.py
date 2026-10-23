"""05.8 — research-only independent blank-session signal screening.

A blank-sigma screening threshold is NOT an independently validated LOD/LOQ,
and cannot be converted to gas ppm without experimental reference standards.
"""
from math import isfinite,sqrt

def _finite(x):
    return isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x)

def source_blank_screening(blank_records, *, source_id, sensor_instance_id,
                           heater_phase, signal_unit, direction="increasing"):
    if not all(isinstance(x,str) and x for x in (source_id,sensor_instance_id,signal_unit)):
        raise ValueError("source, physical sensor and signal units are mandatory")
    if heater_phase not in ("LOW","HIGH","CONSTANT") or direction not in ("increasing","decreasing"):
        raise ValueError("stable documented heater phase and response direction required")
    rows=list(blank_records)
    if len(rows)<10:
        raise ValueError("at least 10 independent blank sessions required")
    sessions=set(); values=[]; dates=set()
    for row in rows:
        if not isinstance(row,dict):
            raise ValueError("blank record required")
        for k in ("session_id","day_id"):
            if not isinstance(row.get(k),str) or not row[k].strip():
                raise ValueError("blank day/session provenance required")
        if row["session_id"] in sessions:
            raise ValueError("multiple records from same blank session cannot count independently")
        if row.get("source_id")!=source_id or row.get("sensor_instance_id")!=sensor_instance_id or row.get("heater_phase")!=heater_phase:
            raise ValueError("blank device/source/heater mismatch")
        val=row.get("signal")
        if not _finite(val):
            raise ValueError("signal must be finite")
        sessions.add(row["session_id"]);dates.add(row["day_id"]);values.append(float(val))
    if len(dates)<2:
        raise ValueError("blank drift assessment requires >=2 independent days")
    mean=sum(values)/len(values)
    sigma=sqrt(sum((x-mean)**2 for x in values)/(len(values)-1))
    if sigma<=0:
        raise ValueError("zero source blank variance does not establish LOD")
    sense=1 if direction=="increasing" else -1
    return {"source_id":source_id,"sensor_instance_id":sensor_instance_id,
        "heater_phase":heater_phase,"signal_unit":signal_unit,"blank_sessions":len(sessions),
        "blank_days":len(dates),"mean":mean,"sample_sd":sigma,
        "screening_3sigma_signal":mean+sense*3*sigma,
        "screening_10sigma_signal":mean+sense*10*sigma,
        "method":"SCREENING_BLANK_3SIGMA_10SIGMA_NOT_LOD_LOQ_CERTIFIED",
        "calibrated_ppm_limits":None,"Tank_G3":False,
        "limitation":"Detection/quantitation require independently referenced low-level gas spikes, false positive/negative and recovery validation."}
