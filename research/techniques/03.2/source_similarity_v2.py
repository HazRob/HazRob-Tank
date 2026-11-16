"""03.2: compare sensor responses only in aligned source, basis, heater and units."""
from math import sqrt,isfinite
def context_similarity(a,b,*,fitted_scales):
    needed=("sensor_ids","source_id","feature_units","reference_definition","heater_phase","features")
    if any(key not in a or key not in b for key in needed):
        raise ValueError("missing context")
    for key in needed[:-1]:
        if a[key]!=b[key]:
            raise ValueError("different source/device/units/phase or channel order")
    v=tuple(a["features"]);w=tuple(b["features"]);sc=tuple(fitted_scales)
    if not v or len(v)!=len(w) or len(v)!=len(sc) or len(set(a["sensor_ids"]))!=len(v):
        raise ValueError("mismatched feature shape")
    if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in v+w):
        raise ValueError("invalid feature")
    if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) or x<=0 for x in sc):
        raise ValueError("scales must be training-fitted")
    dist=sqrt(sum(((x-y)/s)**2 for x,y,s in zip(v,w,sc)))
    return {"scaled_distance":dist,"compatible":True,
            "ppm_approved":False,"gas_identity_confirmed":False,
            "condition":"same-source / feature units / R0 convention / heater, channel alignment"}
