"""Research-only MQ dynamic heater telemetry checker; it never actuates power.

Timings, voltages and phase setpoints must be supplied from verified sensor-
specific manufacturer instructions and real wiring. A pass here is only
consistency with declared model and is not proof that hardware complied.
"""
from dataclasses import dataclass
from math import isfinite


def finite_positive(value,name):
    if isinstance(value,bool) or not isinstance(value,(float,int)) or not isfinite(value) or value<=0:
        raise ValueError(name+" must be positive finite")
    return float(value)


@dataclass(frozen=True)
class DynamicHeaterSpec:
    sensor_instance_id: str
    model: str
    source_reference: str
    high_v: float
    low_v: float
    high_seconds: float
    low_seconds: float
    settle_seconds: float
    voltage_tolerance_v: float

    def __post_init__(self):
        for key in ("sensor_instance_id","model","source_reference"):
            if not isinstance(getattr(self,key),str) or not getattr(self,key).strip():
                raise ValueError(key+" required")
        for key in ("high_v","low_v","high_seconds","low_seconds"):
            finite_positive(getattr(self,key),key)
        if self.high_v<=self.low_v:
            raise ValueError("declared dynamic cycle has no higher-voltage phase")
        if isinstance(self.settle_seconds,bool) or not isinstance(self.settle_seconds,(int,float)) or not isfinite(self.settle_seconds) or self.settle_seconds<0 or self.settle_seconds>=min(self.high_seconds,self.low_seconds):
            raise ValueError("settling interval not physically bounded")
        if isinstance(self.voltage_tolerance_v,bool) or not isinstance(self.voltage_tolerance_v,(int,float)) or not isfinite(self.voltage_tolerance_v) or self.voltage_tolerance_v<0 or self.voltage_tolerance_v>=self.low_v:
            raise ValueError("invalid voltage tolerance")

    @property
    def period(self):
        return self.high_seconds+self.low_seconds


def evaluate_heater_sample(spec,elapsed_s,measured_heater_v,logged_phase):
    if isinstance(elapsed_s,bool) or not isinstance(elapsed_s,(int,float)) or not isfinite(elapsed_s) or elapsed_s<0:
        raise ValueError("heater elapsed_s invalid")
    if logged_phase not in ("HIGH","LOW","UNKNOWN"):
        raise ValueError("unrecognized physical heater phase")
    v=finite_positive(measured_heater_v,"measured heater voltage")
    t=elapsed_s % spec.period
    expected="HIGH" if t<spec.high_seconds else "LOW"
    into=t if expected=="HIGH" else t-spec.high_seconds
    desired=spec.high_v if expected=="HIGH" else spec.low_v
    base={"sensor_instance_id":spec.sensor_instance_id,
          "model":spec.model,"source_reference":spec.source_reference,
          "expected_phase":expected,"logged_phase":logged_phase,
          "measured_heater_v":v,"target_heater_v":desired,
          "seconds_into_phase":into,"control_actuation":False,
          "gas_concentration_approved":False}
    if into<spec.settle_seconds:
        status="HEATER_TRANSITION_WINDOW"
    elif logged_phase=="UNKNOWN":
        status="HEATER_PHASE_UNOBSERVED"
    elif logged_phase!=expected:
        status="PHASE_LOG_MISMATCH"
    elif abs(v-desired)>spec.voltage_tolerance_v:
        status="HEATER_VOLTAGE_MISMATCH"
    else:
        status="DECLARED_PROFILE_CONSISTENT"
    return {**base,"status":status,"sample_eligible_in_declared_model":status=="DECLARED_PROFILE_CONSISTENT"}
