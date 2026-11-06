#!/usr/bin/env python3
"""R3 false-alarm / unknown gate ablation on external source ADC category data.

Five purged temporal-block folds, 6 MQ models common to Tank. Six-model
analogue is *NOT* a Tank calibration. Categories are not molecular gas IDs.
No source session/device independence; no thresholds promoted to production.
Compare RF alone, train-only NoGas Mahalanobis envelope, uncalibrated RF
gas-score cutoffs, and explicit UNKNOWN. Never tune release gates on test.
"""
import argparse,json,csv,pathlib,collections,statistics
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.covariance import LedoitWolf
from sklearn.metrics import confusion_matrix
LABELS=("NoGas","Perfume","Smoke","Mixture")
FEATURES=("MQ2","MQ3","MQ5","MQ7","MQ8","MQ135")
def read(p):
    with p.open(encoding="utf-8-sig",newline="") as f:rows=list(csv.DictReader(f))
    assert len(rows)==6400
    y=np.array([LABELS.index(r["Gas"]) for r in rows])
    serial=np.array([int(r["Serial Number"]) for r in rows])
    X=np.array([[float(r[q]) for q in FEATURES] for r in rows],dtype=float)
    assert np.isfinite(X).all()
    assert all(int((y==i).sum())==1600 for i in range(4))
    return X,y,serial
def fold_masks(serial):
    block=serial//160
    for fold in range(5):
        selected={fold,fold+5}
        test=np.isin(block,list(selected));train=~test
        for b in selected:
            train &= ~((serial>=b*160-40)&(serial<=(b+1)*160-1+40))
        yield fold,train,test
def score(y,p):
    return dict(
      n=len(y), correct=int(np.sum(y==p)),
      accepted=int(np.sum(p!=4)),
      exact_accuracy=float(np.mean(y==p)),
      accepted_precision=float(np.sum(y==p)/np.sum(p!=4)) if np.any(p!=4) else None,
      unknown_fraction=float(np.mean(p==4)),
      NoGas_false_gas_fraction=float(np.mean(np.isin(p[y==0],[1,2,3]))),
      NoGas_unknown_fraction=float(np.mean(p[y==0]==4)),
      Smoke_recall=float(np.mean(p[y==2]==2)),
      all_gas_recall=float(np.mean(p[y>0]==y[y>0])),
      false_positive_count=int(np.sum(np.isin(p[y==0],[1,2,3]))),
      source_categories_not_chemical_ids=True
    )
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source",type=pathlib.Path,required=True)
    p.add_argument("--out",type=pathlib.Path,required=True)
    a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
    X,y,serial=read(a.source)
    policy={}
    for fold,train,test in fold_masks(serial):
        rf=RandomForestClassifier(n_estimators=120,min_samples_leaf=5,
          max_features="sqrt",random_state=20261106,n_jobs=2)
        rf.fit(X[train],y[train])
        prob=rf.predict_proba(X[test])
        assert list(rf.classes_)==list(range(4))
        baseline=np.argmax(prob,axis=1).astype(int)
        cleanTrain=X[train][y[train]==0]
        # Derived from training-only known NoGas, not owner Tank R_air.
        cov=LedoitWolf().fit(cleanTrain)
        distTrain=cov.mahalanobis(cleanTrain)
        distTest=cov.mahalanobis(X[test])
        pred={}
        pred["RF_BASELINE"]=baseline
        for q in (.95,.99,.999):
          quantile=float(np.quantile(distTrain,q))
          pred[f"TRAIN_NOGAS_COV_Q{q}"]=np.where(distTest<=quantile,0,baseline)
        for cutoff in (.55,.65,.75,.85,.95):
          # Probability is not calibrated posterior, treated as score only.
          z=prob.copy()
          winner=np.argmax(z[:,1:],axis=1)+1
          gasScore=z[np.arange(len(z)),winner]
          pred[f"UNCALIBRATED_GAS_SCORE_{cutoff}"]=np.where(gasScore>=cutoff,winner,0)
        for cutoff in (.55,.70,.85):
          # Explicit unknown when no class meets research-only confidence
          best=np.argmax(prob,axis=1)
          top=prob[np.arange(len(prob)),best]
          pred[f"ABSTAIN_LOW_CLASS_SCORE_{cutoff}"]=np.where(top>=cutoff,best,4)
        for q in (.95,.99):
          quantile=float(np.quantile(distTrain,q))
          for cutoff in (.65,.85):
            win=np.argmax(prob[:,1:],axis=1)+1
            gasScore=prob[np.arange(len(prob)),win]
            pred[f"NOGAS_ENVELOPE_Q{q}_AND_GASSCORE_{cutoff}"]=np.where(
              (distTest>quantile)&(gasScore>=cutoff),win,0)
        for name,prediction in pred.items():
          policy.setdefault(name,[]).append(dict(fold=fold,**score(y[test],prediction)))
    assert len(policy)==16,len(policy)
    rows=[]
    for name,folds in sorted(policy.items()):
        assert len(folds)==5
        row={"policy":name,"fold_count":5}
        for key in ["exact_accuracy","accepted_precision","unknown_fraction",
                    "NoGas_false_gas_fraction","NoGas_unknown_fraction",
                    "Smoke_recall","all_gas_recall"]:
            values=[r[key] for r in folds if r[key] is not None]
            row[key]=round(statistics.mean(values),6) if values else None
        row["false_positive_count_across_folds"]=sum(r["false_positive_count"] for r in folds)
        row["test_rows_across_folds"]=sum(r["n"] for r in folds)
        rows.append(row)
    with (a.out/"r3_nogas_binary_gate_policy_sweep.csv").open("w",encoding="utf8",newline="") as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    good=[r for r in rows if r["NoGas_false_gas_fraction"]<=.05 and r["Smoke_recall"]>=.8]
    exploratory=sorted(good,key=lambda r:(-r["Smoke_recall"],-r["exact_accuracy"]))
    payload={"schema":"chemcore.next.r3.external_mq6400_nogas_gate_ablation/1",
      "source_raw_rows":len(y),"methods":len(policy),"folds":5,
      "source_device":"NOT_TANK","one_exposure_sequence_per_class":True,
      "real_gas_concentration_qualification":False,
      "source_NoGas_training_calibration_not_tank_ambient":True,
      "rf_scores_are_not_calibrated_probabilities":True,
      "thresholds_posthoc_screened_not_deployed":True,
      "no_independent_session_or_device_holdout":True,
      "G2_independent_external":False,"G3_Tank":False,
      "results":rows,"all_fold_metrics":policy,
      "exploratory_policy_meeting_5pct_falsegas_80pct_smoke":
        exploratory[0] if exploratory else None}
    (a.out/"r3_nogas_binary_gate_summary.json").write_text(json.dumps(payload,indent=2)+"\n")
    print("R3_NOGAS_SOURCE_ABLATION_PASS",json.dumps({
      "policies":len(policy),
      "baseline":next(r for r in rows if r["policy"]=="RF_BASELINE"),
      "highest_smoke_at_at_most_5pct_falsegas":exploratory[0] if exploratory else None,
      "Tank_G3":False},sort_keys=True))
if __name__=="__main__":main()
