"""06.3 — temperature scaling with independent exposure calibration groups."""
from probability_calibration_v1 import fit_temperature, apply_temperature, expected_calibration_error

def calibrate_grouped(logits, labels, groups, training_groups, *,
                      source_id, sensor_ids, model_id):
    rows=list(logits); y=list(labels); g=list(groups); training=set(training_groups)
    sensors=tuple(sensor_ids)
    if not all(isinstance(v,str) and v for v in (source_id,model_id)) or not sensors or len(set(sensors))!=len(sensors):
        raise ValueError("source, model and fixed sensor identities required")
    if len(rows)<6 or len(rows)!=len(y) or len(rows)!=len(g):
        raise ValueError(">=6 aligned grouped calibration exposures required")
    if any(not isinstance(v,str) or not v.strip() for v in g) or len(set(g))!=len(g):
        raise ValueError("one independent calibration exposure per feature row")
    if set(g)&training:
        raise ValueError("training/calibration leakage")
    calibrated=fit_temperature(rows,y)
    probabilities=[apply_temperature(row,calibrated) for row in rows]
    score=expected_calibration_error(probabilities,y)
    return {"method":"GROUP_DISJOINT_TEMPERATURE_SCALING","source_id":source_id,
       "model_id":model_id,"sensor_ids":sensors,"n_calibration_exposures":len(g),
       "calibration_groups":tuple(sorted(g)),"temperature":calibrated["temperature"],
       "calibration_ece_descriptive":score["ece"],"status":"RESEARCH_CALIBRATION_ONLY",
       "Tank_G3":False,"independent_final_test_not_run":True}
