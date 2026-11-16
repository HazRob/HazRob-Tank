"""05.7: prevent upstream softmax scores becoming ppm without calibrated identity.

Even when provisional evidence metadata is present, output remains research only.
"""
from identity_conditional_v1 import estimate_conditional

def guarded_identity_conditional(raw_identity_scores, calibrators, observation, *,
                                 source_id, probability_calibration_source,
                                 conditional_calibration_source,
                                 identity_scope="PURE_GAS"):
    if not source_id or identity_scope!="PURE_GAS":
        return {"status":"ABSTAIN","reason":"UNKNOWN_OR_COMPLEX_IDENTITY_SCOPE","Tank_G3":False}
    if not probability_calibration_source or not conditional_calibration_source:
        return {"status":"ABSTAIN","reason":"MISSING_INDEPENDENT_CALIBRATION_EVIDENCE",
                "Tank_G3":False}
    # Both evidence identifiers are asserted by caller; source bytes and validation
    # must be independently audited before any production use.
    result=estimate_conditional(raw_identity_scores,calibrators,observation)
    return {"status":"RESEARCH_CONDITIONAL_RESULT","source_id":source_id,
            "probability_calibration_claim":probability_calibration_source,
            "gas_calibration_claim":conditional_calibration_source,
            "unqualified_result":result,"Tank_G3":False,"production_ppm_approved":False}
