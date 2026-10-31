"""07.3: Leave entire reference concentration levels and sessions out."""
from math import isfinite
def concentration_holdout(records, *, source_id, gas_label, heldout_ppm):
    data=list(records);target=set(heldout_ppm)
    if not target or any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) or x<=0 for x in target):
        raise ValueError("positive known heldout ppm required")
    train=[];test=[];session_train=set();session_test=set()
    for r in data:
        if not isinstance(r,dict) or r.get("source_id")!=source_id or r.get("gas_label")!=gas_label:
            raise ValueError("source/gas identity mismatch")
        if not r.get("reference_trace_id") or not r.get("session_id") or not r.get("exposure_id"):
            raise ValueError("traceable reference, session and exposure required")
        ppm=r.get("reference_ppm")
        if isinstance(ppm,bool) or not isinstance(ppm,(int,float)) or not isfinite(ppm) or ppm<=0:
            raise ValueError("source ppm invalid")
        if ppm in target:
            test.append(r);session_test.add(r["session_id"])
        else:
            train.append(r);session_train.add(r["session_id"])
    if not train or not test or session_train&session_test:
        raise ValueError("holdout missing or leaked across sessions")
    if {x["exposure_id"] for x in train}&{x["exposure_id"] for x in test}:
        raise ValueError("duplicate exposure")
    levels=sorted({float(x["reference_ppm"]) for x in train})
    return {"train":train,"test":test,"heldout_levels_ppm":sorted(target),
            "heldout_domain":{str(c):"INTERPOLATION" if levels[0]<c<levels[-1] else "EXTRAPOLATION"
                              for c in target},
            "reference_certificate_independently_verified":False,"Tank_G3":False}
