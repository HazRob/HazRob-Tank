#!/usr/bin/env python3
"""ChemCore Next research-source MQ135 temporal fingerprints and open-set G2 tests.

Only ORIGINAL SHA-verified UCI1081 exposures from extract_uci1081_verified.py.
No invented Tank R0, no source ppm, no use of true test gas as model feature.
All training/threshold calibration groups distinct from actual heldout exposures.
"""
import argparse
import csv
import hashlib
import json
import pathlib
from collections import Counter
import numpy as np
from sklearn.linear_model import LogisticRegression, Ridge
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

GASES=("ethanol","acetone","toluene","ethyl acetate","isopropanol","hexane")
LEVELS=(50,100,200)
MODES={"MQ135_3WINDOW":(4,14,24),
       "EXTERNAL_10SENSOR_30WINDOW_CEILING":tuple(range(30))}

def dump_csv(path, data, head):
    with path.open("w",encoding="utf8",newline="") as f:
        w=csv.DictWriter(f,head,extrasaction="ignore")
        w.writeheader()
        w.writerows(data)

def load(file,manifest):
    meta=json.loads(pathlib.Path(manifest).read_text(encoding="utf8"))
    blob=pathlib.Path(file).read_bytes()
    if hashlib.sha256(blob).hexdigest()!=meta["features_csv_sha256"]:
        raise ValueError("UCI1081_SHA_FEATURE_MISMATCH")
    if meta["original_archive_sha256"]!="813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab":
        raise ValueError("UCI1081_ORIGINAL_ARCHIVE_UNVERIFIED")
    with pathlib.Path(file).open(encoding="utf8",newline="") as f:
        reader=csv.DictReader(f)
        rows=list(reader)
    assert len(rows)==90 and len(rows[0])==33
    gas=np.asarray([r["gas"] for r in rows],dtype=object)
    ppb=np.asarray([int(r["ppb"]) for r in rows],dtype=int)
    ids=np.asarray([int(r["row_id"]) for r in rows])
    assert (ids==np.arange(90)).all()
    assert set(gas)==set(GASES) and set(ppb)==set(LEVELS)
    X=np.asarray([[float(r["f%02d"%j]) for j in range(30)] for r in rows])
    if not np.isfinite(X).all():raise ValueError("NONFINITE_UCI_SOURCE_FEATURE")
    for g in GASES:
        for c in LEVELS:assert np.sum((gas==g)&(ppb==c))==5
    repeat=np.empty(90,dtype=int)
    for g in GASES:
        for c in LEVELS:
            repeat[np.where((gas==g)&(ppb==c))[0]]=np.arange(5)
    return dict(gas=gas,ppb=ppb,row_id=ids,x=X,repeat=repeat,
                manifest=meta)

def softmax_model(x,labels,allowed):
    assert len(set(labels.tolist()))==len(allowed)
    if len(x)<=len(allowed):raise ValueError("No independent source exposures")
    mdl=make_pipeline(StandardScaler(),
        LogisticRegression(C=0.4,max_iter=700,solver="lbfgs"))
    mdl.fit(x,labels)
    return mdl

def selected(X,ids,index):
    return X[np.ix_(ids,index)]

def source_centroid_profiles(data,training,mode,features,heldout):
    rec=[]
    for gas in GASES:
        n=training&(data["gas"]==gas)
        vals=data["x"][n][:,features]
        if len(vals)<8:raise ValueError("not enough source training replicates")
        center=np.mean(vals,axis=0)
        spread=np.std(vals,axis=0,ddof=1)
        rec.append(dict(source_root_id="UCI1081_ORIGINAL_10_CHANNEL_90",
            source_archive_sha256=data["manifest"]["original_archive_sha256"],
            source_csv_sha256=data["manifest"]["original_csv_sha256"],
            fingerprint_type="TEMPORAL_WINDOW_SOURCE_TRAIN_ONLY",
            source_gas_label=gas,source_ppb_training_levels="|".join(map(str,sorted(set(
                data["ppb"][n].tolist())))),
            source_sensor_space=mode,feature_coordinates=len(features),
            endpoint_features=1 if mode.startswith("MQ135") else 10,
            train_exposures=int(len(vals)),heldout_ppb_level=heldout,
            train_feature_mean_json=json.dumps(center.tolist(),separators=(",",":")),
            train_feature_std_json=json.dumps(spread.tolist(),separators=(",",":")),
            source_sensor_hardware="external_UCI1081_not_Tank",
            measured_Tank_R0=False,quantitative_ppb_reference_source="UCI_author_record",
            verified_unknown_gas_identification=False,g3_qualified=False))
    return rec

def heldout_concentration(data):
    gas,ppb,rep,X=(data[k] for k in ("gas","ppb","repeat","x"))
    predictions=[];folds=[];fingerprints=[]
    for held in LEVELS:
        fit=(ppb!=held)&(rep<4)
        cal=(ppb!=held)&(rep==4)
        test=ppb==held
        assert fit.sum()==48 and cal.sum()==12 and test.sum()==30
        assert not (fit&test).any() and not (cal&test).any()
        for mode,cols in MODES.items():
            classifier=softmax_model(X[fit][:,cols],gas[fit],GASES)
            fingerprints.extend(source_centroid_profiles(data,fit,mode,cols,held))
            proba_cal=classifier.predict_proba(X[cal][:,cols])
            confidence_cal=np.max(proba_cal,axis=1)
            # Threshold fixed by only known-source calibration exposures,
            # not the 30 withheld concentration exposures.
            acceptance_threshold=float(np.quantile(confidence_cal,0.10))
            proba=classifier.predict_proba(X[test][:,cols])
            names=classifier.classes_
            winner=np.argmax(proba,axis=1)
            top3=np.argsort(-proba,axis=1)[:,:3]
            label=np.asarray([names[i] for i in winner])
            accept=np.max(proba,axis=1)>=acceptance_threshold
            true=gas[test]
            known_accuracy=float((label==true).mean())
            selective_accuracy=float((label[accept]==true[accept]).mean()) if accept.any() else None
            # Source gas-specific quantitative regressions trained on two concentrations.
            # A 'known gas oracle' is *explicitly not* a deployed inference.
            oracle=[];predicted_class_quantity=[]
            idxs=np.where(test)[0]
            per_gas={}
            for species in GASES:
                indices=fit&(gas==species)
                model=make_pipeline(StandardScaler(),Ridge(alpha=5.0))
                model.fit(X[indices][:,cols],ppb[indices].astype(float))
                per_gas[species]=model
            for idx,chosen in zip(idxs,label):
                oracle.append(float(per_gas[gas[idx]].predict(X[idx,cols].reshape(1,-1))[0]))
                predicted_class_quantity.append(float(per_gas[chosen].predict(X[idx,cols].reshape(1,-1))[0]))
            oracle=np.asarray(oracle)
            predq=np.asarray(predicted_class_quantity)
            for j,i in enumerate(idxs):
                predictions.append(dict(source_row_id=int(i),heldout_ppb=int(held),
                    mode=mode,original_gas=gas[i],source_ppb=int(ppb[i]),
                    guessed_top1=label[j],
                    guessed_top3="|".join(names[top3[j]].tolist()),
                    top1_score=float(proba[j,winner[j]]),
                    research_cal_threshold=acceptance_threshold,
                    accepted_research_label=bool(accept[j]),
                    predicted_class_conditional_ppb=float(predq[j]),
                    oracle_known_gas_concentration_ppb=float(oracle[j]),
                    concentration_extrapolates_beyond_training_level=True,
                    source_target_identification_proven=False,physical_Tank_G3=False))
            folds.append(dict(mode=mode,heldout_ppb=held,
                n_train=48,n_calibration=12,n_test=30,
                class_accuracy=known_accuracy,
                top3_accuracy=float(np.mean([gas[idx] in names[top3[j]] for j,idx in enumerate(idxs)])),
                accepted_fraction=float(accept.mean()),
                accepted_correct_fraction=selective_accuracy,
                oracle_concentration_mae_ppb=float(np.mean(np.abs(oracle-ppb[test]))),
                predicted_identity_conditional_mae_ppb=float(np.mean(np.abs(predq-ppb[test]))),
                concentration_verified_on_Tank=False))
    assert len(fingerprints)==2*3*6 and len(folds)==6 and len(predictions)==180
    return fingerprints,folds,predictions

def learn_open_threshold(cal_known_scores,cal_unknown_scores):
    if not len(cal_known_scores) or not len(cal_unknown_scores):
        raise ValueError("missing independent calibration observations")
    scores=np.sort(np.unique(np.r_[cal_known_scores,cal_unknown_scores]))
    thresholds=np.r_[0.0, (scores[:-1]+scores[1:])/2.0,1.0]
    best=None
    for t in thresholds:
        k=float(np.mean(cal_known_scores>=t))
        u=float(np.mean(cal_unknown_scores<t))
        objective=(k+u)/2
        # Deterministic tie: conservatively abstain at higher threshold.
        if best is None or objective>best[0]+1e-12 or (
            abs(objective-best[0])<=1e-12 and t>best[1]):
            best=(objective,float(t))
    return best[1]

def open_set(data):
    X,gas,ppb,rep=(data[k] for k in ("x","gas","ppb","repeat"))
    folds=[];classified=[]
    for unknown in GASES:
        for cal_unknown in GASES:
            if unknown==cal_unknown:continue
            known=tuple(x for x in GASES if x not in (unknown,cal_unknown))
            for heldout_level in LEVELS:
                train=np.isin(gas,known)&(ppb!=heldout_level)&(rep<4)
                calibration_known=np.isin(gas,known)&(ppb!=heldout_level)&(rep==4)
                calibration_unknown=gas==cal_unknown  # all 3 levels, different source gas
                test_known=np.isin(gas,known)&(ppb==heldout_level)
                test_unknown=gas==unknown  # all 3 levels; unknown never in train or calibration
                assert (train.sum(),calibration_known.sum(),calibration_unknown.sum(),
                    test_known.sum(),test_unknown.sum())==(32,8,15,20,15)
                assert not any((train&test_unknown)|(calibration_unknown&test_unknown))
                for mode,cols in MODES.items():
                    model=softmax_model(X[train][:,cols],gas[train],known)
                    c_know=np.max(model.predict_proba(X[calibration_known][:,cols]),axis=1)
                    c_unk=np.max(model.predict_proba(X[calibration_unknown][:,cols]),axis=1)
                    threshold=learn_open_threshold(c_know,c_unk)
                    def evaluate(mask,kind):
                        idx=np.where(mask)[0]
                        probs=model.predict_proba(X[idx][:,cols])
                        top=np.max(probs,axis=1)
                        predicted=model.classes_[np.argmax(probs,axis=1)]
                        accepted=top>=threshold
                        for j,i in enumerate(idx):
                            classified.append(dict(mode=mode,unseen_test_gas=unknown,
                                independent_calibration_unknown_gas=cal_unknown,
                                test_known_level_ppb=int(heldout_level),test_type=kind,
                                original_row=int(i),real_source_label=gas[i],
                                predicted_source_class=predicted[j],
                                score=float(top[j]),threshold_from_disjoint_calibration=threshold,
                                decision="KNOWN_CANDIDATE" if accepted[j] else "ABSTAIN",
                                accepted_unknown_error=bool(kind=="UNSEEN" and accepted[j]),
                                accepted_correct_known=bool(kind=="KNOWN" and accepted[j] and predicted[j]==gas[i]),
                                tank_G3=False))
                        return idx,accepted,predicted
                    known_idx,ka,kpred=evaluate(test_known,"KNOWN")
                    unk_idx,ua,upred=evaluate(test_unknown,"UNSEEN")
                    folds.append(dict(mode=mode,unseen_test_gas=unknown,
                        disjoint_calibration_unknown_gas=cal_unknown,
                        heldout_known_concentration_ppb=heldout_level,
                        train_exposures=32,cal_known=8,cal_other_unknown=15,
                        test_known=20,test_unknown=15,threshold=float(threshold),
                        known_correct_accepted=int(np.sum(ka&(kpred==gas[known_idx]))),
                        known_accepted=int(np.sum(ka)),
                        unknown_rejected=int(np.sum(~ua)),
                        unknown_falsely_accepted=int(np.sum(ua)),
                        unknown_rejection_rate=float(np.mean(~ua)),
                        known_correct_accept_fraction=float(np.mean(ka&(kpred==gas[known_idx]))),
                        heldout_unit_not_available=True))
    assert len(folds)==2*6*5*3==180
    assert len(classified)==180*(20+15)
    return folds,classified

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--features",required=True)
    p.add_argument("--manifest",required=True)
    p.add_argument("--out",required=True)
    a=p.parse_args()
    d=load(a.features,a.manifest)
    root=pathlib.Path(a.out)
    root.mkdir(parents=True,exist_ok=True)
    fingerprints,classification,pred=heldout_concentration(d)
    folds,records=open_set(d)
    dump_csv(root/"next_uci_mq135_training_fingerprints.csv",fingerprints,list(fingerprints[0]))
    dump_csv(root/"next_uci_concentration_holdout_metrics.csv",classification,list(classification[0]))
    dump_csv(root/"next_uci_concentration_holdout_predictions.csv",pred,list(pred[0]))
    dump_csv(root/"next_uci_open_set_folds.csv",folds,list(folds[0]))
    dump_csv(root/"next_uci_open_set_decisions.csv",records,list(records[0]))
    grouped={}
    for mode in MODES:
        cc=[v for v in classification if v["mode"]==mode]
        oo=[v for v in folds if v["mode"]==mode]
        grouped[mode]=dict(
            concentration_heldout_macro_class_accuracy=float(np.mean([x["class_accuracy"] for x in cc])),
            source_training_quantitative_oracle_mae_ppb=float(np.mean([x["oracle_concentration_mae_ppb"] for x in cc])),
            concentration_heldout_predicted_class_ppb_mae=float(np.mean([x["predicted_identity_conditional_mae_ppb"] for x in cc])),
            open_set_unseen_gas_rejection_fraction=float(np.mean([x["unknown_rejection_rate"] for x in oo])),
            open_set_known_correct_accepted_fraction=float(np.mean([x["known_correct_accept_fraction"] for x in oo])))
    summary=dict(schema="chemcore.next.uci1081_temporal_g2_source_fingerprint/1",
        source_root="MQ-EXT-15_UCI1081",original_zip_sha256=d["manifest"]["original_archive_sha256"],
        original_source_csv_sha256=d["manifest"]["original_csv_sha256"],
        n_original_exposures=90,n_source_gases=6,n_source_levels=3,
        concentration_units="ppb_not_Tank_ppm",
        mq135_source_window_features=(4,14,24),
        temporal_train_only_fingerprints=len(fingerprints),
        grouped_heldout_concentration_folds=len(classification),
        original_test_exposure_predictions=len(pred),
        distinct_partition_protocol_open_set_folds=len(folds),
        open_set_test_rows_reused_across_disjoint_calibration_splits=len(records),
        metrics_by_source_sensor_mode=grouped,
        no_random_row_train_test_leak=True,unknown_test_gas_never_in_training_or_threshold_calibration=True,
        no_tank_r0=True,no_Tank_G3=True,no_source_ppb_as_device_certified_ppm=True,
        no_new_independent_exposure_claim=True,source_fingerprint_models_activated_in_ChemCoreNext_evaluate=False,
        limits=["This is a single externally collected 10-channel device, MQ135 only 3 windows compatible by sensor name but not device/R0",
                "train/cal/test split by full exposure and source concentration; no device/day metadata for truly independent device validation",
                "90 original exposure rows reused in multiple predefined folds, not new exposures",
                "quantitative gas-conditioned expert is an oracle research comparison when gas label known, not an identity detector",
                "no certified Tank G3, no field gas alarms"])
    (root/"next_uci_source_experiment.json").write_text(
        json.dumps(summary,indent=2,allow_nan=False)+"\n",encoding="utf8")
    for mode,v in grouped.items():
        print("NEXT_UCI_SOURCE_SCORE",mode,
            "HOLDOUT_CLASS_ACCURACY=%.6f"%v["concentration_heldout_macro_class_accuracy"],
            "UNKNOWN_REJECTION=%.6f"%v["open_set_unseen_gas_rejection_fraction"],
            "KNOWN_CORRECT_ACCEPT=%.6f"%v["open_set_known_correct_accepted_fraction"])
    print("CHEMCORE_NEXT_UCI_REAL_SOURCE_PASS source_exposures=90 temporal_fingerprints=%d ppb_folds=%d open_set_folds=%d G3=0"%
        (len(fingerprints),len(classification),len(folds)))

if __name__=="__main__":
    main()
