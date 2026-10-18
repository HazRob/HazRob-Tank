"""Research-only transparent abstention decision gate."""
from math import isfinite
def decide(scores,*,minimum_top=0.7,minimum_margin=0.15,ood=False,conflict=False,valid=True):
    if not isinstance(scores,dict) or len(scores)<2 or any(not isinstance(k,str) or not k or not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for k,v in scores.items()) or abs(sum(scores.values())-1)>1e-6:raise ValueError("normalized scores required")
    if not 0<=minimum_top<=1 or not 0<=minimum_margin<=1:raise ValueError("invalid thresholds")
    ranked=sorted(scores,key=lambda k:(-scores[k],k))
    reasons=[]
    if not valid:reasons.append("INVALID_MEASUREMENT")
    if ood:reasons.append("OOD")
    if conflict:reasons.append("EVIDENCE_CONFLICT")
    if scores[ranked[0]]<minimum_top:reasons.append("LOW_SUPPORT")
    if scores[ranked[0]]-scores[ranked[1]]<minimum_margin:reasons.append("LOW_MARGIN")
    return {"status":"ABSTAIN" if reasons else "TENTATIVE_CANDIDATE","candidate":None if reasons else ranked[0],"reasons":reasons,
            "warning":"Scores may be uncalibrated; thresholds require held-out evaluation; never a safety clearance."}
