"""04.9 forward MQ-QSPR research surrogate from labelled gas exposures.

Molecular descriptor values must come from a traceable source such as PubChem;
they are NOT measured MQ sensor responses. Input record fields:
exposure_id, molecule_id, ppm, descriptors, ratios (sensor -> Rs/R0).
"""
from math import log10,isfinite
from source_forward_ridge_v2 import fit_ridge,number

def fit_source_qspr(records, sensor_ids, descriptor_names, *, source_id, heater_phase):
    r=list(records);ids=tuple(sensor_ids);features=tuple(descriptor_names)
    if not source_id or heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("source/heater protocol required")
    if not ids or len(set(ids))!=len(ids) or not features or len(set(features))!=len(features):
        raise ValueError("unique sensor and molecular descriptors required")
    if len(r)<len(features)+5:
        raise ValueError("insufficient labelled source exposures")
    if any(not isinstance(v,str) or not v for v in ids+features):
        raise ValueError("malformed channel/descriptor names")
    seen=set();rows=[];targets={s:[] for s in ids};ppm_values=[];desc_values={d:[] for d in features};molecules=set()
    for record in r:
        if record.get("source_id")!=source_id or record.get("heater_phase")!=heater_phase:
            raise ValueError("mixed source or heater protocols")
        exposure=record.get("exposure_id"); molecule=record.get("molecule_id")
        if not isinstance(exposure,str) or not exposure or not isinstance(molecule,str) or not molecule:
            raise ValueError("traceable exposure and molecule IDs required")
        if exposure in seen:
            raise ValueError("duplicate exposure IDs: must aggregate time rows first")
        seen.add(exposure);molecules.add(molecule)
        ppm=number(record.get("concentration_ppm"))
        if ppm<=0:raise ValueError("ppm must be independently known and positive")
        descriptors=record.get("descriptors",{})
        if set(descriptors)!=set(features):
            raise ValueError("molecular property vector changed")
        vec=[number(descriptors[name]) for name in features]
        if set(record.get("ratios",{}))!=set(ids):
            raise ValueError("sensor array incomplete or changed")
        for sensor in ids:
            ratio=number(record["ratios"][sensor])
            if ratio<=0:raise ValueError("Rs/R0 must be positive")
            targets[sensor].append(log10(ratio))
        ppm_values.append(ppm)
        for name,value in zip(features,vec):desc_values[name].append(value)
        rows.append([1.,log10(ppm)]+vec)
    if len(molecules)<3 or len(set(ppm_values))<2:
        raise ValueError("at least 3 source molecules and 2 gas ppm levels")
    # Standardization uses TRAINING source records only.
    cols=list(zip(*(row[1:] for row in rows)))
    mean=[sum(c)/len(c) for c in cols]
    std=[(sum((v-m)**2 for v in col)/len(col))**.5 or 1.
         for col,m in zip(cols,mean)]
    matrix=[[1.]+[(v-m)/s for v,m,s in zip(row[1:],mean,std)] for row in rows]
    weights={sensor:fit_ridge(matrix,targets[sensor]) for sensor in ids}
    return {"source_id":source_id,"heater_phase":heater_phase,"sensor_ids":ids,
            "descriptor_names":features,"normalization_mean":mean,"normalization_scale":std,
            "weights":weights,"concentration_range_ppm":(min(ppm_values),max(ppm_values)),
            "descriptor_ranges":{k:(min(v),max(v)) for k,v in desc_values.items()},
            "training_exposure_ids":tuple(sorted(seen)),"training_molecules":tuple(sorted(molecules)),
            "status":"FITTED_SOURCE_FORWARD_QSPR_UNCALIBRATED",
            "G3_Tank":False}
