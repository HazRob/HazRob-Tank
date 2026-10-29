#!/usr/bin/env python3
"""External UCI1081 90-exposure benchmark, research-only. Original zip required.
Never report this as Tank G3, calibration ppm or cross-device accuracy.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import io
import json
import zipfile
from collections import Counter
from pathlib import Path
import numpy as np
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, balanced_accuracy_score, f1_score, confusion_matrix
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

EXPECTED_ARCHIVE_SHA256 = "813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab"
GASES = ("ethanol","acetone","toluene","ethyl acetate","isopropanol","hexane")
LEVELS = (50,100,200)
CHANNEL_NAMES = ("TGS2603","TGS2630","TGS813","TGS822","MQ135","MQ137","MQ138","2M012","VOCS-P","2SH12")

def load(path):
    raw = Path(path).read_bytes()
    sha = hashlib.sha256(raw).hexdigest()
    if sha != EXPECTED_ARCHIVE_SHA256:
        raise ValueError("original UCI1081 archive SHA256 mismatch: " + sha)
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        if archive.namelist() != ["gsalc.csv"]:
            raise ValueError("unexpected archive layout")
        text = archive.read("gsalc.csv")
    csv_sha = hashlib.sha256(text).hexdigest()
    rows = list(csv.reader(io.StringIO(text.decode("utf-8-sig"))))
    if len(rows) != 90 or any(len(row) != 9002 for row in rows):
        raise ValueError("expected 90 exposure rows each with 900 seconds x 10 channels")
    records = []
    for i, row in enumerate(rows):
        gas = row[0].strip().lower()
        label = row[1].strip().lower()
        if gas not in GASES or not label.endswith("ppb"):
            raise ValueError("unrecognized gas or concentration unit")
        ppb = int(label.removesuffix("ppb"))
        if ppb not in LEVELS:
            raise ValueError("unexpected concentration")
        # UCI1081 CSV stores 900 consecutive samples PER SENSOR, then next sensor.
        # Prior reshape(900,10) interleaved unrelated channel blocks and
        # invalidated the published pilot metrics. Correct (10,900).T layout
        # reproduces the pinned source MQ135 endpoint/baseline reference 0.94717.
        values = np.asarray([float(v) for v in row[2:]],dtype=float).reshape(10,900).T
        if not np.all(np.isfinite(values)):
            raise ValueError("non-finite original signal values")
        records.append({"row_id":i, "gas":gas, "ppb":ppb, "signals":values})
    # Layout parity with pinned independent MQ135 reference, derived from
    # acetone 50 ppb (5 source replicates), before training any classifier.
    check = [record for record in records if record["gas"] == "acetone" and record["ppb"] == 50]
    ratios = [record["signals"][840:900,4].mean() /
              record["signals"][:300,4].mean() for record in check]
    if len(ratios)!=5 or abs(float(np.mean(ratios))-0.9471726656594102)>1e-9:
        raise ValueError("UCI1081 source sensor layout mismatches pinned MQ135 reference")
    freq = Counter((r["gas"],r["ppb"]) for r in records)
    if len(freq) != 18 or set(freq.values()) != {5}:
        raise ValueError("source missing original replicate or gas/level")
    return records,sha,csv_sha

def features(record):
    signal = record["signals"]
    baseline = signal[:300].mean(axis=0)
    start = signal[300:360].mean(axis=0)
    middle = signal[600:660].mean(axis=0)
    endpoint = signal[840:900].mean(axis=0)
    if np.any(np.abs(baseline)<1e-9):
        raise ValueError("zero baseline, no imputation permitted")
    # Three temporal windows relative to each source sensor baseline.
    # Input is raw source signal, NOT a chemically calibrated Rs/R0.
    return np.concatenate((endpoint/baseline-1,start/baseline-1,middle/baseline-1))

def evaluate(records,mode,model_name):
    labels = np.asarray([r["gas"] for r in records])
    levels = np.asarray([r["ppb"] for r in records])
    feats = np.asarray([features(r) for r in records])
    X = feats[:,[4,14,24]] if mode == "MQ135_only" else feats
    folds = []
    for withheld in LEVELS:
        train = levels != withheld
        test = levels == withheld
        if model_name == "LDA":
            model = make_pipeline(StandardScaler(),
                LinearDiscriminantAnalysis(solver="lsqr",shrinkage="auto"))
        else:
            model = make_pipeline(StandardScaler(),
                LogisticRegression(max_iter=4000,C=0.1,random_state=20261029))
        model.fit(X[train],labels[train])
        pred = model.predict(X[test])
        folds.append({
            "held_out_ppb":int(withheld),
            "train_exposures":int(train.sum()),
            "test_exposures":int(test.sum()),
            "accuracy":float(accuracy_score(labels[test],pred)),
            "balanced_accuracy":float(balanced_accuracy_score(labels[test],pred)),
            "macro_f1":float(f1_score(labels[test],pred,labels=list(GASES),average="macro",zero_division=0)),
            "confusion_rows_true_cols_pred_gas_order":confusion_matrix(labels[test],pred,labels=list(GASES)).tolist(),
            "source_test_indices":[int(v) for v in np.where(test)[0]],
            "source_predictions":[str(v) for v in pred]
        })
    return {"channels":mode,"model":model_name,"folds":folds,
        "mean_balanced_accuracy":float(np.mean([f["balanced_accuracy"] for f in folds])),
        "mean_macro_f1":float(np.mean([f["macro_f1"] for f in folds]))}

def run(path):
    records,zip_sha,csv_sha = load(path)
    models = [evaluate(records,mode,name)
              for mode in ("MQ135_only","full_source_10_channels")
              for name in ("LogisticRegression","LDA")]
    return {
      "schema":"chemcore-tank-v6-uci1081-external-benchmark/2",
      "source_url":"https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration",
      "zip_sha256":zip_sha, "csv_sha256":csv_sha, "independent_exposure_rows":len(records),
      "source_layout":"900 samples PER sensor, 10 contiguous sensor blocks per row (10,900).T",
      "supersedes_flawed_interleaved_layout_pilot_2026_10_29":True,
      "source_sensor_names":CHANNEL_NAMES,"mq135_index_zero_based":4,
      "source_gas_labels":GASES,"source_concentrations_ppb":LEVELS,
      "feature_schema":"baseline first 300 s; first60 after exposure; middle 600-660s; last60 840-900s",
      "evaluation":"Leave one entire concentration level out in each of three folds",
      "no_tank_g3_claim":True, "m4_quantification_validated":False,
      "no_invented_rs_r0":True,
      "split_limitation":"Device and acquisition-day independence not established in source CSV; external exploratory cross-level result ONLY",
      "comparison_runs":models
    }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--archive",required=True)
    parser.add_argument("--out",required=True)
    args=parser.parse_args()
    result=run(args.archive)
    dest=Path(args.out);dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n")
    print(json.dumps({"source_sha":result["zip_sha256"],"exposures":result["independent_exposure_rows"],
          "comparisons":[{"channels":v["channels"],"model":v["model"],
                          "mean_balanced_accuracy":v["mean_balanced_accuracy"]} for v in result["comparison_runs"]]},indent=2))

if __name__ == "__main__":
    main()
