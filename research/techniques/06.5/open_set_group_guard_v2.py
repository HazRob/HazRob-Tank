"""06.5 — source-specific open set known/unknown class split guard."""
from open_set_recognition_v1 import fit_open_set, evaluate_open_set

def evaluate_heldout_unknowns(train, known_test, unknown_test, *,
                              source_id, sensor_ids):
    if not isinstance(source_id,str) or not source_id or not sensor_ids or len(set(sensor_ids))!=len(sensor_ids):
        raise ValueError("source/channel identity required")
    samples=[list(train),list(known_test),list(unknown_test)]
    if any(not s for s in samples):
        raise ValueError("train, known and intentionally withheld unknown required")
    group_sets=[];labels=[]
    for section in samples:
        groups=set(); labs=set()
        for row in section:
            if not isinstance(row,dict) or row.get("source_id")!=source_id:
                raise ValueError("source mismatch")
            if not isinstance(row.get("exposure_id"),str) or not row["exposure_id"]:
                raise ValueError("independent exposure id required")
            if row["exposure_id"] in groups:
                raise ValueError("one feature vector per independent exposure")
            groups.add(row["exposure_id"])
            if row.get("scope")!="PURE_GAS" or not isinstance(row.get("gas_label"),str) or not row["gas_label"]:
                raise ValueError("unknown product class is not pure gas")
            labs.add(row["gas_label"])
            if len(row.get("features",()))!=len(sensor_ids):
                raise ValueError("sensor feature width changed")
        group_sets.append(groups);labels.append(labs)
    if any(group_sets[i]&group_sets[j] for i,j in [(0,1),(0,2),(1,2)]):
        raise ValueError("train/known/unknown exposure leakage")
    if not labels[1].issubset(labels[0]) or labels[0]&labels[2]:
        raise ValueError("holdout unknown molecules must not occur in training")
    m=fit_open_set([(r["gas_label"],r["features"]) for r in samples[0]])
    out=evaluate_open_set(m,[r["features"] for r in samples[1]],
                          [r["features"] for r in samples[2]])
    out.update({"source_id":source_id,"known_training_gases":tuple(sorted(labels[0])),
                "unknown_test_gases":tuple(sorted(labels[2])),
                "model_status":"TRAINING_THRESHOLD_OPTIMISTIC_BASELINE",
                "G2_real_source_verified":False,"Tank_G3":False})
    return out
