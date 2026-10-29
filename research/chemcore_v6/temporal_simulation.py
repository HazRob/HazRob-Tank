#!/usr/bin/env python3
"""HazRob Tank V6 SIM-B research harness: synthetic physics hypotheses only.

These simple first-order dynamics are intentionally NOT fitted to a Tank sensor,
manufacturer data sheet, real calibration, or benchmark. Their purpose is to
test signal-processing invariants and failure modes before real-data promotion.
"""
import argparse
import json
import math
import random
from pathlib import Path

SENSORS=("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135")
SIMULATED_PHASES=("BASELINE","EXPOSURE","RECOVERY")
DEFAULT_AMPLITUDES=(-.45,-.25,-.28,-.22,-.40,-.31,-.16,-.33)

def response_fraction(t,exposure_start=60,exposure_end=150,rise_tau=25,recover_tau=40):
    if min(rise_tau,recover_tau)<=0 or exposure_end<=exposure_start:
        raise ValueError("invalid synthetic kinetic parameters")
    if t<exposure_start:return 0.
    if t<exposure_end:return 1-math.exp(-(t-exposure_start)/rise_tau)
    at_end=1-math.exp(-(exposure_end-exposure_start)/rise_tau)
    return at_end*math.exp(-(t-exposure_end)/recover_tau)

def phase(t):
    return "BASELINE" if t<60 else "EXPOSURE" if t<150 else "RECOVERY"

def simulate(seed=20261029,days=0,rh=50,temperature=25,noise_sd=.005,
             shared_fraction=.5,missing=(),amplitudes=DEFAULT_AMPLITUDES,
             mixture_interaction=0.,interferent=0.):
    if len(amplitudes)!=len(SENSORS) or not 0<=shared_fraction<=1:
        raise ValueError("invalid synthetic simulation settings")
    if not math.isfinite(noise_sd) or noise_sd<0 or not 0<=rh<=100:
        raise ValueError("invalid synthetic noise or RH")
    if not all(s in SENSORS for s in missing):
        raise ValueError("unknown missing sensor")
    gen=random.Random(seed)
    records=[]
    for t in range(211):
        common=gen.gauss(0,1)*noise_sd*math.sqrt(shared_fraction)
        amount=response_fraction(t)
        data={}
        for i,s in enumerate(SENSORS):
            if s in missing:
                data[s]=None
                continue
            private=gen.gauss(0,1)*noise_sd*math.sqrt(1-shared_fraction)
            # All coefficients below are unvalidated ASSUMPTIONS, not gas physics.
            # Nonlinear interaction explicitly differs from additive pure-gas fits.
            effect=amplitudes[i]*amount + mixture_interaction*(amount**2)*(i+1)/8
            log_ratio=(effect + .002*(rh-50) + .001*(temperature-25)
                      + .0007*days + interferent*amount+common+private)
            data[s]=round(log_ratio,9)
        records.append({"t_s":t,"phase":phase(t),"log10_rs_over_r0":data,
                        "synthetic":True,"physical_device_validated":False})
    return records

def chronology_ok(records):
    times=[r.get("t_s") for r in records]
    return all(isinstance(t,int) and t>=0 for t in times) and all(
        b>a for a,b in zip(times,times[1:]))

def scenario_cases():
    base=simulate(noise_sd=0)
    humid=simulate(noise_sd=0,rh=70)
    drift=simulate(noise_sd=0,days=30)
    mixture=simulate(noise_sd=0,mixture_interaction=.16)
    interferent=simulate(noise_sd=0,interferent=.12)
    dropout=simulate(noise_sd=0,missing=("MQ4","MQ9"))
    random_correlated=simulate(noise_sd=.05,shared_fraction=.80)
    idx=lambda t:base[t]["log10_rs_over_r0"]["MQ2"]
    cases=[
      {"id":"SIM-B01","case":"humidity_shift_20_pct_points",
       "observed_offset_log_ratio":round(humid[100]["log10_rs_over_r0"]["MQ2"]-idx(100),7),
       "expected_type":"synthetic environmental confound"},
      {"id":"SIM-B02","case":"thirty_day_baseline_drift",
       "observed_offset_log_ratio":round(drift[40]["log10_rs_over_r0"]["MQ2"]-idx(40),7),
       "expected_type":"synthetic drift confound"},
      {"id":"SIM-B03","case":"first_order_rise_and_recovery",
       "baseline":idx(40),"exposure":idx(120),"recovery":idx(200),
       "expected_type":"synthetic response dynamics"},
      {"id":"SIM-B04","case":"out_of_order_and_stale_timestamp",
       "monotone_valid":chronology_ok(base),
       "reordered_rejected":not chronology_ok([base[10],base[9]]),
       "duplicate_timestamp_rejected":not chronology_ok([base[10],base[10]]),
       "expected_type":"signal quality validation"},
      {"id":"SIM-B05","case":"six_sensor_overlap_with_two_missing",
       "missing_sensor_mask":list(("MQ4","MQ9")),
       "available_sensor_count":sum(v is not None for v in dropout[100]["log10_rs_over_r0"].values()),
       "expected_type":"no fabricated MQ4/MQ9"},
      {"id":"SIM-B06","case":"shared_common_noise",
       "seed":20261029,"shared_noise_fraction":.80,
       "first_time_sample":random_correlated[0]["log10_rs_over_r0"],
       "expected_type":"correlated source noise does not establish independent evidence"},
      {"id":"SIM-B08","case":"nonadditive_synthetic_mixture",
       "interaction_delta_log_ratio":round(mixture[100]["log10_rs_over_r0"]["MQ8"]-base[100]["log10_rs_over_r0"]["MQ8"],7),
       "expected_type":"hypothetical mixture, cannot identify gases"},
      {"id":"SIM-B09","case":"hypothetical_interferent",
       "interferent_delta_log_ratio":round(interferent[100]["log10_rs_over_r0"]["MQ2"]-idx(100),7),
       "expected_type":"not a measured interference coefficient"},
    ]
    return {"schema":"tank-v6-sim-b/1","seed":20261029,
        "scenario_count":len(cases),"cases":cases,
        "input_kind":"ASSUMED_SYNTHETIC_NOT_MEASURED",
        "original_research_exposures_used":0,
        "Tank_G3_validations":0,"real_ppm_accuracy":None,
        "claims_m4":False}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--out",type=Path)
    args=p.parse_args()
    payload=scenario_cases()
    if args.out:
        args.out.parent.mkdir(parents=True,exist_ok=True)
        args.out.write_text(json.dumps(payload,indent=2,sort_keys=True)+"\n")
    print(json.dumps(payload,indent=2,sort_keys=True))
if __name__=="__main__":main()
