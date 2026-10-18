"""HazRob Tank research-only R0 estimation; never assume one calibration gas across MQ types."""
from math import isfinite
from statistics import median

def r0_from_rs(rs_ohms, reference_ratio):
    """Rs/R0 = reference_ratio at documented gas concentration and heater phase."""
    if not all(isfinite(float(x)) for x in (rs_ohms,reference_ratio)):
        raise ValueError("non-finite calibration input")
    if rs_ohms <= 0 or reference_ratio <= 0:
        raise ValueError("Rs and reference ratio must be positive")
    return rs_ohms / reference_ratio

def estimate_r0(rs_samples_ohms, reference_ratio, *, min_samples=5, max_relative_mad=0.10):
    """Robust median estimator with quality gate; MAD/median, not a calibrated uncertainty."""
    samples=list(rs_samples_ohms)
    if len(samples) < min_samples:
        raise ValueError("insufficient samples")
    if any(not isfinite(float(x)) or x <= 0 for x in samples):
        raise ValueError("invalid Rs sample")
    if not isfinite(float(max_relative_mad)) or max_relative_mad < 0:
        raise ValueError("invalid MAD threshold")
    center=median(samples)
    relative_mad=median(abs(x-center) for x in samples)/center
    if relative_mad > max_relative_mad:
        raise ValueError("unstable calibration window")
    return {"r0_ohms":r0_from_rs(center,reference_ratio), "r0_kohms":r0_from_rs(center,reference_ratio)/1000.0, "rs_median_ohms":center, "relative_mad":relative_mad, "n":len(samples)}
