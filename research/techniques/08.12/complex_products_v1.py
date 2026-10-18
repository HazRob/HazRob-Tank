"""Research-only complex-product signature comparison; no ingredient identification."""
from math import sqrt,isfinite
def compare(sample,references):
    x=list(sample)
    if not x or not references or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x):raise ValueError("finite sample and references required")
    results=[]
    for name,signature in references.items():
        y=list(signature)
        if not isinstance(name,str) or not name or len(y)!=len(x) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in y):raise ValueError("invalid reference")
        results.append({"reference":name,"distance":sqrt(sum((a-b)**2 for a,b in zip(x,y)))})
    results.sort(key=lambda z:(z["distance"],z["reference"]))
    return {"matches":results,"warning":"Reference similarity cannot establish chemical ingredients or product safety."}
