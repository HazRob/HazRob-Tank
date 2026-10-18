"""Research-only conservative routing of gas identity to gas-specific calibration."""
from math import isfinite

def _prob(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and 0<=v<=1

def estimate_conditional(identity_probabilities, calibrators, observation, *,
                         min_identity_probability=0.8, min_margin=0.15):
    """Calibrators map gas label to a callable returning {'ppm': float|None,'status':str}.
    Probability values must be independently calibrated; no claim of calibration here.
    """
    if not _prob(min_identity_probability) or not _prob(min_margin):
        raise ValueError("invalid thresholds")
    if not isinstance(identity_probabilities,dict) or not identity_probabilities:
        raise ValueError("nonempty probability dictionary required")
    if not isinstance(calibrators,dict):
        raise ValueError("calibrators must be a dictionary")
    if any(not isinstance(k,str) or not k or not _prob(v)
           for k,v in identity_probabilities.items()):
        raise ValueError("invalid identity labels or probabilities")
    total=sum(identity_probabilities.values())
    if abs(total-1.0)>1e-6:
        raise ValueError("probabilities must sum to one")
    ordered=sorted(identity_probabilities.items(),key=lambda p:(-p[1],p[0]))
    gas,confidence=ordered[0]
    runner_up=ordered[1][1] if len(ordered)>1 else 0.
    margin=confidence-runner_up
    result={"gas_label":gas,"identity_probability":confidence,
            "identity_margin":margin,"ppm":None,"status":"ABSTAIN"}
    if confidence<min_identity_probability:
        result["reason"]="LOW_IDENTITY_CONFIDENCE";return result
    if margin<min_margin:
        result["reason"]="AMBIGUOUS_IDENTITY";return result
    if gas not in calibrators or not callable(calibrators[gas]):
        result["reason"]="MISSING_GAS_CALIBRATOR";return result
    try:
        estimate=calibrators[gas](observation)
    except (ValueError,ArithmeticError,OverflowError,TypeError):
        result["reason"]="CALIBRATOR_INPUT_REJECTED";return result
    if not isinstance(estimate,dict):
        result["reason"]="INVALID_CALIBRATOR_RESULT";return result
    ppm=estimate.get("ppm")
    status=estimate.get("status")
    if not isinstance(status,str) or status not in ("WITHIN_CALIBRATED_RANGE",
        "WITHIN_CALIBRATED_CONCENTRATION_RANGE","WITHIN_TRAINING_CONCENTRATION_RANGE"):
        result["reason"]="CALIBRATOR_ABSTAINED";return result
    if not isinstance(ppm,(int,float)) or isinstance(ppm,bool) or not isfinite(ppm) or ppm<=0:
        result["reason"]="INVALID_CONCENTRATION";return result
    result.update({"ppm":float(ppm),"status":"CONDITIONAL_ESTIMATE",
                   "reason":"KNOWN_GAS_CALIBRATOR_APPLIED",
                   "warning":"Conditional on identity; no calibrated end-to-end correctness guarantee."})
    return result
