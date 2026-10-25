"""10.1 research-only joint non-detection evidence with correlated MQ channels.

One sensor array/session is ONE joint event. Multiplying its channel
likelihoods independently may invent unjustified certainty.
"""
from math import isfinite
import re

_SHA = re.compile(r"[0-9a-f]{64}\Z")


def compare_joint_nondetection(priors, *, source_id, physical_session_id,
                                sensor_array_id, detections, sensor_quality,
                                joint_pattern_likelihood_by_gas=None,
                                joint_reference_source_sha256=None,
                                joint_reference_independently_verified=False):
    """Return conservative research-only gas ranking, never hard exclusions.

    Likelihoods are P(observed *joint pattern*|gas, context), NOT individual
    per-channel powers. `joint_reference_independently_verified` is a caller
    assertion and is not independently checked by this routine.
    """
    if not all(isinstance(x, str) and x.strip() for x in
               (source_id, physical_session_id, sensor_array_id)):
        raise ValueError("physical session, array and source required")
    if not isinstance(priors, dict) or len(priors) < 2 or any(
            not isinstance(g, str) or not g.strip() or
            isinstance(p, bool) or not isinstance(p, (int, float)) or
            not isfinite(p) or p < 0 for g, p in priors.items()):
        raise ValueError("valid nonnegative gas priors required")
    total = sum(priors.values())
    if not isfinite(total) or total <= 0:
        raise ValueError("nonzero finite prior sum required")
    if not isinstance(detections, dict) or not detections or not isinstance(sensor_quality, dict):
        raise ValueError("array measurements and quality required")
    if set(detections) != set(sensor_quality):
        raise ValueError("quality must be specified for all array channels")
    if any(not isinstance(name, str) or not name.strip() or
           (flag is not None and not isinstance(flag, bool)) or
           not isinstance(sensor_quality[name], bool)
           for name,flag in detections.items()):
        raise ValueError("invalid channel state or quality")
    usable = {s: state for s,state in detections.items()
              if sensor_quality[s] and state is not None}
    base = {gas: p/total for gas,p in priors.items()}
    result = {"source_id": source_id, "physical_session_id": physical_session_id,
              "sensor_array_id": sensor_array_id,
              "posterior_research_only": base, "hard_exclusions": [],
              "sensor_channels_count": len(detections),
              "usable_channel_count": len(usable),
              "joint_model_certification_verified_by_function": False,
              "gas_identity_verified": False, "Tank_G3": False}
    if not usable:
        result["status"] = "NO_VALID_MEASUREMENT"
        return result
    if not isinstance(joint_reference_independently_verified, bool):
        raise ValueError("joint reference verification flag must be boolean")
    if not joint_reference_independently_verified:
        result["status"] = "UNVERIFIED_JOINT_LIKELIHOOD_NO_UPDATE"
        return result
    if (not isinstance(joint_reference_source_sha256, str) or
        not _SHA.fullmatch(joint_reference_source_sha256)):
        raise ValueError("joint reference needs SHA256 format")
    if (not isinstance(joint_pattern_likelihood_by_gas, dict) or
        set(joint_pattern_likelihood_by_gas) != set(priors)):
        raise ValueError("joint model must cover exactly the candidate gases")
    likelihood = joint_pattern_likelihood_by_gas
    if any(isinstance(x, bool) or not isinstance(x, (int,float)) or not isfinite(x)
           or not 0 < x <= 1 for x in likelihood.values()):
        raise ValueError("only strictly positive joint pattern probabilities")
    products = {g: base[g]*likelihood[g] for g in priors}
    denom = sum(products.values())
    if denom == 0 or not isfinite(denom):
        raise ValueError("joint evidence underflow / invalid")
    result["posterior_research_only"] = {g: x/denom for g,x in products.items()}
    result["status"] = "SOURCE_CLAIMED_CONDITIONAL_JOINT_MODEL_RESEARCH_ONLY"
    result["joint_reference_source_sha256_claimed"] = joint_reference_source_sha256
    return result
