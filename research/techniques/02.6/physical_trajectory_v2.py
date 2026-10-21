"""02.6: measured 3D/4D environmental/time response, no fabricated concentration dimension."""
from math import isfinite,log
def build_multiaxis(times_s, rs_ohm, rh_pct, *, sensor_instance_id, source_id,
                    baseline_rs_ohm, concentration_ppm=None, independent_ppm_reference=None):
    t=list(times_s);r=list(rs_ohm);rh=list(rh_pct)
    if not sensor_instance_id or not source_id or len(t)<3 or len(t)!=len(r) or len(t)!=len(rh):
        raise ValueError("source, identity, aligned measured time+Rs+RH required")
    for values in (t,r,rh):
        if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in values):
            raise ValueError("invalid measurements")
    if t[0]<0 or any(a>=b for a,b in zip(t,t[1:])) or any(v<=0 for v in r) or any(not 0<=v<=100 for v in rh):
        raise ValueError("invalid time/Rs/RH")
    if not isinstance(baseline_rs_ohm,(int,float)) or isinstance(baseline_rs_ohm,bool) or not isfinite(baseline_rs_ohm) or baseline_rs_ohm<=0:
        raise ValueError("positive baseline required")
    ppm=None
    if concentration_ppm is not None:
        if not independent_ppm_reference or len(concentration_ppm)!=len(t):
            raise ValueError("cannot manufacture 4th concentration axis")
        ppm=list(concentration_ppm)
        if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) or x<=0 for x in ppm):
            raise ValueError("bad source ppm")
    points=[(t[i],log(r[i]/baseline_rs_ohm),rh[i])+( () if ppm is None else (ppm[i],))
            for i in range(len(t))]
    return {"source_id":source_id,"sensor_instance_id":sensor_instance_id,
            "dimensions":3 if ppm is None else 4,"columns":["time_s","log_rs_ratio","RH_pct"]+([] if ppm is None else ["reference_ppm"]),
            "points":points,"independent_ppm_reference":independent_ppm_reference if ppm is not None else None,
            "Tank_G3":False}
