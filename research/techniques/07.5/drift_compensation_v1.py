"""Research-only additive reference-offset compensation."""
from math import isfinite
def estimate_offsets(reference_before,reference_after):
    a=list(reference_before);b=list(reference_after)
    if not a or len(a)!=len(b) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in a+b):raise ValueError("matched reference vectors required")
    return {"offsets":[new-old for old,new in zip(a,b)],"n":len(a),
            "warning":"Valid only for stable matched reference conditions; may erase true analyte response otherwise."}
def compensate(model,measurement):
    if not isinstance(model,dict) or not isinstance(model.get("n"),int) or isinstance(model["n"],bool) or model["n"]<1 or not isinstance(model.get("offsets"),(tuple,list)) or len(model["offsets"])!=model["n"] or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in model["offsets"]):
        raise ValueError("invalid compensation model")
    x=list(measurement)
    if len(x)!=model["n"] or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("invalid measurement")
    return [v-d for v,d in zip(x,model["offsets"])]
