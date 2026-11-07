"""Session-locked baseline drift comparison. Not an adaptive recalibrator.

Reference and current measurements must describe the SAME physical MQ,
calibrant state, heater phase and comparable environment. A drift flag is
evidence to investigate, not a correction to R0 or a safe gas indication.
"""
from dataclasses import dataclass
from math import log,isfinite
from statistics import median

def numeric(x,key,positive=False):
    if isinstance(x,bool) or not isinstance(x,(float,int)) or not isfinite(x) or (positive and x<=0):
        raise ValueError("invalid "+key)
    return float(x)

@dataclass(frozen=True)
class BaselineWindow:
    physical_sensor_id: str
    session_id: str
    reference_label: str
    heater_phase: str
    temp_c: float
    rh_percent: float
    rs_ohm: tuple

    def __post_init__(self):
        for key in ("physical_sensor_id","session_id","reference_label"):
            if not isinstance(getattr(self,key),str) or not getattr(self,key).strip():
                raise ValueError("missing "+key)
        if self.heater_phase not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("unknown heater phase")
        numeric(self.temp_c,"temperature")
        rh=numeric(self.rh_percent,"rh")
        if not 0<=rh<=100:raise ValueError("RH out of bounds")
        if len(self.rs_ohm)<5:raise ValueError("insufficient baseline samples")
        for x in self.rs_ohm:numeric(x,"Rs",positive=True)

def evaluate_drift(reference,current,*,max_temp_delta_c=2.0,max_rh_delta_pct=5.0,
                   max_within_relative_mad=.05,significant_log_shift=.10):
    limits=(numeric(max_temp_delta_c,"max_temp"),numeric(max_rh_delta_pct,"max_rh"),
            numeric(max_within_relative_mad,"max_mad"),numeric(significant_log_shift,"log_shift"))
    if any(x<0 for x in limits):raise ValueError("negative tolerance")
    if reference.physical_sensor_id!=current.physical_sensor_id:
        return {"status":"INCOMPARABLE_DEVICE","drift_flag":None,"r0_update_authorized":False}
    if reference.reference_label!=current.reference_label or reference.heater_phase!=current.heater_phase:
        return {"status":"INCOMPARABLE_GAS_OR_HEATER","drift_flag":None,"r0_update_authorized":False}
    if abs(reference.temp_c-current.temp_c)>max_temp_delta_c or abs(reference.rh_percent-current.rh_percent)>max_rh_delta_pct:
        return {"status":"INCOMPARABLE_ENVIRONMENT","drift_flag":None,"r0_update_authorized":False}
    medref=median(reference.rs_ohm);medcur=median(current.rs_ohm)
    madref=median(abs(x-medref) for x in reference.rs_ohm)/medref
    madcur=median(abs(x-medcur) for x in current.rs_ohm)/medcur
    if max(madref,madcur)>max_within_relative_mad:
        return {"status":"UNSTABLE_REFERENCE_WINDOW","drift_flag":None,"r0_update_authorized":False}
    change=log(medcur/medref)
    return {"status":"COMPARABLE_BASELINES_RESEARCH_ONLY","log_rs_shift":change,
            "relative_rs_change":medcur/medref-1,
            "drift_flag":abs(change)>significant_log_shift,
            "sensor_instance_id":reference.physical_sensor_id,
            "reference_session_id":reference.session_id,
            "current_session_id":current.session_id,
            "r0_update_authorized":False,"gas_absence_proven":False}
