"""06.9: Selective prediction risk/coverage on external, independent test groups.
The threshold is chosen only on disjoint calibration exposures, NEVER on tests.
This is a research evaluation, not calibrated gas-safety probability.
"""
from math import isfinite

def _rows(rows):
    values=list(rows)
    if len(values)<3: raise ValueError("at least 3 exposures needed")
    groups=set()
    for r in values:
        if not isinstance(r,dict) or not all(r.get(k) for k in ("group_id","source_id","session_id")):
            raise ValueError("source/group/session IDs required")
        if r["group_id"] in groups: raise ValueError("only one observation per independent group")
        groups.add(r["group_id"])
        score=r.get("confidence")
        if isinstance(score,bool) or not isinstance(score,(float,int)) or not isfinite(score) or not 0<=score<=1:
            raise ValueError("confidence must be finite in [0,1]")
        if type(r.get("correct")) is not bool: raise ValueError("verified accuracy label required")
    if len({r["source_id"] for r in values})!=1: raise ValueError("mixed physical source")
    return values

def evaluate_selective_risk(calibration,heldout,*,max_calibration_error=.10):
    c=_rows(calibration);h=_rows(heldout)
    if isinstance(max_calibration_error,bool) or not isinstance(max_calibration_error,(float,int)) or not isfinite(max_calibration_error) or not 0<=max_calibration_error<=1:
        raise ValueError("invalid error tolerance")
    if {r["source_id"] for r in c}!={r["source_id"] for r in h}:
        raise ValueError("source changes require a separate transfer protocol")
    if {r["group_id"] for r in c}&{r["group_id"] for r in h} or {r["session_id"] for r in c}&{r["session_id"] for r in h}:
        raise ValueError("session/exposure leakage")
    thresholds=sorted({r["confidence"] for r in c}|{1.})
    eligible=[]
    for t in thresholds:
        accepted=[r for r in c if r["confidence"]>=t]
        if accepted:
            risk=sum(not r["correct"] for r in accepted)/len(accepted)
            if risk<=max_calibration_error:
                eligible.append((len(accepted),-risk,-t,t))
    if not eligible:
        return {"status":"ABSTAIN_NO_CALIBRATION_THRESHOLD","coverage":0.0,
                "risk":None,"Tank_G3":False}
    threshold=max(eligible)[-1]
    accepted=[r for r in h if r["confidence"]>=threshold]
    return {"status":"SOURCE_SELECTIVE_TEST_ONLY","threshold_from_calibration":threshold,
            "accepted":len(accepted),"total":len(h),"coverage":len(accepted)/len(h),
            "risk":sum(not r["correct"] for r in accepted)/len(accepted) if accepted else None,
            "warning":"Empirical heldout risk is not an occupational safety bound",
            "Tank_G3":False}
