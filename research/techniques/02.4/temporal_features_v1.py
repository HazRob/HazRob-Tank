"""Temporal response features; research-only, single exposure window."""
from math import isfinite
def temporal_features(times_s,values,baseline,*,direction="auto"):
    t=list(times_s);y=list(values)
    if len(t)<3 or len(t)!=len(y): raise ValueError("invalid sample count")
    if not isfinite(baseline) or any(not isfinite(v) for v in t+y): raise ValueError("nonfinite data")
    if any(t[i+1]<=t[i] for i in range(len(t)-1)): raise ValueError("time not increasing")
    if direction not in ("auto","up","down"): raise ValueError("invalid direction")
    if direction=="auto": direction="up" if max(y)-baseline>=baseline-min(y) else "down"
    sign=1 if direction=="up" else -1
    response=[sign*(v-baseline) for v in y]
    peak=max(response)
    if peak<=0: raise ValueError("no positive response")
    idx=response.index(peak)
    def crossing(frac):
        threshold=peak*frac
        if response[0]>=threshold: return t[0]
        for i in range(1,idx+1):
            if response[i]>=threshold and response[i-1]<threshold:
                f=(threshold-response[i-1])/(response[i]-response[i-1])
                return t[i-1]+f*(t[i]-t[i-1])
        return None
    t10=crossing(.1);t90=crossing(.9)
    area=sum((response[i]+response[i+1])*.5*(t[i+1]-t[i]) for i in range(len(t)-1))
    slopes=[(response[i+1]-response[i])/(t[i+1]-t[i]) for i in range(idx)]
    recovery=None
    for i in range(idx+1,len(t)):
        if response[i]<=peak*.5 and response[i-1]>peak*.5:
            f=(response[i-1]-peak*.5)/(response[i-1]-response[i])
            recovery=t[i-1]+f*(t[i]-t[i-1])-t[idx]
            break
    return {"direction":direction,"peak_response":peak,"peak_time_s":t[idx],
        "t10_s":t10,"t90_s":t90,
        "rise_10_90_s":t90-t10 if t10 is not None and t90 is not None else None,
        "max_rising_slope_per_s":max(slopes) if slopes else None,
        "signed_response_area":area,"recovery_t50_s":recovery}
