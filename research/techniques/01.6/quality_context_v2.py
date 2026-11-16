"""Context-aware QC of sampled MQ ADC time windows, research-only.

Sampling irregularity, fan state changes, heater phase ambiguity and ADC rails
may make an otherwise numeric signal unfit for chemical feature extraction.
A clean signal is NOT clean air and is NOT a certified gas-free condition.
"""
from dataclasses import dataclass
from math import isfinite


@dataclass(frozen=True)
class WindowProtocol:
    sensor_instance_id: str
    session_id: str
    expected_sampling_s: float
    max_gap_multiplier: float
    adc_max_code: int
    max_missing_fraction: float = 0.1
    max_saturation_fraction: float = 0.02
    min_samples: int = 5

    def __post_init__(self):
        if not self.sensor_instance_id or not self.session_id:
            raise ValueError("physical sensor/session identities required")
        for name in ("expected_sampling_s","max_gap_multiplier"):
            v=getattr(self,name)
            if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0:
                raise ValueError(name+" invalid")
        if self.max_gap_multiplier<1:
            raise ValueError("max_gap_multiplier below 1")
        if isinstance(self.adc_max_code,bool) or not isinstance(self.adc_max_code,int) or self.adc_max_code<2:
            raise ValueError("invalid ADC max code")
        if isinstance(self.min_samples,bool) or not isinstance(self.min_samples,int) or self.min_samples<2:
            raise ValueError("invalid min_samples")
        for name in ("max_missing_fraction","max_saturation_fraction"):
            v=getattr(self,name)
            if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or not 0<=v<=1:
                raise ValueError(name+" invalid")


def inspect_window(protocol, observations):
    """One exposure window. Each row: time_s, adc_code or None, fan_state, heater_phase."""
    items=list(observations)
    if len(items)<protocol.min_samples:
        raise ValueError("insufficient sampling window")
    times=[];codes=[];fan=[];heaters=[]
    for s in items:
        if not isinstance(s,dict):
            raise ValueError("each observation must be a mapping")
        t=s.get("time_s")
        if isinstance(t,bool) or not isinstance(t,(int,float)) or not isfinite(t) or t<0:
            raise ValueError("invalid timestamp")
        x=s.get("adc_code")
        if x is not None and (isinstance(x,bool) or not isinstance(x,int) or not 0<=x<=protocol.adc_max_code):
            raise ValueError("invalid ADC reading")
        f=s.get("fan_state")
        if f not in ("ON","OFF"):
            raise ValueError("unknown fan state")
        h=s.get("heater_phase")
        if h not in ("HIGH","LOW","CONSTANT","UNKNOWN"):
            raise ValueError("unrecognized heater state")
        times.append(t);codes.append(x);fan.append(f);heaters.append(h)
    gaps=[b-a for a,b in zip(times,times[1:])]
    if any(x<=0 for x in gaps):
        raise ValueError("non-increasing observation time")
    missing=sum(x is None for x in codes)
    saturated=sum(x in (0,protocol.adc_max_code) for x in codes if x is not None)
    n=len(codes)
    flags=[]
    if missing/n>protocol.max_missing_fraction:flags.append("MISSING_READINGS")
    if saturated/n>protocol.max_saturation_fraction:flags.append("ADC_SATURATION")
    if any(g>protocol.expected_sampling_s*protocol.max_gap_multiplier for g in gaps):flags.append("SAMPLING_GAP")
    if len(set(fan))!=1:flags.append("FAN_STATE_CHANGED")
    if len(set(heaters))!=1 or "UNKNOWN" in heaters:flags.append("HEATER_PHASE_INCONSISTENT")
    return {"sensor_instance_id":protocol.sensor_instance_id,"session_id":protocol.session_id,
            "n":n,"missing_fraction":missing/n,"saturation_fraction":saturated/n,
            "max_sampling_gap_s":max(gaps),"flags":flags,
            "eligible_for_features":not flags,
            "chemical_blank_proven":False,"gas_concentration_approved":False}
