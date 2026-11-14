"""01.3: source-locked gas-specific log sensitivity prior (research only).

Rs/R0 = a * (C_ppm / output_scale_ppm)**b.
Concentration output scale is essential: current MQ3 library uses scale 50.
The numeric inversion is a mathematical curve inversion, NOT validated ppm.
"""
from dataclasses import dataclass
from math import isfinite

_TANK={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"}


def _positive(x, name):
    if isinstance(x, bool) or not isinstance(x,(int,float)) or not isfinite(x) or x<=0:
        raise ValueError(name+" requires positive finite number")
    return float(x)


@dataclass(frozen=True)
class LogCurvePrior:
    sensor_model: str
    target_gas: str
    a: float
    b: float
    output_scale_ppm: float
    lower_ppm: float
    upper_ppm: float
    r0_definition_id: str
    heater_phase: str
    source_id: str
    evidence_tier: str = "DIGITIZED_MANUFACTURER_PRIOR"

    def __post_init__(self):
        if self.sensor_model not in _TANK:
            raise ValueError("unsupported sensor")
        for key in ("target_gas","r0_definition_id","source_id"):
            if not isinstance(getattr(self,key),str) or not getattr(self,key).strip():
                raise ValueError("missing "+key)
        for key in ("a","output_scale_ppm","lower_ppm","upper_ppm"):
            _positive(getattr(self,key),key)
        if self.lower_ppm >= self.upper_ppm:
            raise ValueError("invalid source concentration bounds")
        if isinstance(self.b,bool) or not isinstance(self.b,(int,float)) or not isfinite(self.b) or self.b==0:
            raise ValueError("invalid log exponent")
        if self.heater_phase not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("heater profile unknown")
        if self.evidence_tier not in ("DIGITIZED_MANUFACTURER_PRIOR","EXTERNAL_EXPERIMENTAL_FIT_UNCALIBRATED"):
            raise ValueError("no certified-ppm evidence tier accepted")

    def forward_rs_r0(self, concentration_ppm):
        c=_positive(concentration_ppm,"ppm")
        if not self.lower_ppm<=c<=self.upper_ppm:
            raise ValueError("no curve extrapolation")
        ratio=self.a*(c/self.output_scale_ppm)**self.b
        return _positive(ratio,"model Rs/R0")

    def inverse_ppm_prior(self, ratio):
        r=_positive(ratio,"Rs/R0")
        c=self.output_scale_ppm*(r/self.a)**(1./self.b)
        if not isfinite(c) or not self.lower_ppm<=c<=self.upper_ppm:
            raise ValueError("not invertible inside documented source range")
        return c

    def check_sensor_r0_compatibility(self, sensor_model, r0_definition_id, heater_phase):
        if (sensor_model!=self.sensor_model or r0_definition_id!=self.r0_definition_id
                or heater_phase!=self.heater_phase):
            raise ValueError("curve/R0/source/heater mismatch")
        return True
