"""Additional reproducible UCI1081 diagnostics for 02.4/02.7/03.1/03.2/03.5.

Input is the unaltered CC-BY-4.0 UCI1081 archive, verified by SHA256.
This is external-sensor domain evidence, not a Tank 8-MQ experimental validation.
"""
import argparse
import collections
import json
import statistics
from pathlib import Path
from uci1081_actual_modules_v1 import parse_archive,features,splits,repo_module,SENSORS,GASES

def run(path):
    rows=parse_archive(path)
    time_mod=repo_module("02.4","temporal_features_v1.py")
    select_mod=repo_module("02.7","feature_selection_v1.py")
    pca_mod=repo_module("03.1","pca_v1.py")
    similarity_mod=repo_module("03.2","similarity_v1.py")
    sensor_mod=repo_module("03.5","sensor_importance_v1.py")
    scaler_mod=repo_module("02.2","scaling_v1.py")
    temporal_counts=collections.Counter()
    rise_times=[]
    # This tests numeric execution of transient features on 900 true
    # gas×channel exposure signals. Does NOT verify chemical transient physics.
    for r in rows:
        for sensor in r["trace"]:
            baseline=statistics.median(sensor[180:300])
            try:
                d=time_mod.temporal_features(list(range(600)),sensor[300:],baseline)
                temporal_counts["pass"]+=1
                if d["rise_10_90_s"] is not None:
                    rise_times.append(d["rise_10_90_s"])
                else:
                    temporal_counts["rise_undefined"]+=1
                if d["recovery_t50_s"] is None:
                    temporal_counts["recovery_undefined"]+=1
            except ValueError:
                temporal_counts["not_supported"]+=1
    feature_matrix=[features(r["trace"],"early_late_delta20") for r in rows]
    late_matrix=[features(r["trace"],"late10") for r in rows]
    feature_ids=[f"{phase}_{sensor}" for phase in ("early_delta","late_delta") for sensor in SENSORS]
    y=[r["gas"] for r in rows]
    selection=[]
    importance=[]
    pca=[]
    similarity=[]
    for fold in splits(rows,"leave_one_concentration_ppb"):
        ti,vi=fold["train"],fold["test"]
        xtr=[feature_matrix[i] for i in ti];xte=[feature_matrix[i] for i in vi]
        ranked=select_mod.rank_features(xtr,[y[i] for i in ti],feature_ids)
        selected=select_mod.select_indices(ranked,5)
        assert len(selected)==5 and all(0<=k<20 for k in selected)
        selection.append({"heldout_ppb":fold["heldout"],"top5_features":[r["feature"] for r in ranked[:5]],
                          "training_n":len(ti)})
        ir=sensor_mod.rank_sensor_importance([late_matrix[i] for i in ti],
                                              [y[i] for i in ti],SENSORS)
        importance.append({"heldout_ppb":fold["heldout"],"top5_sensors":[z["sensor_id"] for z in ir[:5]]})
        scaler=scaler_mod.fit_scaler(xtr,method="standard")
        scaled_tr=scaler_mod.transform(xtr,scaler)
        scaled_te=scaler_mod.transform(xte,scaler)
        model=pca_mod.fit_pca(scaled_tr,n_components=2)
        projected=pca_mod.transform_pca(scaled_te,model)
        assert len(projected)==len(vi) and all(len(row)==2 for row in projected)
        pca.append({"heldout_ppb":fold["heldout"],
                    "train_explained_variance_ratio_2pc":model["explained_variance_ratio"],
                    "n_train":len(ti),"n_heldout_projected":len(vi)})
        scales=[1.0]*20
        nn_correct=0
        own_closer=0
        margins=[]
        for idx,q in zip(vi,scaled_te):
            nearest=similarity_mod.nearest_reference(q,scaled_tr,scales=scales)
            if y[ti[nearest["index"]]]==y[idx]:
                nn_correct+=1
            # Uses true held-out labels only for *evaluation*, not any model fit.
            own=min(similarity_mod.compare(q,train,scales=scales)["scaled_euclidean_distance"]
                    for i,train in enumerate(scaled_tr) if y[ti[i]]==y[idx])
            other=min(similarity_mod.compare(q,train,scales=scales)["scaled_euclidean_distance"]
                    for i,train in enumerate(scaled_tr) if y[ti[i]]!=y[idx])
            own_closer+=int(own<other)
            margins.append(other-own)
        similarity.append({"heldout_ppb":fold["heldout"],"correct_nearest_reference":nn_correct,
                           "n_heldout":len(vi),"true_class_nearer_than_other_n":own_closer,
                           "median_distance_margin":statistics.median(margins)})
    return {
        "study":"UCI1081 reference array numerical diagnostics; not Tank calibration",
        "source":"UCI1081 DOI 10.24432/C5CK6F, CC BY 4.0",
        "techniques_exercised":["02.2","02.4","02.7","03.1","03.2","03.5"],
        "temporal_02_4":{"sensor_exposure_windows":900,"counts":dict(temporal_counts),
                         "n_valid_rise_times":len(rise_times),
                         "median_rise_10_90_s":statistics.median(rise_times) if rise_times else None},
        "train_only_selection_02_7":selection,
        "train_only_sensor_importance_03_5":importance,
        "train_only_pca_03_1":pca,
        "heldout_similarity_03_2":similarity,
        "limitations":[
           "Temporal t0=300s post-label injection onset assumes UCI schedule, not independently time-verified.",
           "All trial signals use external sensor array; only MQ135 sensor-family overlap.",
           "Top features are descriptive only and may reflect concentration, instrument batch or drift.",
           "PCA is a dimensionality-reduction audit, not identification accuracy.",
           "Holdout of concentration level still not an independent device/session evaluation.",
           "Do not interpret any of these numbers as chemical specificity, occupational exposure or Tank safety."
        ]
    }
if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("source_zip")
    p.add_argument("--output",default="uci1081_diagnostics.json")
    a=p.parse_args()
    d=run(a.source_zip)
    Path(a.output).write_text(json.dumps(d,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    print("diagnostics UCI1081")
    print("temporal:",json.dumps(d["temporal_02_4"]))
    for key in ("train_only_selection_02_7","train_only_sensor_importance_03_5","train_only_pca_03_1","heldout_similarity_03_2"):
        print(key,":",json.dumps(d[key]))
