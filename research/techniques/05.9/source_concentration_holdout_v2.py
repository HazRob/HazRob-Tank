"""05.9 — full-session and entire-concentration held-out evaluation plan."""
from math import isfinite

def grouped_concentration_plan(records, *, heldout_ppm, source_id, sensor_instance_id):
    rows=list(records)
    if not rows:
        raise ValueError("source reference records missing")
    hold=set(heldout_ppm)
    if not hold or any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) or x<=0 for x in hold):
        raise ValueError("positive physical gas ppm holdout levels required")
    sessions=[set(),set()]; exposures=[set(),set()]; levels=[set(),set()]
    for row in rows:
        if not isinstance(row,dict) or row.get("source_id")!=source_id or row.get("sensor_instance_id")!=sensor_instance_id:
            raise ValueError("source/device identity mismatch")
        if any(not isinstance(row.get(k),str) or not row.get(k).strip() for k in ("session_id","exposure_id","gas_label","reference_trace_id")):
            raise ValueError("physical session, exposure, gas and trace required")
        c=row.get("reference_ppm")
        if isinstance(c,bool) or not isinstance(c,(int,float)) or not isfinite(c) or c<=0:
            raise ValueError("gas-phase reference ppm must be finite positive")
        i=int(c in hold)
        sessions[i].add(row["session_id"]);exposures[i].add(row["exposure_id"]);levels[i].add(c)
    if sessions[0]&sessions[1] or exposures[0]&exposures[1]:
        raise ValueError("holdout shares physical session or exposure with training")
    if len(levels[0])<2 or not levels[1] or not hold.issubset(levels[1]):
        raise ValueError("at least two training ppm levels and all intended holdout levels required")
    lo=min(levels[0]);hi=max(levels[0])
    return {"source_id":source_id,"sensor_instance_id":sensor_instance_id,
        "train_ppm_levels":sorted(levels[0]),"heldout_ppm_levels":sorted(levels[1]),
        "heldout_region":{str(x):"INTERPOLATION" if lo<x<hi else "EXTRAPOLATION" for x in sorted(levels[1])},
        "n_train_sessions":len(sessions[0]),"n_heldout_sessions":len(sessions[1]),
        "training_range_ppm":(lo,hi),"real_source_reference_independently_verified":False,
        "Tank_G3":False,
        "limitation":"Independent session labels are asserted; publisher calibration and source SHA must be audited."}
