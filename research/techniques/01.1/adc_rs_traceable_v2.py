"""MQ ADC-to-Rs research conversion with bounded quantization and explicit circuit identity.

No physical sensor verification, gas identity, ppm inference or actuator control.
Requires actual measured circuit topology: MQ sensing resistor connected to supply,
load resistor RL connected to ground, and V_RL sampled through an attenuation stage.
An ideal linear ADC is assumed only for this calculation. Real ADC nonlinearities
and source impedance require separate characterization.
"""
from dataclasses import dataclass
from math import isfinite


def _positive(x, label):
    if isinstance(x, bool) or not isinstance(x, (int, float)) or not isfinite(x) or x <= 0:
        raise ValueError(label + " must be positive finite")
    return float(x)


@dataclass(frozen=True)
class Circuit:
    sensor_instance_id: str
    circuit_id: str
    rl_ohm: float
    vc_volts: float
    adc_reference_volts: float
    adc_max_code: int
    sensor_to_adc_voltage_gain: float = 1.0
    circuit_topology: str = "RS_SUPPLY_RL_GROUND"

    def __post_init__(self):
        if not isinstance(self.sensor_instance_id, str) or not self.sensor_instance_id.strip():
            raise ValueError("physical sensor_instance_id required")
        if not isinstance(self.circuit_id, str) or not self.circuit_id.strip():
            raise ValueError("traceable circuit_id required")
        if self.circuit_topology != "RS_SUPPLY_RL_GROUND":
            raise ValueError("unsupported voltage divider topology")
        for key in ("rl_ohm", "vc_volts", "adc_reference_volts"):
            _positive(getattr(self, key), key)
        gain = _positive(self.sensor_to_adc_voltage_gain, "sensor_to_adc_voltage_gain")
        if gain < 1:
            raise ValueError("gain cannot describe physical attenuation when below 1")
        if isinstance(self.adc_max_code, bool) or not isinstance(self.adc_max_code, int) or self.adc_max_code < 2:
            raise ValueError("adc_max_code must be integer >=2")


def resistance_from_voltage(vrl_volts, circuit):
    """Return Rs in ohm from measured V_RL, not raw voltage at ESP GPIO."""
    v = _positive(vrl_volts, "vrl_volts")
    if v >= circuit.vc_volts:
        raise ValueError("V_RL must be below measured sensor supply voltage")
    return circuit.rl_ohm * (circuit.vc_volts / v - 1.0)


def evaluate_adc(code, circuit, *, quantization_bounds=True):
    """Assess a raw ADC sample, preserving invalid and saturation states.

    Code 0 and max are unquantifiable; no guess or clamp of Rs is returned.
    ±0.5 ADC LSB ideal-code boundaries offer a quantization-only interval,
    NOT an expanded measurement-uncertainty certificate.
    """
    if isinstance(code, bool) or not isinstance(code, int) or not 0 <= code <= circuit.adc_max_code:
        raise ValueError("ADC sample must be an integer code within configured range")
    common = {"sensor_instance_id": circuit.sensor_instance_id,
              "circuit_id": circuit.circuit_id, "raw_adc": code,
              "circuit_topology": circuit.circuit_topology,
              "rl_ohm": circuit.rl_ohm, "vc_volts": circuit.vc_volts,
              "ppm_qualified": False, "physical_calibration_verified": False}
    if code in (0, circuit.adc_max_code):
        return {**common, "status": "ADC_RAIL_SATURATION", "rs_ohm": None,
                "quantization_only_interval_ohm": None}
    step = circuit.adc_reference_volts * circuit.sensor_to_adc_voltage_gain / circuit.adc_max_code
    v = step * code
    if not 0 < v < circuit.vc_volts:
        return {**common, "status": "OUTSIDE_PHYSICAL_DIVIDER", "vrl_volts": v,
                "rs_ohm": None, "quantization_only_interval_ohm": None}
    rs = resistance_from_voltage(v, circuit)
    interval = None
    status = "IDEAL_ADC_MODEL_ONLY"
    if quantization_bounds:
        lower_v = step * (code - 0.5)
        upper_v = step * (code + 0.5)
        if lower_v <= 0 or upper_v >= circuit.vc_volts:
            return {**common, "status": "VOLTAGE_RAIL_UNCERTAIN", "vrl_volts": v,
                    "rs_ohm": None, "quantization_only_interval_ohm": None}
        interval = [resistance_from_voltage(upper_v, circuit),
                    resistance_from_voltage(lower_v, circuit)]
    return {**common, "status": status, "vrl_volts": v, "rs_ohm": rs,
            "rs_kohm": rs / 1000.0, "quantization_only_interval_ohm": interval}


def evaluate_series(samples, circuit):
    values = list(samples)
    if not values:
        raise ValueError("no ADC observations")
    out = [evaluate_adc(v, circuit) for v in values]
    return {"sensor_instance_id": circuit.sensor_instance_id,
            "n": len(out), "n_ideal_valid": sum(x["rs_ohm"] is not None for x in out),
            "observations": out, "no_gas_reference": True}
