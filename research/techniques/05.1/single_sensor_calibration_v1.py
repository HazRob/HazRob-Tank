"""Research-only MQ single-sensor log-log calibration at known gas identity."""
from math import isfinite, log10
def _positive(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and v>0

def fit_single_sensor(points,*,gas_label,sensor_id,r0_ohms):
    """Fit log10(Rs/R0)=a+b*log10(ppm) by ordinary least squares."""
    if not isinstance(gas_label,str) or not gas_label or not isinstance(sensor_id,str) or not sensor_id:
        raise ValueError("gas and sensor IDs required")
    if not _positive(r0_ohms):raise ValueError("positive measured R0 required")
    pairs=[tuple(p) for p in points]
    if len(pairs)<3 or any(len(p)!=2 or not all(_positive(v) for v in p) for p in pairs):
        raise ValueError("at least three positive ppm and Rs observations required")
    concentrations=[p[0] for p in pairs]
    if len(set(concentrations))<3:raise ValueError("three distinct calibration concentrations required")
    x=[log10(ppm) for ppm,rs in pairs];y=[log10(rs/r0_ohms) for ppm,rs in pairs]
    xm=sum(x)/len(x);ym=sum(y)/len(y)
    xx=sum((v-xm)**2 for v in x)
    if xx<=0:raise ValueError("degenerate concentrations")
    slope=sum((u-xm)*(v-ym) for u,v in zip(x,y))/xx
    intercept=ym-slope*xm
    if abs(slope)<1e-9:raise ValueError("flat response; concentration not identifiable")
    residuals=[v-(intercept+slope*u) for u,v in zip(x,y)]
    rmse=(sum(v*v for v in residuals)/len(residuals))**.5
    return {"gas_label":gas_label,"sensor_id":sensor_id,"r0_ohms":r0_ohms,
            "intercept":intercept,"slope":slope,"log10_rmse":rmse,
            "min_ppm":min(concentrations),"max_ppm":max(concentrations),
            "n_points":len(pairs),"model":"log10(Rs/R0)=a+b*log10(ppm)"}

def estimate_known_gas_ppm(rs_ohms,model):
    """Interpolate only within calibration concentration range; never extrapolate."""
    if not _positive(rs_ohms):raise ValueError("positive Rs required")
    if not isinstance(model,dict) or any(k not in model for k in ("r0_ohms","slope","intercept","min_ppm","max_ppm")):
        raise ValueError("invalid model")
    logppm=(log10(rs_ohms/model["r0_ohms"])-model["intercept"])/model["slope"]
    if not isfinite(logppm):raise ValueError("invalid estimate")
    lower=log10(model["min_ppm"]);upper=log10(model["max_ppm"])
    if logppm<lower-1e-10 or logppm>upper+1e-10:
        return {"ppm":None,"status":"OUTSIDE_CALIBRATED_RANGE"}
    ppm=10**min(upper,max(lower,logppm))
    return {"ppm":ppm,"status":"WITHIN_CALIBRATED_RANGE",
            "warning":"Assumes independently established gas identity; not safety-certified."}
