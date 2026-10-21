"""05.x: source-conditioned ppm regression experiments; no certified gas result.

Training and heldout samples must refer to independently labelled, physically
traceable ppm exposures. SHA format in metadata is NOT a source-byte audit.
"""
import importlib.util
from math import isfinite
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
METHODS={
"05.1":("single_sensor_calibration_v1.py","fit_single_sensor","estimate_known_gas_ppm"),
"05.2":("multipoint_calibration_v1.py","fit_multipoint","estimate_multipoint_ppm"),
"05.3":("nonlinear_regression_v1.py","fit_nonlinear","estimate_ppm"),
"05.4":("multisensor_regression_v1.py","fit_multisensor","estimate_multisensor"),
"05.5":("pls_regression_v1.py","fit_pls","estimate_pls"),
"05.6":("tree_regression_v1.py","fit_tree_ensemble","estimate_tree_ensemble")
}

def _check(meta, rows):
    if not isinstance(meta,dict) or not isinstance(rows,(list,tuple)):
        raise ValueError("calibration metadata and rows required")
    keys=("source_id","device_id","gas_label","heater_phase","reference_trace_id","r0_definition_id")
    if any(not isinstance(meta.get(k),str) or not meta[k] for k in keys):
        raise ValueError("traceable gas exposure metadata incomplete")
    if meta["heater_phase"] not in ("LOW","HIGH","CONSTANT"):
        raise ValueError("unknown heater phase")
    ids=tuple(meta.get("sensor_ids",()))
    if not ids or len(set(ids))!=len(ids):
        raise ValueError("unique sensor channel list required")
    r0=meta.get("r0_ohm");rl=meta.get("rl_ohm")
    if not isinstance(r0,dict) or not isinstance(rl,dict) or set(r0)!=set(ids) or set(rl)!=set(ids):
        raise ValueError("individual R0 and RL per sensor required")
    for item in list(r0.values())+list(rl.values()):
        if isinstance(item,bool) or not isinstance(item,(int,float)) or not isfinite(item) or item<=0:
            raise ValueError("positive physical R0/RL values required")
    seen=set();vectors=[];targets=[]
    for row in rows:
        if row.get("source_id")!=meta["source_id"] or row.get("gas_label")!=meta["gas_label"]:
            raise ValueError("source or known gas identity changed")
        id=row.get("exposure_id")
        if not isinstance(id,str) or not id or id in seen:
            raise ValueError("unique independent exposure IDs required")
        seen.add(id)
        if row.get("reference_trace_id")!=meta["reference_trace_id"]:
            raise ValueError("independent gas concentration traceability changed")
        ppm=row.get("reference_ppm")
        if isinstance(ppm,bool) or not isinstance(ppm,(int,float)) or not isfinite(ppm) or ppm<=0:
            raise ValueError("known gas-phase ppm required")
        rs=row.get("rs_ohm")
        if not isinstance(rs,dict) or set(rs)!=set(ids):
            raise ValueError("unaligned sensor Rs")
        if any(isinstance(rs[s],bool) or not isinstance(rs[s],(int,float)) or not isfinite(rs[s]) or rs[s]<=0 for s in ids):
            raise ValueError("invalid observed sensor Rs")
        vectors.append(tuple(rs[s] for s in ids));targets.append(ppm)
    return ids,vectors,targets,seen

def source_grouped_regression(method, train_records, test_records, *, metadata):
    if method not in METHODS:raise ValueError("unknown conditional estimator method")
    ids,train,ppm,g=_check(metadata,train_records)
    ids2,test,true,h=_check(metadata,test_records)
    if ids!=ids2 or g&h or len(g)<6 or len(h)<2 or len(set(ppm))<4:
        raise ValueError("train/test exposure independence and four gas-phase levels required")
    filename,fit_name,pred_name=METHODS[method]
    p=ROOT/"techniques"/method/filename
    spec=importlib.util.spec_from_file_location("tank_source_ppm_"+method.replace(".","_"),p)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    if method in ("05.1","05.2","05.3"):
        sensor=ids[0]
        model=getattr(m,fit_name)([(v[0],x) for v,x in zip(ppm,train)],
                         gas_label=metadata["gas_label"],sensor_id=sensor,r0_ohms=metadata["r0_ohm"][sensor])
        predicted=[getattr(m,pred_name)(v[0],model) for v in test]
    else:
        ratio=[[v[j]/metadata["r0_ohm"][ids[j]] for j in range(len(ids))] for v in train]
        model=getattr(m,fit_name)(ratio,ppm,sensor_ids=ids,gas_label=metadata["gas_label"])
        query=[[v[j]/metadata["r0_ohm"][ids[j]] for j in range(len(ids))] for v in test]
        predicted=getattr(m,pred_name)(query,model)
    covered=[(p["ppm"],actual) for p,actual in zip(predicted,true) if p.get("ppm") is not None]
    return {"method":method,"source_id":metadata["source_id"],
            "gas_identity_assumed":metadata["gas_label"],"sensor_ids":ids,
            "train_independent_exposure_ids":len(g),"test_independent_exposure_ids":len(h),
            "test_coverage":len(covered)/len(true),
            "source_heldout_mae_ppm":sum(abs(pred-actual) for pred,actual in covered)/len(covered) if covered else None,
            "physical_reference_claim_not_audited":True,
            "Tank_G3":False,"production_ppm_qualified":False,
            "warning":"Conditional source regression only; do not use as occupational exposure reading"}
