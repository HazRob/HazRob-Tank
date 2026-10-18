"""Research-only log-log MQ curve matching, using explicitly supplied reference curves."""
from math import log10,isfinite,sqrt

def _positive(value):
    return isinstance(value,(int,float)) and not isinstance(value,bool) and isfinite(value) and value>0

def _validate_curve(points):
    pts=[tuple(p) for p in points]
    if len(pts)<2 or any(len(p)!=2 or not all(_positive(v) for v in p) for p in pts):
        raise ValueError("curve needs >=2 positive (ppm, Rs/R0) points")
    if any(pts[i][0]>=pts[i+1][0] for i in range(len(pts)-1)):
        raise ValueError("concentrations must be strictly increasing")
    return pts

def predict_ratio(curve,concentration_ppm):
    """Log-log interpolation within the supplied curve; no extrapolation."""
    pts=_validate_curve(curve)
    if not _positive(concentration_ppm):raise ValueError("invalid concentration")
    if not pts[0][0]<=concentration_ppm<=pts[-1][0]:
        raise ValueError("outside curve calibration range")
    for i in range(len(pts)-1):
        c0,r0=pts[i];c1,r1=pts[i+1]
        if c0<=concentration_ppm<=c1:
            t=(log10(concentration_ppm)-log10(c0))/(log10(c1)-log10(c0))
            return 10**(log10(r0)+t*(log10(r1)-log10(r0)))
    raise ValueError("no interval")

def rank_curve_candidates(observed_ratios, reference_curves, concentration_ppm, *, min_channels=2):
    """Compare log10(Rs/R0) across aligned sensor IDs for a known concentration."""
    if not isinstance(observed_ratios,dict) or not observed_ratios or not all(isinstance(k,str) and k and _positive(v) for k,v in observed_ratios.items()):
        raise ValueError("invalid observed ratios")
    if not isinstance(reference_curves,dict) or not reference_curves:
        raise ValueError("reference curves required")
    if not isinstance(min_channels,int) or isinstance(min_channels,bool) or min_channels<1:
        raise ValueError("invalid minimum channels")
    results=[]
    for gas,by_sensor in reference_curves.items():
        if not isinstance(gas,str) or not gas or not isinstance(by_sensor,dict):
            raise ValueError("invalid gas entry")
        shared=sorted(set(observed_ratios)&set(by_sensor))
        errors=[];used=[]
        for sensor in shared:
            try:expected=predict_ratio(by_sensor[sensor],concentration_ppm)
            except ValueError:continue
            errors.append((log10(observed_ratios[sensor])-log10(expected))**2)
            used.append(sensor)
        if len(used)>=min_channels:
            results.append({"gas":gas,"rmse_log10_ratio":sqrt(sum(errors)/len(errors)),
                            "sensors_used":used,"n_sensors":len(used)})
    return sorted(results,key=lambda v:(v["rmse_log10_ratio"],-v["n_sensors"],v["gas"]))
