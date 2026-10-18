"""Research-only 3D/4D time-aligned MQ feature trajectories."""
from math import isfinite,sqrt

def build_trajectory(times_s, sensor_series, sensor_ids, *, environment=None):
    """3D: (time, sensorA log ratio, sensorB log ratio);
    4D: (time, sensorA, sensorB, environmental covariate).
    All sensor_series are already baseline-normalized log ratios.
    """
    t=list(times_s);ids=tuple(sensor_ids);series=[list(s) for s in sensor_series]
    if len(t)<2 or len(ids)!=2 or len(set(ids))!=2 or len(series)!=2:
        raise ValueError("requires two distinct sensors and >=2 samples")
    if any(len(s)!=len(t) for s in series) or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) for x in t+sum(series,[])):
        raise ValueError("invalid trajectory")
    if any(t[i+1]<=t[i] for i in range(len(t)-1)): raise ValueError("timestamps must increase")
    env=None if environment is None else list(environment)
    if env is not None and (len(env)!=len(t) or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) for x in env)):
        raise ValueError("invalid environment")
    rows=[(t[i],series[0][i],series[1][i]) + (() if env is None else (env[i],)) for i in range(len(t))]
    return {"dimensions":3 if env is None else 4,"sensor_ids":ids,"points":rows,
            "environment_included":env is not None}

def trajectory_path_length(points, *, scales):
    """Euclidean path length in explicitly scaled coordinates (no implicit unit mixing)."""
    pts=[tuple(p) for p in points];sc=list(scales)
    if len(pts)<2 or not sc or any(len(p)!=len(sc) for p in pts):
        raise ValueError("invalid shape")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for p in pts for v in p):
        raise ValueError("invalid points")
    if any(not isinstance(s,(int,float)) or isinstance(s,bool) or not isfinite(s) or s<=0 for s in sc):
        raise ValueError("invalid scales")
    return sum(sqrt(sum(((b-a)/scale)**2 for a,b,scale in zip(p,q,sc))) for p,q in zip(pts,pts[1:]))
