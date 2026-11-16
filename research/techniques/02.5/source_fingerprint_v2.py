"""02.5: source-faithful MQ fingerprints: pure gas is not food, wood, smoke or perfume."""
from math import isfinite,log
KINDS={"KNOWN_PURE_GAS","KNOWN_MIXTURE","COMPLEX_PRODUCT","OPERATIONAL_CLASS","UNKNOWN"}
def fingerprint(*,sensor_ids, baseline_ohm, exposure_ohm, source_id, exposure_id,
                identity_kind, label, verified_gas_ppm=None, ppm_reference_id=None):
    ids=tuple(sensor_ids);b=tuple(baseline_ohm);e=tuple(exposure_ohm)
    if not ids or len(ids)!=len(set(ids)) or len(ids)!=len(b) or len(ids)!=len(e):
        raise ValueError("physical sensor vector shape/order invalid")
    if any(not isinstance(s,str) or not s.strip() for s in ids):
        raise ValueError("missing sensor identity")
    if identity_kind not in KINDS or not all(isinstance(s,str) and s.strip() for s in (source_id,exposure_id,label)):
        raise ValueError("source/exposure/type provenance required")
    for v in b+e:
        if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0:
            raise ValueError("Rs resistance must be positive finite ohms")
    if verified_gas_ppm is not None:
        if identity_kind!="KNOWN_PURE_GAS" or not isinstance(ppm_reference_id,str) or not ppm_reference_id.strip():
            raise ValueError("single-component ppm cannot be attached to a mixture/product/operational class")
        if isinstance(verified_gas_ppm,bool) or not isinstance(verified_gas_ppm,(int,float)) or not isfinite(verified_gas_ppm) or verified_gas_ppm<=0:
            raise ValueError("bad reference ppm")
    return {"sensor_ids":ids,"log_rs_fold":tuple(log(x/y) for x,y in zip(e,b)),
            "source_id":source_id,"exposure_id":exposure_id,"identity_kind":identity_kind,
            "label":label,"reference_ppm":verified_gas_ppm,"ppm_reference_id":ppm_reference_id,
            "device_ppm_approved":False}
def unique_exposure_count(records):
    rows=list(records);keys={(x["source_id"],x["exposure_id"]) for x in rows}
    return {"row_count":len(rows),"distinct_exposure_count":len(keys),
            "warning":"rows and sessions must not be treated as independent gas exposures"}
