"""Source-limited MQ log power sensitivity curves (research-only, not ppm approval).

Data model accepts one manufacturer-digitized curve OR one experimentally fitted
curve, with its original concentration convention and R0 definition. Both are
scientific priors; passing this module's tests does not certify calibration.
No mixed-gas quantitative inference and no automatic source averaging.
"""
from dataclasses import dataclass
from math import isfinite, log, exp

def _positive(value, key):
    if isinstance(value,bool) or not isinstance(value,(int,float)) or not isfinite(value) or value<=0:
        raise ValueError(key+" must be finite and positive")
    return float(value)

@dataclass(frozen=True)
class SourceCurve:
    sensor_model: str
    gas_label: str
    source_id: str
    reference_type: str
    reference_definition: str
    curve_a: float
    curve_b: float
    ppm_per_internal_unit: float
    min_internal: float
    max_internal: float
    model_kind: str = "MANUFACTURER_PRIOR"
    reference_heater_phase: str = "CONSTANT"
    physical_sensor_instance_id: str = ""

    def __post_init__(self):
        for key in ("sensor_model","gas_label","source_id","reference_type","reference_definition"):
            value=getattr(self,key)
            if not isinstance(value,str) or not value.strip():
                raise ValueError("provenance required for "+key)
        if self.model_kind not in ("MANUFACTURER_PRIOR","EXTERNAL_EMPIRICAL_RESEARCH"):
            raise ValueError("unsupported evidence type")
        if self.reference_heater_phase not in ("CONSTANT","LOW","HIGH"):
            raise ValueError("heater phase is missing")
        for key in ("curve_a","ppm_per_internal_unit","min_internal","max_internal"):
            _positive(getattr(self,key),key)
        if self.max_internal<=self.min_internal:
            raise ValueError("concentration range inverted")
        if isinstance(self.curve_b,bool) or not isinstance(self.curve_b,(int,float)) or not isfinite(self.curve_b) or self.curve_b==0:
            raise ValueError("zero/invalid power exponent")

    @property
    def ppm_min(self):
        return self.min_internal * self.ppm_per_internal_unit

    @property
    def ppm_max(self):
        return self.max_internal * self.ppm_per_internal_unit


def predicted_ratio(curve, concentration_ppm):
    """Forward shape hypothesis; do not call it a physical sensor calibration."""
    ppm=_positive(concentration_ppm,"concentration_ppm")
    if not curve.ppm_min<=ppm<=curve.ppm_max:
        raise ValueError("outside source curve concentration domain")
    internal=ppm/curve.ppm_per_internal_unit
    value=exp(log(curve.curve_a)+curve.curve_b*log(internal))
    if not isfinite(value) or value<=0:
        raise ValueError("unstable curve computation")
    return {"rs_over_r0_prior":value,"source_id":curve.source_id,
            "evidence_kind":curve.model_kind,"heater_phase":curve.reference_heater_phase,
            "assumed_gas":curve.gas_label,"physical_device_validated":False}


def inverse_candidate(curve, observed_rs_over_r0, *, r0_calibration_id=None,
                      source_sensor_instance_id=None):
    """Inverse only as tagged candidate, never approved ppm.

    The user must supply a physical R0 calibration identifier and a physical
    sensor instance; these are metadata gates, not proofs of gas concentration.
    """
    ratio=_positive(observed_rs_over_r0,"observed_rs_over_r0")
    if not isinstance(r0_calibration_id,str) or not r0_calibration_id.strip():
        raise ValueError("R0 calibration identity required for ratio interpretation")
    if not isinstance(source_sensor_instance_id,str) or not source_sensor_instance_id.strip():
        raise ValueError("physical sensor instance identity required")
    if (curve.physical_sensor_instance_id and
            curve.physical_sensor_instance_id!=source_sensor_instance_id):
        raise ValueError("reference curve belongs to another physical sensor instance")
    internal=exp((log(ratio)-log(curve.curve_a))/curve.curve_b)
    if not isfinite(internal) or internal<=0:
        raise ValueError("unbounded model inverse")
    if not curve.min_internal<=internal<=curve.max_internal:
        raise ValueError("source curve cannot extrapolate concentration")
    ppm=internal*curve.ppm_per_internal_unit
    if not isfinite(ppm):
        raise ValueError("unbounded source concentration")
    return {"candidate_nominal_ppm":ppm,"assumed_gas":curve.gas_label,
            "sensor_instance_id":source_sensor_instance_id,
            "r0_calibration_id":r0_calibration_id,
            "source_id":curve.source_id,
            "evidence_kind":curve.model_kind,
            "reference_definition":curve.reference_definition,
            "device_ppm_approved":False,
            "mixture_components_quantified":False,
            "tank_G3_validated":False}
