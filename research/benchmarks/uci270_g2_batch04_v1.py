"""G2-04 ten technique IDs on ORIGINAL measured UCI270 drift source.

07.1,07.4-07.10,03.4 executed on real original 13,910 labelled
exposures with strict caveats; 07.2 BLOCKED (one physical sensor array).
10 research IDs reviewed, no real HazRob Tank MQ calibration.
Reference: UCI270 DOI 10.24432/C5MK6M, source archive SHA pinned.

The UCI donor's descriptive page has a research-only/no-commercial clause
that conflicts with UCI current CC BY 4.0 listing. Keep exclusively research.
"""
import argparse
import collections
import json
import math
import statistics
from pathlib import Path
from uci1081_actual_modules_v1 import repo_module
from uci270_source_adapter_v1 import parse_original,select_sensor_features,DOI,ZIP_SHA256,LABELS

IDS=("03.4","07.1","07.2","07.4","07.5","07.6","07.7","07.8","07.9","07.10")
SENSOR_FEATURES=tuple(f"TGS_{j+1:02d}_normalized_DR" for j in range(16))


def distance(a,b):
    return math.sqrt(sum((x-y)**2 for x,y in zip(a,b)))

def mean_vector(rows):
    if not rows or not rows[0]:raise ValueError("nonempty vectors required")
    return [statistics.fmean(row[j] for row in rows) for j in range(len(rows[0]))]

def source_and_target(rows):
    src=[r for r in rows if r["batch"] in (1,2)]
    tgt=[r for r in rows if r["batch"]==10]
    if len(src)!=1689 or len(tgt)!=3600:raise AssertionError("source/target counts")
    return src,tgt

def labels_report(rows):
    d={}
    for batch in range(1,11):
        b=[r for r in rows if r["batch"]==batch]
        d[str(batch)]={"n":len(b),
                       "counts":dict(sorted(collections.Counter(r["gas"] for r in b).items())),
                       "unique_gas_concentrations":len({(r["gas"],r["ppm"]) for r in b})}
    return d

def chronological_batch_gate(rows):
    mod=repo_module("07.1","cross_session_tests_v1.py")
    source=[{"session":f"B{r['batch']:02d}"} for r in rows]
    fs=mod.folds(source)
    if len(fs)!=10:raise AssertionError("ten temporal batch splits")
    batches={i:f["test_indices"] for i,f in enumerate(fs)}
    if sorted(i for f in fs for i in f["test_indices"])!=list(range(len(rows))):
        raise AssertionError("missing heldout records")
    if any(set(f["train_indices"])&set(f["test_indices"]) for f in fs):
        raise AssertionError("group leakage")
    return {"id":"07.1","status":"EXECUTED_BATCH_PROXY_NOT_SESSIONS",
            "n_folds":len(fs),"n_holdout_exposures_per_temporal_batch":[len(batches[k]) for k in sorted(batches)],
            "note":"Source batch ID (aggregate months) is NOT an independently tracked session. Splits are valid by batch."}

def device_gate(rows):
    mod=repo_module("07.2","cross_device_tests_v1.py")
    # Entire source used the same sensor array (one instrument), unlike
    # chronological periods. Explicitly refuse invented device IDs.
    try:
        mod.leave_one_device_out([{"device":"single_original_source_array"} for _ in range(2)])
    except ValueError:
        result="BLOCKED_ONE_PHYSICAL_DEVICE"
    else:
        raise AssertionError("single-device holdout erroneously accepted")
    return {"id":"07.2","status":result,"n_independent_verified_devices":1,
            "note":"Do not reinterpret source chronological batches as different robots."}

def matched_standard(rows):
    src,tgt=source_and_target(rows)
    sr=collections.defaultdict(list);tg=collections.defaultdict(list)
    for r in src:
        sr[(r["gas"],r["ppm"])].append(r)
    for r in tgt:
        tg[(r["gas"],r["ppm"])].append(r)
    candidates=sorted((min(len(sr[k]),len(tg[k])),len(sr[k])+len(tg[k]),k)
                       for k in sr.keys()&tg.keys() if len(sr[k])>=3 and len(tg[k])>=4)
    if not candidates: return None
    _,_,key=candidates[-1]
    return {"gas":key[0],"ppm":key[1],"before":sr[key],"after":tg[key],
            "candidate_matched_gas_ppm_pairs":len(candidates)}

def shifts_and_offsets(rows):
    detected=repo_module("07.4","drift_detection_v1.py")
    compensated=repo_module("07.5","drift_compensation_v1.py")
    pair=matched_standard(rows)
    if pair is None:
        return {"07.4":{"status":"BLOCKED_NO_MATCHED_REFERENCE"},
                "07.5":{"status":"BLOCKED_NO_MATCHED_REFERENCE"}}
    before=[select_sensor_features(r) for r in pair["before"]]
    after=[select_sensor_features(r) for r in pair["after"]]
    n_ref=max(2,len(after)//2)
    calibr=after[:n_ref];heldout=after[n_ref:]
    if not heldout: raise AssertionError("no matched target holdout")
    basemodel=detected.fit_baseline(before)
    flags=[detected.detect(basemodel,row,threshold=3.)["status"]=="SHIFT_FLAG" for row in heldout]
    mean_before=mean_vector(before)
    mean_after_cal=mean_vector(calibr)
    offsets=compensated.estimate_offsets(mean_before,mean_after_cal)
    corrected=[compensated.compensate(offsets,vec) for vec in heldout]
    before_distance=[distance(r,mean_before) for r in heldout]
    after_distance=[distance(r,mean_before) for r in corrected]
    common={"matched_gas":pair["gas"],"matched_reference_ppm":pair["ppm"],
            "source_batches":[1,2],"target_batch":10,
            "n_source_reference_exposures":len(before),"n_target_calibration_exposures":len(calibr),
            "n_target_heldout_exposures":len(heldout),
            "total_matching_gas_and_concentration_pairs":pair["candidate_matched_gas_ppm_pairs"]}
    return {
        "07.4":{"status":"EXECUTED_MATCHED_REFERENCE_BATCH_SHIFT",**common,
                "heldout_threshold_z":3.,"heldout_shift_flag_fraction":sum(flags)/len(flags),
                "warning":"Same gas+ppm only, but temperature and instrument operational conditions uncontrolled; shift ≠ proven drift."},
        "07.5":{"status":"EXECUTED_MATCHED_REFERENCE_OFFSET_DIAGNOSTIC",**common,
                "mean_heldout_distance_to_source_centroid_before":statistics.fmean(before_distance),
                "mean_heldout_distance_to_source_centroid_after":statistics.fmean(after_distance),
                "warning":"Additive matching calibrated using distinct target reference exposures. This does not prove transfer to other gases."},
    }

def target_adaptation(rows):
    mod=repo_module("07.6","domain_adaptation_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    src,tgt=source_and_target(rows)
    srcx=[select_sensor_features(r) for r in src]
    targetx=[select_sensor_features(r) for r in tgt]
    fit_source=scaler.fit_scaler(srcx)
    source_train=scaler.transform(srcx,fit_source)
    calibrated=[row for i,row in enumerate(targetx) if i%5==0]
    heldout=[row for i,row in enumerate(targetx) if i%5!=0]
    calibrated=scaler.transform(calibrated,fit_source)
    heldout=scaler.transform(heldout,fit_source)
    shift=mod.fit_alignment(source_train,calibrated)
    before=mean_vector(heldout)
    after=mean_vector([mod.target_to_source(shift,v) for v in heldout])
    srcmean=mean_vector(source_train)
    return {"id":"07.6","status":"EXECUTED_UNSUPERVISED_MARGINAL_ALIGNMENT",
            "source_exposures":len(source_train),"target_unlabelled_calibration_exposures":len(calibrated),
            "target_heldout_exposures":len(heldout),
            "heldout_centroid_distance_before":distance(before,srcmean),
            "heldout_centroid_distance_after":distance(after,srcmean),
            "warning":"Alignment can erase analyte signal; source/target class distribution and concentrations may differ; no classification claim."}

def centroid_predict(model,vector):
    return min(model["centroids"],key=lambda label:(sum((a-b)**2 for a,b in
                                                zip(vector,model["centroids"][label])),label))

def transfer_head(rows):
    mod=repo_module("07.7","transfer_learning_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    src,tgt=source_and_target(rows)
    srcx=[select_sensor_features(r) for r in src]
    fitted_scaler=scaler.fit_scaler(srcx)
    scaled_source=scaler.transform(srcx,fitted_scaler)
    source_head=mod.fit_head(scaled_source,[r["gas"] for r in src])
    by_gas=collections.Counter()
    supervised_target=[];heldout=[]
    for row in tgt:
        if by_gas[row["gas"]]<10:
            supervised_target.append(row)
            by_gas[row["gas"]]+=1
        else:
            heldout.append(row)
    if len(supervised_target)!=60 or len(heldout)!=3540:
        raise AssertionError("need exactly 10 labelled target exposures per gas")
    adapted_head=mod.fit_head(
        scaler.transform([select_sensor_features(r) for r in supervised_target],fitted_scaler),
        [r["gas"] for r in supervised_target])
    target_features=scaler.transform([select_sensor_features(r) for r in heldout],fitted_scaler)
    base=sum(mod.predict(source_head,x)==r["gas"] for r,x in zip(heldout,target_features))
    adapted=sum(mod.predict(adapted_head,x)==r["gas"] for r,x in zip(heldout,target_features))
    return {"id":"07.7","status":"EXECUTED_LABELLED_TARGET_NEAREST_CENTROID_HEAD",
            "source_train_exposures":len(src),"target_reference_labels_used_to_fit_head":60,
            "target_heldout_exposures":len(heldout),
            "frozen_source_head_correct":base,
            "adapted_target_head_correct":adapted,
            "frozen_source_head_accuracy":base/len(heldout),
            "adapted_target_head_accuracy":adapted/len(heldout),
            "warning":"Nearest-centroid supervised head, NOT neural network transfer/fine-tuning. Target calibration labels cannot be assumed available operationally."}

def active_candidates(rows):
    mod=repo_module("07.8","active_learning_doe_v1.py")
    head=repo_module("07.7","transfer_learning_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    src,tgt=source_and_target(rows)
    srcx=[select_sensor_features(r) for r in src]
    sc=scaler.fit_scaler(srcx)
    model=head.fit_head(scaler.transform(srcx,sc),[r["gas"] for r in src])
    # Target labels NEVER used to construct probabilities or select candidates.
    # Conversion of distances to probabilities is an UNCALIBRATED heuristic.
    candidate_idx=list(range(0,len(tgt),12))
    feats=scaler.transform([select_sensor_features(tgt[i]) for i in candidate_idx],sc)
    candidates=[]
    for idx,x in zip(candidate_idx,feats):
        vals=[-distance(x,mean) for mean in model["centroids"].values()]
        offset=max(vals);weights=[math.exp(v-offset) for v in vals];den=sum(weights)
        candidates.append({"ppm":tgt[idx]["ppm"],"probabilities":[v/den for v in weights]})
    rank=mod.rank_candidates(candidates,concentration_weight=0.1)
    selected=[tgt[candidate_idx[r["index"]]] for r in rank[:30]]
    fixed_first=[tgt[i] for i in candidate_idx[:30]]
    return {"id":"07.8","status":"EXECUTED_UNCALIBRATED_UNCERTAINTY_RANKING",
            "candidate_exposures":len(candidate_idx),"ranked_top_n":30,
            "unique_gases_top30":len({r["gas"] for r in selected}),
            "unique_gases_unranked_first30":len({r["gas"] for r in fixed_first}),
            "top30_gas_counts":dict(collections.Counter(r["gas"] for r in selected)),
            "warnings":["Input pseudo-probabilities are uncalibrated normalized distances.",
                        "This is offline ranking only; no experimental lab acquisition or genuine active learning improvement.",
                        "Known ppm labels are supplied as metadata, not estimated by a sensor."]}

def continual_batches(rows):
    mod=repo_module("07.9","continual_learning_v1.py")
    baseline=repo_module("07.7","transfer_learning_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    first=[r for r in rows if r["batch"]==1]
    fit=scaler.fit_scaler([select_sensor_features(r) for r in first])
    first_scaled=scaler.transform([select_sensor_features(r) for r in first],fit)
    initial=baseline.fit_head(first_scaled,[r["gas"] for r in first])
    s=mod.new_state(16)
    # train initial state using verified source labels, same batch 1
    by_gas=collections.Counter()
    for row,x in zip(first,first_scaled):
        if by_gas[row["gas"]]>=10:continue
        s=mod.update(s,row["gas"],x,approved=True)
        by_gas[row["gas"]]+=1
    results=[]
    for batch in range(2,11):
        target=[r for r in rows if r["batch"]==batch]
        feats=scaler.transform([select_sensor_features(r) for r in target],fit)
        # evaluate BEFORE any labels of current chronological batch are used
        current_correct=sum(centroid_predict({"centroids":s["means"]},x)==r["gas"]
                            for r,x in zip(target,feats))
        frozen_correct=sum(baseline.predict(initial,x)==r["gas"]
                           for r,x in zip(target,feats))
        added=collections.Counter()
        for r,x in zip(target,feats):
            if added[r["gas"]]>=10:continue
            s=mod.update(s,r["gas"],x,approved=True)
            added[r["gas"]]+=1
        results.append({"target_batch":batch,"n_test":len(target),
                        "correct_incremental_before_target_update":current_correct,
                        "correct_initial_frozen":frozen_correct,
                        "labelled_reference_updates_after_test":sum(added.values())})
    return {"id":"07.9","status":"EXECUTED_SEQUENTIAL_HUMAN_APPROVED_LABEL_PROTOTYPES",
            "batch_results":results,
            "n_reference_labelled_updates":len(s["audit"]),
            "warning":"Original labels used only AFTER batch test; still a nearest-prototype update, NOT continual neural learner; chronological sensor drift and concentration confounded."}

def resource_budget():
    mod=repo_module("07.10","resource_profiling_v1.py")
    # Nearest-centroid head from 07.7: 6 classes, 16 features, 32bit floats.
    out=mod.estimate(parameters=6*16,bytes_per_parameter=4,
                     activations_bytes=16*4,operations_per_inference=6*16*3,
                     frequency_hz=1)
    return {"id":"07.10","status":"EXECUTED_ANALYTICAL_ONLY_NOT_DEVICE_PROFILE",
            "head_parameters":96,**out,
            "warning":"Analytical estimate only; no ESP32 MCU latency/RAM/camera/WiFi/power measurement."}

def binary_discriminant(rows):
    mod=repo_module("03.4","discriminant_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    src,tgt=source_and_target(rows)
    selected={"ethanol","acetone"}
    train=[r for r in src if r["gas"] in selected]
    test=[r for r in tgt if r["gas"] in selected]
    if min(collections.Counter(r["gas"] for r in train).values())<2:
        raise AssertionError("binary train requires 2+ per class")
    xtr=[select_sensor_features(r) for r in train]
    xt=[select_sensor_features(r) for r in test]
    sc=scaler.fit_scaler(xtr)
    ztr=scaler.transform(xtr,sc)
    zt=scaler.transform(xt,sc)
    labels=[r["gas"] for r in train]
    stats={}
    for name,fit in [("LDA",mod.fit_binary_lda(ztr,labels)),
                     ("PLS1DA",mod.fit_binary_plsda(ztr,labels))]:
        p=mod.project(zt,fit)
        guesses=[fit["classes"][1] if z>0 else fit["classes"][0] for z in p]
        stats[name]={"correct":sum(pred==r["gas"] for pred,r in zip(guesses,test)),
                     "n_test":len(test),
                     "accuracy":sum(pred==r["gas"] for pred,r in zip(guesses,test))/len(test)}
    return {"id":"03.4","status":"EXECUTED_BINARY_ONLY_DISCRIMINANT_ON_TEMPORAL_BATCH_HOLDOUT",
            "source_batches":[1,2],"target_batch":10,
            "gas_pair":["acetone","ethanol"],"n_train":len(train),"n_test":len(test),
            "estimators":stats,
            "warning":"Binary LDA/PLS1-DA only; not multiclass. Same instrument and unadjusted gas concentration shifts."}

def run(path):
    rows,meta=parse_original(path)
    if sorted({r["batch"] for r in rows})!=list(range(1,11)):
        raise AssertionError("bad source batch groups")
    return {"project":"HazRob Tank external G2 drift benchmark",
            "source_dataset":"UCI270",
            "source_doi":DOI,"source_zip_sha256":ZIP_SHA256,
            "source_licensing_note":"UCI webpage CC BY 4.0 but older description explicitly research-only/no commercial; use for research only, check terms before any redistribution/commercialization.",
            "sensor_system":"16 source TGS gas sensors (8 derived features each), NOT Tank MQ",
            "source_overlapping_uci224":"UCI224 is same original exposure collection; DO NOT double count",
            "record_count":len(rows),"feature_count":128,
            "profile":meta,
            "gas_counts_by_temporal_batch":labels_report(rows),
            "technique_ids":list(IDS),
            "03.4":binary_discriminant(rows),
            "07.1":chronological_batch_gate(rows),
            "07.2":device_gate(rows),
            "07.4_07.5":shifts_and_offsets(rows),
            "07.6":target_adaptation(rows),
            "07.7":transfer_head(rows),
            "07.8":active_candidates(rows),
            "07.9":continual_batches(rows),
            "07.10":resource_budget(),
            "limits":["Data are 128 derived descriptors, not 128 independent sensors or real-time raw streams.",
                      "Ten chronological batches, one source sensor array; independent Tank hardware validation is absent.",
                      "Unmatched gas/concentration and environment may confound purported drift.",
                      "Adaptive head requires 60 labelled exposures from target batch.",
                      "No industrial hazard identification, ppm instrument or gas clearance claims."]}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("uci270_original_zip")
    p.add_argument("--output",default="uci270_g2_batch04_results.json")
    a=p.parse_args()
    result=run(a.uci270_original_zip)
    Path(a.output).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    print("SOURCE OK SHA256",ZIP_SHA256,"records",result["record_count"],"derived features",result["feature_count"])
    print("TECHNIQUES",result["technique_ids"])
    print("03.4",result["03.4"])
    print("07.1",result["07.1"])
    print("07.2",result["07.2"])
    print("07.4/07.5",result["07.4_07.5"])
    print("07.6",result["07.6"])
    print("07.7",result["07.7"])
    print("07.8",result["07.8"])
    print("07.9",result["07.9"])
    print("07.10",result["07.10"])
if __name__=="__main__":
    main()
