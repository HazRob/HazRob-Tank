"""Fail-closed signal/R0 provenance gate for measured G2 evidence.

This module validates metadata, not physical calibration. A passing record is
only a candidate for independent review; never a safety approval.
"""
import math
import re

HEX64 = re.compile(r"^[0-9a-f]{64}$")
RAW_UNITS = {"ADC_COUNTS", "VOLT", "OHM", "KOHM", "SOURCE_UNIT_UNVERIFIED"}
DERIVED_UNITS = {"ADC_COUNTS", "VOLT", "OHM", "KOHM", "DIMENSIONLESS", "SOURCE_UNIT_UNVERIFIED"}
R0_MODES = {"NOT_USED", "MEASURED_REFERENCE", "MANUFACTURER_CURVE_PRIOR"}
FORMULAS = {"RAW_SIGNAL", "MEDIAN_RESPONSE_MINUS_MEDIAN_BASELINE", "RS_OVER_R0"}

def _finite_positive(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value) and value > 0

def _window(value):
    return (isinstance(value, (list, tuple)) and len(value) == 2 and
            all(isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v) for v in value) and
            0 <= value[0] < value[1])

def audit_signal_lineage(record):
    """Return scientific evidence issues for signal transformations.

    G2 requires known physical source units. R0 is a denominator for Rs/R0,
    not an offset to subtract from ADC, voltage, or resistance.
    """
    problems = []
    p = record.get("signal_processing")
    if not isinstance(p, dict):
        return ["MISSING_SIGNAL_PROCESSING_PROVENANCE"]
    source_units = p.get("source_signal_units")
    output_units = p.get("derived_signal_units")
    formula = p.get("feature_formula")
    mode = p.get("r0_mode")
    if not isinstance(source_units, str) or source_units not in RAW_UNITS:
        problems.append("INVALID_SOURCE_SIGNAL_UNITS")
    elif source_units == "SOURCE_UNIT_UNVERIFIED":
        problems.append("SOURCE_SIGNAL_UNITS_UNVERIFIED_FOR_G2")
    if not isinstance(output_units, str) or output_units not in DERIVED_UNITS:
        problems.append("INVALID_DERIVED_SIGNAL_UNITS")
    if not isinstance(formula, str) or formula not in FORMULAS:
        problems.append("UNSUPPORTED_OR_UNDOCUMENTED_FEATURE_FORMULA")
    if not isinstance(mode, str) or mode not in R0_MODES:
        problems.append("R0_MODE_UNDOCUMENTED")
    ids = p.get("sensor_ids")
    if (not isinstance(ids, list) or not ids or
        any(not isinstance(s, str) or not s.strip() for s in ids) or len(set(ids)) != len(ids)):
        problems.append("INVALID_SENSOR_IDENTITY_MAP")
    if p.get("r0_subtracted_from_reading") is True or p.get("r0_subtracted_from_adc") is True:
        problems.append("INVALID_R0_SUBTRACTION_AS_MQ_NORMALIZATION")
    if formula == "MEDIAN_RESPONSE_MINUS_MEDIAN_BASELINE":
        if mode != "NOT_USED":
            problems.append("BASELINE_DELTA_MISLABELLED_AS_R0_NORMALIZATION")
        if not _window(p.get("baseline_window_s")) or not _window(p.get("response_window_s")):
            problems.append("MISSING_OR_INVALID_BASELINE_RESPONSE_WINDOWS")
        elif p["baseline_window_s"][1] > p["response_window_s"][0]:
            problems.append("BASELINE_RESPONSE_WINDOWS_OVERLAP")
        if source_units != output_units:
            problems.append("BASELINE_SUBTRACTION_MUST_PRESERVE_UNITS")
    if formula == "RAW_SIGNAL":
        if mode != "NOT_USED":
            problems.append("RAW_SIGNAL_CANNOT_CLAIM_R0_NORMALIZATION")
        if source_units != output_units:
            problems.append("RAW_SIGNAL_UNITS_CHANGED_WITHOUT_CONVERSION")
    if formula == "RS_OVER_R0":
        if mode == "NOT_USED":
            problems.append("RS_OVER_R0_REQUIRES_R0")
        if source_units not in {"OHM", "KOHM"} or output_units != "DIMENSIONLESS":
            problems.append("RS_OVER_R0_REQUIRES_RESISTANCE_INPUT_AND_DIMENSIONLESS_OUTPUT")
        if mode == "MANUFACTURER_CURVE_PRIOR":
            problems.append("MANUFACTURER_R0_PRIOR_NOT_PHYSICAL_G2_CALIBRATION")
        ref = p.get("r0_reference")
        if not isinstance(ref, dict):
            problems.append("MISSING_SENSOR_BOUND_R0_REFERENCE")
        else:
            if not _finite_positive(ref.get("r0_ohm")):
                problems.append("INVALID_R0_OHMS")
            if ref.get("sensor_instance_id") not in (ids if isinstance(ids, list) else []):
                problems.append("R0_SENSOR_INSTANCE_MISMATCH")
            if ref.get("heater_phase") not in {"HIGH", "LOW", "CONSTANT"}:
                problems.append("R0_HEATER_PHASE_UNDOCUMENTED")
            if not isinstance(ref.get("reference_id"), str) or not ref["reference_id"].strip():
                problems.append("R0_REFERENCE_ID_MISSING")
            if not isinstance(ref.get("raw_sha256"), str) or not HEX64.fullmatch(ref["raw_sha256"]):
                problems.append("R0_SOURCE_SHA256_MISSING_OR_INVALID")
            if ref.get("source_bytes_verified") is not True:
                problems.append("R0_SOURCE_BYTES_NOT_VERIFIED")
            if ref.get("reference_kind") != "MEASURED_GAS_STANDARD":
                problems.append("R0_REFERENCE_NOT_MEASURED_GAS_STANDARD")
            if not _finite_positive(ref.get("reference_concentration_ppm")):
                problems.append("R0_REFERENCE_PPM_NOT_VERIFIED")
            if not isinstance(ref.get("reference_gas"), str) or not ref["reference_gas"].strip():
                problems.append("R0_REFERENCE_GAS_MISSING")
    elif mode in {"MEASURED_REFERENCE", "MANUFACTURER_CURVE_PRIOR"}:
        problems.append("R0_DECLARED_BUT_NOT_USED_IN_FORMULA")
    return problems
