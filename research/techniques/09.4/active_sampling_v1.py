"""Research-only uncertainty-based waypoint suggestion without movement control."""
from math import isfinite
def rank_waypoints(waypoints):
    out=[]
    for i,w in enumerate(waypoints):
        if not isinstance(w,dict) or any(k not in w for k in ("x","y","uncertainty","risk")):raise ValueError("waypoint fields required")
        if any(not isinstance(w[k],(int,float)) or isinstance(w[k],bool) or not isfinite(w[k]) for k in ("x","y","uncertainty","risk")) or not 0<=w["uncertainty"]<=1 or not 0<=w["risk"]<=1:raise ValueError("finite bounded scores required")
        out.append({"index":i,"x":w["x"],"y":w["y"],"priority":w["uncertainty"]*(1-w["risk"])})
    return sorted(out,key=lambda v:(-v["priority"],v["index"]))
