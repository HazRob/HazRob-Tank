"""Research-only uncertainty sampling with basic concentration coverage."""
from math import isfinite
def rank_candidates(candidates, *, concentration_weight=0.1):
    if not isinstance(concentration_weight,(int,float)) or not isfinite(concentration_weight) or concentration_weight<0:raise ValueError("invalid weight")
    out=[]
    for i,c in enumerate(candidates):
        if not isinstance(c,dict) or not isinstance(c.get("probabilities"),(list,tuple)) or len(c["probabilities"])<2:raise ValueError("probabilities required")
        p=c["probabilities"]
        if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for v in p) or abs(sum(p)-1)>1e-6:raise ValueError("normalized probabilities required")
        if not isinstance(c.get("ppm"),(int,float)) or not isfinite(c["ppm"]) or c["ppm"]<=0:raise ValueError("positive ppm required")
        uncertainty=1-max(p)
        out.append({"index":i,"score":uncertainty+concentration_weight/(1+c["ppm"]),"uncertainty":uncertainty})
    return sorted(out,key=lambda x:(-x["score"],x["index"]))
