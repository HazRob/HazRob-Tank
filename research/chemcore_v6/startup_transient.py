#!/usr/bin/env python3
"""V6-003 MQ power-on transient features: provenance and quality controlled only.

This module NEVER removes the required warm-up, computes ppm, calibrates R0 or
compares MQ heating with a Bosch multi-temperature heater profile.

The first valid resistance sample is a within-window feature reference, NOT R0.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
from typing import Any

SCHEMA="hazrob-tank-mq-startup-transient/1"
MQ=("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135")
WINDOW_END_MS=60000
MIN_POINTS=24
MAX_GAP_MS=5000
VALID_PHASES={"POWER_ON_TRANSIENT","STABLE_GAS_EXPOSURE_TRANSIENT"}
VALID_HEATER_STATES={"VENDOR_NOMINAL_FIXED","VENDOR_VALIDATED_CYCLIC"}


def _valid_number(x: Any) -> bool:
    return type(x) in (int,float) and math.isfinite(x)


def _rejected(reason: str, record: dict[str,Any]) -> dict[str,Any]:
    return {
        "schema":SCHEMA,"status":"REJECTED_QC","reason":reason,
        "device_id":record.get("device_id"),"sensor_id":record.get("sensor_id"),
        "power_event_id":record.get("power_event_id"),"phase":record.get("phase"),
        "gas_identity":None,"ppm_estimate":None,"R0_calibration":None,
        "Tank_G3_qualification":False,
        "heater_profile_is_bosch_scan":False,
    }


def extract(record: dict[str,Any]) -> dict[str,Any]:
    """Extract research-only first 60 s features for ONE sensor and ONE power event.
    
    record has device_id, sensor_id, power_event_id, phase, heater_state,
    samples [{t_ms, rs_positive}]. The caller must obtain t_ms from ESP32
    relay.power_ms or a verified monotonic timestamp relative to that edge.
    """
    if not isinstance(record,dict):
        return _rejected("INVALID_RECORD",{})
    for k in ("device_id","sensor_id","power_event_id","phase","heater_state"):
        if not isinstance(record.get(k),str) or not record[k].strip():
            return _rejected("MISSING_"+k.upper(),record)
    if record["sensor_id"].upper() not in MQ:
        return _rejected("UNRECOGNIZED_MQ_CHANNEL",record)
    if record["phase"] not in VALID_PHASES:
        return _rejected("UNSUPPORTED_PHASE_NOT_MQ_TRANSIENT",record)
    if record["heater_state"] not in VALID_HEATER_STATES:
        return _rejected("HEATER_STATE_UNVERIFIED",record)
    # Heater state is not a declaration of physical sensor variant. MQ7/9
    # specifically must never be taken as validated based on a fixed-5V flag.
    if record["sensor_id"].upper() in ("MQ7","MQ9") and \
       record["heater_state"]!="VENDOR_VALIDATED_CYCLIC":
        return _rejected("MQ7_MQ9_HEATER_VARIANT_CYCLE_NOT_VALIDATED",record)
    if record.get("relay_restarted",False) or record.get("power_event_changed",False):
        return _rejected("POWER_EVENT_CHANGED",record)
    arr=record.get("samples")
    if not isinstance(arr,list) or len(arr)<MIN_POINTS:
        return _rejected("INSUFFICIENT_SAMPLE_COUNT",record)
    observed=[]
    last=-1
    for s in arr:
        if not isinstance(s,dict):
            return _rejected("INVALID_SAMPLE_RECORD",record)
        t,rs=s.get("t_ms"),s.get("rs")
        if not _valid_number(t) or not _valid_number(rs) or t<0 or rs<=0:
            return _rejected("INVALID_TIME_OR_RESISTANCE",record)
        if t<=last or t>WINDOW_END_MS:
            return _rejected("OUT_OF_ORDER_OR_OUTSIDE_CAPTURE_WINDOW",record)
        # Values are in the firmware's original Rs unit, not assumed Ω.
        observed.append((float(t),float(rs)))
        last=t
    times=[a for a,b in observed]
    if times[0]>2000 or times[-1]<58000:
        return _rejected("FIRST_LAST_COVERAGE_INSUFFICIENT",record)
    gaps=[b-a for a,b in zip(times,times[1:])]
    if gaps and max(gaps)>MAX_GAP_MS:
        return _rejected("GAP_EXCEEDS_5_SECONDS",record)
    def band(t0,t1):
        return [(t,x) for t,x in observed if t0<=t<t1]
    first=band(0,10000);mid=band(20000,40000);last_band=band(50000,60001)
    if len(first)<4 or len(mid)<4 or len(last_band)<4:
        return _rejected("TIMING_BANDS_UNDERSAMPLED",record)
    # First 4 observations form a *short-window* reference only: sensor could
    # already be hot or moving; this is NOT a clean-air calibration R0.
    reference=sum(x for t,x in observed[:4])/4
    if reference<=0:
        return _rejected("INVALID_WINDOW_REFERENCE",record)
    shape=[(t,math.log10(rs/reference)) for t,rs in observed]
    def slope(nodes):
        n=len(nodes)
        mx=sum(t for t,v in nodes)/n
        my=sum(v for t,v in nodes)/n
        denom=sum((t-mx)**2 for t,v in nodes)
        return 1000*sum((t-mx)*(v-my) for t,v in nodes)/denom if denom else float("nan")
    def trapz(t0,t1):
        # No extrapolation to unobserved bounds; integrates measured nodes.
        selected=[(t,v) for t,v in shape if t0<=t<=t1]
        if len(selected)<2:return None
        return sum((v1+v2)/2*(t2-t1)/1000 for (t1,v1),(t2,v2) in zip(selected,selected[1:]))
    early=[(t,v) for t,v in shape if t<20000]
    late=[(t,v) for t,v in shape if t>=40000]
    early_s=slope(early) if len(early)>=4 else float("nan")
    late_s=slope(late) if len(late)>=4 else float("nan")
    if not (math.isfinite(early_s) and math.isfinite(late_s)):
        return _rejected("UNDEFINED_TEMPORAL_SLOPE",record)
    auc20=trapz(0,20000);auc60=trapz(0,60000)
    if auc20 is None or auc60 is None:
        return _rejected("INSUFFICIENT_AUC_COVERAGE",record)
    return {
        "schema":SCHEMA,"status":"FEATURES_RESEARCH_ONLY",
        "device_id":record["device_id"],"sensor_id":record["sensor_id"],
        "power_event_id":record["power_event_id"],"phase":record["phase"],
        "heater_state":record["heater_state"],
        "source_resistance_units":"SAME_SOURCE_ORIGINAL_UNIT_UNVERIFIED",
        "sample_count":len(observed),"max_gap_ms":max(gaps) if gaps else 0,
        "first_sample_ms":times[0],"last_sample_ms":times[-1],
        "window_reference_rs":reference,
        "early_slope_log10_per_sec":early_s,
        "late_slope_log10_per_sec":late_s,
        "auc20_log10_seconds":auc20,"auc60_log10_seconds":auc60,
        "last_log10_ratio_to_first_window":shape[-1][1],
        "max_abs_log10_shape":max(abs(v) for t,v in shape),
        "absolute_ppm_model":None,"gas_identity":None,
        "ppm_estimate":None,"R0_calibration":None,
        "Tank_G3_qualification":False,
        "heater_profile_is_bosch_scan":False,
        "warmup_may_be_skipped":False,
        "reason":"EXPLORATORY_MQ_TRANSIENT_FEATURES_NOT_STABLE_GAS_MEASUREMENT",
    }


def demo() -> dict[str,Any]:
    # Single deterministic synthetic startup response, unrelated to measured gas.
    arr=[{"t_ms":t*1000,"rs":10000-2500*(1-math.exp(-t/11))}
         for t in range(61)]
    r=extract({"device_id":"SYNTHETIC_TEST_DEVICE","sensor_id":"MQ2",
       "power_event_id":"SYNTHETIC_RELAY_EDGE_0001","phase":"POWER_ON_TRANSIENT",
       "heater_state":"VENDOR_NOMINAL_FIXED","samples":arr})
    r["synthetic"]=True
    r["evidence_kind"]="ASSUMED_FIRST_ORDER_NO_REAL_GAS"
    return r


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--demo-synthetic",action="store_true")
    p.add_argument("--input-json",type=Path)
    p.add_argument("--out",type=Path)
    args=p.parse_args()
    if not args.demo_synthetic and not args.input_json:
        p.error("provide --demo-synthetic or --input-json")
    output=demo() if args.demo_synthetic else extract(json.loads(args.input_json.read_text()))
    text=json.dumps(output,indent=2,sort_keys=True,ensure_ascii=False)+"\n"
    if args.out:
        args.out.parent.mkdir(parents=True,exist_ok=True)
        args.out.write_text(text)
    print(text,end="")

if __name__=="__main__":main()
