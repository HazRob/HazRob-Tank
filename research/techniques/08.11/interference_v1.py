"""Research-only controlled paired interferent-effect quantification."""
from math import isfinite
def paired_effect(control,interferent):
    a=list(control);b=list(interferent)
    if len(a)<2 or len(a)!=len(b) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in a+b):raise ValueError("matched finite replicate measurements required")
    diffs=[v-u for u,v in zip(a,b)]
    mean=sum(diffs)/len(diffs)
    return {"n_pairs":len(diffs),"mean_effect":mean,"max_absolute_effect":max(abs(v) for v in diffs),
            "warning":"Associations require controlled concentration, humidity, temperature, sensor age and randomized paired exposures."}
