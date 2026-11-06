#!/usr/bin/env python3
"""ChemCore Next R3 external-MQ source-only benchmark (non-Tank device).

6400 raw ADC rows, four *categories*, 7 MQ channels, 1600 consecutive rows
per category. No independent physical session/device IDs exist in the source.
Purged contiguous-block CV is exploratory and cannot show transfer accuracy.
Historical ethanol experiment and R0/K formulas are explicitly not used.
Any data-dependent hyperparameter selection must require another independent
dataset before moving to ChemCore Stable or deploying to Tank.
"""
import argparse,csv,json,math,pathlib,collections,statistics
import numpy as np
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.ensemble import RandomForestClassifier, HistGradientBoostingClassifier
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score,f1_score,confusion_matrix,classification_report

SEED=20261106
LABELS=("NoGas","Perfume","Smoke","Mixture")
SETS={
  "MQ3_ONLY":("MQ3",),
  "SIX_TANK_OVERLAP":("MQ2","MQ3","MQ5","MQ7","MQ8","MQ135"),
  "SEVEN_SOURCE":("MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135")
}
def read_raw(p):
    with p.open(newline="",encoding="utf-8-sig") as f:rows=list(csv.DictReader(f))
    assert len(rows)==6400,len(rows)
    assert set(x["Gas"] for x in rows)==set(LABELS)
    counts=collections.Counter(x["Gas"] for x in rows)
    assert all(counts[x]==1600 for x in LABELS),counts
    for gas in LABELS:
        pos=[int(x["Serial Number"]) for x in rows if x["Gas"]==gas]
        assert pos==list(range(1600)), "source must have contiguous single recorded sequence per class"
    names=("MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135")
    x=np.asarray([[float(row[k]) for k in names] for row in rows],dtype=np.float64)
    y=np.asarray([LABELS.index(row["Gas"]) for row in rows],dtype=np.int64)
    serial=np.asarray([int(row["Serial Number"]) for row in rows])
    assert np.isfinite(x).all()
    return x,y,serial,names
def models():
    return {
      "LDA_SHRINKAGE":lambda:make_pipeline(StandardScaler(),LinearDiscriminantAnalysis(solver="lsqr",shrinkage="auto")),
      "RBF_SVC":lambda:make_pipeline(StandardScaler(),SVC(C=3.0,gamma="scale",kernel="rbf")),
      "RANDOM_FOREST":lambda:RandomForestClassifier(n_estimators=120,
        min_samples_leaf=5,max_features="sqrt",random_state=SEED,n_jobs=2),
      "HIST_GRADIENT_BOOST":lambda:HistGradientBoostingClassifier(max_iter=130,
        max_leaf_nodes=15,learning_rate=0.06,l2_regularization=1.0,random_state=SEED)
    }
def splits(y,serial):
    # 5 predeclared validation folds; each class contributes two disjoint
    # 160-row intervals (20%). Purge 40 chronological samples around test
    # boundaries within its own class. Still not independent exposure sessions.
    block=serial//160
    for fold in range(5):
        selected={fold,fold+5}
        test=np.isin(block,list(selected))
        train=~test
        for b in selected:
            train &= ~((serial>=b*160-40)&(serial<=(b+1)*160-1+40))
        assert len(np.unique(y[test]))==len(LABELS)
        assert len(np.unique(y[train]))==len(LABELS)
        assert not np.any(train&test)
        yield "PURGED_BLOCK_FOLD_"+str(fold),train,test
    # Stricter forward-time transfer within each class (one session).
    train=serial<1200
    test=serial>=1280
    assert not np.any(train&test)
    yield "FORWARD_LAST_320",train,test
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source",type=pathlib.Path,required=True)
    p.add_argument("--out",type=pathlib.Path,required=True)
    a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
    x,y,serial,names=read_raw(a.source)
    outcomes=[]
    for splitname,train,test in splits(y,serial):
      for setname,subset in SETS.items():
        cols=[names.index(q) for q in subset]
        X_train,X_test=x[train][:,cols],x[test][:,cols]
        y_train,y_test=y[train],y[test]
        for alg,maker in models().items():
          model=maker();model.fit(X_train,y_train)
          pred=model.predict(X_test)
          conf=confusion_matrix(y_test,pred,labels=np.arange(4))
          classes=classification_report(y_test,pred,labels=np.arange(4),
            target_names=LABELS,output_dict=True,zero_division=0)
          accuracy=accuracy_score(y_test,pred)
          macro=f1_score(y_test,pred,average="macro")
          noGas=conf[0].sum()
          smoke=conf[2].sum()
          row={
             "split":splitname,"portfolio":setname,"method":alg,
             "train_rows":int(train.sum()),"test_rows":int(test.sum()),
             "test_accuracy":round(float(accuracy),6),
             "macro_F1":round(float(macro),6),
             "NoGas_false_gas_fraction":round(float(1-conf[0,0]/noGas),6),
             "Smoke_recall":round(float(conf[2,2]/smoke),6),
             "category_labels":list(LABELS),
             "confusion_matrix":conf.tolist(),
             "per_class":{z:{"precision":round(classes[z]["precision"],6),
                  "recall":round(classes[z]["recall"],6),
                  "f1":round(classes[z]["f1-score"],6),
                  "support":int(classes[z]["support"])} for z in LABELS},
             "source_data_not_tank":True,
             "source_label_classes_not_molecular_identification":True,
             "no_independent_day_device_split":True
          }
          outcomes.append(row)
          print("R3_MQ6400_BLOCKED",splitname,setname,alg,
              "accuracy",row["test_accuracy"],"macro_f1",row["macro_F1"],flush=True)
    assert len(outcomes)==72,len(outcomes)
    table=[]
    for r in outcomes:
      table.append({k:r[k] for k in ("split","portfolio","method","train_rows",
          "test_rows","test_accuracy","macro_F1","NoGas_false_gas_fraction","Smoke_recall")})
    with (a.out/"r3_mq6400_source_grouped_fold_metrics.csv").open("w",newline="",encoding="utf-8") as f:
      w=csv.DictWriter(f,fieldnames=list(table[0]));w.writeheader();w.writerows(table)
    grouped=collections.defaultdict(list)
    for r in outcomes:
      if r["split"].startswith("PURGED_BLOCK"):
        grouped[(r["portfolio"],r["method"])].append(r)
    group_summary=[]
    for (portfolio,algorithm),records in sorted(grouped.items()):
      group_summary.append({
        "portfolio":portfolio,"method":algorithm,"folds":len(records),
        "accuracy_mean":round(statistics.mean(r["test_accuracy"] for r in records),6),
        "accuracy_min":min(r["test_accuracy"] for r in records),
        "accuracy_max":max(r["test_accuracy"] for r in records),
        "macro_F1_mean":round(statistics.mean(r["macro_F1"] for r in records),6),
        "NoGas_false_gas_fraction_mean":round(statistics.mean(r["NoGas_false_gas_fraction"] for r in records),6),
        "Smoke_recall_mean":round(statistics.mean(r["Smoke_recall"] for r in records),6)
      })
    summary={
      "schema":"chemcore.next.r3.mq6400.purged_source_adc_cv/1",
      "original_raw_rows":len(y),"source_category_labels":LABELS,
      "original_source_has_independent_session_ids":False,
      "source_categories_are_not_pure_gas_cas_ids":True,
      "evaluation":"five predetermined purged block folds plus forward-last-320 holdout",
      "source_sequential_time_correlation_not_eliminated":True,
      "independent_external_test":False,
      "transfer_to_tank_validated":False,
      "R0_source_used":False,"owner_ethanol_experiment_used":False,
      "g2_independent_validation_met":False,"tank_g3_met":False,
      "not_empirical_tank_90_percent_proof":True,
      "all_fold_results":outcomes,
      "purged_block_means":group_summary,
      "forward_holdout_results":[r for r in outcomes if r["split"]=="FORWARD_LAST_320"]
    }
    (a.out/"r3_mq6400_grouped_cv_summary.json").write_text(json.dumps(summary,indent=2)+"\n")
    print("R3_MQ6400_PURGED_SOURCE_CV_PASS",json.dumps({
      "raw_rows":len(y),"comparisons":len(outcomes),
      "best_exploratory":max(group_summary,key=lambda r:r["macro_F1_mean"]),
      "worst_exploratory":min(group_summary,key=lambda r:r["macro_F1_mean"]),
      "independent_gas_accuracy":None,
      "one_session_per_class":True},sort_keys=True))
if __name__=="__main__":main()
