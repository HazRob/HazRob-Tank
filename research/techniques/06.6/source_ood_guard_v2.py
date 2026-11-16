"""06.6 — source-specific training envelope with heater/environment guard."""
from ood_detection_v1 import fit,screen
from math import isfinite

def source_ood_check(training_rows,query, *, source_id, sensor_ids,
                     heater_phase, reference_r0_id, temperature_c, rh_percent,
                     observed_temperature_c, observed_rh_percent):
    if not all(isinstance(x,str) and x for x in (source_id,heater_phase,reference_r0_id)):
        raise ValueError("source/R0/heater reference missing")
    if heater_phase not in ("HIGH","LOW","CONSTANT") or not sensor_ids or len(set(sensor_ids))!=len(sensor_ids):
        raise ValueError("invalid fixed sensor/heater definition")
    for item in (temperature_c,rh_percent,observed_temperature_c,observed_rh_percent):
        if isinstance(item,bool) or not isinstance(item,(int,float)) or not isfinite(item):
            raise ValueError("finite environmental context required")
    if not 0<=rh_percent<=100 or not 0<=observed_rh_percent<=100:
        raise ValueError("RH outside physical domain")
    if abs(observed_temperature_c-temperature_c)>3 or abs(observed_rh_percent-rh_percent)>10:
        return {"status":"ABSTAIN_ENVIRONMENT_OUTSIDE_SOURCE_CONTEXT",
                "Tank_G3":False,"gas_identity_verified":False}
    rows=list(training_rows)
    if any(len(row)!=len(sensor_ids) for row in rows) or len(query)!=len(sensor_ids):
        raise ValueError("MQ channel layout mismatch")
    m=fit(rows)
    out=screen(m,query)
    out.update({"source_id":source_id,"reference_r0_id":reference_r0_id,
                "gas_identity_verified":False,"Tank_G3":False,
                "warning":"Envelope is not a calibrated OOD rate or chemical identification."})
    return out
