"""Research-only fail-closed advisory gate; never emits motor commands."""
from math import isfinite

def advisory(*, obstacle_cm, cliff_detected, communication_ok, pose_fresh,
             sensor_fresh, chemical_evidence_valid, operator_authorized,
             clearance_cm=15.0, plume_confidence=None):
    flags = (cliff_detected, communication_ok, pose_fresh, sensor_fresh,
             chemical_evidence_valid, operator_authorized)
    if any(type(v) is not bool for v in flags):
        raise ValueError('all state flags must be bool')
    if any(type(v) not in (int,float) or not isfinite(v) for v in (obstacle_cm, clearance_cm)):
        raise ValueError('distances must be finite real numbers')
    if obstacle_cm < 0 or clearance_cm <= 0:
        raise ValueError('invalid distance')
    if plume_confidence is not None and (type(plume_confidence) not in (int,float) or not isfinite(plume_confidence) or not 0 <= plume_confidence <= 1):
        raise ValueError('confidence must be a probability')
    reasons=[]
    if cliff_detected: reasons.append('CLIFF')
    if obstacle_cm <= clearance_cm: reasons.append('OBSTACLE_CLEARANCE')
    if not communication_ok: reasons.append('COMMUNICATION_LOSS')
    if not pose_fresh: reasons.append('STALE_POSE')
    if not sensor_fresh: reasons.append('STALE_SENSOR')
    if not chemical_evidence_valid: reasons.append('UNVERIFIED_CHEMICAL_EVIDENCE')
    if not operator_authorized: reasons.append('OPERATOR_NOT_AUTHORIZED')
    return {'intent': 'HOLD_AND_REQUEST_OPERATOR' if reasons else 'ADVISORY_ONLY',
            'reasons': reasons, 'motor_command': None, 'ppm_approved': False,
            'production_qualified': False, 'plume_confidence_research_only': plume_confidence}
