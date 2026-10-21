"""04.2: type-aware masked fingerprint ranking on a single source protocol."""
from math import isfinite,sqrt

_TYPES={"PURE_GAS","KNOWN_MIXTURE","PRODUCT_HEADSPACE","OPERATIONAL_CLASS","UNKNOWN"}
def match_source_fingerprints(observation, references, *, source_id, signal_unit,
                              r0_definition_id, heater_phase, min_channels=2,
                              reject_distance=None):
    if not isinstance(source_id,str) or not source_id or signal_unit not in ("ADC_COUNTS","RS_OHM","RS_R0_RATIO"):
        raise ValueError("source signal and unit required")
    if heater_phase not in ("LOW","HIGH","CONSTANT") or not r0_definition_id:
        raise ValueError("sensor definition and heater phase required")
    if not isinstance(observation,dict) or not observation or not isinstance(references,(list,tuple)):
        raise ValueError("measured channels and frozen references required")
    if any(not isinstance(k,str) or not k or isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v)
           for k,v in observation.items()):
        raise ValueError("nonfinite observation")
    if not isinstance(min_channels,int) or isinstance(min_channels,bool) or min_channels<2:
        raise ValueError("minimum channels invalid")
    if reject_distance is not None and (isinstance(reject_distance,bool) or not isinstance(reject_distance,(int,float)) or not isfinite(reject_distance) or reject_distance<0):
        raise ValueError("invalid predeclared gate")
    choices=[]
    for ref in references:
        if (ref.get("source_id")!=source_id or ref.get("signal_unit")!=signal_unit or
            ref.get("r0_definition_id")!=r0_definition_id or ref.get("heater_phase")!=heater_phase):
            continue
        if ref.get("identity_kind") not in _TYPES or not isinstance(ref.get("label"),str) or not ref["label"]:
            raise ValueError("reference identity kind and label required")
        values=ref.get("sensor_values")
        if not isinstance(values,dict):
            raise ValueError("reference sensor vector required")
        overlap=tuple(sorted(set(observation)&set(values)))
        if len(overlap)<min_channels:continue
        if any(isinstance(values[k],bool) or not isinstance(values[k],(int,float)) or not isfinite(values[k]) for k in overlap):
            raise ValueError("invalid source reference vector")
        dist=sqrt(sum((observation[k]-values[k])**2 for k in overlap)/len(overlap))
        choices.append({"label":ref["label"],"identity_kind":ref["identity_kind"],
                        "distance":dist,"used_sensor_ids":overlap,
                        "chemical_gas_identification":False})
    # Comparable channel sets only, to avoid rewarding references with missing sensors.
    if choices:
        largest=max(len(x["used_sensor_ids"]) for x in choices)
        choices=[x for x in choices if len(x["used_sensor_ids"])==largest]
    choices.sort(key=lambda v:(v["distance"],v["identity_kind"],v["label"]))
    decision="NO_COMPARABLE_REFERENCE" if not choices else "REFERENCE_MATCH_ONLY"
    if choices and reject_distance is not None and choices[0]["distance"]>reject_distance:
        decision="ABSTAIN_DISTANCE"
    return {"decision":decision,"ranked":choices,"Tank_G3":False,
            "note":"Product/mixture classes are not pure gases and signal distance is not calibrated confidence."}
