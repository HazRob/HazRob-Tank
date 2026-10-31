"""Strict, research-only R0 provenance contract for each physical MQ instance.

R0 determined from a reference exposure is not a universal model value.
A manufacturer clean-air factor is an unverified curve PRIOR, not actual ppm
calibration. Source bytes must be hashed separately; a claimed hex digest alone
does not establish that a real file exists. No device control/production calls.
"""
from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from math import isfinite
from statistics import median


def _num(v, name, *, positive=False):
    if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v):
        raise ValueError(name+" must be finite real number")
    if positive and v<=0:
        raise ValueError(name+" must be >0")
    return float(v)


def _identity(v,name):
    if not isinstance(v,str) or not v.strip():
        raise ValueError(name+" must identify actual source")
    return v


def verify_sha256(raw_bytes, expected_lowercase_hex):
    """Verify source bytes, rather than merely trusting a hash-shaped string."""
    if not isinstance(raw_bytes,bytes) or not isinstance(expected_lowercase_hex,str):
        raise ValueError("raw data bytes and expected digest required")
    if len(expected_lowercase_hex)!=64 or any(c not in "0123456789abcdef" for c in expected_lowercase_hex):
        raise ValueError("invalid SHA256 digest format")
    return sha256(raw_bytes).hexdigest()==expected_lowercase_hex


@dataclass(frozen=True)
class CalibrationSpec:
    sensor_model: str
    sensor_instance_id: str
    calibration_id: str
    reference_id: str
    reference_type: str
    reference_label: str
    concentration_ppm: float
    ratio_rs_to_r0: float
    ratio_origin: str
    heater_phase: str
    rl_ohm: float
    vc_volts: float
    reference_temperature_c: float
    reference_humidity_pct: float
    source_sha256: str

    def __post_init__(self):
        for k in ("sensor_model","sensor_instance_id","calibration_id","reference_id","reference_label"):
            _identity(getattr(self,k),k)
        if self.reference_type not in ("gas_reference","clean_air_factor"):
            raise ValueError("reference_type must be gas_reference or clean_air_factor")
        if self.ratio_origin not in ("measured_reference","manufacturer_curve_prior"):
            raise ValueError("ratio origin unspecified")
        if self.heater_phase not in ("HIGH","LOW","CONSTANT"):
            raise ValueError("heater phase must be measured, not UNKNOWN")
        if self.reference_type=="clean_air_factor":
            if self.reference_label!="CLEAN_AIR" or self.concentration_ppm!=0:
                raise ValueError("clean-air factor does not define a gas calibration")
            if self.ratio_origin!="manufacturer_curve_prior":
                raise ValueError("clean-air factor must remain a prior")
        else:
            _num(self.concentration_ppm,"concentration_ppm",positive=True)
            if self.reference_label in ("CLEAN_AIR","UNKNOWN"):
                raise ValueError("known gas identity required")
        _num(self.ratio_rs_to_r0,"ratio",positive=True)
        _num(self.rl_ohm,"RL",positive=True)
        _num(self.vc_volts,"Vc",positive=True)
        _num(self.reference_temperature_c,"temperature")
        rh=_num(self.reference_humidity_pct,"humidity")
        if not 0<=rh<=100:
            raise ValueError("relative humidity out of range")
        if not isinstance(self.source_sha256,str) or len(self.source_sha256)!=64 or any(
            c not in "0123456789abcdef" for c in self.source_sha256):
            raise ValueError("source SHA256 must be lower-case hex; verify actual bytes separately")


def derive_r0(spec, samples, *, min_samples=5, max_relative_mad=0.1):
    """Return research R0 candidate; all samples must be from one controlled window.

    Each sample: dict(sensor_instance_id, heater_phase, reference_id, time_s,
                      rs_ohm). Time must rise strictly and resistance be positive.
    """
    if isinstance(min_samples,bool) or not isinstance(min_samples,int) or min_samples<2:
        raise ValueError("min_samples must be >=2")
    stability=_num(max_relative_mad,"max_relative_mad")
    if stability<0 or stability>1:
        raise ValueError("unstable threshold definition")
    items=list(samples)
    if len(items)<min_samples:
        raise ValueError("insufficient window")
    times=[];values=[]
    for s in items:
        if not isinstance(s,dict):
            raise ValueError("measurement must be a dict")
        if (s.get("sensor_instance_id")!=spec.sensor_instance_id or
            s.get("heater_phase")!=spec.heater_phase or
            s.get("reference_id")!=spec.reference_id):
            raise ValueError("physical device, heater or reference changed in calibration window")
        times.append(_num(s.get("time_s"),"time_s"))
        values.append(_num(s.get("rs_ohm"),"rs_ohm",positive=True))
    if any(a>=b for a,b in zip(times,times[1:])):
        raise ValueError("non-increasing or repeated sample timestamps")
    center=median(values)
    mad=median(abs(v-center) for v in values)
    rel_mad=mad/center
    if rel_mad>stability:
        raise ValueError("unstable Rs reference window")
    r0=center/spec.ratio_rs_to_r0
    grade="RESEARCH_CURVE_PRIOR" if spec.ratio_origin=="manufacturer_curve_prior" else "REFERENCE_METADATA_REQUIRES_INDEPENDENT_VERIFICATION"
    return {"sensor_model":spec.sensor_model,"sensor_instance_id":spec.sensor_instance_id,
            "calibration_id":spec.calibration_id,"reference_id":spec.reference_id,
            "reference_type":spec.reference_type,"reference_label":spec.reference_label,
            "reference_concentration_ppm":spec.concentration_ppm,
            "ratio_origin":spec.ratio_origin,"heater_phase":spec.heater_phase,
            "rl_ohm":spec.rl_ohm, "source_sha256":spec.source_sha256,
            "r0_ohm":r0, "r0_kohm":r0/1000.0,
            "rs_median_ohm":center,"relative_mad":rel_mad,"n":len(items),
            "evidence_grade":grade,"source_bytes_sha256_verified_here":False,
            "device_ppm_approved":False,"tank_G3_validated":False}
