"""G2-02 — measured UCI1081 external-array diagnostics across 10 technique IDs.

The source is authentic measured external gas-sensor data, NOT HazRob Tank MQ8.
All models are imported from versioned research/techniques modules. The Tank-only
02.5 schema is explicitly BLOCKED rather than fabricating MQ channels.
No real Tank calibration/identity/ppm/safety claims.
"""
import argparse
import collections
import json
import math
import statistics
from pathlib import Path
from uci1081_actual_modules_v1 import (
    SENSORS, GASES, PPB, parse_archive, features, splits, repo_module,
)

IDS=("02.5","03.3","03.6","03.7","03.8","04.10","05.9","06.3","06.5","07.3")
FEATURE_NAMES=tuple("delta_"+str(i)+"_"+sensor
    for i in ("early","late") for sensor in SENSORS)


def fit_softmax_logits(model,rows):
    """Reconstruct ordered logits using the exact 04.4 research coefficients."""
    n=model["features"]
    out=[]
    for row in rows:
        if len(row)!=n: raise ValueError("feature mismatch")
        z=[1.0]+[(row[j]-model["mean"][j])/model["scale"][j] for j in range(n)]
        out.append([sum(w*v for w,v in zip(weights,z)) for weights in model["weights"]])
    return out


def external_sensor_schema_gate():
    module=repo_module("02.5","fingerprints_v1.py")
    if len(module.SENSORS)!=8 or module.SENSORS==SENSORS:
        raise AssertionError("unexpected Tank fingerprint sensor schema")
    try:
        module.build_fingerprint([1.]*len(SENSORS),[2.]*len(SENSORS),
                                 sensor_ids=SENSORS)
    except ValueError:
        pass
    else:
        raise AssertionError("02.5 silently accepted non-MQ Tank channels")
    return {"id":"02.5","status":"BLOCKED_INCOMPATIBLE_SENSOR_SCHEMA",
            "tested_real_MQ8_vectors":0,
            "source_sensor_count":len(SENSORS),"required_sensor_ids":list(module.SENSORS),
            "note":"UCI1081 external 10-channel raw data cannot substitute for eight measured Tank MQ resistances."}


def train_only_subset_and_overlap(rows,x_late,x_delta):
    overlap=repo_module("03.3","class_overlap_v1.py")
    subset=repo_module("03.6","sensor_subsets_v1.py")
    confound=repo_module("03.7","concentration_confounding_v1.py")
    identify=repo_module("03.8","identifiability_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    knn=repo_module("04.3","knn_v1.py")
    bench=repo_module("04.10","benchmarking_v1.py")
    unseen=repo_module("05.9","unseen_concentrations_v1.py")
    splitmod=repo_module("07.3","cross_concentration_tests_v1.py")
    y=[r["gas"] for r in rows]
    meta=[{"gas":r["gas"],"ppm":r["ppb"]/1000.,
           "group":"CONCENTRATION_"+str(int(r["ppb"]))+"ppb",
           "label":r["gas"],"source_row":r["source_row"]} for r in rows]
    units=sorted({r["ppb"]/1000. for r in rows})
    if units!=[0.05,0.1,0.2]:raise AssertionError("units conversion error")
    conc=confound.concentration_report(
        x_late,y,[r["ppb"]/1000. for r in rows],sensor_ids=SENSORS)
    fold_checks=splitmod.leave_one_level_out(meta)
    if [f["held_out_ppm"] for f in fold_checks]!=units:
        raise AssertionError("07.3 held-out levels mismatch")
    folds=splits(rows,"leave_one_concentration_ppb")
    fold_results=[]
    predictions={"subset_knn3":[],"all10_knn3":[]}
    truths=[]
    cnt_abstentions=collections.Counter()
    total_screen=collections.Counter()
    concentration_score=[]
    for fold,split in zip(folds,fold_checks):
        ti,vi=fold["train"],fold["test"]
        held=fold["heldout"]/1000.
        if not (held==split["held_out_ppm"] and
                set(ti)==set(split["train_indices"]) and
                set(vi)==set(split["test_indices"])):
            raise AssertionError("07.3 split inconsistent with original rows")
        # 04.10 grouped test only by measured *concentration level*, not
        # acquisition group; the latter is unprovided and cannot be invented.
        tr,te=bench.grouped_split(meta,holdout_groups={meta[vi[0]]["group"]})
        if {r["source_row"] for r in tr}!={rows[i]["source_row"] for i in ti}:
            raise AssertionError("04.10 wrong group split")
        if {r["source_row"] for r in te}!={rows[i]["source_row"] for i in vi}:
            raise AssertionError("04.10 wrong test group")
        s=scaler.fit_scaler([x_late[i] for i in ti])
        ztr=scaler.transform([x_late[i] for i in ti],s)
        zte=scaler.transform([x_late[i] for i in vi],s)
        labs=[y[i] for i in ti]
        overlap_result=overlap.overlap_report(ztr,labs)
        ranked=subset.compare_subsets(ztr,labs,SENSORS,min_size=1,max_size=2)
        if len(ranked)!=55:
            raise AssertionError("expected 10 choose 1 + 10 choose 2")
        chosen=ranked[0]
        xtr_sub=subset.select_subset(ztr,chosen)
        xte_sub=subset.select_subset(zte,chosen)
        model_subset=knn.fit_knn(xtr_sub,labs,chosen["sensor_ids"])
        model_all=knn.fit_knn(ztr,labs,SENSORS)
        guessed_subset=[knn.predict_knn(row,model_subset,k=3)["predicted_label"]
                        for row in xte_sub]
        guessed_full=[knn.predict_knn(row,model_all,k=3)["predicted_label"]
                      for row in zte]
        predictions["subset_knn3"].extend(guessed_subset)
        predictions["all10_knn3"].extend(guessed_full)
        truths.extend(y[i] for i in vi)
        # Conservative fixed research radius; NEVER fit using held-out labels.
        threshold=0.5
        collisions=identify.identifiability_report(
            ztr,labs,threshold=threshold)
        screens=[identify.screen_query(q,ztr,labs,threshold=threshold)
                 for q in zte]
        counts=collections.Counter(z["decision"] for z in screens)
        for reason,n in counts.items():total_screen[reason]+=n
        # 05.9 can score provided predictions; it does NOT fit a ppm estimator.
        # We supply concentration-blind geometric mean of two training levels
        # as explicit weak baseline with no sample features or gas oracle.
        partition=unseen.split_by_concentration(meta,[held])
        lo,hi=partition["train_range_ppm"]
        guess=math.sqrt(lo*hi)
        scored=unseen.score_predictions(
            [rows[i]["ppb"]/1000. for i in vi],[guess]*len(vi),
            train_range_ppm=(lo,hi))
        concentration_score.append({"held_out_ppb":fold["heldout"],
                                    "naive_prediction_ppm":guess,
                                    "score":scored})
        fold_results.append({
            "held_out_ppb":fold["heldout"],
            "n_train_exposures":len(ti),"n_test_exposures":len(vi),
            "03.3_train_ambiguity_fraction":overlap_result["ambiguous_fraction"],
            "03.6_n_subset_candidates_train_only":len(ranked),
            "03.6_selected_source_sensors":chosen["sensor_ids"],
            "03.6_train_ambiguity_fraction":chosen["ambiguous_fraction"],
            "03.6_subset_knn_correct":sum(y[i]==p for i,p in zip(vi,guessed_subset)),
            "03.6_all10_knn_correct":sum(y[i]==p for i,p in zip(vi,guessed_full)),
            "03.8_train_collision_pairs":collisions["collision_pair_count"],
            "03.8_test_reference_screen":dict(counts),
            "07.3_extrapolation":split["extrapolation"],
        })
    if len(truths)!=90:
        raise AssertionError("expected 90 heldout outcomes")
    comparison=bench.compare_predictions(truths,predictions)
    compact={}
    for name,record in comparison.items():
        compact[name]={
            "n":record["n"],"accuracy":record["accuracy_with_abstention_as_error"],
            "balanced_recall":record["balanced_recall"],
            "coverage":record["coverage"],
            "per_gas_recall":{gas:obj["recall"]
                              for gas,obj in record["per_class"].items()
                              if obj["support"]},
            "confusion":record["confusion"],
        }
    top_by_gas={}
    for gas,summary in conc["by_gas"].items():
        if summary["status"]!="DESCRIPTIVE":
            top_by_gas[gas]={"status":summary["status"]}
            continue
        observed=[(key,val) for key,val in summary["correlations"].items() if val is not None]
        strongest=max(observed,key=lambda q:(abs(q[1]),q[0])) if observed else None
        top_by_gas[gas]={"n":summary["n"],"concentration_levels":summary["n_concentrations"],
                         "strongest_abs_correlation":({"sensor":strongest[0],"r_pearson_log_ppm":strongest[1]}
                                                       if strongest else None)}
    return {
        "03.3_03.6_03.8_fold_diagnostics":fold_results,
        "03.7_within_gas_concentration_correlation":top_by_gas,
        "04.10_nested_subset_vs_full_sensor_metrics":compact,
        "05.9_concentration_blind_baseline":concentration_score,
        "07.3_holdout_test_counts":[len(f["test_indices"]) for f in fold_checks],
        "03.8_total_reference_decisions":dict(total_screen),
        "warning":"Subset selection on training exposures only; 55 subsets per fold, NOT 55 independently validated choices; sessions/devices not independent."
    }


def heldout_logit_calibration(rows,x):
    soft=repo_module("04.4","logistic_regression_v1.py")
    calib=repo_module("06.3","probability_calibration_v1.py")
    scalar=repo_module("02.2","scaling_v1.py")
    classes=sorted(set(r["gas"] for r in rows))
    folds=splits(rows,"leave_one_concentration_ppb")
    results=[]
    for f in folds:
        # For each of the two train concentrations, allocate 1/5 exposure
        # per gas to calibrate temperature; never fit the model on this set.
        model_idx=[i for i in f["train"] if rows[i]["replicate_ordinal"]!=0]
        calibr_idx=[i for i in f["train"] if rows[i]["replicate_ordinal"]==0]
        test_idx=f["test"]
        if (len(model_idx),len(calibr_idx),len(test_idx))!=(48,12,30):
            raise AssertionError("expected 48 model, 12 calibration, 30 test")
        s=scalar.fit_scaler([x[i] for i in model_idx])
        # 04.4 internally scales on its own 48 training exposures as well;
        # external scaling is train-only and not statistically necessary.
        x_model=scalar.transform([x[i] for i in model_idx],s)
        x_calibr=scalar.transform([x[i] for i in calibr_idx],s)
        x_test=scalar.transform([x[i] for i in test_idx],s)
        fit=soft.fit_softmax(x_model,[rows[i]["gas"] for i in model_idx],
                            epochs=200,learning_rate=.1,l2=.01)
        if fit["classes"]!=classes:raise AssertionError("class order mismatch")
        cal_logits=fit_softmax_logits(fit,x_calibr)
        test_logits=fit_softmax_logits(fit,x_test)
        cal_labels=[classes.index(rows[i]["gas"]) for i in calibr_idx]
        test_labels=[classes.index(rows[i]["gas"]) for i in test_idx]
        temperature=calib.fit_temperature(cal_logits,cal_labels)
        original=[calib.softmax(v) for v in test_logits]
        adjusted=[calib.apply_temperature(v,temperature) for v in test_logits]
        pre=calib.expected_calibration_error(original,test_labels)
        post=calib.expected_calibration_error(adjusted,test_labels)
        results.append({
            "held_out_ppb":f["heldout"],
            "n_model_train":len(model_idx),"n_temperature_calibration":len(calibr_idx),
            "n_unseen_ppb_test":len(test_idx),
            "selected_temperature":temperature["temperature"],
            "calibration_nll":temperature["calibration_nll"],
            "test_nll_before":calib.negative_log_likelihood(test_logits,test_labels,1.),
            "test_nll_after":calib.negative_log_likelihood(
                test_logits,test_labels,temperature["temperature"]),
            "test_ece_before":pre["ece"],"test_ece_after":post["ece"],
            "test_accuracy":sum(max(range(len(v)),key=lambda k:v[k])==truth
                                for v,truth in zip(adjusted,test_labels))/len(test_labels),
        })
    return results


def leave_one_gas_unknown(rows,x):
    mod=repo_module("06.5","open_set_recognition_v1.py")
    scaler=repo_module("02.2","scaling_v1.py")
    results=[]
    for gas in sorted(GASES):
        train=[i for i,r in enumerate(rows)
               if r["gas"]!=gas and r["replicate_ordinal"]!=0]
        known_test=[i for i,r in enumerate(rows)
                    if r["gas"]!=gas and r["replicate_ordinal"]==0]
        unknown_test=[i for i,r in enumerate(rows) if r["gas"]==gas]
        if [len(train),len(known_test),len(unknown_test)]!=[60,15,15]:
            raise AssertionError("expected 60 known training, 15 known heldout and 15 unknown")
        fit_scale=scaler.fit_scaler([x[i] for i in train])
        model=mod.fit_open_set([
            (rows[i]["gas"],v) for i,v in zip(
                train,scaler.transform([x[i] for i in train],fit_scale))
        ],rejection_quantile=.95)
        known=scaler.transform([x[i] for i in known_test],fit_scale)
        unknown=scaler.transform([x[i] for i in unknown_test],fit_scale)
        evaluation=mod.evaluate_open_set(model,known,unknown)
        known_labels=[rows[i]["gas"] for i in known_test]
        known_pred=[mod.predict_open_set(model,q) for q in known]
        known_id_correct=sum(a==p["candidate"] and p["status"]=="KNOWN_CANDIDATE"
                             for a,p in zip(known_labels,known_pred))
        results.append({"gas_left_as_unknown":gas,
                        "training_known_gases":sorted(model["known_classes"]),
                        "known_train_n":len(train),"heldout_known_n":len(known),
                        "heldout_unknown_n":len(unknown),
                        "known_accept_rate":evaluation["known_accept_rate"],
                        "known_correct_and_accepted":known_id_correct,
                        "unknown_reject_rate":evaluation["unknown_reject_rate"],
                        "training_in_sample_threshold":model["threshold"]})
    return results


def run(archive_path):
    rows=parse_archive(archive_path)
    delta=[features(r["trace"],"early_late_delta20") for r in rows]
    late=[features(r["trace"],"late10") for r in rows]
    return {
       "project":"HazRob Tank / research only",
       "dataset":"UCI1081 external measured source", "source_doi":"10.24432/C5CK6F",
       "source_sha256":"813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab",
       "source_units":"Gas concentration in ppm (exact ppb/1000 conversion), raw response electrical unit not established",
       "source_sensors":list(SENSORS),
       "number_of_independent_labeled_exposures":90,
       "predeclared_feature":"late10 for subset and concentration association; early_late_delta20 for calibration and open set",
       "02.5_sensor_schema_gate":external_sensor_schema_gate(),
       "measured_fold_diagnostics":train_only_subset_and_overlap(rows,late,delta),
       "06.3_three_way_train_calibrate_test":heldout_logit_calibration(rows,delta),
       "06.5_leave_one_gas_out":leave_one_gas_unknown(rows,delta),
       "caveats":[
           "10 technique IDs reviewed; 02.5 blocked by missing actual eight MQ resistance vectors.",
           "Only one source/device and no independently verified session groups.",
           "05.9 scores a concentration-BLIND geometric mean baseline, not a trained ppm model.",
           "06.3 12 independent *exposures* for each calibration fold; not independent sessions.",
           "06.5 unknown gas means leave-one-ONE-OF-SIX source gas classes out; other unmeasured toxic gases are not represented.",
           "06.5 threshold fitted to in-sample training distances and optimistic; not calibrated open-world rejection.",
           "03.6 subset selection uses training folds only, no best-of-test outcome; evaluating 55 subsets per fold is exploratory.",
           "03.8 numerical radius=0.5 in training z-score units is arbitrary, not an evidence-based chemical threshold.",
           "None of these metrics qualifies Tank gas identification, concentration or occupational safety."
       ],
    }


if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("original_uci1081_zip")
    p.add_argument("--output",default="uci1081_g2_batch02_result.json")
    args=p.parse_args()
    result=run(args.original_uci1081_zip)
    Path(args.output).write_text(json.dumps(result,indent=2,ensure_ascii=False,allow_nan=False)+"\n",encoding="utf-8")
    print("G2-02 UCI1081 real external array: ten technique IDs reviewed, one blocked for Tank MQ8 sensor schema")
    for f in result["measured_fold_diagnostics"]["03.3_03.6_03.8_fold_diagnostics"]:
        print("ppb",f["held_out_ppb"],"subset",f["03.6_selected_source_sensors"],
              "correct",f["03.6_subset_knn_correct"],"/30; all10",
              f["03.6_all10_knn_correct"],"/30")
    for k,v in result["measured_fold_diagnostics"]["04.10_nested_subset_vs_full_sensor_metrics"].items():
        print("04.10",k,v["accuracy"],v["n"])
    for r in result["06.3_three_way_train_calibrate_test"]:
        print("06.3",r["held_out_ppb"],"temperature",r["selected_temperature"],
              "NLL before",round(r["test_nll_before"],4),"after",round(r["test_nll_after"],4))
    for r in result["06.5_leave_one_gas_out"]:
        print("06.5",r["gas_left_as_unknown"],"unknown_reject",r["unknown_reject_rate"],
              "known_accept",r["known_accept_rate"])
    # Durable machine-readable record extracted from a successful CI job and
    # committed unchanged under research/benchmarks/results/.
    print("G2_BATCH02_JSON_BEGIN")
    print(json.dumps(result,sort_keys=True,ensure_ascii=False,allow_nan=False))
    print("G2_BATCH02_JSON_END")
