"""G2-03: strict source-compatibility audit and real waveform filtering.

10 HazRob research techniques reviewed; only 02.1 is eligible for raw unknown-
unit numerical time-series filtering. All other techniques require independent
electrical, calibration or heater/environment metadata absent from UCI1081.
No external sensor voltages are misrepresented as MQ ADC, Rs, R0 or ppm.
"""
import argparse
import json
import math
import statistics
from pathlib import Path
from uci1081_actual_modules_v1 import parse_archive,repo_module,SENSORS

TARGET_IDS=("01.1","01.2","01.3","01.4","01.5","01.6",
            "01.7","02.1","02.3","05.1")
BLOCKED={
    "01.1":("BLOCKED_MISSING_ADC_DIVIDER_METADATA",
            ("adc_code","full_scale_code","full_scale_voltage_V",
             "voltage_divider_gain","Vc_V","RL_ohm"),
            "Source values have no verified ADC quantization or RL/Vc topology."),
    "01.2":("BLOCKED_MISSING_R0_CALIBRATION",
            ("Rs_ohm","reference_gas_calibration_ratio","heater_phase"),
            "Cannot convert arbitrary source voltage/response into MQ R0."),
    "01.3":("BLOCKED_MISSING_VALIDATED_MQ_CURVE",
            ("MQ_sensor_model","Rs_over_R0","manufacturer_curve_reference",
             "calibrated_ppm_domain"),
            "Real exposures alone do not supply manufacturer-specific MQ Rs/R0 curves."),
    "01.4":("BLOCKED_MISSING_HEATER_PHASE_LOG",
            ("heater_voltage_V","heater_phase","heater_cycle_timing_s"),
            "The UCI exposure protocol is not an MQ7/MQ9 heater-voltage schedule."),
    "01.5":("BLOCKED_MISSING_R0_AND_BME",
            ("Rs_ohm","temperature_C","RH_pct","pressure_hPa",
             "fit_reference_conditions"),
            "No paired controlled BME readings or reference Rs."),
    "01.6":("BLOCKED_MISSING_ADC_CODE_PROVENANCE",
            ("raw_adc_codes","adc_rail_min","adc_rail_max","ADC_topology"),
            "The 01.6 implementation assumes genuine ADC codes and rail limits."),
    "01.7":("BLOCKED_MISSING_CONTROLLED_RESISTANCE_BASELINES",
            ("paired_Rs_ohm","controlled_reference_gas","heater_phase",
             "environment_match","session_or_device_id"),
            "Time-separated baseline portions within one exposure cannot establish drift."),
    "02.3":("BLOCKED_MISSING_RS_OHM",
            ("baseline_Rs_ohm","exposure_Rs_ohm","Tank_sensor_mapping"),
            "The 02.3 metrics explicitly return ohms; arbitrary source values have unknown units."),
    "05.1":("BLOCKED_MISSING_KNOWN_GAS_RS_R0",
            ("verified_Rs_ohm","measured_R0_ohm","MQ_sensor_id",
             "calibration_gas","known_reference_ppm"),
            "Even labelled 0.05/0.10/0.20 ppm exposures cannot provide a calibrated MQ curve without measured Rs/R0."),
}

def audit_compatibility():
    module=repo_module("02.1","filtering_v1.py")
    if not all(callable(getattr(module,f,None)) for f in
               ("causal_median","causal_ema","causal_moving_average")):
        raise ValueError("actual 02.1 module does not expose expected algorithms")
    results={}
    for ident in TARGET_IDS:
        if ident=="02.1":
            results[ident]={
                "status":"EXTERNAL_ARRAY_NUMERIC_SIGNAL_COMPATIBLE",
                "source_fields":["sample_order","finite_numeric_channel_values"],
                "physical_signal_unit":"UNVERIFIED",
                "physical_gas_calibration":"NOT_SUPPORTED",
                "note":"Causal filtering valid on generic waveform samples; not validated MQ accuracy."}
        else:
            status,fields,reason=BLOCKED[ident]
            results[ident]={"status":status,"required_fields_missing":list(fields),
                            "note":reason}
    if len(results)!=10 or sum(not x["status"].startswith("BLOCKED") for x in results.values())!=1:
        raise AssertionError("eligibility audit drift")
    return results

def mad(x):
    center=statistics.median(x)
    return statistics.median(abs(v-center) for v in x)

def onset_half_crossing(x,baseline_median,final_median):
    delta=final_median-baseline_median
    if abs(delta)<1e-10:
        return None
    midpoint=baseline_median+delta*.5
    for i in range(300,900):
        if (x[i]>=midpoint if delta>0 else x[i]<=midpoint):
            return i-300
    return None

def numeric_signal_experiment(rows):
    module=repo_module("02.1","filtering_v1.py")
    totals={"windows":0,"sensor_models":set(),"exposure_records":set(),
            "raw_valid_points":0}
    deltas={"raw":[],"causal_median_5":[],"causal_moving_average_5":[],
            "causal_ema_alpha_0_25":[]}
    raw_baseline_mads=[]
    baseline_mad={"raw":[],"causal_median_5":[],
                  "causal_moving_average_5":[],"causal_ema_alpha_0_25":[]}
    onset_diffs={k:[] for k in deltas if k!="raw"}
    for row in rows:
        totals["exposure_records"].add(row["source_row"])
        for sensor_name,signal in zip(SENSORS,row["trace"]):
            if len(signal)!=900 or any(not math.isfinite(v) for v in signal):
                raise ValueError("invalid measured time-series")
            xs=list(signal)
            variants={
                "raw":xs,
                "causal_median_5":module.causal_median(xs,window=5),
                "causal_moving_average_5":module.causal_moving_average(xs,window=5),
                "causal_ema_alpha_0_25":module.causal_ema(xs,alpha=.25)
            }
            if any(len(v)!=len(xs) for v in variants.values()):
                raise AssertionError("filter length mismatch")
            for name,series in variants.items():
                if abs(series[0]-xs[0])>1e-12:
                    raise AssertionError("causal filters must not shift first sample")
                b=statistics.median(series[180:300])
                end=statistics.median(series[780:900])
                deltas[name].append(end-b)
                baseline_mad[name].append(mad(series[180:300]))
            raw_start=onset_half_crossing(xs,
                                         statistics.median(xs[180:300]),
                                         statistics.median(xs[780:900]))
            for name,series in variants.items():
                if name=="raw":continue
                filtered_start=onset_half_crossing(
                    series,statistics.median(series[180:300]),
                    statistics.median(series[780:900]))
                if raw_start is not None and filtered_start is not None:
                    onset_diffs[name].append(filtered_start-raw_start)
            totals["windows"]+=1
            totals["raw_valid_points"]+=len(xs)
            totals["sensor_models"].add(sensor_name)
    if totals["windows"]!=900 or totals["raw_valid_points"]!=810000 or len(totals["exposure_records"])!=90:
        raise AssertionError("unexpected source inclusion")
    summaries={}
    for name in deltas:
        mads=baseline_mad[name]
        summaries[name]={
            "n_sensor_exposure_windows":len(mads),
            "median_baseline_mad_source_units":statistics.median(mads),
            "mean_baseline_mad_source_units":statistics.mean(mads),
            "median_absolute_baseline_to_exposure_change_source_units":
                statistics.median(abs(d) for d in deltas[name]),
        }
        if name!="raw":
            lag=onset_diffs[name]
            summaries[name]["n_onset_halftime_pairs"]=len(lag)
            summaries[name]["median_onset_halfstep_lag_vs_unfiltered_samples"]=(statistics.median(lag) if lag else None)
    raw_med=summaries["raw"]["median_baseline_mad_source_units"]
    for name in summaries:
        summaries[name]["baseline_mad_median_ratio_to_unfiltered"]=(
            summaries[name]["median_baseline_mad_source_units"]/raw_med if raw_med else None)
    return {
        "source_records":len(totals["exposure_records"]),
        "sensor_channel_count":len(totals["sensor_models"]),
        "channel_exposure_windows":totals["windows"],
        "source_numeric_points":totals["raw_valid_points"],
        "sample_frequency_hz":1,
        "median_baseline_window_samples":"180:300 (120 samples per sensor)",
        "injection_late_window_samples":"780:900 (120 samples per sensor)",
        "filter_selection":"predeclared EMA alpha=0.25, median window=5, moving-average window=5",
        "metric_notes":"Median absolute deviation within baseline is descriptive sensor-noise proxy; source electrical unit is unspecified. A smoothing latency or noise reduction is not improvement in chemical identification.",
        "results":summaries,
    }

def evaluate(path):
    rows=parse_archive(path)
    checked=audit_compatibility()
    eligible=numeric_signal_experiment(rows)
    return {
        "project":"HazRob Tank research-only; G2-03 measured external source",
        "original_source":"UCI1081 DOI 10.24432/C5CK6F, CC BY 4.0",
        "source_archive_sha256":"813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab",
        "source_exposures":90,
        "target_ids":list(TARGET_IDS),
        "n_eligible_techniques":1,
        "n_blocked_techniques":9,
        "source_compatibility":checked,
        "02.1_real_external_measured_array_filter_results":eligible,
        "empirical_Tank_G3_qualified":0,
        "notes":[
            "Source is external ten-channel array, not Tank eight MQ array.",
            "No measured MQ divider rail, RL, R0, heater phase, BME or known raw voltage unit.",
            "02.1 is evaluated as generic causal signal-processing; no gas recognition/ppm performance tested.",
            "All nine physically conditioned methods blocked from numeric interpretation, notwithstanding G1 synthetic software tests.",
            "Need real calibrated Tank hardware sessions for the other nine; no fabricated data or coefficients used."
        ],
    }
if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("original_uci1081_zip")
    parser.add_argument("--output",default="uci1081_g2_batch03.json")
    args=parser.parse_args()
    result=evaluate(args.original_uci1081_zip)
    Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    print("G2-03 eligibility: 1 executed on measured external response streams; 9 blocked due to absent electrical/calibration metadata")
    print("02.1:",json.dumps(result["02.1_real_external_measured_array_filter_results"]["results"]))
