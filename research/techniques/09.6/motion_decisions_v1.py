"""Research-only conservative decision gate; returns intent, NEVER motor commands."""
from math import isfinite
def recommend(*, obstacle_cm,cliff_detected,gas_alarm,measurement_valid,communication_ok,localization_valid=True, minimum_clearance_cm=15):
    if not isinstance(obstacle_cm,(int,float)) or isinstance(obstacle_cm,bool) or not isfinite(obstacle_cm) or obstacle_cm<0 or not isinstance(minimum_clearance_cm,(int,float)) or not isfinite(minimum_clearance_cm) or minimum_clearance_cm<=0:raise ValueError("finite distances required")
    flags=[cliff_detected,gas_alarm,measurement_valid,communication_ok,localization_valid]
    if any(not isinstance(v,bool) for v in flags):raise ValueError("boolean status required")
    reasons=[]
    if cliff_detected:reasons.append("CLIFF")
    if obstacle_cm<minimum_clearance_cm:reasons.append("OBSTACLE")
    if gas_alarm:reasons.append("GAS_ALARM_REVIEW")
    if not measurement_valid:reasons.append("INVALID_SENSOR_DATA")
    if not communication_ok:reasons.append("COMMUNICATION_LOSS")
    if not localization_valid:reasons.append("LOCALIZATION_UNRELIABLE")
    return {"intent":"HOLD_AND_REQUEST_OPERATOR" if reasons else "ELIGIBLE_FOR_OPERATOR_REVIEW",
            "reasons":reasons,"motor_command":None,
            "warning":"No autonomous motion authorization; this research gate is not a certified safety controller."}
