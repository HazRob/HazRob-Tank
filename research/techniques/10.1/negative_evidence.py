"""Research-only non-detection likelihoods; not an operational alarm."""
from math import log, exp, isfinite

def update(priors, detections, capabilities, quality=None, floor=1e-9):
    """Update gas priors using externally calibrated P(detection|gas,sensor).

    Capabilities must be specific to concentration, observation time, environment,
    sensor health and calibration; unavailable evidence is ignored.
    """
    if not priors or any(not isfinite(v) or v < 0 for v in priors.values()) or sum(priors.values()) <= 0:
        raise ValueError("invalid priors")
    if not (0 < floor < .5):
        raise ValueError("invalid floor")
    scores={}
    for gas, prior in priors.items():
        ll=log(max(prior,floor))
        for sensor, detected in detections.items():
            if detected is None or (quality is not None and not quality.get(sensor,False)):
                continue
            power=capabilities.get(gas,{}).get(sensor)
            if power is None:
                continue
            if not isfinite(power) or not 0 <= power <= 1:
                raise ValueError("invalid detection probability")
            p=min(1-floor,max(floor,power))
            ll+=log(p if detected else 1-p)
        scores[gas]=ll
    mx=max(scores.values())
    weights={k:exp(v-mx) for k,v in scores.items()}
    total=sum(weights.values())
    return {k:v/total for k,v in weights.items()}
