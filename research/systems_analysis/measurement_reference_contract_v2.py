"""HazRob Tank: research-only source-qualified measurement and analysis contracts.

Batch 01 of the source-driven 80-technique plan. Each public function is a
guarded reference interface for a named research technique, NOT a calibration
or an operational gas-alert implementation. No physical Tank MQ8 trials exist.
All datasets used by unit tests are deliberately synthetic.
"""
from __future__ import annotations
from hashlib import sha256
import math
from statistics import median
from typing import Mapping

HEATER_CYCLED = frozenset(("MQ7", "MQ9"))
TANK_SENSORS = ("MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135")
VALID_REFERENCE = frozenset(("certified_gas", "reference_instrument",
                             "clean_air_ratio", "manufacturer_graph",
                             "external_paper"))
INDEPENDENT_REFERENCE = frozenset(("certified_gas", "reference_instrument"))
LABEL_KINDS = frozenset(("PURE_GAS", "KNOWN_MIXTURE", "COMPLEX_HEADSPACE",
                         "OPERATIONAL_CLASS", "UNLABELLED", "SIMULATED"))
VALID_HEATER_PHASES = frozenset(("HIGH", "LOW", "CONSTANT"))


def _positive(value, name):
    if (not isinstance(value, (int, float)) or isinstance(value, bool)
            or not math.isfinite(value) or value <= 0):
        raise ValueError(name + " must be positive, finite and numeric")
    return float(value)


def _name(value, name):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(name + " must be a nonempty string")
    return value.strip()


def _heater(model, phase, protocol_id):
    _name(model, "sensor model")
    if phase not in VALID_HEATER_PHASES:
        raise ValueError("UNKNOWN heater phase is not admissible")
    _name(protocol_id, "heater protocol ID")
    if model in HEATER_CYCLED and phase not in ("HIGH", "LOW"):
        raise ValueError("MQ7/MQ9 require observed high/low heater phase")
    if model not in HEATER_CYCLED and phase != "CONSTANT":
        raise ValueError("other MQ models must use declared CONSTANT phase")
    return {"sensor_model": model, "heater_phase": phase,
            "protocol_id": protocol_id, "heater_actuation": False}


def adc_to_rs_guarded(*, adc_code, adc_full_scale_code, adc_full_scale_volts,
                       divider_gain, vc_volts, rl_ohms):
    """01.1: Reject rails, distorted divider and ambiguous unit mappings."""
    if (not isinstance(adc_code, int) or isinstance(adc_code, bool)
            or not isinstance(adc_full_scale_code, int)
            or isinstance(adc_full_scale_code, bool)
            or adc_full_scale_code <= 1 or not 0 < adc_code < adc_full_scale_code):
        raise ValueError("non-rail integer ADC code and full scale are required")
    fs = _positive(adc_full_scale_volts, "ADC full-scale voltage")
    gain = _positive(divider_gain, "divider gain")
    vc = _positive(vc_volts, "sensor circuit Vc")
    rl = _positive(rl_ohms, "sensor load RL")
    if gain < 1:
        raise ValueError("divider gain cannot be below unity")
    vrl = (adc_code / adc_full_scale_code) * fs * gain
    if not 0 < vrl < vc:
        raise ValueError("V_RL must lie strictly inside 0..Vc")
    rs = rl * (vc / vrl - 1)
    return {"rs_ohm": rs, "rs_kohm": rs / 1000,
            "vrl_volts": vrl, "rl_ohm": rl,
            "source_measurement": "ADC_WITH_EXPLICIT_CIRCUIT",
            "device_calibrated_ppm": False}


def heater_sample_contract(*, sensor_model, phase, protocol_id,
                            phase_elapsed_s, settle_s):
    """01.4: phase-tag an observation, never synthesize heater waveform."""
    result = _heater(sensor_model, phase, protocol_id)
    elapsed = float(phase_elapsed_s) if isinstance(phase_elapsed_s, (int, float)) else float("nan")
    settle = float(settle_s) if isinstance(settle_s, (int, float)) else float("nan")
    if not math.isfinite(elapsed) or not math.isfinite(settle) or elapsed < 0 or settle < 0:
        raise ValueError("finite nonnegative heater times required")
    return {**result, "eligible_to_sample_phase": elapsed >= settle,
            "phase_elapsed_s": elapsed, "settle_s": settle}


def r0_from_source_window(*, rs_samples_ohm, sensor_model, sensor_instance_id,
                          heater_phase, heater_protocol_id, reference_ratio,
                          reference_kind, calibration_id, reference_id,
                          reference_gas=None, reference_ppm=None,
                          max_relative_mad=0.1):
    """01.2: source-qualified R0 evidence. Never makes a safety calibration."""
    _heater(sensor_model, heater_phase, heater_protocol_id)
    _name(sensor_instance_id, "sensor instance ID")
    _name(calibration_id, "calibration ID")
    _name(reference_id, "source reference ID")
    if reference_kind not in VALID_REFERENCE:
        raise ValueError("unsupported R0 evidence kind")
    if reference_kind in INDEPENDENT_REFERENCE:
        _name(reference_gas, "independently measured reference gas")
        _positive(reference_ppm, "independently measured gas concentration")
    elif reference_kind == "clean_air_ratio":
        if reference_gas not in (None, "CLEAN_AIR"):
            raise ValueError("clean-air ratio cannot imply gas-standard calibration")
        if reference_ppm is not None:
            raise ValueError("ambient-air R0 does not include gas-standard ppm")
    ratio = _positive(reference_ratio, "reference Rs/R0 ratio")
    values = list(rs_samples_ohm)
    if len(values) < 5:
        raise ValueError("at least five measured Rs samples required")
    values = [_positive(x, "Rs sample") for x in values]
    threshold = float(max_relative_mad)
    if not math.isfinite(threshold) or not 0 <= threshold <= 1:
        raise ValueError("invalid relative MAD threshold")
    center = median(values)
    mad = median(abs(x-center) for x in values) / center
    if mad > threshold:
        raise ValueError("unstable R0 measurement window")
    r0 = center / ratio
    return {"sensor_model": sensor_model, "sensor_instance_id": sensor_instance_id,
            "calibration_id": calibration_id, "reference_id": reference_id,
            "reference_kind": reference_kind, "heater_phase": heater_phase,
            "heater_protocol_id": heater_protocol_id, "reference_gas": reference_gas,
            "reference_ppm": reference_ppm, "rs_median_ohm": center,
            "reference_ratio": ratio, "r0_ohm": r0, "r0_kohm": r0/1000,
            "relative_mad": mad, "n": len(values),
            "independent_gas_reference_recorded": reference_kind in INDEPENDENT_REFERENCE,
            "device_qualified_for_safety": False}


def sensitivity_curve_prior(*, source_id, sensor_model, gas_label,
                             a, b, low_ppm, high_ppm, output_scale=1):
    """01.3: source-only manufacturer/peer curve with explicit MQ3 scale."""
    source_id = _name(source_id, "curve source")
    sensor_model = _name(sensor_model, "sensor model")
    gas_label = _name(gas_label, "gas label")
    a = _positive(a, "curve a")
    b = float(b)
    if not math.isfinite(b) or b == 0:
        raise ValueError("nonzero finite curve exponent required")
    low = _positive(low_ppm, "low ppm")
    high = _positive(high_ppm, "high ppm")
    scale = _positive(output_scale, "concentration scaling")
    if not low < high:
        raise ValueError("strict curve ppm range required")
    return {"source_id": source_id, "sensor_model": sensor_model,
            "gas_label": gas_label, "a": a, "b": b,
            "min_ppm": low, "max_ppm": high, "output_scale": scale,
            "curve_evidence": "SOURCE_PRIOR_NOT_DEVICE_CALIBRATION"}


def reference_curve_ratio(curve, concentration_ppm):
    """Bounded forward prior; ratio cannot be an independently measured ppm."""
    c = _positive(concentration_ppm, "curve concentration")
    if not curve["min_ppm"] <= c <= curve["max_ppm"]:
        raise ValueError("never extrapolate manufacturer curve outside ppm range")
    return curve["a"] * (c / curve["output_scale"]) ** curve["b"]


def sampling_window_check(*, timestamps_s, sensor_signal, flushing_observed,
                          airflow_reference_id, minimum_n=5):
    """01.6: missing washout and delayed sampling are explicit defects."""
    t = list(timestamps_s)
    y = list(sensor_signal)
    if len(t) != len(y) or len(t) < minimum_n:
        raise ValueError("window length mismatch or too few points")
    if not all(isinstance(v, (int, float)) and not isinstance(v, bool)
               and math.isfinite(v) for v in t + y):
        raise ValueError("nonfinite time/signal")
    if any(t[i] >= t[i + 1] for i in range(len(t) - 1)):
        raise ValueError("times must be strictly increasing")
    flags = []
    if flushing_observed is not True:
        flags.append("PURGE_NOT_VERIFIED")
    if not airflow_reference_id:
        flags.append("AIRFLOW_NOT_DOCUMENTED")
    return {"n": len(t), "duration_s": t[-1]-t[0], "flags": flags,
            "ready_for_uncontaminated_fingerprint": not flags,
            "gas_identity_assigned": False}


def label_safe_fingerprint(*, source_id, label_kind, sensor_models,
                           baseline_rs_ohm, sample_rs_ohm, label,
                           independent_gas_identity=False):
    """02.5: one or eight sensor fingerprints allowed; label semantics persist."""
    _name(source_id, "source ID")
    _name(label, "class label")
    if label_kind not in LABEL_KINDS:
        raise ValueError("invalid label kind")
    sensors = list(sensor_models)
    baseline = list(baseline_rs_ohm)
    sample = list(sample_rs_ohm)
    if not sensors or len(set(sensors)) != len(sensors):
        raise ValueError("at least one distinct sensor required")
    if len(sensors) != len(baseline) or len(sensors) != len(sample):
        raise ValueError("sensor length mismatch")
    if any(not isinstance(x, str) or not x for x in sensors):
        raise ValueError("malformed sensor identity")
    baseline = [_positive(x, "baseline Rs") for x in baseline]
    sample = [_positive(x, "exposure Rs") for x in sample]
    if independent_gas_identity and label_kind != "PURE_GAS":
        raise ValueError("only actual pure gases may have independent identity labels")
    from math import log
    features = [log(s/b) for s,b in zip(sample, baseline)]
    return {"source_id": source_id, "label": label,
            "label_kind": label_kind, "sensor_models": sensors,
            "features_log_rs_fold": features,
            "can_train_pure_gas_identity": bool(
                independent_gas_identity and label_kind == "PURE_GAS"),
            "real_tank_8mq_validated": False}


def verify_source_bytes(*, payload, expected_sha256, source_id):
    """02.8: verify actual raw bytes, not merely checksum string syntax."""
    _name(source_id, "source ID")
    if not isinstance(payload, bytes) or not payload:
        raise ValueError("raw nonempty bytes required")
    if not isinstance(expected_sha256, str) or len(expected_sha256) != 64:
        raise ValueError("expected SHA256 must contain 64 hex digits")
    try:
        bytes.fromhex(expected_sha256)
    except ValueError as exc:
        raise ValueError("expected SHA256 is not hex") from exc
    actual = sha256(payload).hexdigest()
    if actual != expected_sha256.lower():
        raise ValueError("SOURCE_BYTES_CHECKSUM_MISMATCH")
    return {"source_id": source_id, "sha256": actual,
            "bytes": len(payload), "raw_bytes_verified": True,
            "reference_gas_validated": False}


def holdout_whole_sessions(records, *, holdout_session_ids):
    """04.10: a complete source/device/session must be held out together."""
    rows=list(records)
    hold = set(holdout_session_ids)
    if not rows or not hold:
        raise ValueError("nonempty records and holdout groups required")
    for row in rows:
        if not isinstance(row, Mapping) or any(
            not isinstance(row.get(k), str) or not row[k]
            for k in ("source_id", "device_id", "session_id",
                      "exposure_id", "label")):
            raise ValueError("incomplete source/device/session/label")
    training = [r for r in rows if r["session_id"] not in hold]
    testing = [r for r in rows if r["session_id"] in hold]
    if not training or not testing:
        raise ValueError("train and test must be nonempty")
    train_exposures = {(r["source_id"],r["device_id"],r["exposure_id"])
                       for r in training}
    test_exposures = {(r["source_id"],r["device_id"],r["exposure_id"])
                      for r in testing}
    if train_exposures & test_exposures:
        raise ValueError("leakage: same exposure crosses sessions and holdout")
    return {"train": training, "test": testing, "split_level": "SESSION",
            "leakage_checked": True}


def fit_known_gas_source_curve(points, *, sensor_instance_id, gas_label,
                               calibration_id, reference_id, r0_ohm,
                               heater_phase, heater_protocol_id,
                               sensor_model):
    """05.1: only reference-backed source curve; synthetic tests not physical G3."""
    _name(sensor_instance_id, "sensor instance")
    _name(gas_label, "gas")
    _name(calibration_id, "calibration")
    _name(reference_id, "traceable gas reference")
    _heater(sensor_model, heater_phase, heater_protocol_id)
    r0 = _positive(r0_ohm, "R0")
    rows=list(points)
    if len(rows)<3 or len({x.get("concentration_ppm") for x in rows
                          if isinstance(x,Mapping)})<3:
        raise ValueError("three distinct concentration levels required")
    for row in rows:
        if (not isinstance(row,Mapping) or
                row.get("reference_kind") not in INDEPENDENT_REFERENCE or
                row.get("reference_id") != reference_id or
                row.get("sensor_instance_id") != sensor_instance_id or
                row.get("gas_label") != gas_label):
            raise ValueError("each observation must have matching independent gas reference")
    c=[_positive(x["concentration_ppm"],"ppm") for x in rows]
    rs=[_positive(x["rs_ohm"],"Rs") for x in rows]
    x=[math.log10(v) for v in c]
    y=[math.log10(v/r0) for v in rs]
    xm=sum(x)/len(x); ym=sum(y)/len(y)
    spread=sum((z-xm)**2 for z in x)
    if spread < 1e-16:
        raise ValueError("degenerate reference concentrations")
    slope=sum((u-xm)*(v-ym) for u,v in zip(x,y))/spread
    if abs(slope)<1e-9:
        raise ValueError("flat gas response; concentration not identifiable")
    intercept=ym-slope*xm
    rmse=(sum((v-intercept-slope*u)**2 for u,v in zip(x,y))/len(y))**0.5
    return {"model":"SOURCE_LINKED_LOG_CALIBRATION",
            "sensor_model":sensor_model,"sensor_instance_id":sensor_instance_id,
            "gas_label":gas_label,"calibration_id":calibration_id,
            "reference_id":reference_id,"heater_phase":heater_phase,
            "r0_ohm":r0,"intercept_log10":intercept,"slope_log10":slope,
            "min_ppm":min(c),"max_ppm":max(c),"in_sample_log_rmse":rmse,
            "n_points":len(c),"heldout_performance_verified":False,
            "safety_grade_ppm":False}


def screening_lod_from_unique_blanks(blank_records, *, signal_field="signal",
                                     min_unique_blanks=10):
    """05.8: basic blank sigma screen; explicitly NEVER a certified ppm LOD."""
    rows=list(blank_records)
    if len(rows)<min_unique_blanks or len(rows)<2:
        raise ValueError("insufficient blank exposure trials")
    ids=[]
    values=[]
    for row in rows:
        if not isinstance(row, Mapping) or row.get("label")!="BLANK":
            raise ValueError("blank-labelled exposure required")
        ids.append(_name(row.get("exposure_id"),"blank exposure ID"))
        x=row.get(signal_field)
        if (not isinstance(x,(int,float)) or isinstance(x,bool)
                or not math.isfinite(x)):
            raise ValueError("finite blank signal required")
        values.append(float(x))
    if len(set(ids))!=len(ids):
        raise ValueError("each blank must be a distinct exposure, not a time sample")
    mean=sum(values)/len(values)
    sd=(sum((x-mean)**2 for x in values)/(len(values)-1))**0.5
    if sd<=0:
        raise ValueError("cannot infer LOD from zero-variance blanks")
    return {"blank_mean":mean,"blank_sd":sd,
            "three_sigma_screen":3*sd,"ten_sigma_screen":10*sd,
            "n_independent_blank_records":len(values),
            "lod_ppm":None,"loq_ppm":None,"validated_detection_power":False,
            "status":"SCREENING_ONLY_NO_PPM"}
