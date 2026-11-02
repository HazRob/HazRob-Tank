#!/usr/bin/env python3
"""ChemCore Next descriptive/no-gas/complex-smoke category source fingerprints.

A *single contiguous source recording* of 6400 ADC rows, 4 categories.
Class segments, even blocked in time, are NOT independently labelled exposures.
Only source-class signal classification; no pure gas or ppm.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import pathlib
from collections import Counter
import numpy as np
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression

BLOB_SHA="451fdd05a18db8531abc219089de071ccc0dd461"
CLASSES=("NoGas","Perfume","Smoke","Mixture")
CH=("MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135")
TANK_CH=("MQ2","MQ3","MQ5","MQ7","MQ8","MQ135")
ORIGINAL_COLUMNS=("Serial Number",*CH,"Gas")
MODES={"MQ_SOURCE_7CHANNEL":CH,"TANK_OVERLAP_6CHANNEL":TANK_CH}
SOURCE_ROOT="MQ-EXT-03_MULTIMODAL_ORIGINAL_OR_MIRROR"

def dump(path,rows,keys):
    with path.open("w",encoding="utf8",newline="") as fh:
        w=csv.DictWriter(fh,keys,extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)

def load(path):
    raw=pathlib.Path(path).read_bytes()
    blobhash=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if blobhash!=BLOB_SHA:
        raise ValueError("FAIL_CLOSED_ORIGINAL_GITHUB_BLOB_MISMATCH "+blobhash)
    with pathlib.Path(path).open(encoding="utf8",newline="") as f:
        rd=csv.DictReader(f)
        assert tuple(rd.fieldnames)==ORIGINAL_COLUMNS
        arr=list(rd)
    if len(arr)!=6400:raise ValueError("source count must be 6400")
    label=np.asarray([r["Gas"] for r in arr],dtype=object)
    serial=np.asarray([int(r["Serial Number"]) for r in arr])
    X=np.asarray([[float(r[c]) for c in CH] for r in arr])
    assert set(label)==set(CLASSES)
    if not np.isfinite(X).all() or (X<0).any() or (X>1023).any():
        raise ValueError("invalid 10-bit ADC source range")
    for c in CLASSES:
        indices=np.where(label==c)[0]
        assert len(indices)==1600
        assert (serial[indices]==np.arange(1600)).all(), "source serial resets per category"
        assert np.all(np.diff(indices)==1), "each class is ONE contiguous recording, not repeats"
    return label,serial,X,blobhash

def sigmoid_model(data,labels):
    model=make_pipeline(StandardScaler(),LogisticRegression(
        C=0.5,max_iter=500,solver="lbfgs"))
    model.fit(data,labels)
    return model

def score_fold(features, labels, train, cal, test):
    assert not (train&test).any()
    model=sigmoid_model(features[train],labels[train])
    calibration=model.predict_proba(features[cal]).max(axis=1)
    threshold=float(np.quantile(calibration,0.10))
    test_scores=model.predict_proba(features[test])
    guessed=model.classes_[test_scores.argmax(axis=1)]
    probs=test_scores.max(axis=1)
    accepted=probs>=threshold
    return float(np.mean(guessed==labels[test])), float(np.mean(accepted)), \
           float(np.mean((guessed==labels[test])&accepted)),threshold,guessed,accepted,probs

def blocked_class_replay(label,serial,X):
    rows=[];models=[];profiles=[]
    block=serial//100
    assert set(block)==set(range(16))
    for hold in range(16):
        cal_block=(hold-1)%16
        train=(block!=hold)&(block!=cal_block)
        calibration=(block==cal_block)
        test=(block==hold)
        assert (int(train.sum()),int(calibration.sum()),int(test.sum()))==(5600,400,400)
        for mode,columns in MODES.items():
            cols=[CH.index(x) for x in columns]
            parts=X[:,cols]
            accuracy,accepted,correctaccepted,threshold,guess,accept,probs=score_fold(
                parts,label,train,calibration,test)
            models.append(dict(mode=mode,heldout_contiguous_block=hold,
                calibrated_contiguous_block=cal_block,
                train_source_rows=5600,calibration_rows=400,test_rows=400,
                test_class_accuracy=accuracy,accepted_fraction=accepted,
                correct_and_accepted_fraction=correctaccepted,
                calibrated_probability_threshold=threshold,
                independent_source_sessions_established=False))
            for cls in CLASSES:
                sample=parts[train&(label==cls)]
                assert len(sample)==1400
                profiles.append(dict(original_source_root=SOURCE_ROOT,
                    original_raw_blob_sha=BLOB_SHA,source_class=cls,mode=mode,
                    feature_channel_order="|".join(columns),
                    training_source_rows=int(len(sample)),
                    training_fingerprint_median_adc_json=json.dumps(
                        np.median(sample,axis=0).tolist(),separators=(",",":")),
                    training_fingerprint_mad_adc_json=json.dumps(
                        np.median(np.abs(sample-np.median(sample,axis=0)),axis=0).tolist(),
                        separators=(",",":")),
                    heldout_serial_block=hold,gas_identity="SOURCE_CLASS_ONLY",
                    class_to_molecular_CAS_mapping_approved=False,
                    concentration_ppm_available=False,
                    independent_exposure_qualification="NOT_VERIFIED",
                    actual_Tank_G3=False))
            ids=np.where(test)[0]
            for j,i in enumerate(ids):
                rows.append(dict(source_row_id=int(i),
                    source_serial=int(serial[i]),mode=mode,
                    source_class=label[i],heldout_block=hold,
                    model_class=guess[j],accepted=bool(accept[j]),score=float(probs[j]),
                    research_threshold=threshold,source_category_only=True,
                    molecule_identification=False,ppm_claim=False))
    assert len(models)==32 and len(profiles)==128 and len(rows)==12800
    return models,profiles,rows

def unknown_class_replay(label,serial,X):
    """Calibrated unknown and test unknown MUST be two DIFFERENT source classes."""
    stats=[]
    block=serial//100
    for unknown in CLASSES:
        for independent_unknown in CLASSES:
            if unknown==independent_unknown:continue
            known=[g for g in CLASSES if g not in (unknown,independent_unknown)]
            for heldout in (1,5,9,13):
                cal_block=(heldout-1)%16
                train=np.isin(label,known)&(block!=heldout)&(block!=cal_block)
                cal_known=np.isin(label,known)&(block==cal_block)
                test_known=np.isin(label,known)&(block==heldout)
                cal_unknown=label==independent_unknown
                test_unknown=label==unknown
                assert (int(train.sum()),int(cal_known.sum()),int(test_known.sum()))==(2800,200,200)
                assert int(cal_unknown.sum())==1600 and int(test_unknown.sum())==1600
                assert not np.any(train&test_unknown)
                for mode,columns in MODES.items():
                    cols=[CH.index(ch) for ch in columns]
                    parts=X[:,cols]
                    model=sigmoid_model(parts[train],label[train])
                    calK=model.predict_proba(parts[cal_known]).max(axis=1)
                    calU=model.predict_proba(parts[cal_unknown]).max(axis=1)
                    vals=np.sort(np.unique(np.r_[calK,calU]))
                    thresholds=np.r_[0.0,(vals[:-1]+vals[1:])/2,1.0]
                    ranked=[((np.mean(calK>=t)+np.mean(calU<t))/2,float(t)) for t in thresholds]
                    _,threshold=max(ranked,key=lambda v:(v[0],v[1]))
                    known_prob=model.predict_proba(parts[test_known])
                    unknown_prob=model.predict_proba(parts[test_unknown])
                    known_cls=model.classes_[known_prob.argmax(axis=1)]
                    known_accept=known_prob.max(axis=1)>=threshold
                    reject=unknown_prob.max(axis=1)<threshold
                    stats.append(dict(mode=mode,heldout_contiguous_block=heldout,
                        test_unknown_class=unknown,
                        separately_calibrated_unknown_class=independent_unknown,
                        source_train_rows=2800,calibration_known_rows=200,
                        calibration_unknown_rows=1600,test_known_rows=200,
                        test_unknown_rows=1600,threshold=float(threshold),
                        unknown_rejected_fraction=float(reject.mean()),
                        known_correct_accepted_fraction=float(np.mean(
                            (known_cls==label[test_known])&known_accept)),
                        no_independent_device_or_session=True,
                        no_molecular_gas_identification=True))
    assert len(stats)==96
    return stats

def main():
    arg=argparse.ArgumentParser()
    arg.add_argument("--source",required=True)
    arg.add_argument("--out",required=True)
    args=arg.parse_args()
    out=pathlib.Path(args.out)
    out.mkdir(parents=True,exist_ok=True)
    label,serial,X,sha=load(args.source)
    folds,profiles,pred=blocked_class_replay(label,serial,X)
    open_set=unknown_class_replay(label,serial,X)
    dump(out/"next_mq6400_source_class_fingerprints.csv",profiles,list(profiles[0]))
    dump(out/"next_mq6400_block_holdout_metrics.csv",folds,list(folds[0]))
    dump(out/"next_mq6400_block_holdout_predictions.csv",pred,list(pred[0]))
    dump(out/"next_mq6400_source_class_open_set.csv",open_set,list(open_set[0]))
    summary={}
    for mode in MODES:
        a=[x for x in folds if x["mode"]==mode]
        b=[x for x in open_set if x["mode"]==mode]
        summary[mode]=dict(blocked_accuracy=float(np.mean([x["test_class_accuracy"] for x in a])),
            blocked_correct_and_accepted=float(np.mean([x["correct_and_accepted_fraction"] for x in a])),
            unknown_class_rejection=float(np.mean([x["unknown_rejected_fraction"] for x in b])),
            known_correct_accepted_under_unknown=float(np.mean([x["known_correct_accepted_fraction"] for x in b])))
    report=dict(schema="chemcore.next.mq6400_categorical_source_stress/1",
        original_github_blob_sha1=sha,original_mendeley_doi="10.17632/zkwgkjkjn9.2",
        source_root="MQ-EXT-03",source_rows=6400,
        source_classes=list(CLASSES),physical_source_recordings_demonstrable=4,
        independently_replicated_exposures=0,source_sensor_channels=list(CH),
        tank_model_overlapping_channels=list(TANK_CH),
        source_class_fingerprints_training_only=len(profiles),
        train_cal_test_contiguous_block_folds=len(folds),
        open_set_distinct_calibration_unknown_folds=len(open_set),
        original_sensor_rows_reused_across_folds=True,
        metrics=summary,
        limitations=["Only source CLASS labels, not molecular chemical CAS, LPG, NOx or concentrations",
        "Original class recordings contiguous; blocked time slices are not statistically independent experimental exposures",
        "Source files GitHub mirror and Mendeley represent same origin, not separate sources",
        "ADC 10 bit 0..1023 cannot be directly mapped to Tank ADS1115 without electrical scaling",
        "Source has MQ6 but Tank does not, and lacks Tank MQ4/MQ9; benchmark six overlapping MODELS, not same device",
        "Result is G1 signal/software stress and does not qualify G2 independent exposure or Tank G3",
        "Abstain in deployed ChemCore Next; never show GHS from model classifications"],
        gas_identity_confirmed=False,ppm_qualified=False,physical_Tank_G3=False,
        activated_in_ChemCoreNext_evaluate=False)
    (out/"next_mq6400_source_category_experiment.json").write_text(
        json.dumps(report,indent=2,allow_nan=False)+"\n",encoding="utf8")
    for mode,s in summary.items():
        print("NEXT_MQ6400_CATEGORY_SCORE",mode,
            "BLOCKED_ACCURACY=%.6f"%s["blocked_accuracy"],
            "UNKNOWN_REJECTION=%.6f"%s["unknown_class_rejection"])
    print("CHEMCORE_NEXT_MQ6400_CATEGORY_PASS rows=6400 fingerprints=128 folds=32 open_folds=96 G3=0 G2_independent=0")

if __name__=="__main__":
    main()
