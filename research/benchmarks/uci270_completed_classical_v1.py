"""UCI270 actual chronological-drift transfer with real LDA/PLS-DA/SVM/XGBoost.

The source provides 128 precomputed features of a 16-TGS sensor array.
We use one documented normalized response descriptor per source sensor (16).
Each source batch is chronological, not a distinct physical device.
Research-only. Donor/source license discrepancy: do not re-distribute archive.
"""
import argparse
import collections
import json
from pathlib import Path

from sklearn.metrics import accuracy_score,balanced_accuracy_score,confusion_matrix,f1_score
from uci270_source_adapter_v1 import parse_original,select_sensor_features,LABELS,ZIP_SHA256
from uci1081_completed_classical_v1 import METHODS,fit_predict

SCENARIOS=((1,2),(1,2,3,4,5))
TARGET_BATCH=10

def prepare(rows,source_batches,target_batch=TARGET_BATCH):
    if target_batch in source_batches:raise ValueError("target batch appears in training")
    train=[r for r in rows if r["batch"] in source_batches]
    test=[r for r in rows if r["batch"]==target_batch]
    if not train or not test or {r["source_row"] for r in train}&{r["source_row"] for r in test}:
        raise ValueError("invalid chronological source split")
    return train,test

def score(known,pred):
    classes=tuple(sorted(set(LABELS.values())))
    return {
        "correct":sum(a==b for a,b in zip(known,pred)),
        "n_test":len(known),
        "accuracy":float(accuracy_score(known,pred)),
        "balanced_accuracy":float(balanced_accuracy_score(known,pred)),
        "macro_f1":float(f1_score(known,pred,labels=classes,average="macro",zero_division=0)),
        "confusion_labels":list(classes),
        "confusion":confusion_matrix(known,pred,labels=classes).tolist(),
        "per_gas_recall":{
            gas:(sum(a==b==gas for a,b in zip(known,pred))/sum(a==gas for a in known))
                 if sum(a==gas for a in known) else None
            for gas in classes
        }
    }

def run(archive):
    rows,inventory=parse_original(archive)
    results={}
    for batches in SCENARIOS:
        training,test=prepare(rows,batches)
        xtrain=[select_sensor_features(r) for r in training]
        xtest=[select_sensor_features(r) for r in test]
        ytrain=[r["gas"] for r in training]
        ytest=[r["gas"] for r in test]
        key="batch"+",".join(str(i) for i in batches)+"_to_10"
        result={}
        for method in METHODS:
            pred=fit_predict(method,xtrain,ytrain,xtest)
            if len(pred)!=len(test):raise AssertionError("unexpected predictions")
            result[method]=score(ytest,pred)
        results[key]={
            "source_batches":list(batches),"target_batch":10,
            "n_train":len(training),"n_test":len(test),
            "gas_counts_train":dict(sorted(collections.Counter(ytrain).items())),
            "gas_counts_test":dict(sorted(collections.Counter(ytest).items())),
            "models":result
        }
    return {
        "dataset":"UCI270 Gas Sensor Array Drift at Different Concentrations",
        "source_doi":"10.24432/C5MK6M","source_sha256":ZIP_SHA256,
        "verified_source_exposures":inventory["total_exposure_records"],
        "tested_source_chronological_batches":[10],
        "source_type":"16 TGS source sensors, 128 precomputed features; 16 normalized-response descriptors used",
        "source_donor_license_issue":"UCI CC BY metadata vs earlier donor noncommercial wording: internal research only",
        "g3_tank_validated":False,
        "limits":[
          "Chronological test in source batch10 does not establish independent device transfer or Tank MQ8 performance.",
          "Same 3600 target exposure records reused across two training-history scenarios, not 7200 distinct experiments.",
          "Source temporal/concentration distributions may differ across months; do not compare to UCI1081 scores as same experiment.",
          "Only source gas identity classification; no exposure ppm, open-set, mixtures, or safety calibration.",
          "All preprocessing and estimators fitted only to source chronological batches; no target labels in fitting."
        ],
        "scenarios":results
    }

def main():
    p=argparse.ArgumentParser();p.add_argument("source_zip")
    p.add_argument("--output",required=True);a=p.parse_args()
    result=run(a.source_zip)
    Path(a.output).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    for scenario,part in result["scenarios"].items():
        print(scenario,part["n_train"],"training, ",part["n_test"],"chronological target")
        for model,metric in part["models"].items():
            print(model,metric["correct"],"/",metric["n_test"],
                  "macroF1=",round(metric["macro_f1"],4),
                  "balanced=",round(metric["balanced_accuracy"],4))
if __name__=="__main__":main()
