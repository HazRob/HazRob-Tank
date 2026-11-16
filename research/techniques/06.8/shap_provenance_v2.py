"""06.8 — source-labelled Shapley explanations, explicitly non-causal."""
from shap_v1 import explain

def explain_source_features(predict,baseline,query, *, source_id,
                            sensor_ids, correlated_sensor_pairs=()):
    sensors=tuple(sensor_ids)
    if not isinstance(source_id,str) or not source_id or not sensors or len(set(sensors))!=len(sensors):
        raise ValueError("source and unique sensor identities required")
    if len(baseline)!=len(sensors) or len(query)!=len(sensors):
        raise ValueError("shap vector/channel mismatch")
    pairs=list(correlated_sensor_pairs)
    if any(not isinstance(p,(tuple,list)) or len(p)!=2 or p[0] not in sensors or p[1] not in sensors or p[0]==p[1] for p in pairs):
        raise ValueError("only named distinct correlated sensor pairs allowed")
    out=explain(predict,baseline,query,max_features=8)
    out.update({"source_id":source_id,"sensor_ids":sensors,
        "correlated_sensors_declared":bool(pairs),"physical_causality_claim":False,
        "Tank_G3":False,
        "warning":"Baseline dependent interventional SHAP; correlated MQ features may create impossible sensor combinations."})
    return out
