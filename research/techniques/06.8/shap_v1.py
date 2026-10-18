"""Research-only exact Shapley attributions for small feature sets."""
from itertools import combinations
from math import factorial,isfinite
def explain(predict,baseline,query,max_features=8):
    if not callable(predict) or not isinstance(max_features,int) or isinstance(max_features,bool) or max_features<1:
        raise ValueError("invalid predictor or max_features")
    a=list(baseline);b=list(query);n=len(a)
    if n<1 or n!=len(b) or n>max_features or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in a+b):raise ValueError("invalid feature vectors")
    def value(subset):
        x=[b[j] if j in subset else a[j] for j in range(n)]
        v=predict(x)
        if not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v):raise ValueError("finite scalar model output required")
        return v
    base=value(set());end=value(set(range(n)));contrib=[]
    for i in range(n):
        v=0.
        others=[j for j in range(n) if j!=i]
        for k in range(n):
            for subset in combinations(others,k):
                s=set(subset);weight=factorial(k)*factorial(n-k-1)/factorial(n)
                v+=weight*(value(s|{i})-value(s))
        contrib.append(v)
    return {"baseline_output":base,"query_output":end,"attributions":contrib,"residual":end-base-sum(contrib),
            "warning":"Baseline-dependent exact interventional Shapley; not causal and may generate implausible correlated sensor combinations."}
