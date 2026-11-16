"""01.2 — traceable research R0 estimate, not physical gas calibration.

An Rs/R0 ratio must belong to a specified calibrant, concentration, heater
phase, definition and physical MQ instance; never use a universal table R0.
"""
from dataclasses import dataclass
from math import isfinite
from statistics import median
import re

_TANK = {"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"}
_SHA = re.compile(r"^[0-9a-f]{64}$")


def _positive(value, name):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not isfinite(value) or value <= 0:
        raise ValueError(name + " requires finite positive number")
    return float(value)


@dataclass(frozen=True)
class R0Evidence:
    device_id: str
    sensor_instance_id: str
    sensor_model: str
    measured_rl_ohm: float
    calibrant: str
    reference_concentration_ppm: float
    reference_ratio_rs_over_r0: float
    r0_definition_id: str
    heater_phase: str
    reference_source_id: str
    source_sha256: str

    def __post_init__(self):
        for name in ("device_id","sensor_instance_id","calibrant","r0_definition_id","reference_source_id"):
            if not isinstance(getattr(self, name), str) or not getattr(self, name).strip():
                raise ValueError(name + " required")
        if self.sensor_model not in _TANK:
            raise ValueError("source sensor model not part of Tank MQ8")
        if self.heater_phase not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("known fixed heater phase required; UNKNOWN forbidden")
        for name in ("measured_rl_ohm","reference_concentration_ppm","reference_ratio_rs_over_r0"):
            _positive(getattr(self, name), name)
        if not isinstance(self.source_sha256, str) or not _SHA.fullmatch(self.source_sha256):
            raise ValueError("source SHA256 syntax invalid; actual file bytes must be verified separately")


def estimate_r0(reference: R0Evidence, rs_samples_ohm, *, minimum_count=5, maximum_relative_mad=.10):
    """Median Rs/reference ratio: source-linked parameter only, not certified.

    The SHA256 metadata field is not a substitute for hashing source raw bytes.
    Grouping/window stability does not establish an independent gas standard.
    """
    if not isinstance(reference, R0Evidence):
        raise TypeError("R0Evidence metadata required")
    if isinstance(minimum_count, bool) or not isinstance(minimum_count, int) or minimum_count < 5:
        raise ValueError("minimum_count must be integer >=5")
    if isinstance(maximum_relative_mad, bool) or not isinstance(maximum_relative_mad,(int,float)) or not isfinite(maximum_relative_mad) or maximum_relative_mad < 0:
        raise ValueError("invalid stability threshold")
    samples=[_positive(x, "Rs") for x in rs_samples_ohm]
    if len(samples)<minimum_count:
        raise ValueError("too few source Rs samples")
    rs=median(samples)
    mad=median(abs(x-rs) for x in samples)/rs
    if mad > maximum_relative_mad:
        raise ValueError("source window unstable")
    r0=rs/reference.reference_ratio_rs_over_r0
    return {"r0_ohm":r0,"r0_kohm":r0/1000.,
            "median_rs_ohm":rs,"relative_mad":mad,"n":len(samples),
            "device_id":reference.device_id,"sensor_instance_id":reference.sensor_instance_id,
            "sensor_model":reference.sensor_model,
            "measured_rl_ohm":reference.measured_rl_ohm,
            "calibrant":reference.calibrant,
            "reference_concentration_ppm":reference.reference_concentration_ppm,
            "r0_definition_id":reference.r0_definition_id,
            "heater_phase":reference.heater_phase,
            "reference_source_id":reference.reference_source_id,
            "claimed_source_sha256":reference.source_sha256,
            "status":"RESEARCH_R0_ESTIMATE_NOT_DEVICE_OR_GAS_CERTIFICATION"}
