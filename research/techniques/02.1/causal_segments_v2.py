"""Causal Rs filter segmented by exposure/session/heater state, research-only.

Never smooth across independent experiments, purge changes or heater phases.
No look-ahead, missing signal interpolation, source data substitution or ppm.
"""
from dataclasses import dataclass
from math import isfinite


@dataclass(frozen=True)
class CausalFilterSpec:
    alpha: float
    max_gap_s: float

    def __post_init__(self):
        for key in ("alpha","max_gap_s"):
            v=getattr(self,key)
            if isinstance(v,bool) or not isinstance(v,(float,int)) or not isfinite(v) or v<=0:
                raise ValueError(key+" invalid")
        if self.alpha>1:
            raise ValueError("alpha outside unit interval")


def filter_exposure_stream(spec,records):
    """Records time_s,rs_ohm,session_id,stage,heater_phase,physical_sensor_id."""
    items=list(records)
    if not items:raise ValueError("empty record stream")
    previous_key=None;previous_time=None;ema=None
    out=[]
    for row in items:
        if not isinstance(row,dict):raise ValueError("observation must be dict")
        timestamp=row.get("time_s");rs=row.get("rs_ohm")
        if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in (timestamp,rs)) or timestamp<0 or rs<=0:
            raise ValueError("finite time and positive Rs required")
        key=tuple(row.get(k) for k in ("physical_sensor_id","session_id","stage","heater_phase"))
        if any(not isinstance(v,str) or not v.strip() for v in key):
            raise ValueError("observation missing group or physical heater identity")
        if key[2] not in ("BASELINE","EXPOSURE","PURGE") or key[3] not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("unrecognized sampling stage or heater phase")
        if previous_time is not None and timestamp<=previous_time:
            raise ValueError("timestamps must be globally increasing")
        reset=(key!=previous_key or previous_time is None or timestamp-previous_time>spec.max_gap_s)
        ema=float(rs) if reset else spec.alpha*rs+(1-spec.alpha)*ema
        out.append({"time_s":timestamp,"filtered_rs_ohm":ema,"reset":reset,
                    "source_session_id":key[1],"sampling_stage":key[2],
                    "sensor_instance_id":key[0],"heater_phase":key[3],
                    "filter_latency_qualifier":"CAUSAL_RECURSIVE_FILTER_UNMEASURED_PHYSICAL_LAG",
                    "ppm_approved":False})
        previous_time=timestamp;previous_key=key
    return out
