"""Research-only odor plume encounter/event detector."""
from math import isfinite
def encounters(values,baseline,threshold=3):
    x=list(values)
    if not x or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in x+[baseline,threshold]) or threshold<=0:raise ValueError("finite measurements and positive threshold required")
    events=[];inside=False;start=None
    for i,v in enumerate(x):
        active=v-baseline>=threshold
        if active and not inside:start=i;inside=True
        if not active and inside:events.append({"start":start,"end_exclusive":i});inside=False
    if inside:events.append({"start":start,"end_exclusive":len(x)})
    return {"events":events,"warning":"Threshold events are not gas identification or safe-navigation instructions."}
