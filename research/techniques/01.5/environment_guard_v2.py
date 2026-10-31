"""Condition-locked environmental Rs normalization for source-specific research.

The numerical coefficients must originate from separately fitted experimental
training exposures. A coefficient is never universal across gas, physical sensor
instance, heater profile, calibration or source study.
"""
from dataclasses import dataclass
from math import exp, log, isfinite


def _real(x,name):
    if isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x):
        raise ValueError(name+" must be finite numeric")
    return float(x)


@dataclass(frozen=True)
class EnvironmentCalibration:
    model_id: str
    source_id: str
    physical_sensor_instance_id: str
    gas_label: str
    heater_phase: str
    ref_t_c: float
    ref_rh_pct: float
    ref_pressure_hpa: float
    coefficients: tuple
    temperature_range: tuple
    rh_range: tuple
    pressure_range: tuple
    training_group_ids: tuple

    def __post_init__(self):
        for k in ("model_id","source_id","physical_sensor_instance_id","gas_label"):
            if not isinstance(getattr(self,k),str) or not getattr(self,k).strip():
                raise ValueError(k+" required")
        if self.heater_phase not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("heater phase not conditioned")
        if len(self.coefficients)!=3 or any(not isinstance(x,(float,int)) or isinstance(x,bool) or not isfinite(x) for x in self.coefficients):
            raise ValueError("three learned finite correction coefficients required")
        for k in ("temperature_range","rh_range","pressure_range"):
            v=getattr(self,k)
            if len(v)!=2 or any(not isinstance(x,(float,int)) or isinstance(x,bool) or not isfinite(x) for x in v) or v[0]>=v[1]:
                raise ValueError(k+" invalid")
        if self.rh_range[0]<0 or self.rh_range[1]>100 or self.pressure_range[0]<=0:
            raise ValueError("invalid environmental physical range")
        if not isinstance(self.training_group_ids,(list,tuple)) or not self.training_group_ids or any(
                not isinstance(x,str) or not x.strip() for x in self.training_group_ids):
            raise ValueError("training experiment groups required")
        if len(set(self.training_group_ids))!=len(self.training_group_ids):
            raise ValueError("duplicate training group")
        _real(self.ref_t_c,"ref_t_c")
        _real(self.ref_rh_pct,"ref_rh_pct")
        _real(self.ref_pressure_hpa,"ref_pressure_hpa")
        for x,limits in ((self.ref_t_c,self.temperature_range),(self.ref_rh_pct,self.rh_range),
                         (self.ref_pressure_hpa,self.pressure_range)):
            if not limits[0]<=x<=limits[1]:
                raise ValueError("reference outside training domain")


def no_group_leakage(calibration, heldout_groups):
    """Explicit experimental group holdout check (not an accuracy test)."""
    groups=list(heldout_groups)
    if not groups or any(not isinstance(x,str) or not x.strip() for x in groups):
        raise ValueError("heldout group IDs required")
    if len(set(groups))!=len(groups):
        raise ValueError("duplicate heldout group")
    if set(groups)&set(calibration.training_group_ids):
        raise ValueError("training/heldout group overlap")
    return True


def normalize_reference_only(calibration, rs_ohm, temp_c, rh_pct, pressure_hpa,
                             *, sensor_instance_id, gas_label, heater_phase):
    v=_real(rs_ohm,"Rs")
    if v<=0:
        raise ValueError("Rs must be >0")
    environment=(_real(temp_c,"temperature"),_real(rh_pct,"humidity"),
                 _real(pressure_hpa,"pressure"))
    if sensor_instance_id!=calibration.physical_sensor_instance_id or gas_label!=calibration.gas_label or heater_phase!=calibration.heater_phase:
        raise ValueError("source reference does not match target gas/device/heater")
    limits=(calibration.temperature_range,calibration.rh_range,calibration.pressure_range)
    if any(not (a<=v<=b) for v,(a,b) in zip(environment,limits)):
        raise ValueError("outside calibrated environment domain")
    refs=(calibration.ref_t_c,calibration.ref_rh_pct,calibration.ref_pressure_hpa)
    correction=sum(beta*(x-y) for beta,x,y in zip(calibration.coefficients,environment,refs))
    if abs(correction)>40:
        raise ValueError("unstable environmental extrapolation")
    ratio=exp(-correction)
    result=rs_ohm*ratio
    if not isfinite(result) or result<=0:
        raise ValueError("non-finite corrected Rs")
    return {"corrected_rs_ohm":result,"correction_log":correction,
            "reference_id":calibration.model_id,"source_id":calibration.source_id,
            "gas_label":gas_label,"physical_sensor_instance_id":sensor_instance_id,
            "ppm_approved":False,"real_Tank_G3":False,
            "coefficient_source_conditioned":True}
