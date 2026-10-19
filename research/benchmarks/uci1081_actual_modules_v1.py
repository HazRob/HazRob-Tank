"""UCI1081 exploratory external-sensor benchmark for HazRob Tank ChemCore research.

Uses actual repository research modules 02.2, 04.2, 04.3, 04.4 and 04.6.
Only a scientific REFERENCE: array has 1 MQ135, not 8 Tank MQ sensors.
Never use reported scores for calibration, ppm determination or safety decisions.

Original dataset: Tian, Zhao & Deng (2023), DOI 10.24432/C5CK6F,
UCI 1081, licensed CC BY 4.0. Source archive must be supplied by caller.
"""
import argparse
import collections
import csv
import hashlib
import importlib.util
import io
import json
import math
from pathlib import Path
import re
import statistics
import zipfile

SOURCE_SHA256="813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab"
SENSORS=("TGS2603","TGS2630","TGS813","TGS822","MQ135",
         "MQ137","MQ138","2M012","VOCS-P","2SH12")
GASES=("ethanol","acetone","toluene","ethyl acetate","isopropanol","hexane")
PPB=(50.0,100.0,200.0)
REPRS=("late10","early_late_delta20")
MODELS=("majority","fingerprint","knn3","softmax","forest")

def repo_module(technique,filename):
    root=Path(__file__).resolve().parents[1]
    path=root/"techniques"/technique/filename
    if not path.is_file():
        raise FileNotFoundError("research technique module missing: "+str(path))
    spec=importlib.util.spec_from_file_location("tank_"+technique.replace(".","_"),path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def parse_archive(archive_path):
    path=Path(archive_path)
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if digest!=SOURCE_SHA256:
        raise ValueError("UCI1081 archive SHA256 mismatch: "+digest)
    out=[]
    with zipfile.ZipFile(path) as z:
        if sorted(z.namelist())!=["gsalc.csv"]:
            raise ValueError("unexpected UCI1081 ZIP file inventory")
        with io.TextIOWrapper(z.open("gsalc.csv"),encoding="utf-8") as handle:
            for idx,row in enumerate(csv.reader(handle)):
                if len(row)!=9002:
                    raise ValueError("row "+str(idx)+" expected 9002 fields")
                gas=row[0].strip()
                label=row[1].strip()
                if gas not in GASES or not re.fullmatch(r"(50|100|200)ppb",label):
                    raise ValueError("unexpected target on row "+str(idx))
                ppb=float(label[:-3])
                try:
                    values=[float(x) for x in row[2:]]
                except (ValueError,TypeError) as exc:
                    raise ValueError("non-numeric response at row "+str(idx)) from exc
                if len(values)!=9000 or any(not math.isfinite(x) for x in values):
                    raise ValueError("nonfinite or missing sensor value")
                trace=tuple(tuple(values[j*900:(j+1)*900]) for j in range(10))
                out.append({"source_row":idx,"gas":gas,"ppb":ppb,"trace":trace})
    cells=collections.Counter((row["gas"],row["ppb"]) for row in out)
    if len(out)!=90 or set(cells)!={(g,c) for g in GASES for c in PPB} or set(cells.values())!={5}:
        raise ValueError("expected 6×3×5 fully labelled exposures")
    counts=collections.Counter()
    for row in out:
        key=(row["gas"],row["ppb"])
        row["replicate_ordinal"]=counts[key]
        counts[key]+=1
    return out

def features(trace,representation):
    """10 sensor-major traces, 300s baseline + 600s injection, 1 Hz (UCI)."""
    if representation not in REPRS or len(trace)!=10 or any(len(t)!=900 for t in trace):
        raise ValueError("unrecognized representation or sensor-time schema")
    # Fixed within-sample summary statistics; no label-dependent transformations.
    baseline=[statistics.median(t[180:300]) for t in trace]
    late=[statistics.median(t[780:900]) for t in trace]
    if representation=="late10":
        return late
    early=[statistics.median(t[300:420]) for t in trace]
    return [e-b for e,b in zip(early,baseline)]+[l-b for l,b in zip(late,baseline)]

def splits(rows,protocol):
    if protocol not in ("replicate_ordinal_proxy","leave_one_concentration_ppb"):
        raise ValueError("unknown protocol")
    key="replicate_ordinal" if protocol=="replicate_ordinal_proxy" else "ppb"
    levels=sorted({r[key] for r in rows})
    if len(levels)!=(5 if key=="replicate_ordinal" else 3):
        raise ValueError("unexpected fold count")
    result=[]
    for level in levels:
        train=[i for i,r in enumerate(rows) if r[key]!=level]
        test=[i for i,r in enumerate(rows) if r[key]==level]
        if not train or not test or set(train)&set(test):
            raise ValueError("empty or overlapping split")
        if set(rows[i]["gas"] for i in train)!=set(GASES):
            raise ValueError("training fold misses target gas")
        result.append({"heldout":level,"train":train,"test":test})
    if sorted(i for f in result for i in f["test"])!=list(range(len(rows))):
        raise ValueError("test exposure repeated or uncovered")
    return result

def predict_model(name,xtrain,ytrain,xtest,ids):
    if name=="majority":
        counts=collections.Counter(ytrain)
        winner=sorted(counts,key=lambda k:(-counts[k],k))[0]
        return [winner]*len(xtest)
    scaling=repo_module("02.2","scaling_v1.py")
    if name in ("fingerprint","knn3"):
        scaler=scaling.fit_scaler(xtrain,method="standard")
        scales=scaler["scale"]
        if name=="fingerprint":
            m=repo_module("04.2","fingerprint_matching_v1.py")
            lib=m.fit_fingerprint_library(xtrain,ytrain,ids,scales=scales)
            return [m.match_fingerprint(x,lib,top_k=1)["ranking"][0]["gas_label"] for x in xtest]
        m=repo_module("04.3","knn_v1.py")
        model=m.fit_knn(xtrain,ytrain,ids,scales=scales)
        return [m.predict_knn(x,model,k=3)["predicted_label"] for x in xtest]
    if name=="softmax":
        m=repo_module("04.4","logistic_regression_v1.py")
        model=m.fit_softmax(xtrain,ytrain,epochs=200,learning_rate=.1,l2=.01)
        return [d["predicted_label"] for d in m.predict_softmax(xtest,model)]
    if name=="forest":
        m=repo_module("04.6","random_forest_v1.py")
        model=m.fit_forest(xtrain,ytrain,n_trees=15,max_depth=5,min_leaf=2,seed=41)
        return [d["predicted_label"] for d in m.predict_forest(xtest,model)]
    raise ValueError("unknown model")

def summarize(predictions):
    # predictions: (actual, predicted, exposure_ppb, fold_id) tuples
    n=len(predictions)
    correct=sum(true==pred for true,pred,_,_ in predictions)
    recall={}
    precision={}
    f1={}
    for gas in GASES:
        tp=sum(true==gas and pred==gas for true,pred,_,_ in predictions)
        fn=sum(true==gas and pred!=gas for true,pred,_,_ in predictions)
        fp=sum(true!=gas and pred==gas for true,pred,_,_ in predictions)
        recall[gas]=tp/(tp+fn) if tp+fn else None
        precision[gas]=tp/(tp+fp) if tp+fp else 0.0
        f1[gas]=2*tp/(2*tp+fp+fn) if (2*tp+fp+fn) else 0.0
    by_ppb={
        str(int(c)):{"n":sum(v[2]==c for v in predictions),
                     "correct":sum(t==p and ppb==c for t,p,ppb,_ in predictions)}
        for c in PPB
    }
    for v in by_ppb.values():
        v["accuracy"]=v["correct"]/v["n"] if v["n"] else None
    return {
        "n_tested":n,"correct":correct,"accuracy":correct/n,
        "macro_recall":sum(recall.values())/len(recall),
        "macro_f1":sum(f1.values())/len(f1),
        "per_gas_recall":recall,"per_gas_precision":precision,
        "per_ppb":by_ppb,
        "confusion_matrix":{
            gas:{h:sum(t==gas and p==h for t,p,_,_ in predictions) for h in GASES}
            for gas in GASES
        },
        "fold_scores":[
            {"heldout":holdout,"n":sum(f==holdout for _,_,_,f in predictions),
             "correct":sum(t==p and f==holdout for t,p,_,f in predictions)}
            for holdout in sorted({v[3] for v in predictions})
        ]
    }

def evaluate(path):
    rows=parse_archive(path)
    y=[r["gas"] for r in rows]
    records={}
    for representation in REPRS:
        xx=[features(r["trace"],representation) for r in rows]
        feature_ids=[f"c{i:02d}_{key}" for key in ("late",) if representation=="late10" for i in range(10)]
        if representation!="late10":
            feature_ids=[f"c{i:02d}_{phase}" for phase in ("early_delta","late_delta") for i in range(10)]
        for protocol in ("replicate_ordinal_proxy","leave_one_concentration_ppb"):
            fold_data=splits(rows,protocol)
            for model_name in MODELS:
                predictions=[]
                for fold in fold_data:
                    it,iv=fold["train"],fold["test"]
                    pred=predict_model(model_name,[xx[i] for i in it],[y[i] for i in it],
                                       [xx[i] for i in iv],feature_ids)
                    if len(pred)!=len(iv):
                        raise AssertionError("wrong prediction count")
                    predictions.extend((y[i],p,rows[i]["ppb"],fold["heldout"]) for i,p in zip(iv,pred))
                if len(predictions)!=90:
                    raise AssertionError("not all exposures assessed")
                records[f"{representation}/{protocol}/{model_name}"]=summarize(predictions)
    return {
        "study":"UCI1081 exploratory external-sensor reference, NOT HazRob Tank device",
        "dataset_doi":"10.24432/C5CK6F",
        "license":"CC BY 4.0; cite Tian, Zhao & Deng (2023), UCI 1081",
        "source_zip_sha256":SOURCE_SHA256,
        "source_gas_labels":list(GASES),"source_sensor_order":list(SENSORS),
        "n_exposures":90,"n_sensor_scalar_samples":810000,"frequency_hz":1,
        "per_sensor_baseline_seconds":300,"per_sensor_injection_seconds":600,
        "source_response_units":"not established as Rs, Rs/R0, voltage or ADC",
        "feature_protocol":"fixed in advance; baseline median t180..299, early injection median t300..419, late injection median t780..899",
        "hyperparameters":"fixed before evaluation; no test-fold search; softmax epochs 200, kNN 3, RF 15 trees",
        "split_warning":"Replicate ordinal is inferred within gas×ppb cell, NOT a verified independent session/device; concentration holdout is not external-device validation.",
        "sensor_warning":"Only source MQ135 resembles one Tank sensor; other source TGS/MQ sensors are not interchangeable with Tank 8-MQ array.",
        "limitations":["training and test exposures may share device, day and drift;",
                       "no MQ8 R0/BME/heater-phase calibration; source signal response units unspecified;",
                       "small n=90, no unseen-gas/OOD verification in this experiment;",
                       "none of these exploratory results qualify a gas safety alarm or ppm instrument."],
        "results":records
    }

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("original_uci1081_zip")
    ap.add_argument("--output",default="-")
    args=ap.parse_args()
    data=evaluate(args.original_uci1081_zip)
    payload=json.dumps(data,indent=2,ensure_ascii=False,allow_nan=False)+"\n"
    if args.output=="-":
        print(payload)
    else:
        Path(args.output).write_text(payload,encoding="utf-8")
        print(f"saved {len(data['results'])} benchmark configurations to {args.output}")
        for key,val in data["results"].items():
            print(f"{key} | accuracy={val['accuracy']:.4f} | correct={val['correct']}/90")
if __name__=="__main__":main()
