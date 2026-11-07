"""06.7 — negative evidence cannot exclude gas without reference detection power."""
from evidence_conflicts_v1 import assess

def source_negative_evidence(candidate,readings,response_matrix, *,
                             source_id,reference_validation,minimum_detection_power=0.95):
    if not isinstance(source_id,str) or not source_id:
        raise ValueError("source required")
    if reference_validation is not True:
        return {"status":"ABSTAIN_NO_REFERENCED_DETECTION_POWER",
                "hard_exclusion":False,"Tank_G3":False}
    result=assess(candidate,readings,response_matrix,
                  minimum_detection_power=minimum_detection_power)
    result["Tank_G3"]=False
    result["hard_exclusion"]=False
    result["source_reference_claim_not_independently_verified"]=True
    return result
