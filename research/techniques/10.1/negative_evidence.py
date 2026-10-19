"""Research-only non-detection likelihoods; not an operational alarm."""
from math import log, exp, isfinite

def update(priors, detections, capabilities, quality=None, floor=1e-9):
    """Update gas priors using externally calibrated P(detection|gas,sensor).

    Capabilities must be specific to concentration, observation time, environment,
    sensor health and calibration; unavailable evidence is ignored.
    """
    if not isinstance(priors,dict) or not priors or any(not isinstance(k,str) or not k or not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for k,v in priors.items()) or sum(priors.values())<=0:
        raise ValueError("invalid priors")
    if not isinstance(detections,dict) or any(not isinstance(k,str) or not k or (v is not None and not isinstance(v,bool)) for k,v in detections.items()):
        raise ValueError("detection states must be bool or None per named sensor")
    if not isinstance(capabilities,dict) or (quality is not None and (not isinstance(quality,dict) or any(not isinstance(v,bool) for v in quality.values()))):
        raise ValueError("invalid capability or sensor-quality mapping")
    if not isinstance(floor,(int,float)) or isinstance(floor,bool) or not isfinite(floor) or not 0<floor<.5:
        raise ValueError("invalid floor")
    scores={}
    for gas, prior in priors.items():
        ll=log(max(prior,floor))
        for sensor, detected in detections.items():
            if detected is None or (quality is not None and not quality.get(sensor,False)):
                continue
            gas_capabilities=capabilities.get(gas,{})
            if not isinstance(gas_capabilities,dict):
                raise ValueError("invalid per-gas detection capabilities")
            power=gas_capabilities.get(sensor)
            if power is None:
                continue
            if not isinstance(power,(int,float)) or isinstance(power,bool) or not isfinite(power) or not 0 <= power <= 1:
                raise ValueError("invalid detection probability")
            p=min(1-floor,max(floor,power))
            ll+=log(p if detected else 1-p)
        scores[gas]=ll
    mx=max(scores.values())
    weights={k:exp(v-mx) for k,v in scores.items()}
    total=sum(weights.values())
    return {k:v/total for k,v in weights.items()}
