"""06.4 — split conformal research set under disjoint exposure-group assumption."""
from conformal_prediction_v1 import fit_split_conformal, predict_set

def fit_grouped_conformal(calibration_probs, labels, calibration_groups,
                          training_groups, *, source_id, alpha=0.1):
    rows=list(calibration_probs);y=list(labels);g=list(calibration_groups)
    if not isinstance(source_id,str) or not source_id:
        raise ValueError("source provenance required")
    if not rows or len(rows)!=len(y) or len(rows)!=len(g):
        raise ValueError("aligned source calibration rows required")
    if any(not isinstance(v,str) or not v.strip() for v in g) or len(set(g))!=len(g):
        raise ValueError("repeated within-exposure rows are not independent calibration events")
    if set(g)&set(training_groups):
        raise ValueError("training/calibration group contamination")
    model=fit_split_conformal(rows,y,alpha=alpha)
    if model["threshold"]==float("inf"):
        raise ValueError("too few calibration exposures for finite threshold at chosen alpha")
    return {"source_id":source_id,"model":model,"calibration_groups":tuple(sorted(g)),
         "exchangeability_verified":False,"Tank_G3":False,
         "status":"EXCHANGEABILITY_ASSUMED_CONFORMAL_SOURCE_RESEARCH"}

def grouped_conformal_set(fitted,probabilities, *, source_id, test_group_id):
    if source_id!=fitted["source_id"] or not isinstance(test_group_id,str) or not test_group_id:
        raise ValueError("test provenance mismatch")
    if test_group_id in fitted["calibration_groups"]:
        raise ValueError("calibration/test group overlap")
    out=predict_set(probabilities,fitted["model"])
    out["Tank_G3"]=False
    out["source_domain_exchangeability_unverified"]=True
    return out
