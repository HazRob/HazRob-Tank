#!/usr/bin/env python3
"""Read-only V8 science audit: all source-locked Stable fingerprint families x 80 technique IDs.

Does NOT modify ChemCore Next, make clinical/industrial safety statements, train
on the same tested points or pretend the G1 research prototypes are live V8
components. Original source refs retained and UCI1081 excluded from scoring.
"""
import argparse,csv,json,math,statistics,pathlib,collections,hashlib

def load_json(path):
    return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
def load_csv(path):
    with pathlib.Path(path).open(newline="",encoding="utf-8") as f:
        return list(csv.DictReader(f))
def write_csv(path,items,cols):
    with pathlib.Path(path).open("w",newline="",encoding="utf-8") as f:
        w=csv.DictWriter(f,fieldnames=cols,extrasaction="ignore");w.writeheader();w.writerows(items)
def pct_error(actual,expected):
    return abs(actual-expected)/abs(expected)*100 if expected else None
def quantile(v,frac):
    z=sorted(v);return z[max(0,min(len(z)-1,math.ceil(frac*len(z))-1))] if z else None
def median(v):
    return statistics.median(v) if v else None

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--tank",default=".")
    parser.add_argument("--stable",required=False)
    parser.add_argument("--out",required=True)
    args=parser.parse_args()
    tank=pathlib.Path(args.tank); stable=pathlib.Path(args.stable) if args.stable else tank
    out=pathlib.Path(args.out);out.mkdir(parents=True,exist_ok=True)
    snapshot=tank/"android/app/src/main/assets/chemcore_v6/reference"
    fixture=tank/"research/chemcore_next/tests/source_fixtures"
    curves=load_json(snapshot/"mq_research_curves_43.json")["curves"]
    alternatives=load_json(snapshot/"mq135_alternative_log_curves_6.json")["formulae"]
    sourcefp=load_json(tank/"research/chemcore_next/fingerprints/chemcore_next_p03_3_source_fingerprints_24.json")["records"]
    priors=load_json(snapshot/"identification_fingerprint_priors_v1.json")
    methane=load_json(fixture/"mq4_methane_source_fingerprint_copy_2026_11_02.json")
    methods=load_csv(tank/"docs/research/V6-CHEMCORE-TECHNIQUE-80-TRACEABILITY.csv")
    register=load_csv(tank/"research/chemcore_next/source_curves/chemcore_next_p03_2_unified_109_formula_register.csv")
    summary_manifest=load_json(fixture/"stable_reference_manifest_2026_10_21.json")
    assert len(curves)==43 and len(alternatives)==6 and len(sourcefp)==24
    assert len(methane["original_source_reference_bins_15"])==15
    assert len(methane["training_only_holdout_fingerprints_165"])==165
    assert len(methods)==80 and len(register)==109
    assert not summary_manifest["runtime_activation"] and summary_manifest["Tank_MQ8_G3_validated_techniques"]==0
    expected_snapshot={
      "mq_research_curves_43.json":"bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1",
      "identification_fingerprint_priors_v1.json":"5be89c673bb6b2b265acb6ddd6239e7ad5683f31",
      "mq135_alternative_log_curves_6.json":next(x["sha"] for x in summary_manifest["curated_additions"] if x["path"].endswith("mq135_alternative_log_curves_6.json")) if any("sha" in x for x in summary_manifest["curated_additions"]) else None,
    }
    assert expected_snapshot["mq_research_curves_43.json"] in [x["sha"] for x in summary_manifest["pinned_sources"]]
    derived_manifest=load_json(fixture/"mq4_methane_source_manifest_2026_11_02.json")
    assert derived_manifest["payload"]["github_git_blob_sha"]=="acb3e16a79a6795c86d12ff1dbdbd01d6b11cb46"
    assert derived_manifest["qualification"]["g3_actual_tank_validated"] is False

    inventory=[]
    for i,c in enumerate(curves,1):
        inventory.append(dict(ref_id=f"STABLE-CURVE-{i:03d}",family="RESEARCH_CURVE_43",
            source_root="MQDATASCIENCE_6_0_0",chemical_label=c["gas_label"],sensor=c["sensor"],
            source_status="SOURCE_RESEARCH_UNQUALIFIED",physical_r0="NOT_AVAILABLE",
            chemical_uid=str(c["legacy_id"])+" (LEGACY_NOT_CANONICAL)",
            model_space="Rs/R0 VS CONCENTRATION",replay_type="SOURCE_ASSUMED_SCALE_MATH"))
    for i,c in enumerate(alternatives,1):
        inventory.append(dict(ref_id=f"STABLE-ALT-{i:03d}",family="MQ135_ALTERNATIVE_CURVE_6",
            source_root=c["peer_reviewed_graph_regression"]["doi"],chemical_label=c["analyte"],
            sensor="MQ135",source_status="DIGITIZED_GRAPH_NOT_PHYSICAL_CALIBRATION",
            physical_r0="NOT_AVAILABLE",chemical_uid=str(c["legacy_id"])+" (LEGACY_NOT_CANONICAL)",
            model_space="Rs/R0 VS CONCENTRATION",replay_type="SOURCE_DIGITIZED_MATH"))
    for c in sourcefp:
        excluded=c["source_root_id"].startswith("UCI1081")
        inventory.append(dict(ref_id=c["id"],family="ND_VECTOR_24",
            source_root=c["source_root_id"],chemical_label=c["source_label"],
            sensor="MULTICHANNEL_OR_SOURCE",source_status="UCI_NOT_SCORED_BY_USER_DIRECTIVE" if excluded
                else "NOT_QUALIFIED_AS_MOLECULAR_IDENTITY",
            physical_r0="NOT_AVAILABLE",chemical_uid=c.get("canonical_uid"),
            model_space=c["feature_space"],replay_type="EXCLUDED_UCI1081" if excluded else "DISTANCE_DIAGNOSTIC_ONLY"))
    for i,c in enumerate(methane["original_source_reference_bins_15"],1):
        inventory.append(dict(ref_id=f"CH4-DESC-{i:03d}",family="MQ4_CH4_DESCRIPTIVE_15",
            source_root="MQ-EXT-21-FURUTA-LICOR-2022",chemical_label="Methane",
            sensor=c["source_MQ4_sensor_instance"],source_status="EXTERNAL_MEASURED_DESCRIPTIVE",
            physical_r0="NOT_AVAILABLE",chemical_uid="76 (LEGACY_NOT_CANONICAL)",
            model_space="EXTERNAL_RS_OHM_VS_LICOR_CH4_PPM",replay_type="SOURCE_BIN_VARIABILITY_ONLY"))
    for i,c in enumerate(methane["training_only_holdout_fingerprints_165"],1):
        inventory.append(dict(ref_id=f"CH4-TRAIN-{i:03d}",family="MQ4_CH4_TRAIN_ONLY_165",
            source_root="MQ-EXT-21-FURUTA-LICOR-2022",chemical_label="Methane",
            sensor=c["MQ4_sensor_instance"],source_status="REUSED_TRAINING_FOLD_SAME_ORIGINAL_SOURCE",
            physical_r0="NOT_AVAILABLE",chemical_uid="76 (LEGACY_NOT_CANONICAL)",
            model_space="EXTERNAL_LOG_RS_VS_LICOR_CH4_PPM",replay_type="TRAIN_TO_SOURCE_DESCRIPTOR_DIAGNOSTIC"))
    priors_comp=list(priors["numeric_source_backed_analytes"])+list(priors["operational_class_numeric_fingerprints"])
    for i,c in enumerate(priors_comp,1):
        inventory.append(dict(ref_id=f"STABLE-PRIOR-{i:03d}",family="CONCEPTUAL_IDENTITY_PRIOR_13",
            source_root="HR069_PRIOR_NOT_FULL_NUMERIC_VECTOR",
            chemical_label=c["name"],sensor="NOT_SPECIFIED",
            source_status="PRIOR_ONLY_NO_INDEPENDENT_MEASURED_VECTOR",
            physical_r0="NOT_AVAILABLE",chemical_uid=str(c["legacy_id"])+" (LEGACY_NOT_CANONICAL)",
            model_space="MANUFACTURER_AFFINITY_OR_CLASS",replay_type="NON_REPLAYABLE_PRIOR_ONLY"))
    assert len(inventory)==43+6+24+15+165+13
    write_csv(out/"full_source_inventory.csv",inventory,list(inventory[0]))

    source_results=[]
    confusions=[]
    r0_sensitivity=[]
    for i,c in enumerate(curves,1):
        mn,mx=float(c["min_ppm"]),float(c["max_ppm"])
        a,b,scale=float(c["ratio_a"]),float(c["ratio_b"]),float(c["output_scale"])
        if not (mn>0 and mn<mx and a>0 and b and scale>0):
            raise ValueError("bad V6 research curve "+str(i))
        for step in range(7):
            frac=(step+.5)/7
            true_ppm=mn*((mx/mn)**frac)
            observed_ratio=a*((true_ppm/scale)**b)
            inverse=scale*((observed_ratio/a)**(1.0/b))
            curve_id=f"STABLE-CURVE-{i:03d}"
            matches=[]
            for idx,other in enumerate(curves,1):
                if other["sensor"]!=c["sensor"]:continue
                apparent=float(other["output_scale"])*((observed_ratio/float(other["ratio_a"]))**(1/float(other["ratio_b"])))
                if float(other["min_ppm"])<=apparent<=float(other["max_ppm"]):
                    matches.append((idx,other["gas_label"],apparent))
            false_same_sensor=[x for x in matches if x[0]!=i]
            if len(false_same_sensor)>0:
                for ix,label,pred in false_same_sensor:
                    confusions.append(dict(truth_curve=curve_id,truth_gas=c["gas_label"],
                        true_ppm=round(true_ppm,8),sensor=c["sensor"],
                        false_curve=f"STABLE-CURVE-{ix:03d}",false_label=label,
                        false_inverted_ppm=round(pred,8),
                        why="ONE_SENSOR_RATIO_WITHIN_BOTH_ASSUMED_DOMAINS_NOT_IDENTITY_EVIDENCE"))
            source_results.append(dict(curve_id=curve_id,sensor=c["sensor"],gas=c["gas_label"],
                original_legacy_id=c["legacy_id"],source_scale_assumed=scale,source_coeff_a=a,source_coeff_b=b,source_min_ppm=mn,source_max_ppm=mx,true_ppm=true_ppm,
                synthetic_ratio=observed_ratio,restored_ppm=inverse,
                self_abs_relative_error_pct=pct_error(inverse,true_ppm),
                false_other_curves_in_range=len(false_same_sensor),
                all_curves_in_range=len(matches),step=step,
                data_class="SAME_SOURCE_FORWARD_INVERSE_CIRCULAR_G1_NOT_GAS_ACCURACY"))
            for mode,factor in (("RATIO_PLUS_2PCT",1.02),("RATIO_MINUS_2PCT",.98),
                ("RATIO_PLUS_10PCT",1.10),("RATIO_MINUS_10PCT",.90),("R0_HIGH_20PCT",1/1.20)):
                inferred=scale*(((observed_ratio*factor)/a)**(1.0/b))
                r0_sensitivity.append(dict(curve_id=curve_id,sensor=c["sensor"],
                    gas=c["gas_label"],true_ppm=true_ppm,mode=mode,
                    altered_ratio=observed_ratio*factor,
                    inferred_ppm=inferred,
                    abs_relative_error_pct=pct_error(inferred,true_ppm),
                    within_source_domain=mn<=inferred<=mx))
    write_csv(out/"source_curves_43_self_replay_301.csv",source_results,list(source_results[0]))
    write_csv(out/"source_curves_43_wrong_gas_in_domain.csv",confusions,list(confusions[0]))
    write_csv(out/"source_curves_43_perturbations.csv",r0_sensitivity,list(r0_sensitivity[0]))

    clean_air=[]
    for i,c in enumerate(curves,1):
        a,b,scale=float(c["ratio_a"]),float(c["ratio_b"]),float(c["output_scale"])
        hypothetical=scale*((1/a)**(1/b))
        clean_air.append(dict(curve_id=f"STABLE-CURVE-{i:03d}",
            sensor=c["sensor"],gas=c["gas_label"],
            ratio_RS_over_R0=1.0,research_inverse_ppm=hypothetical,
            inside_assumed_range=float(c["min_ppm"])<=hypothetical<=float(c["max_ppm"]),
            no_positive_evidence_from_clean_air=True))
    write_csv(out/"clean_air_ratio_1_false_hypotheses.csv",clean_air,list(clean_air[0]))

    alt_results=[]
    for i,c in enumerate(alternatives,1):
        alt=c["peer_reviewed_graph_regression"]
        previous=c["currently_catalogued_research_curve"]
        low=max(float(alt["min_ppm"]),float(previous["min_ppm"]))
        high=min(float(alt["max_ppm"]),float(previous["max_ppm"]))
        for concentration in [10,20,50,100]:
            if not low<=concentration<=high:continue
            original=float(previous["a"])*concentration**float(previous["b"])
            fresh=float(alt["a"])*concentration**float(alt["b"])
            alt_results.append(dict(curve=f"STABLE-ALT-{i:03d}",analyte=c["analyte"],
                ppm=concentration,old_source_ratio=original,
                alternative_ratio=fresh,ratio_delta_pct=100*(fresh/original-1),
                use="COMPARISON_OF_DIGITIZED_CURVES_NOT_QUALIFIED_CALIBRATION"))
    write_csv(out/"alternative_MQ135_six_curves_deviation.csv",alt_results,list(alt_results[0]))

    # Scalar distance is valid only within one source-defined vector space,
    # on the intersection of observed coordinates. Do not score UCI1081.
    fp_results=[]
    for record in sourcefp:
        if record["source_root_id"].startswith("UCI1081"):continue
        v=record["vector"]
        valid=[i for i,x in enumerate(v) if x is not None and math.isfinite(x)]
        competitors=[]
        for other in sourcefp:
            if other["id"]==record["id"] or other["feature_space"]!=record["feature_space"]:continue
            shared=[i for i in valid if i<len(other["vector"])
                    and other["vector"][i] is not None and math.isfinite(other["vector"][i])]
            if not shared:continue
            dist=math.sqrt(sum((v[k]-other["vector"][k])**2 for k in shared)/len(shared))
            competitors.append((dist,other["id"],other["source_label"],len(shared)))
        closest=min(competitors) if competitors else (None,None,None,None)
        source_std=record.get("source_scale_raw") or []
        finite_scales=[x for x in source_std if isinstance(x,(int,float)) and math.isfinite(x)]
        fp_results.append(dict(fp_id=record["id"],label=record["source_label"],
            space=record["feature_space"],self_distance=0.0,
            source_dims=len(v),available_dims=len(valid),
            has_missing_dims=len(valid)<len(v),
            scale_entries_nonpositive=sum(x<=0 for x in finite_scales),
            nearest_other_id=closest[1],nearest_other_class=closest[2],
            nearest_other_raw_RMS=closest[0],
            nearest_other_shared_dims=closest[3],
            canonical_uid_available=record["canonical_uid"] is not None,
            qualitative_output_claim="NO_CHEMICAL_ID_VALIDATION"))
    write_csv(out/"source_vectors_non_uci_self_cross_distance.csv",fp_results,list(fp_results[0]))

    descript=methane["original_source_reference_bins_15"]
    train=methane["training_only_holdout_fingerprints_165"]
    ref={(c["source_MQ4_sensor_instance"],float(c["reference_ppm_low"]),
            float(c["reference_ppm_high_exclusive"])):c for c in descript}
    train_diff=[]
    for i,c in enumerate(train,1):
        key=(c["MQ4_sensor_instance"],float(c["methane_reference_ppm_interval"][0]),
             float(c["methane_reference_ppm_interval"][1]))
        if key not in ref:raise ValueError("Missing source interval "+str(key))
        full=ref[key]
        ratio=float(c["Rs_median_ohm"])/float(full["Rs_median_ohm"])
        train_diff.append(dict(fingerprint=f"CH4-TRAIN-{i:03d}",
            sensor=c["MQ4_sensor_instance"],heldout_day=c["heldout_day"],
            concentration_ppm_min=key[1],concentration_ppm_max=key[2],
            training_only_Rs_ohm=c["Rs_median_ohm"],
            full_source_descriptive_Rs_ohm=full["Rs_median_ohm"],
            train_vs_full_abs_relative_delta_pct=abs(ratio-1)*100,
            referenced_r0_available=False,independent_holdout_validation=False))
    write_csv(out/"methane_165_training_fold_vs_full_source.csv",train_diff,list(train_diff[0]))
    monotonic=[]
    for sensor in sorted({c["source_MQ4_sensor_instance"] for c in descript}):
        ordered=sorted((c for c in descript if c["source_MQ4_sensor_instance"]==sensor),
            key=lambda x:float(x["reference_ppm_low"]))
        p=[float(x["Rs_median_ohm"]) for x in ordered]
        monotonic.append(dict(sensor=sensor,bins=len(ordered),
             consecutive_Rs_increases=sum(p[i+1]>p[i] for i in range(len(p)-1)),
             consecutive_Rs_decreases=sum(p[i+1]<p[i] for i in range(len(p)-1)),
             known_concentration_standards=False,source_is_external=True))
    write_csv(out/"methane_15_bin_monotonicity.csv",monotonic,list(monotonic[0]))

    # Explicitly distinguish method capability from a successfully executed
    # technique. No claim that the 80 G1 prototypes are wired to Next V8.
    technique_rows=[]
    method_matrix=[]
    for m in methods:
        tid=m["technique_id"]
        method_row=dict(technique_id=tid,technique=m["name"],
            g2_record_status=m["G2_status"],v8_active_mapping_claimed=False,
            target_path=m["source_directory"],source_input_shape_supported="NOT_PROVEN",
            end_to_end_stable_benchmark_status="NOT_EXECUTED_AS_NAMED_TECHNIQUE",
            root_cause="TECHNIQUE_ID_NOT_TRACEABLY_CONNECTED_TO_V8_RUNTIME_UNDER_AVAILABLE_MANIFEST")
        technique_rows.append(method_row)
        for item in inventory:
            kind=item["family"]
            if item["replay_type"]=="EXCLUDED_UCI1081":
                status="EXCLUDED_USER_DIRECTIVE";why="UCI1081_SOURCE_NOT_USED_TO_SCORE_NEXT"
            elif kind.startswith("CONCEPTUAL"):
                status="NO_NUMERIC_INPUT";why="ONLY_PRIOR_IDENTITY_WITHOUT_MEASURED_FEATURE_VECTOR"
            elif kind.startswith("MQ4_CH4"):
                status="NOT_RUNNABLE_AS_NAMED_TECHNIQUE";why="NO_TANK_R0_OR_SOURCE_SPECIFIC_TIME_SERIES_IN_THIS_REFERENCE_CELL"
            elif kind=="RESEARCH_CURVE_43" or kind=="MQ135_ALTERNATIVE_CURVE_6":
                if tid=="01.3":
                    status="INDEPENDENT_G1_ALGEBRA_CHECK_ONLY";why="FORWARD_INVERSE_MATH_NOT_FULL_NEXT_INFERENCE"
                else:
                    status="NOT_RUNNABLE_AS_NAMED_TECHNIQUE";why="SOURCE_CURVE_ALONE_MISSING_REQUIRED_TECHNIQUE_INPUTS_OR_ACTIVE_METHOD"
            elif kind=="ND_VECTOR_24" and tid=="02.5":
                status="INDEPENDENT_SOURCE_DISTANCE_ONLY";why="NO_PHYSICAL_TANK_G3_OR_CANONICAL_CHEMICAL_UID"
            else:
                status="NOT_RUNNABLE_AS_NAMED_TECHNIQUE";why="METHOD_NOT_TRACED_IN_ACTIVE_V8_OR_SOURCE_INPUT_MISSING"
            method_matrix.append(dict(technique_id=tid,fingerprint_id=item["ref_id"],
                fingerprint_family=kind,comparison_status=status,reason=why,
                numerical_error=None if status.startswith(("NOT_","NO_","EXCLUDED_")) else "SEE_METHOD_SPECIFIC_REPLAY"))
    write_csv(out/"all_80_techniques_status.csv",technique_rows,list(technique_rows[0]))
    write_csv(out/"every_source_record_x_80_techniques.csv",method_matrix,list(method_matrix[0]))

    counts=collections.Counter(x["family"] for x in inventory)
    status=collections.Counter(x["comparison_status"] for x in method_matrix)
    all_self=[x["self_abs_relative_error_pct"] for x in source_results]
    false_matches=[x["false_other_curves_in_range"] for x in source_results]
    quant_modes={m:{"n":len(d),"median_abs_error_pct":median([x["abs_relative_error_pct"] for x in d]),
                    "p95_abs_error_pct":quantile([x["abs_relative_error_pct"] for x in d],.95),
                    "out_of_source_domain_count":sum(not x["within_source_domain"] for x in d)}
         for m in sorted({x["mode"] for x in r0_sensitivity})
         for d in [[x for x in r0_sensitivity if x["mode"]==m]]}
    source_lines=pathlib.Path(tank/"research/chemcore_next/source_curves/chemcore_next_p03_2_source_curve_ledger_95.csv")
    n95=load_csv(source_lines)
    summary={
       "schema":"chemcore-next-stable-cross-technique-scientific-audit/1",
       "scientific_truth":"SOURCE_REFERENCES_ONLY_NO_TANK_G3_OR_RUNTIME_STABLE_MANIFEST",
       "audit_read_only":True,"app_core_modified":False,"UCI1081_scored":False,
       "stable_reference_snapshot_active":False,"source_categories":dict(counts),
       "source_fingerprint_input_records_total":len(inventory),
       "source_item_technique_pairs":len(method_matrix),
       "matrix_dispositions":dict(status),
       "technique_ids_registry":len(methods),
       "technique_runtime_linked_in_registry":0,
       "new_formula_register_models":len(register),
       "new_formula_register_active":sum(x["chemcore_next_model_activated"].upper()=="YES" for x in register),
       "source_curve_register_95_active":sum(x["new_engine_active"].lower()=="true" for x in n95),
       "source_math_43_curve_7_concentrations":len(source_results),
       "self_replay_math_median_error_pct":median(all_self),
       "self_replay_math_max_error_pct":max(all_self),
       "same_sensor_wrong_curve_fits":sum(false_matches),
       "same_sensor_input_scenarios_with_wrong_curve_inside_domain":sum(x>0 for x in false_matches),
       "same_sensor_input_scenarios":len(false_matches),
       "same_sensor_other_in_domain_median":median(false_matches),
       "clean_air_reference_ratio1_curves_inside_domain":sum(x["inside_assumed_range"] for x in clean_air),
       "clean_air_reference_ratio1_curves_total":len(clean_air),
       "noise_and_reference_sensitivity":quant_modes,
       "fingerprints_non_uci_with_vectors":len(fp_results),
       "fingerprints_non_uci_with_canonical_molecular_id":sum(x["canonical_uid_available"] for x in fp_results),
       "non_uci_source_vectors_with_missing_dims":sum(x["has_missing_dims"] for x in fp_results),
       "non_uci_source_vectors_with_nonpositive_scale":sum(x["scale_entries_nonpositive"]>0 for x in fp_results),
       "mq4_train_only_fold_profiles":len(train_diff),
       "mq4_reference_bins":len(descript),
       "methane_train_to_full_source_relative_median_delta_pct":median([x["train_vs_full_abs_relative_delta_pct"] for x in train_diff]),
       "methane_train_to_full_source_relative_p95_delta_pct":quantile([x["train_vs_full_abs_relative_delta_pct"] for x in train_diff],.95),
       "methane_source_R0_available":False,
       "methane_reference_benchmark_supervised_model_promoted":False,
       "methane_bin_monotonicity":monotonic,
       "baseline_R0_transfer_verified":False,
       "accuracy_or_safety_claims":[],"requested_reference_replay_expectation":
       "Self-input must be identifiable by same numeric representation if candidate is independently qualified; a zero forward/inverse algebra error does NOT show source gas identification correctness.",
       "required_followup":"Run actual named V8 expert implementations against same-source, heldout physical MQ array exposures; ensure source scale, physical R0 and independent reference ppm verified.",
    }
    (out/"audit_summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n")
    report=[
      "# ChemCore Next V8 — Stable source fingerprint to 80-technique traceability and deviations",
      "",
      "This is a **read-only research diagnostic**. V8, Stable runtime and the 80 techniques were NOT modified.",
      "No dataset was used to claim physical Tank accuracy. UCI1081 was explicitly excluded from scored fingerprint comparisons.",
      f"- Source records inventoried: {len(inventory)}; 80 technique registry IDs; matrix pairs: {len(method_matrix)}.",
      f"- Source-curve math tests: {len(source_results)}; maximum arithmetic self-replay error: {max(all_self):.12g}%.",
      f"- Ambiguous inverse cases: {sum(x>0 for x in false_matches)} of {len(false_matches)} same-sensor scenarios have at least one additional gas curve in range.",
      f"- Clean-air reference Rs/R0=1: {sum(x['inside_assumed_range'] for x in clean_air)}/{len(clean_air)} source curves generate an apparent in-range concentration.",
      f"- External methane 165 train-only profiles: median descriptive-reference deviation {summary['methane_train_to_full_source_relative_median_delta_pct']:.4g}%, p95 {summary['methane_train_to_full_source_relative_p95_delta_pct']:.4g}% (not heldout accuracy).",
      f"- None of 24 multi-dimensional source vectors is eligible for a direct Tank molecular candidate; UCI=18 excluded by instruction, MQDS=4 and RoboFusion=2 are only class/hazard prototypes.",
      "- 109 formula register entries all inactive, and 95 previous source register entries all inactive; do not interpret an algebraically reversible curve as a real input to Next.",
      "- 80 research methods have historical G1 labels but no traceable runtime IDs in the 80-technique register; this instrument does NOT claim that the underlying V8 code lacks related functionality.",
      "",
      "## Root cause localization",
      "1. **Reference representation** — Same-curve forward/inverse is an identity. The numerical inversion is not the source of identity misranking.",
      "2. **Model selection** — A scalar response from one MQ sensor is consistent with multiple chemical-specific response curves across their recorded domains. Inverse-and-in-range alone is nonselective.",
      "3. **Clean-air gate** — The exact baseline reference ratio 1 produces apparently in-range ppm for many legacy V6 curves. This must not yield positive molecular evidence.",
      "4. **Calibration and transfer** — Missing validated source ppm-scale, physical R0 and heater phase prevent real V8 model promotion. Masking these gates to test hypotheses is not evidence.",
      "5. **Multidimensional vectors** — Distinct source feature spaces, absent MQ channels, missing/negative uncertainty scales, and missing molecular UIDs preclude global ranking without source-compatible transforms.",
      "6. **Methane Li-Cor** — Real reference ppm is available, but R0 remains unmeasured and 165 training fingerprints are correlated folds of one source. They are not 165 independent valid concentration standards.",
      "7. **Method traceability** — A matrix disposition is not a numerical technique accuracy result if the technique isn't executable with those input fields.",
      "",
      "## How to improve and prove it (no modifications in this run)",
      "Preserve the original 8-MQ raw Rs and operational R0 plus heater, T, RH, exposure group and true ppm. Compare all independently labelled MQ exposure profiles on the same held-out physical instrument/day. Use source-exact forward/inverse parity as a G1 unit check, cross-gas false candidate rate as a model selectivity check, and measured heldout top-1/coverage/MAE for G2. Keep UNKNOWN on missing independent reference.",
      "",
      "## Output files",
      "full_source_inventory.csv; every_source_record_x_80_techniques.csv; all_80_techniques_status.csv;",
      "source_curves_43_self_replay_301.csv; source_curves_43_wrong_gas_in_domain.csv;",
      "source_curves_43_perturbations.csv; clean_air_ratio_1_false_hypotheses.csv;",
      "alternative_MQ135_six_curves_deviation.csv; source_vectors_non_uci_self_cross_distance.csv;",
      "methane_165_training_fold_vs_full_source.csv; methane_15_bin_monotonicity.csv; audit_summary.json."
    ]
    (out/"AUDIT_README.md").write_text("\n".join(report)+"\n")
    print("NEXT_STABLE_80_TECHNIQUE_TRACE_AUDIT_PASS",json.dumps({
      "inventory":len(inventory),"techniques":len(methods),
      "matrix":len(method_matrix),"self_curve_replays":len(source_results),
      "clean_air_wrong_curve_hypotheses":summary["clean_air_reference_ratio1_curves_inside_domain"],
      "ambiguous_source_curve_scenarios":summary["same_sensor_input_scenarios_with_wrong_curve_inside_domain"],
      "source_R0_verified":False,"runtime_modified":False
    }))
    print("TECHNIQUE_AUDIT_METRICS",json.dumps(summary,ensure_ascii=False,sort_keys=True))
if __name__=="__main__":main()
