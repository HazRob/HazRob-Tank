"""Measured MQ transient features from baseline / exposure / recovery stages.

Computes baseline-relative log resistance kinetics (peak, rise, area, recovery).
Times are seconds, Rs is ohm. No chemical identity, ppm, or pure-gas assumption.
"""
from math import log,isfinite
from statistics import median


def valid_stage(items,stage):
    out=list(items)
    if len(out)<2:
        raise ValueError(stage+" needs two or more measurements")
    times=[];values=[]
    for row in out:
        if not isinstance(row,dict):
            raise ValueError("sample must be a mapping")
        t=row.get("time_s");v=row.get("rs_ohm")
        if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in (t,v)) or t<0 or v<=0:
            raise ValueError("invalid time or ohm sample")
        times.append(float(t));values.append(float(v))
    if any(b<=a for a,b in zip(times,times[1:])):
        raise ValueError("timestamps must strictly increase within stage")
    return times,values


def _cross_time(times,values,limit,*,from_above=False):
    for i in range(1,len(times)):
        a,b=abs(values[i-1]),abs(values[i])
        crossed = (a>=limit>=b) if from_above else (a<=limit<=b)
        if crossed:
            if a==b:
                return times[i]
            f=(limit-a)/(b-a)
            return times[i-1]+f*(times[i]-times[i-1])
    return None


def transient_features(sensor_instance_id,session_id,baseline,exposure,recovery):
    if not isinstance(sensor_instance_id,str) or not sensor_instance_id.strip():
        raise ValueError("sensor identity required")
    if not isinstance(session_id,str) or not session_id.strip():
        raise ValueError("session identity required")
    tb,rb=valid_stage(baseline,"baseline")
    te,re=valid_stage(exposure,"exposure")
    tr,rr=valid_stage(recovery,"recovery")
    if not tb[-1]<te[0] or not te[-1]<tr[0]:
        raise ValueError("baseline, exposure and recovery time ranges overlap")
    baseline_rs=median(rb)
    response=[log(x/baseline_rs) for x in re]
    recovery_log=[log(x/baseline_rs) for x in rr]
    max_index=max(range(len(response)),key=lambda i:abs(response[i]))
    peak=response[max_index]
    area=sum(.5*(abs(response[i])+abs(response[i-1]))*
             (te[i]-te[i-1]) for i in range(1,len(te)))
    if abs(peak)>0:
        initial=[0.0]+response
        start=[te[0]]+te
        # First crossing to >= half maximal logarithmic response.
        t50=_cross_time(start,initial,abs(peak)*.5)
        # Include final exposure peak as recovery starting boundary.
        recovery_start=[te[-1]]+tr
        recovery_segment=[response[-1]]+recovery_log
        t_recover=_cross_time(recovery_start,recovery_segment,abs(peak)*.5,from_above=True)
    else:
        t50=None;t_recover=None
    return {"sensor_instance_id":sensor_instance_id,"session_id":session_id,
            "baseline_median_rs_ohm":baseline_rs,
            "peak_log_response":peak,"peak_time_s":te[max_index],
            "peak_abs_log_response":abs(peak),
            "integrated_abs_log_response_s":area,
            "time_to_half_peak_s":t50-te[0] if t50 is not None else None,
            "half_recovery_after_exposure_s":t_recover-te[-1] if t_recover is not None else None,
            "peak_direction":"RS_DECREASE" if peak<0 else "RS_INCREASE" if peak>0 else "NO_RESPONSE",
            "gas_identity_proven":False,"ppm_approved":False}
