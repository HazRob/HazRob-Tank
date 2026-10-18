"""Research-only spectral residual mixture-suspicion heuristic."""
from math import sqrt,isfinite
def assess(observed,signatures,threshold=0.25):
    x=list(observed)
    if not x or not signatures or not isinstance(threshold,(int,float)) or not isfinite(threshold) or threshold<0:raise ValueError("invalid inputs")
    n=len(x)
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("finite observed vector required")
    for s in signatures.values():
        if len(s)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in s):raise ValueError("finite aligned signatures required")
    norm=max(sqrt(sum(v*v for v in x)),1e-12)
    residuals={k:sqrt(sum((a-b)**2 for a,b in zip(x,s)))/norm for k,s in signatures.items()}
    nearest=min(residuals,key=lambda k:(residuals[k],k))
    return {"status":"UNEXPLAINED_RESPONSE" if residuals[nearest]>threshold else "SINGLE_SIGNATURE_COMPATIBLE",
            "nearest":nearest,"relative_residual":residuals[nearest],
            "warning":"Residual alone cannot distinguish mixtures from unknown gases, concentrations, humidity, drift or faults."}
