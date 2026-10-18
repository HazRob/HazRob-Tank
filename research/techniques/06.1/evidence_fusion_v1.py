"""Research-only conservative fusion of correlated categorical evidence."""
from math import isfinite,log,exp

def fuse_evidence(sources, *, prior, max_group_weight=1.0, minimum_sources=2):
    """Log-opinion pooling of probability distributions, grouped by shared evidence.
    Group weights sum to <= max_group_weight, avoiding multiplying correlated votes.
    This is NOT a calibrated Bayesian posterior or a chemical identity decision.
    """
    if not isinstance(prior,dict) or len(prior)<2 or any(not isinstance(k,str) or not k or
        not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0
        for k,v in prior.items()) or abs(sum(prior.values())-1)>1e-6:
        raise ValueError("positive normalized prior over >=2 candidates required")
    if not isinstance(max_group_weight,(int,float)) or isinstance(max_group_weight,bool) or not isfinite(max_group_weight) or max_group_weight<=0:
        raise ValueError("invalid group cap")
    if not isinstance(minimum_sources,int) or isinstance(minimum_sources,bool) or minimum_sources<1:
        raise ValueError("invalid minimum sources")
    items=list(sources)
    if not items:return {"status":"ABSTAIN","reason":"NO_EVIDENCE","scores":None}
    labels=set(prior);groups={}
    for source in items:
        if not isinstance(source,dict) or not isinstance(source.get("group"),str) or not source["group"]:
            raise ValueError("source must declare dependence group")
        p=source.get("probabilities");w=source.get("weight",1.0)
        if not isinstance(p,dict) or set(p)!=labels or any(not isinstance(v,(int,float)) or isinstance(v,bool)
           or not isfinite(v) or v<=0 for v in p.values()) or abs(sum(p.values())-1)>1e-6:
            raise ValueError("strictly positive normalized scores required")
        if not isinstance(w,(int,float)) or isinstance(w,bool) or not isfinite(w) or w<=0:
            raise ValueError("positive finite source weight required")
        groups.setdefault(source["group"],[]).append((p,w))
    if len(groups)<minimum_sources:
        return {"status":"ABSTAIN","reason":"INSUFFICIENT_INDEPENDENT_GROUPS","scores":None,
                "n_groups":len(groups)}
    # Average log-evidence within group rather than double counting it.
    logs={k:log(v) for k,v in prior.items()}
    for group in groups.values():
        total=sum(w for _,w in group)
        group_strength=min(total,max_group_weight)
        for k in labels:
            mean_log=sum(w*log(p[k]/prior[k]) for p,w in group)/total
            logs[k]+=group_strength*mean_log
    peak=max(logs.values());unnormalized={k:exp(v-peak) for k,v in logs.items()}
    total=sum(unnormalized.values());scores={k:v/total for k,v in unnormalized.items()}
    ordered=sorted(scores,key=lambda k:(-scores[k],k))
    return {"status":"FUSED_RESEARCH_SCORES","scores":scores,"ranking":ordered,
            "n_groups":len(groups),"n_sources":len(items),
            "warning":"Heuristic dependence-aware log pooling; not calibrated probabilities or proof of identity."}
