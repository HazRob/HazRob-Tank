"""Research-only piecewise log-log MQ multipoint calibration for known gas."""
from math import log10,isfinite
def _positive(x):
    return isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x) and x>0

def fit_multipoint(points,*,gas_label,sensor_id,r0_ohms):
    if not all(isinstance(s,str) and s for s in (gas_label,sensor_id)) or not _positive(r0_ohms):
        raise ValueError("gas, sensor and positive R0 required")
    p=[tuple(v) for v in points]
    if len(p)<3 or any(len(v)!=2 or not all(_positive(x) for x in v) for v in p):
        raise ValueError("at least three positive reference pairs required")
    p.sort(key=lambda v:v[0])
    if any(p[i][0]>=p[i+1][0] for i in range(len(p)-1)):
        raise ValueError("duplicate concentrations")
    x=[log10(ppm) for ppm,rs in p]
    y=[log10(rs/r0_ohms) for ppm,rs in p]
    changes=[y[i+1]-y[i] for i in range(len(y)-1)]
    if any(abs(v)<1e-12 for v in changes) or not (all(v>0 for v in changes) or all(v<0 for v in changes)):
        raise ValueError("nonmonotonic or flat calibration response; inverse ambiguous")
    return {"gas_label":gas_label,"sensor_id":sensor_id,"r0_ohms":r0_ohms,
            "log_ppm":x,"log_ratio":y,"direction":"increasing" if changes[0]>0 else "decreasing",
            "min_ppm":p[0][0],"max_ppm":p[-1][0],"n_points":len(p)}

def _interp(v,x,y):
    """Interpolate within monotone reference coordinates only."""
    for i in range(len(x)-1):
        a,b=x[i],x[i+1]
        if min(a,b)-1e-12<=v<=max(a,b)+1e-12:
            return y[i]+(v-a)*(y[i+1]-y[i])/(b-a)
    return None

def estimate_multipoint_ppm(rs_ohms,model):
    if not _positive(rs_ohms):raise ValueError("positive Rs required")
    observed=log10(rs_ohms/model["r0_ohms"])
    value=_interp(observed,model["log_ratio"],model["log_ppm"])
    if value is None:return {"ppm":None,"status":"OUTSIDE_CALIBRATED_RANGE"}
    return {"ppm":10**value,"status":"WITHIN_CALIBRATED_RANGE",
            "warning":"Known gas only; interpolation is not independent validation."}

def expected_rs_at_ppm(ppm,model):
    if not _positive(ppm):raise ValueError("positive ppm required")
    value=_interp(log10(ppm),model["log_ppm"],model["log_ratio"])
    if value is None:return {"rs_ohms":None,"status":"OUTSIDE_CALIBRATED_RANGE"}
    return {"rs_ohms":model["r0_ohms"]*10**value,"status":"WITHIN_CALIBRATED_RANGE"}
