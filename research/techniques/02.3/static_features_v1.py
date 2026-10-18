"""Static features for a single controlled MQ exposure window. Research only."""
from math import isfinite
from statistics import mean,median

def extract_static_features(baseline, exposure, *, min_samples=3):
    """Input finite positive Rs samples (ohms); no gas ID or ppm inferred."""
    def validate(xs):
        a=list(xs)
        if len(a)<min_samples or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) or x<=0 for x in a):
            raise ValueError("invalid or insufficient resistance samples")
        return a
    b=validate(baseline);e=validate(exposure)
    b0=median(b);em=median(e)
    return {
        "baseline_median_ohms":b0,
        "exposure_mean_ohms":mean(e),
        "exposure_median_ohms":em,
        "exposure_min_ohms":min(e),
        "exposure_max_ohms":max(e),
        "delta_median_ohms":em-b0,
        "relative_delta":em/b0-1.0,
        "log_fold_change":__import__("math").log(em/b0),
        "exposure_range_ohms":max(e)-min(e),
        "baseline_n":len(b),
        "exposure_n":len(e),
    }
