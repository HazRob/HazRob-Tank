"""Research-only MQ heater-phase tagging. No heater actuation."""
from dataclasses import dataclass
from math import isfinite

@dataclass(frozen=True)
class HeaterProfile:
    name: str
    high_seconds: float
    low_seconds: float
    settle_seconds: float = 0.0

    def __post_init__(self):
        vals=(self.high_seconds,self.low_seconds,self.settle_seconds)
        if not all(isfinite(v) for v in vals) or self.high_seconds<=0 or self.low_seconds<=0 or self.settle_seconds<0 or self.settle_seconds>=min(self.high_seconds,self.low_seconds):
            raise ValueError("invalid timing profile")

    @property
    def period(self): return self.high_seconds+self.low_seconds

def classify_phase(profile, elapsed_seconds):
    """Returns heater phase and sampling eligibility, never heater voltage."""
    if not isfinite(elapsed_seconds) or elapsed_seconds<0: raise ValueError("invalid time")
    t=elapsed_seconds % profile.period
    high=t<profile.high_seconds
    phase="HIGH" if high else "LOW"
    since_start=t if high else t-profile.high_seconds
    eligible=since_start>=profile.settle_seconds
    return {"phase":phase,"eligible":eligible,"seconds_into_phase":since_start}

def tag_samples(profile, samples):
    """samples: iterable of (elapsed_seconds, raw_value); preserves every sample."""
    return [{"time_s":t,"value":v,**classify_phase(profile,t)} for t,v in samples]
