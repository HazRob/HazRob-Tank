"""Research-only physics-informed feature augmentation for MQ fingerprints.
Not a physical gas identification model: requires independently known ppm.
"""
from math import isfinite, log10

def _positive(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and v>0

def _curve(points):
    p=[tuple(z) for z in points]
    if len(p)<2 or any(len(z)!=2 or not all(_positive(v) for v in z) for z in p):
        raise ValueError("invalid reference curve")
    if any(p[i][0]>=p[i+1][0] for i in range(len(p)-1)):
        raise ValueError("concentrations must increase")
    return p

def _interpolate(points, ppm):
    p=_curve(points)
    if not _positive(ppm) or not p[0][0]<=ppm<=p[-1][0]:
        raise ValueError("ppm outside reference curve")
    for (x0,y0),(x1,y1) in zip(p,p[1:]):
        if x0<=ppm<=x1:
            t=(log10(ppm)-log10(x0))/(log10(x1)-log10(x0))
            return 10**(log10(y0)+t*(log10(y1)-log10(y0))
    raise ValueError("unreachable")

def augment_physics_features(observed, gas_curves, concentration_ppm, sensor_ids):
    """Per-sensor log10(Rs/R0) and reference residuals at KNOWN ppm.
    Missing/out-of-range curve channels are flagged with masks, not silently filled.
    """
    ids=tuple(sensor_ids)
    if not ids or len(set(ids))!=len(ids) or any(not isinstance(v,str) or not v for v in ids):
        raise ValueError("invalid sensor IDs")
    if not isinstance(observed,dict) or set(observed)!=set(ids) or any(not _positive(observed[s]) for s in ids):
        raise ValueError("require aligned positive Rs/R0 observations")
    if not isinstance(gas_curves,dict) or not gas_curves:
        raise ValueError("reference curves required")
    if not _positive(concentration_ppm):raise ValueError("known positive ppm required")
    raw=[log10(observed[s]) for s in ids]
    candidates={}
    for gas,curves in sorted(gas_curves.items()):
        if not isinstance(gas,str) or not gas or not isinstance(curves,dict):
            raise ValueError("invalid gas reference")
        residuals=[];masks=[]
        for s in ids:
            if s not in curves:
                residuals.append(None);masks.append(False);continue
            try:pred=_interpolate(curves[s],concentration_ppm)
            except ValueError:
                residuals.append(None);masks.append(False);continue
            residuals.append(log10(observed[s]/pred));masks.append(True)
        candidates[gas]={"log10_residuals":residuals,"valid_mask":masks,
                         "valid_channels":sum(masks)}
    return {"sensor_ids":ids,"log10_observed":raw,"reference_residuals":candidates,
            "known_concentration_ppm":concentration_ppm,
            "warning":"Candidate-conditioned features; never use true test gas label to select its features."}

def rank_reference_residuals(augmented,*,min_channels=2):
    if not isinstance(min_channels,int) or isinstance(min_channels,bool) or min_channels<1:
        raise ValueError("invalid min_channels")
    rows=[]
    for gas,item in augmented["reference_residuals"].items():
        values=[v for v in item["log10_residuals"] if v is not None]
        if len(values)>=min_channels:
            rows.append({"gas_label":gas,"mean_absolute_log10_residual":sum(abs(v) for v in values)/len(values),
                         "valid_channels":len(values)})
    return sorted(rows,key=lambda x:(x["mean_absolute_log10_residual"],-x["valid_channels"],x["gas_label"]))
