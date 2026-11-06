#!/usr/bin/env python3
"""Separate pinned ChemCore Stable research-library methane fingerprint audit.
Only 165 SOURCE train-only profiles (11 heldout folds, 3 independent sensors,
5 Li-Cor reference ppm bins). Not original Tank Rs/R0, no baseline zero-air,
no non-methane classification and no field safety claim.
"""
import argparse,collections,json,pathlib,math,statistics,csv
def med(x):return statistics.median(x)
def main():
    p=argparse.ArgumentParser();p.add_argument("--payload",required=True,type=pathlib.Path)
    p.add_argument("--out",required=True,type=pathlib.Path);q=p.parse_args()
    q.out.mkdir(parents=True,exist_ok=True)
    j=json.loads(q.payload.read_text(encoding="utf8"))
    assert j["schema"]=="hazrob.chemcore.stable.quantitative_source_fingerprint.v1"
    assert j["evidence_origin"]["physical_source_rows"]==14186
    assert j["evidence_origin"]["distinct_acquisition_days"]==11
    assert j["evidence_origin"]["source_per_instance_r0_kohm"] is None
    rows=j["training_only_holdout_fingerprints_165"];assert len(rows)==165
    by=collections.defaultdict(list)
    for r in rows:
        key=(r["heldout_day"],r["MQ4_sensor_instance"])
        by[key].append(r)
        assert r["source_R0_reference"]=="NOT_MEASURED_BY_THIS_SOURCE"
        assert r["derived_from_split"]=="TRAINING_9_DAYS_ONLY"
        assert r["training_days"]==9
        assert r["heldout_day"]!=r["separate_calibration_day"]
        assert r["Rs_median_kohm"]>0
    assert len(by)==33 and all(len(v)==5 for v in by.values())
    curves=[]
    for (day,sensor),arr in sorted(by.items()):
        ordered=sorted(arr,key=lambda v:v["methane_reference_ppm_interval"][0])
        rs=[r["Rs_median_kohm"] for r in ordered]
        increasing=sum(rs[i+1]>rs[i] for i in range(4))
        curves.append(dict(heldout_day=day,sensor=sensor,profile_count=5,
            increases_with_higher_methane_ppm=increasing,
            monotonic_resistance_decrease=(increasing==0),
            source_rs_min_kohm=min(rs),source_rs_max_kohm=max(rs),
            source_rs_bin_median_kohm=med(rs),
            ratio_max_to_min=max(rs)/min(rs),
            source_r0_supplied=False))
    perbin=collections.defaultdict(list)
    for r in rows:
        lo,hi=r["methane_reference_ppm_interval"]
        perbin[(r["MQ4_sensor_instance"],lo,hi)].append(r["Rs_median_kohm"])
    stats=[]
    for (sensor,lo,hi),vals in sorted(perbin.items()):
        stats.append(dict(sensor=sensor,source_ppm_low=lo,source_ppm_high=hi,
            n_overlapping_training_folds=len(vals),
            median_rs_kohm=med(vals),
            min_rs_kohm=min(vals),max_rs_kohm=max(vals),
            fold_profile_max_to_min=max(vals)/min(vals),
            overlapping_fold_not_independent_measurement=True))
    with (q.out/"stable_methane_33_fold_sensor_curves.csv").open("w",newline="") as f:
        w=csv.DictWriter(f,fieldnames=list(curves[0]));w.writeheader();w.writerows(curves)
    with (q.out/"stable_methane_15_bin_variability.csv").open("w",newline="") as f:
        w=csv.DictWriter(f,fieldnames=list(stats[0]));w.writeheader();w.writerows(stats)
    summary=dict(schema="hazrob.chemcore.next.stable_methane_165_profile_variability/1",
        source_reference="MQ-EXT-21-FURUTA-LICOR-2022",
        source_original_rows=14186, source_days=11,
        fingerprint_profiles=165,fold_sensor_curves=33,concentration_bins_per_profile=5,
        folds_with_nonmonotonic_increases=sum(r["increases_with_higher_methane_ppm"]>0 for r in curves),
        total_increasing_adjacent_bin_pairs=sum(r["increases_with_higher_methane_ppm"] for r in curves),
        source_fold_profile_bin_variability_median_max_min=med([r["fold_profile_max_to_min"] for r in stats]),
        source_fold_profile_bin_variability_max_max_min=max(r["fold_profile_max_to_min"] for r in stats),
        no_source_R0_available=True,
        not_compared_to_real_tank_source_profiles=True,
        no_quantitative_ppm_claim=True,not_independent_acquisition_of_profiles=True,
        stable_runtime_published=False, no_90percent_identity_claim=True)
    (q.out/"stable_methane_165_audit_summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n")
    print("STABLE_METHANE_165_SOURCE_PROFILE_AUDIT_PASS",json.dumps(summary,sort_keys=True))
if __name__=="__main__":main()
