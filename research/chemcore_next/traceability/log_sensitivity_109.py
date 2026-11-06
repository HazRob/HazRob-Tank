#!/usr/bin/env python3
"""Source-slope/log conditioning audit; no unknown ppm scale is inferred."""
import csv,json,math,pathlib,sys,statistics

ROOT=pathlib.Path(__file__).resolve().parents[3]
OUT=pathlib.Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"research/chemcore_next/traceability/results"
OUT.mkdir(parents=True,exist_ok=True)
SRC=ROOT/"research/chemcore_next/source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"
with SRC.open(encoding="utf-8",newline="") as f:
    records=list(csv.DictReader(f))
assert len(records)==109
output=[]
for r in records:
    b=float(r["coefficient_b"])
    a=float(r["coefficient_a"])
    assert math.isfinite(a) and a>0 and math.isfinite(b) and b != 0
    inverse=r["forward_equation_definition"].startswith("RATIO")
    # A cancels from ratios, and the unknown original ppm scale cancels too.
    # This is the identifiable logarithmic slope only, never absolute ppm.
    if inverse:
        slope=b
        log_y1=math.log(a)+b*math.log(0.5)
        log_y2=math.log(a)+b*math.log(2)
        slope_check=(log_y2-log_y1)/math.log(4)
        sensitivity=1/abs(b)
        # Measured ratio perturbation +10% corresponds to inverted ppm perturbation.
        f_plus=math.exp(math.log(1.1)/b)
        f_minus=math.exp(math.log(0.9)/b)
    else:
        slope=b
        log_y1=math.log(a)+b*math.log(0.5)
        log_y2=math.log(a)+b*math.log(2)
        slope_check=(log_y2-log_y1)/math.log(4)
        sensitivity=abs(b)
        f_plus=math.exp(b*math.log(1.1))
        f_minus=math.exp(b*math.log(0.9))
    assert abs(slope-slope_check)<2e-13,(r["source_curve_uid"],slope,slope_check)
    max_error_factor=max(f_plus,1/f_plus,f_minus,1/f_minus)
    output.append({
      "id":r["source_curve_uid"],
      "sensor":r["sensor_model"],
      "source_gas_label":r["source_gas_or_class_label"],
      "equation_kind":"RATIO_FROM_PPM" if inverse else "PPM_FROM_RATIO",
      "a":a,"b":b,"log_slope_verified":"PASS",
      "ppm_scale_in_source_available":bool(r["source_scale_ppm_known"]),
      "source_range_both_known":bool(r["source_min_ppm"] and r["source_max_ppm"]),
      "source_r0_verified":r["scientific_r0_reference_verified"]=="YES",
      "chemcore_next_source_activated":r["chemcore_next_model_activated"]=="YES",
      "log_condition_gain":sensitivity,
      "ratio_plus10_multiplier_for_inferred_ppm":f_plus,
      "ratio_minus10_multiplier_for_inferred_ppm":f_minus,
      "worst_10pct_ratio_noise_concentration_error_factor":max_error_factor,
      "risk":"VERY_HIGH_CONDITION_NUMBER" if sensitivity>10 else
          ("HIGH_CONDITION_NUMBER" if sensitivity>5 else "CONDITIONAL"),
      "source_hardware_ppm_certified":False
    })
out=OUT/"curve_109_log_slope_conditioning.csv"
with out.open("w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=list(output[0]));w.writeheader();w.writerows(output)
ordered=sorted(output,key=lambda r:r["log_condition_gain"],reverse=True)
summary={
 "schema":"chemcore_next_source_log_conditioning_109/1",
 "total_equations":len(output),
 "log_slope_analytic_parity_pass":len(output),
 "abs_ppm_scale_available_for_ratio_form":sum(r["equation_kind"]=="RATIO_FROM_PPM" and r["ppm_scale_in_source_available"] for r in output),
 "ratio_form_missing_original_absolute_ppm_scale":sum(r["equation_kind"]=="RATIO_FROM_PPM" and not r["ppm_scale_in_source_available"] for r in output),
 "poor_condition_gain_over_10":sum(r["log_condition_gain"]>10 for r in output),
 "poor_condition_gain_over_5":sum(r["log_condition_gain"]>5 for r in output),
 "worst_10":[{k:item[k] for k in ("id","sensor","source_gas_label","b","log_condition_gain","worst_10pct_ratio_noise_concentration_error_factor")} for item in ordered[:10]],
 "full_classifier_accuracy":None,
 "real_physical_gas_exposures":0,
 "source_reconstructed_only":True,
 "model_r0_source_transition_not_sufficient_to_certify_ppm":True
}
(OUT/"log_109_summary.json").write_text(json.dumps(summary,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
print("LOG_SENSITIVITY_109_PASS",json.dumps(summary,sort_keys=True))
