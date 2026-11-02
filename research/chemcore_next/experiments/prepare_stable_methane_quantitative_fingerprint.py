#!/usr/bin/env python3
"""Publish a lossless SHA-verifiable CH4 quantitative SOURCE fingerprint payload.

Input: original Li-Cor measured CH4 source + ChemCore Next SHA-verified,
whole-day held-out derived 165 training-only fingerprints. Not device R0,
not a Tank G3 experiment and not activated Stable calibration.
"""
import argparse
import csv
import hashlib
import json
import math
import pathlib
import numpy as np
from next_reference_mq4_quantitative import (
    SOURCE_SHA, SENSORS, CH4_EDGES, MODES, load
)

FINGERPRINT_FILENAME="mq4_methane_li_cor_quantitative_source_fingerprint_v1.json"
PREVIOUS_STABLE="0.10.2-HR069-research-index"
CENTRAL_REFERENCE="MQ-EXT-21"

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def source_rows(file):
    with pathlib.Path(file).open(encoding="utf8",newline="") as f:
        return list(csv.DictReader(f))

def check_float(value,key):
    number=float(value)
    if not math.isfinite(number):raise ValueError("NONFINITE_"+key)
    return number

def pack_record(source):
    return {
        "source_root":"MQ-EXT-21-FURUTA-LICOR-2022",
        "heldout_day":source["fold_test_day"],
        "separate_calibration_day":source["fold_calibration_day"],
        "training_days":int(source["training_day_count"]),
        "MQ4_sensor_instance":source["source_sensor_uid"],
        "methane_reference_ppm_interval":[check_float(source["interval_low_ppm"],"low"),
            check_float(source["interval_high_ppm"],"high")],
        "training_row_count":int(source["training_rows"]),
        "Rs_median_ohm":check_float(source["training_rs_median_ohm"],"rs"),
        "Rs_median_kohm":check_float(source["training_rs_median_ohm"],"rs")/1000,
        "log_Rs_median":check_float(source["training_logrs_median"],"logrs"),
        "log_Rs_MAD":check_float(source["training_logrs_mad"],"mad"),
        "temperature_median_C":check_float(source["training_t_median_C"],"T"),
        "relative_humidity_median_percent":check_float(source["training_rh_median_pct"],"RH"),
        "device_R0_kohm":None,
        "source_R0_reference":"NOT_MEASURED_BY_THIS_SOURCE",
        "derived_from_split":"TRAINING_9_DAYS_ONLY",
        "evaluation_fold_scope":"heldout_day_excluded_and_separate_cal_day_excluded",
    }

def summarise_full_reference(df):
    """
    Descriptive source-only reference bins using all rows. NEVER evaluate on
    this profile as if train/test independent; 165 train-only profiles are
    retained for evaluating model training free from day leakage.
    """
    ppm=df["licor_ch4"].to_numpy(dtype=float)/1000
    rows=[]
    for sensor in SENSORS:
        rs=df[sensor].to_numpy(dtype=float)
        temps=df["t"].to_numpy(dtype=float)
        humidity=df["rh"].to_numpy(dtype=float)
        for lo,hi in zip(CH4_EDGES[:-1],CH4_EDGES[1:]):
            masked=(ppm>=lo)&(ppm<hi)
            data=rs[masked]
            if not len(data):raise ValueError("empty CH4 reference band")
            rs_values=[float(np.quantile(data,q)) for q in (0.1,0.5,0.9)]
            rows.append({
                "gas_CAS":"74-82-8",
                "gas_name":"Methane",
                "source_MQ4_sensor_instance":sensor,
                "concentration_reference":"independent Li-Cor CH4",
                "concentration_original_unit":"ppb",
                "concentration_numeric_unit":"ppm",
                "reference_ppm_low":float(lo),"reference_ppm_high_exclusive":float(hi),
                "observations_in_interval":int(masked.sum()),
                "Rs_quantile_10_ohm":rs_values[0],
                "Rs_median_ohm":rs_values[1],
                "Rs_quantile_90_ohm":rs_values[2],
                "Rs_median_kohm":rs_values[1]/1000,
                "temperature_median_C":float(np.median(temps[masked])),
                "relative_humidity_median_percent":float(np.median(humidity[masked])),
                "R0_device":"UNAVAILABLE",
                "role":"DESCRIPTIVE_FULL_SOURCE_ONLY_NOT_INDEPENDENT_HOLDOUT"
            })
    return rows

def run(args):
    df,days,source_digest=load(args.source)
    assert source_digest==SOURCE_SHA
    source_fingerprints=source_rows(args.fingerprints)
    experiments=json.loads(pathlib.Path(args.experiment_summary).read_text(encoding="utf8"))
    assert experiments["source_csv_sha256"]==SOURCE_SHA
    assert experiments["day_group_holdout_folds"]==44
    assert experiments["heldout_predictions"]==56744
    assert experiments["unseen_instance_plus_day_transfer_tests"]==33
    assert experiments["fingerprints_created"]==165
    assert len(source_fingerprints)==165
    by_day={}
    for day in days:by_day[day]=0
    profiles=[]
    seen=set()
    for record in source_fingerprints:
        profile=pack_record(record)
        key=(profile["heldout_day"],profile["MQ4_sensor_instance"],
             *profile["methane_reference_ppm_interval"])
        if key in seen:raise ValueError("DUPLICATE_FINGERPRINT_CELL")
        seen.add(key)
        by_day[profile["heldout_day"]]+=1
        assert profile["training_days"]==9
        assert profile["separate_calibration_day"]!=profile["heldout_day"]
        assert profile["training_row_count"]>0
        profiles.append(profile)
    assert set(by_day.values())=={15},by_day
    full=summarise_full_reference(df)
    assert len(full)==15
    assert {r["source_MQ4_sensor_instance"] for r in full}==set(SENSORS)
    assert min(r["observations_in_interval"] for r in full)>0
    baseline=experiments["metrics_by_model"]["BASELINE_TRAIN_MEDIAN"]["mean_day_mae_ppm"]
    calibrated=experiments["metrics_by_model"]["NEXT_MQ4_ARRAY_ENV_RIDGE"]["mean_day_mae_ppm"]
    if not calibrated>baseline:
        raise ValueError("EXPECTED_SOURCE_REGRESSION_NO_IMPROVEMENT_GUARD")
    # NOTE Source-only 1.95–10 ppm reference; these are NOT metrology/LEL alarm gates.
    target=df["licor_ch4"].to_numpy(dtype=float)/1000
    payload={
        "schema":"hazrob.chemcore.stable.quantitative_source_fingerprint.v1",
        "record_id":"CH4_MQ4_MN_LICOR_2022_NEXT_G2_20261102",
        "release_version":"0.1.0-research-transfer",
        "created_on":"2026-11-02",
        "canonical_publication_path":"HazRob/build-harness/library/chemcore/stable/reference-evidence/derived/2026-11-02/",
        "canonical_material":{"name":"Methane","cas":"74-82-8",
                              "legacy_catalog_id":76,"formula":"CH4"},
        "evidence_origin":{"reference_id":CENTRAL_REFERENCE,
            "original_repository":"University of Minnesota Conservancy",
            "landing_url":"https://hdl.handle.net/11299/226714",
            "original_file":"10ppm_data.csv",
            "original_csv_sha256":SOURCE_SHA,
            "physical_source_rows":14186,
            "distinct_acquisition_days":11,
            "sensor_models":["MQ4"],
            "physical_sensor_instances":list(SENSORS),
            "instrument_of_reference":"Li-Cor CH4",
            "reference_original_measurement_unit":"ppb",
            "published_resistance_unit":"ohm",
            "gas_range_reference_ppm_observed":[float(target.min()),float(target.max())],
            "source_per_instance_r0_kohm":None,
            "source_reported_heater_and_RL_qualified_for_Tank":False},
        "features":{"dimension":3,"coordinates":[
                "source_MQ4_Rs_ohm_per_instance",
                "temperature_C","relative_humidity_percent"],
            "source_only_not_Tank_signature":True,
            "calibration_relation":"empirical Rs quantile bins given independent Li-Cor concentration, temperature and humidity",
            "R0_source_or_Tank":None,
            "mean_and_scale":"train-only fingerprints retain logRs median and MAD; no universal R0"},
        "original_source_reference_bins_15":full,
        "training_only_holdout_fingerprints_165":profiles,
        "source_benchmark":{"origin_engine":"ChemCore Next research G2 source experiment",
            "original_workflow_run":"https://github.com/HazRob/HazRob-Tank/actions/runs/41385297589",
            "source_data_heldout_days":11,
            "training_only_fingerprints":165,
            "calibration_day_separate":True,
            "reference_prediction_count":56744,
            "heldout_unit_day_tests":33,
            "model_day_weighted_mae_ppm":experiments["metrics_by_model"],
            "source_ridge_array_T_RH_mae_ppm":float(calibrated),
            "source_train_median_baseline_mae_ppm":float(baseline),
            "measured_incremental_mae_improvement_ppm":float(baseline-calibrated),
            "outperforms_no_MQ_baseline":False,
            "safety_alarm_qualified":False,
            "source_gas_identity_discrimination_tested":False},
        "stable_policy":{"source_quantitative_fingerprint_status":"AVAILABLE_EXTERNAL_SOURCE_BUT_NOT_DEVICE_CALIBRATED",
            "fingerprint_state":"TRANSFER",
            "ppm_state":"TRANSFER",
            "m4_state":"NONE",
            "G2_source_experiment":"EXPLORATORY_NONPROMOTED",
            "G3_Tank_qualified":False,
            "runtime_activation":False,
            "production_stable_payload":False,
            "field_ppm_authorized":False,
            "field_alarm_authorized":False,
            "cannot_transfer_sensor_R0_between_devices":True,
            "never_use_entire_dataset_reference_bins_to_score_same_source_samples":True},
        "limitations":[
            "MQ4 responses can vary strongly by individual device, warm-up, heater and environmental history",
            "Source Rs in ohm is not a Tank-measured R0, and a generic factory K cannot replace R0",
            "No multi-gas identification test, methane identity established by independent Li-Cor reference only",
            "11-day heldout linear MQ4 models did not outperform a source train median baseline",
            "All-source 15 fingerprint bins are descriptive and must not be interpreted as independently validated predictive models",
            "No field inference, exposure limits, alarm or occupational safety guarantee"]
    }
    assert all(len(str(key))>0 for key in payload)
    text=json.dumps(payload,sort_keys=True,ensure_ascii=True,separators=(",",":"),allow_nan=False)+"\n"
    path=pathlib.Path(args.out)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(text.encode("ascii"))
    digest=sha(text.encode("ascii"))
    print("CHEMCORE_NEXT_METHANE_STABLE_PAYLOAD_PASS source_rows=14186 source_fingerprint_cells=15 independent_day_training_cells=165 baseline_mae=%.6f ridge_mae=%.6f G3=0"%
          (baseline,calibrated))
    print("NEXT_STABLE_PAYLOAD_SHA256|"+digest)
    print("NEXT_STABLE_PAYLOAD_BYTES|"+str(len(text.encode("ascii"))))
    if args.print_chunks:
        chunks=[text[i:i+3000] for i in range(0,len(text),3000)]
        print("NEXT_STABLE_CHUNKS|"+str(len(chunks)))
        for i,chunk in enumerate(chunks):
            print("NEXT_STABLE_CHUNK_%04d|%s"%(i,chunk))
    return payload

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",required=True)
    p.add_argument("--fingerprints",required=True)
    p.add_argument("--experiment-summary",required=True)
    p.add_argument("--out",required=True)
    p.add_argument("--print-chunks",action="store_true")
    run(p.parse_args())
