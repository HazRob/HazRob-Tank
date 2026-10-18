"""Research-only Bayesian update from supplied class-conditional likelihoods."""
from math import log,exp,isfinite

def update_posterior(prior, evidence, *, min_likelihood=1e-300):
    """Each evidence item: {'id':unique, 'likelihoods':{class: P(e|class)},
    'conditional_independence_justified': True}. Reject unverified independence."""
    if not isinstance(prior,dict) or len(prior)<2 or any(
        not isinstance(k,str) or not k or not isinstance(v,(int,float))
        or isinstance(v,bool) or not isfinite(v) or v<=0 for k,v in prior.items()
    ) or abs(sum(prior.values())-1)>1e-6:
        raise ValueError("positive normalized prior for >=2 classes required")
    if not isinstance(min_likelihood,(int,float)) or isinstance(min_likelihood,bool) or not isfinite(min_likelihood) or not 0<min_likelihood<=1:
        raise ValueError("invalid likelihood floor")
    items=list(evidence)
    if not items:return {"status":"PRIOR_ONLY","posterior":dict(prior),"n_evidence":0,
                          "warning":"Prior only; no observations."}
    labels=set(prior);seen=set();scores={k:log(v) for k,v in prior.items()}
    for item in items:
        if not isinstance(item,dict) or not isinstance(item.get("id"),str) or not item["id"] or item["id"] in seen:
            raise ValueError("unique evidence identifiers required")
        seen.add(item["id"])
        if item.get("conditional_independence_justified") is not True and len(items)>1:
            raise ValueError("joint likelihood required for dependent evidence")
        likelihoods=item.get("likelihoods")
        if not isinstance(likelihoods,dict) or set(likelihoods)!=labels or any(
            not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v)
            or v<0 or v>1 for v in likelihoods.values()):
            raise ValueError("class-conditional likelihoods must be in [0,1]")
        if not any(v>0 for v in likelihoods.values()):
            raise ValueError("evidence impossible under every candidate")
        for k in labels:
            scores[k]+=log(max(min_likelihood,likelihoods[k]))
    peak=max(scores.values());weights={k:exp(v-peak) for k,v in scores.items()}
    total=sum(weights.values())
    posterior={k:v/total for k,v in weights.items()}
    return {"status":"MODEL_CONDITIONAL_POSTERIOR","posterior":posterior,
            "ranking":sorted(labels,key=lambda k:(-posterior[k],k)),
            "n_evidence":len(items),
            "warning":"Posterior is valid only under supplied likelihoods, priors and independence assumptions; not empirically calibrated."}
