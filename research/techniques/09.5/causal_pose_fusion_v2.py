"""Research-only causal pose-aligned response; no inverse lag deconvolution without calibration."""
from math import isfinite

def fuse_causal(samples, *, window_seconds=2.0, max_pose_age_seconds=1.0,
                sensor_delay_seconds=0.0, max_gap_seconds=5.0):
    params=(window_seconds,max_pose_age_seconds,sensor_delay_seconds,max_gap_seconds)
    if any(type(v) not in (int,float) or not isfinite(v) for v in params):raise ValueError('finite parameters required')
    if window_seconds<=0 or max_pose_age_seconds<0 or sensor_delay_seconds<0 or max_gap_seconds<=0:raise ValueError('invalid parameters')
    if sensor_delay_seconds != 0:
        raise ValueError('nonzero sensor delay requires historical pose interpolation not implemented')
    rows=list(samples)
    if not rows:raise ValueError('samples required')
    previous=None;out=[];segment=0
    for r in rows:
        if not isinstance(r,dict) or any(k not in r for k in ('time','pose_time','x','y','response')):raise ValueError('missing fields')
        vals=[r[k] for k in ('time','pose_time','x','y','response')]
        if any(type(v) not in (int,float) or not isfinite(v) for v in vals):raise ValueError('finite numeric samples required')
        t,pt,x,y,response=vals
        if pt>t or t-pt>max_pose_age_seconds:raise ValueError('future or stale pose')
        if previous is not None and t<=previous:raise ValueError('time must increase strictly')
        if previous is not None and t-previous>max_gap_seconds:segment+=1
        previous=t
        # Delay is a declared calibration parameter, not an estimated gas transport lag.
        effective_time=t-sensor_delay_seconds
        if effective_time<pt:raise ValueError('delay would require unavailable historical pose')
        eligible=[p for p in out if p['segment']==segment and effective_time-window_seconds<=p['effective_time']<=effective_time]
        count=len(eligible)+1
        value=(sum(p['raw_response'] for p in eligible)+response)/count
        out.append(dict(time=t,effective_time=effective_time,x=x,y=y,raw_response=response,
                        fused_response=value,segment=segment,n=count))
    return {'points':out,'ppm_approved':False,'physical_source_localized':False,
            'warning':'Causal smoothing only; no deconvolution or spatial source claim'}
