"""Research-only MQ signal quality metrics; thresholds are not safety certifications."""
from math import isfinite
from statistics import median

def assess_signal(samples, *, adc_min=0, adc_max=4095, max_missing_fraction=0.1,
                  max_rail_fraction=0.02, max_relative_mad=0.05, min_valid=5):
    """Samples are raw ADC codes or None; return quality diagnostics, not a gas alarm."""
    if adc_max<=adc_min or min_valid<1 or not 0<=max_missing_fraction<=1 or not 0<=max_rail_fraction<=1 or max_relative_mad<0:
        raise ValueError("invalid configuration")
    values=list(samples)
    if not values: raise ValueError("empty window")
    valid=[]
    missing=0
    out_of_range=0
    for x in values:
        if x is None or isinstance(x,float) and not isfinite(x):
            missing+=1
        elif not isinstance(x,(int,float)) or isinstance(x,bool):
            raise TypeError("samples must be numeric or None")
        elif x<adc_min or x>adc_max:
            out_of_range+=1
        else:
            valid.append(x)
    n=len(values)
    rails=sum(x==adc_min or x==adc_max for x in valid)
    center=median(valid) if valid else None
    mad=median(abs(x-center) for x in valid) if valid else None
    relative_mad=mad/abs(center) if center not in (None,0) else (0.0 if mad==0 else float("inf"))
    flags=[]
    if missing/n>max_missing_fraction: flags.append("MISSING")
    if out_of_range: flags.append("OUT_OF_RANGE")
    if len(valid)<min_valid: flags.append("INSUFFICIENT_VALID")
    if valid and rails/len(valid)>max_rail_fraction: flags.append("RAIL_SATURATION")
    if valid and relative_mad>max_relative_mad: flags.append("NOISY_WINDOW")
    return {"ok":not flags,"flags":flags,"n":n,"valid_n":len(valid),
            "missing_fraction":missing/n,"out_of_range_n":out_of_range,
            "rail_fraction":rails/len(valid) if valid else None,
            "median_code":center,"mad_code":mad,"relative_mad":relative_mad}
