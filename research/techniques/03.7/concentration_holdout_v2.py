"""03.7: design source-specific concentration holdouts without label leakage."""
from math import isfinite
from concentration_confounding_v1 import concentration_report

def concentration_holdout(rows,gas_labels,gas_ppm,exposure_groups,*,sensor_ids,source_id):
    x=[tuple(r) for r in rows];y=list(gas_labels);ppm=list(gas_ppm);g=list(exposure_groups)
    if not source_id or not x or len(x)!=len(y) or len(x)!=len(ppm) or len(x)!=len(g):
        raise ValueError("source records required")
    if any(not isinstance(v,str) or not v for v in g) or len(set(g))<3:
        raise ValueError("real independent exposure identifiers required")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0 for v in ppm):
        raise ValueError("known positive gas phase ppm required")
    if len(set(ppm))<3:
        raise ValueError("at least 3 independent concentration levels")
    descriptive=concentration_report(x,y,ppm,sensor_ids=sensor_ids)
    folds=[]
    for c in sorted(set(ppm)):
        train=[i for i,v in enumerate(ppm) if v!=c]
        test=[i for i,v in enumerate(ppm) if v==c]
        if set(g[i] for i in train)&set(g[i] for i in test):
            raise ValueError("one exposure cannot span train/test")
        if set(y[i] for i in test)-set(y[i] for i in train):
            raise ValueError("unseen gas in heldout fold")
        folds.append({"heldout_ppm":c,"n_train_exposures":len(set(g[i] for i in train)),
                      "n_heldout_exposures":len(set(g[i] for i in test)),
                      "accuracy_computed":False})
    return {"source_id":source_id,"folds":folds,"within_gas":descriptive,
            "gas_ppm_reference_assumed_not_verified":True,
            "Tank_G3":False,"note":"A holdout plan and descriptive concentration association, not gas classification accuracy"}
