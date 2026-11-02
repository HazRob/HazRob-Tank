#!/usr/bin/env python3
"""ChemCore Next G2-source research: physical MQ4 Rs + independent CH4 Li-Cor.

Separate paper source from robot calibration and from ChemCore Next runtime.
11 leave-whole-day-out folds, fixed calibration day, no target-derived features.
Create training-only source quantitative fingerprints and evaluate abstention.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import pathlib
from collections import Counter
import numpy as np
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error

SOURCE_SHA = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"
SENSORS = ("MQ4_1", "MQ4_2", "MQ4_3")
CH4_EDGES = (0.0, 2.5, 4.0, 6.0, 8.0, 11.0)
MODES = ("BASELINE_TRAIN_MEDIAN", "NEXT_MQ4_SINGLE_RIDGE",
         "NEXT_MQ4_ARRAY_RIDGE", "NEXT_MQ4_ARRAY_ENV_RIDGE")
FIELDS = ("datetime","licor_ch4","licor_h2o","t","rh","2600_1","2600_2",
          "2600_3","2602_1","2602_2","2602_3","2611C_1","2611C_2",
          "2611C_3","2611E_1","2611E_2","2611E_3","MQ4_1","MQ4_2","MQ4_3")

def write_csv(file, rows, columns):
    with open(file,"w",encoding="utf-8",newline="") as out:
        writer=csv.DictWriter(out,columns,extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)

def load(path):
    original=pathlib.Path(path).read_bytes()
    digest=hashlib.sha256(original).hexdigest()
    if digest != SOURCE_SHA:
        raise ValueError("SOURCE_HASH_MISMATCH expected "+SOURCE_SHA+" actual "+digest)
    frame=pd.read_csv(path)
    if tuple(frame.columns)!=FIELDS or len(frame)!=14186:
        raise ValueError("NOT_ORIGINAL_MINNESOTA_MQ4_SHAPE")
    frame["datetime"]=pd.to_datetime(frame["datetime"],errors="raise")
    if frame["datetime"].isna().any() or not frame.datetime.is_monotonic_increasing:
        raise ValueError("INVALID_TIMESTAMPS")
    for column in ("licor_ch4","t","rh",*SENSORS):
        vals=frame[column].to_numpy(dtype=float)
        if not np.isfinite(vals).all():
            raise ValueError("NONFINITE_"+column)
        if column in ("licor_ch4",*SENSORS) and not (vals>0).all():
            raise ValueError("NEGATIVE_OR_ZERO_"+column)
    frame["day"]=frame.datetime.dt.strftime("%Y-%m-%d")
    dates=sorted(frame.day.unique().tolist())
    if len(dates)!=11 or dates[0]!="2021-07-21" or dates[-1]!="2021-07-31":
        raise ValueError("INCORRECT_DAY_GROUPING")
    return frame,dates,digest

def train_models(X, Y, train_idx):
    models={}
    for mode in MODES[1:]:
        ids=(0,) if mode=="NEXT_MQ4_SINGLE_RIDGE" else ((0,1,2) if
            mode=="NEXT_MQ4_ARRAY_RIDGE" else (0,1,2,3,4))
        pipe=make_pipeline(StandardScaler(),Ridge(alpha=10.0))
        pipe.fit(X[train_idx][:,ids],Y[train_idx])
        models[mode]=(pipe,ids)
    return models

def quant_fingerprint(row_train, frame, ppm, source):
    """Binned source *training-only* Rs distribution; not a calibrated MQ curve."""
    result=[]
    for k,instance in enumerate(SENSORS):
        for a,b in zip(CH4_EDGES[:-1],CH4_EDGES[1:]):
            indices=row_train & (ppm>=a) & (ppm<b)
            sample=frame.loc[indices,instance].to_numpy(dtype=float)
            if len(sample)<5:
                continue
            temps=frame.loc[indices,"t"].to_numpy(dtype=float)
            rh=frame.loc[indices,"rh"].to_numpy(dtype=float)
            result.append(dict(source_root="MQ-EXT-21-FURUTA-LICOR-2022",
                source_sha256=source,analyte_label="CH4_REFERENCE_ASSIGNED_NOT_IDENTIFIED",
                gas_cas="74-82-8",source_sensor_uid=instance,
                physical_sensor_instances=3,training_rows=int(len(sample)),
                interval_low_ppm=a,interval_high_ppm=b,
                training_rs_median_ohm=float(np.median(sample)),
                training_logrs_median=float(np.median(np.log(sample))),
                training_logrs_mad=float(np.median(np.abs(np.log(sample)-np.median(np.log(sample))))),
                training_t_median_C=float(np.median(temps)),
                training_rh_median_pct=float(np.median(rh)),
                reference_gas_instrument="Li-Cor",ppm_unit_conversion="measured_ppb_div_1000",
                source_R0_is_not_tank_R0=True,validated_on_actual_Tank=False,
                algorithm_qualification="TRAINING_ONLY_SOURCE_REFERENCE_G2_RESEARCH"))
    return result

def stats(Y, pred, accepted, threshold=4.0):
    valid=np.isfinite(pred)
    if not valid.all(): raise ValueError("nonfinite pred")
    marked=pred>=threshold
    positive=Y>=threshold
    return dict(
        n=int(len(Y)),mae_ppm=float(np.mean(np.abs(Y-pred))),
        bias_ppm=float(np.mean(pred-Y)),
        n_at_or_above_4ppm=int(positive.sum()),
        alert_recall_at_4ppm=(float((positive&marked).sum()/positive.sum()) if positive.any() else None),
        false_positive_count_at_4ppm=int((~positive & marked).sum()),
        coverage_fraction=float(accepted.mean()),
        selective_mae_ppm=(float(np.mean(np.abs(Y[accepted]-pred[accepted]))) if accepted.any() else None))

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",required=True)
    parser.add_argument("--out",required=True)
    args=parser.parse_args()
    out=pathlib.Path(args.out)
    out.mkdir(parents=True,exist_ok=True)
    df, days, sha=load(args.source)
    target=df.licor_ch4.to_numpy(dtype=float)/1000.0
    logrs=np.log(df[list(SENSORS)].to_numpy(dtype=float))
    env=df[["t","rh"]].to_numpy(dtype=float)
    X=np.column_stack([logrs,env])
    byday=df.day.to_numpy()
    signatures=[];folds=[];predictions=[];transfers=[]
    for d, test_day in enumerate(days):
        calibration_day=days[(d-1)%len(days)]
        train=(byday!=test_day)&(byday!=calibration_day)
        cal=(byday==calibration_day)
        test=(byday==test_day)
        assert not np.any(train&cal) and not np.any(train&test) and not np.any(cal&test)
        assert train.sum()+cal.sum()+test.sum()==len(df)
        models=train_models(X,target,train)
        signatures.extend({**r,"fold_test_day":test_day,
            "fold_calibration_day":calibration_day,
            "training_day_count":9} for r in quant_fingerprint(train,df,target,sha))
        for mode in MODES:
            if mode==MODES[0]:
                estimate=np.full(len(df),float(np.median(target[train])))
                columns=()
            else:
                model,columns=models[mode]
                estimate=model.predict(X[:,columns])
            # Split conformal residual quantile based exclusively on ONE calibration day.
            residuals=np.abs(target[cal]-estimate[cal])
            ncal=int(cal.sum())
            rank=min(ncal,int(np.ceil((ncal+1)*0.90)))
            conformal_half_width=float(np.partition(residuals,rank-1)[rank-1])
            xused=X[:,columns] if columns else X[:,3:5]
            lower=np.quantile(xused[train],0.01,axis=0)
            upper=np.quantile(xused[train],0.99,axis=0)
            in_domain=np.all((xused[test]>=lower)&(xused[test]<=upper),axis=1)
            indices=np.where(test)[0]
            ytest=target[test]; yhat=estimate[test]
            in_interval=(ytest>=yhat-conformal_half_width)&(ytest<=yhat+conformal_half_width)
            met=stats(ytest,yhat,in_domain)
            fold=dict(test_day=test_day,calibration_day=calibration_day,mode=mode,
                      n_train=int(train.sum()),n_cal=int(cal.sum()),n_test=int(test.sum()),
                      n_unique_train_days=9,source_root="MQ-EXT-21",
                      conformal_90_half_width_ppm=conformal_half_width,
                      interval_coverage=float(in_interval.mean()),
                      **met)
            folds.append(fold)
            for j,idx in enumerate(indices):
                predictions.append(dict(source_row=int(idx),timestamp=str(df.datetime.iloc[idx]),
                    fold_test_day=test_day,fold_calibration_day=calibration_day,
                    mode=mode,source_instrument="Li-Cor",
                    observed_CH4_ppm=float(ytest[j]),
                    estimated_CH4_ppm_research=float(yhat[j]),
                    half_width_ppm=conformal_half_width,
                    in_source_90_interval=bool(in_interval[j]),
                    within_training_sensor_environment_range=bool(in_domain[j]),
                    gas_identity_confirmed_by_next=False,
                    tank_R0_available=False,
                    tank_G3_qualified=False))
        # Independent physical sensor transfer: train on two MQ4 units, test unseen unit,
        # while test day and calibration day are also held out. No unit-specific R0 transfer.
        for held in range(3):
            avail=[v for v in range(3) if v!=held]
            rows_x=[]; rows_y=[]
            for unit in avail:
                for original in np.where(train)[0]:
                    rows_x.append([X[original,unit],X[original,3],X[original,4]])
                    rows_y.append(target[original])
            pipe=make_pipeline(StandardScaler(),Ridge(alpha=10.0))
            pipe.fit(np.asarray(rows_x),np.asarray(rows_y))
            xtest=np.column_stack([X[test,held],X[test,3:5]])
            estimate=pipe.predict(xtest)
            met=stats(target[test],estimate,np.ones(int(test.sum()),dtype=bool))
            transfers.append(dict(heldout_source_instance=SENSORS[held],
                training_instances="|".join(SENSORS[i] for i in avail),
                test_day=test_day,calibration_day=calibration_day,
                n_train_instances=2,n_train_days=9,**met))
    assert len(signatures)>=80 and all(x["training_day_count"]==9 for x in signatures)
    assert len(folds)==44 and len(predictions)==4*len(df)==56744 and len(transfers)==33
    assert set(x["mode"] for x in folds)==set(MODES)
    assert all(x["n_train"]+x["n_cal"]+x["n_test"]==len(df) for x in folds)
    assert all(x["test_day"]!=x["calibration_day"] for x in folds)
    write_csv(out/"next_mq4_quantitative_fingerprints.csv",signatures,list(signatures[0]))
    write_csv(out/"next_mq4_day_holdout_metrics.csv",folds,list(folds[0]))
    write_csv(out/"next_mq4_day_holdout_predictions.csv",predictions,list(predictions[0]))
    write_csv(out/"next_mq4_heldout_instance_transfer.csv",transfers,list(transfers[0]))
    summary={}
    for mode in MODES:
        group=[f for f in folds if f["mode"]==mode]
        summary[mode]=dict(mean_day_mae_ppm=float(np.mean([f["mae_ppm"] for f in group])),
            mean_day_interval_coverage=float(np.mean([f["interval_coverage"] for f in group])),
            mean_day_source_domain_accepted=float(np.mean([f["coverage_fraction"] for f in group])),
            pooled_test_rows=len(df))
    report=dict(schema="chemcore.next.external_mq4_quantitative_source_g2/1",
        scope="NEW_CHEMCORE_NEXT_SOURCE_QUANTITATIVE_FINGERPRINT_EXPERIMENT_NOT_V6_MODEL_REPLAY",
        source_id="MQ-EXT-21",source_csv_sha256=sha,
        physical_source_rows=len(df),source_instrument="Li-Cor CH4",
        concentration_original_unit="ppb",research_concentration_unit="ppm",
        source_devices=list(SENSORS),distinct_days=len(days),
        source_observed_unique_gases=1,
        fingerprints_created=len(signatures),
        day_group_holdout_folds=len(folds),
        heldout_predictions=len(predictions),
        unseen_instance_plus_day_transfer_tests=len(transfers),
        heldout_instance_mae_mean_ppm=float(np.mean([f["mae_ppm"] for f in transfers])),
        metrics_by_model=summary,
        no_tank_r0_used=True,no_tank_g3=True,no_multi_gas_identification_claim=True,
        safety_alert_qualification=False,
        limitations=["source correlations and training day fingerprints are not independent gas exposures",
        "one methane target only","conformal 90 percent interval may under-cover under environmental shifts",
        "4 ppm is a diagnostic source challenge only and not a regulatory alarm",
        "new research fingerprint/linear expert not yet activated in ChemCoreNext.evaluate"])
    (out/"next_mq4_source_experiment.json").write_text(
        json.dumps(report,indent=2,allow_nan=False)+"\n",encoding="utf8")
    for mode,stats in summary.items():
        print("NEXT_MQ4_SOURCE_SCORE",mode,
            "MAE_PPM=%.6f"%stats["mean_day_mae_ppm"],
            "COVERAGE90=%.5f"%stats["mean_day_interval_coverage"])
    print("CHEMCORE_NEXT_MQ4_REAL_REFERENCE_PASS rows=%d fingerprints=%d folds=%d predictions=%d unit_transfer=%d G3=0"%
          (len(df),len(signatures),len(folds),len(predictions),len(transfers)))

if __name__=="__main__":
    main()
