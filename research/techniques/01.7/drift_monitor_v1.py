"""Research-only baseline drift detector for controlled reference measurements."""
from math import isfinite, log, exp
from statistics import median

def assess_baseline_drift(reference_rs, current_rs, *, max_log_change=0.10,
                          min_samples=5, max_within_mad=0.05):
    """Compares reference and current baseline windows in log resistance.
    Only meaningful when gas, heater phase and environment are controlled."""
    if not isfinite(max_log_change) or max_log_change<0 or not isfinite(max_within_mad) or max_within_mad<0 or min_samples<1:
        raise ValueError("invalid configuration")
    def summarize(xs):
        xs=list(xs)
        if len(xs)<min_samples: raise ValueError("insufficient baseline samples")
        if any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) or x<=0 for x in xs):
            raise ValueError("invalid baseline resistance")
        m=median(xs)
        rel_mad=median(abs(x-m) for x in xs)/m
        return m,rel_mad,len(xs)
    ref,ref_mad,n_ref=summarize(reference_rs)
    now,now_mad,n_now=summarize(current_rs)
    stable=ref_mad<=max_within_mad and now_mad<=max_within_mad
    log_change=log(now/ref)
    return {"drift_flag":stable and abs(log_change)>max_log_change,
            "quality_ok":stable,
            "log_change":log_change,
            "relative_change":now/ref-1,
            "reference_median_ohms":ref,
            "current_median_ohms":now,
            "reference_relative_mad":ref_mad,
            "current_relative_mad":now_mad,
            "reference_n":n_ref,"current_n":n_now,
            "interpretation":"CHECK_DRIFT" if stable and abs(log_change)>max_log_change else
                             "UNSTABLE_BASELINE" if not stable else "WITHIN_RESEARCH_THRESHOLD"}
