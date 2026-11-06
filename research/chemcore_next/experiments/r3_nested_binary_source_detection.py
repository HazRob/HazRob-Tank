#!/usr/bin/env python3
"""R3 nested *source-only* NoGas detector -> category classifier ablation.

Outer split: 5 purged temporal blocks (20% rows/class each).
Inner: calibration blocks disjoint from model-fitting, with 40-row purge.
The four labels originate from ONE acquisition sequence per class. This is
not a chemically validated, independent-session, or Tank G3 benchmark.
No data from the older ethanol experiment or Tank R0 are used.
"""
import argparse,csv,json,pathlib,collections,statistics
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import f1_score,accuracy_score,confusion_matrix
FIELDS=("MQ2","MQ3","MQ5","MQ7","MQ8","MQ135")
LABELS=("NoGas","Perfume","Smoke","Mixture")
SEED=20261106
def parse(source):
    with source.open(encoding="utf-8-sig",newline="") as f:rows=list(csv.DictReader(f))
    assert len(rows)==6400
    X=np.array([[float(r[k]) for k in FIELDS] for r in rows])
    y=np.array([LABELS.index(r["Gas"]) for r in rows])
    serial=np.array([int(r["Serial Number"]) for r in rows])
    assert all(int((y==i).sum())==1600 for i in range(4))
    return X,y,serial
def cv(y,serial):
    blocks=serial//160
    for fold in range(5):
        held={fold,fold+5}
        outerTest=np.isin(blocks,list(held))
        outerTrain=~outerTest
        for b in held:
            outerTrain &= ~((serial>=b*160-40)&(serial<=(b+1)*160-1+40))
        # Deterministic inner calibration group from remaining source blocks.
        remaining=[x for x in range(10) if x not in held]
        innerBlocks=set(remaining[i] for i in (2,6))
        cal=outerTrain & np.isin(blocks,list(innerBlocks))
        train=outerTrain & ~cal
        for b in innerBlocks:
            train &= ~((serial>=b*160-40)&(serial<=(b+1)*160-1+40))
        assert not np.any(cal&train) and not np.any(train&outerTest) and not np.any(cal&outerTest)
        assert (y[cal]==0).sum()>=100
        assert all(int((y[train]==i).sum())>=500 for i in range(4))
        yield fold,train,cal,outerTest
def model(seed):
    return RandomForestClassifier(n_estimators=160,min_samples_leaf=5,
        max_features="sqrt",class_weight="balanced_subsample",n_jobs=2,random_state=seed)
def stage2(seed):
    return RandomForestClassifier(n_estimators=160,min_samples_leaf=5,
        max_features="sqrt",n_jobs=2,random_state=seed)
def metrics(y,yp,fold,policy,threshold):
    x=confusion_matrix(y,yp,labels=np.arange(5))
    return {
      "fold":fold,"policy":policy,"source_calibration_threshold":round(float(threshold),8),
      "test_rows":len(y),"correct":int(np.sum(y==yp)),
      "test_accuracy":round(float(accuracy_score(y,yp)),6),
      "macro_F1":round(float(f1_score(y,yp,labels=np.arange(4),average="macro",zero_division=0)),6),
      "NoGas_falsegas_fraction":round(float(np.mean(np.isin(yp[y==0],[1,2,3]))),6),
      "NoGas_unknown_fraction":round(float(np.mean(yp[y==0]==4)),6),
      "Smoke_recall":round(float(np.mean(yp[y==2]==2)),6),
      "gas_class_correct_fraction":round(float(np.mean(y[y>0]==yp[y>0])),6),
      "unknown_fraction":round(float(np.mean(yp==4)),6),
      "confusion":x.tolist()
    }
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source",required=True,type=pathlib.Path)
    p.add_argument("--out",required=True,type=pathlib.Path)
    a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
    X,y,serial=parse(a.source)
    folds=[];thresholds=[]
    for fold,train,cal,test in cv(y,serial):
        binmodel=model(SEED+fold)
        binmodel.fit(X[train],(y[train]!=0).astype(int))
        calProb=binmodel.predict_proba(X[cal])[:,1]
        testProb=binmodel.predict_proba(X[test])[:,1]
        cleanScores=calProb[y[cal]==0]
        q95=float(np.quantile(cleanScores,.95))
        q99=float(np.quantile(cleanScores,.99))
        threshold={
            "BINARY_BASELINE_P05":.5,
            "TRAIN_ONLY_NOGAS_Q95":q95,
            "TRAIN_ONLY_NOGAS_Q99":q99
        }
        classifier=stage2(SEED+fold)
        classifier.fit(X[train & (y>0)],y[train & (y>0)])
        assert list(classifier.classes_)==[1,2,3]
        gasClassProbs=classifier.predict_proba(X[test])
        gasClassMax=gasClassProbs.max(axis=1)
        gasClass=np.asarray(classifier.classes_)[gasClassProbs.argmax(axis=1)]
        for name,cut in threshold.items():
            pred=np.where(testProb>cut,gasClass,0)
            folds.append(metrics(y[test],pred,fold,name,cut))
        for cutoff in (.60,.75,.90):
            pred=np.where(testProb>q95,gasClass,0)
            pred[(testProb>q95)&(gasClassMax<cutoff)]=4
            folds.append(metrics(y[test],pred,fold,"TRAIN_Q95_PLUS_UNKNOWN_"+str(cutoff),q95))
        thresholds.append(dict(fold=fold,cal_rows=int(cal.sum()),
            cal_clean_rows=int((y[cal]==0).sum()),q95=q95,q99=q99,
            test_rows=int(test.sum()),train_rows=int(train.sum())))
        print("R3_SOURCE_BINARY_FOLD",fold,"q95",round(q95,5),"q99",round(q99,5),
              "base_acc",next(x["test_accuracy"] for x in folds if x["fold"]==fold and x["policy"]=="BINARY_BASELINE_P05"),flush=True)
    assert len(folds)==30,len(folds)
    by=collections.defaultdict(list)
    for r in folds:by[r["policy"]].append(r)
    agg=[]
    for policy,items in sorted(by.items()):
        d=dict(policy=policy,folds=len(items))
        for name in ("test_accuracy","macro_F1","NoGas_falsegas_fraction",
                     "NoGas_unknown_fraction","Smoke_recall",
                     "gas_class_correct_fraction","unknown_fraction"):
            d[name]=round(statistics.mean(x[name] for x in items),6)
        d["total_test_rows"]=sum(x["test_rows"] for x in items)
        agg.append(d)
    assert all(x["folds"]==5 for x in agg)
    with (a.out/"r3_nested_source_binary_gate_policies.csv").open("w",newline="",encoding="utf8") as f:
        w=csv.DictWriter(f,fieldnames=list(agg[0]));w.writeheader();w.writerows(agg)
    strict=[x for x in agg if x["NoGas_falsegas_fraction"]<=.05]
    result={"schema":"chemcore.next.r3.nested_source_binary_gas_gate/1",
        "source":"frozen MQ 6400 original ADC, four LABEL CATEGORIES",
        "source_device_is_Tank":False,
        "concentration_known_for_each_category":False,
        "outer_test_rows_per_fold":1280,
        "nested_calibration_disjoint_from_training":True,
        "calibration_blocks_temporal_independence_not_assured":True,
        "original_has_multiple_physical_sessions":False,
        "no_release_threshold_selected":True,
        "g2_external_independent":False,"g3_physical":False,
        "fold_thresholds":thresholds,"all_fold_metrics":folds,
        "policy_averages":agg,
        "best_5pct_nogas_exploratory":max(strict,key=lambda z:z["macro_F1"]) if strict else None}
    (a.out/"r3_nested_source_binary_gate_summary.json").write_text(json.dumps(result,indent=2)+"\n")
    print("R3_NESTED_SOURCE_BINARY_GATE_PASS",json.dumps({
        "policy_count":len(agg),
        "baseline":next(x for x in agg if x["policy"]=="BINARY_BASELINE_P05"),
        "best_5pct_nogas_exploratory":result["best_5pct_nogas_exploratory"],
        "independent_tank_gas_accuracy":None},sort_keys=True))
if __name__=="__main__":main()
