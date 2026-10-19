"""Research-only time-aligned spatial response fusion with causal windows."""
from math import isfinite
def fuse(samples, *, window_seconds=2.0, max_pose_age_seconds=1.0):
    if not isinstance(window_seconds,(int,float)) or isinstance(window_seconds,bool) or not isfinite(window_seconds) or window_seconds<=0 or not isinstance(max_pose_age_seconds,(int,float)) or isinstance(max_pose_age_seconds,bool) or not isfinite(max_pose_age_seconds) or max_pose_age_seconds<0:raise ValueError("invalid windows")
    data=list(samples)
    if not data:raise ValueError("samples required")
    rows=[]
    for row in data:
        if not isinstance(row,dict) or not all(k in row for k in ("time","pose_time","x","y","response")):raise ValueError("timestamped pose and response required")
        vals=[row[k] for k in ("time","pose_time","x","y","response")]
        if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in vals):raise ValueError("finite data required")
        if row["pose_time"]>row["time"] or row["time"]-row["pose_time"]>max_pose_age_seconds:raise ValueError("future or stale pose rejected")
        rows.append(row)
    rows.sort(key=lambda r:r["time"])
    result=[]
    for row in rows:
        window=[p for p in rows if row["time"]-window_seconds<=p["time"]<=row["time"]]
        result.append({"time":row["time"],"x":row["x"],"y":row["y"],
                       "fused_response":sum(p["response"] for p in window)/len(window),
                       "n":len(window)})
    return {"points":result,"warning":"Causal time-window smoothing only; does not correct sensor lag, airflow, spatial uncertainty or motion-induced bias."}
