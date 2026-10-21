"""04.1: candidate-wise source curve ranking with UNKNOWN concentration.

Manufacturer digitized curves are hypotheses, never per-instance certified ppm.
"""
from math import isfinite, log10, sqrt
from curve_matching_v1 import predict_ratio

def curve_candidate_grid(observed_rs_r0, curves, concentration_grid_ppm, *,
                         sensor_metadata, minimum_channels=2):
    if not isinstance(observed_rs_r0,dict) or not observed_rs_r0:
        raise ValueError("source sensor ratios required")
    if any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<=0
           for v in observed_rs_r0.values()):
        raise ValueError("nonphysical Rs/R0")
    ppm=list(concentration_grid_ppm)
    if not ppm or any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) or x<=0 for x in ppm):
        raise ValueError("positive predeclared ppm search grid required")
    if not isinstance(minimum_channels,int) or isinstance(minimum_channels,bool) or minimum_channels<2:
        raise ValueError("minimum two source channels")
    if not isinstance(curves,dict) or not curves:
        raise ValueError("gas candidate curves required")
    # Compare on exactly same observed channels; never score partial overlaps.
    channels=tuple(sorted(observed_rs_r0))
    if len(channels)<minimum_channels or set(sensor_metadata)!=set(channels):
        raise ValueError("aligned channels and circuit/R0 metadata required")
    ranking=[]
    for gas,by_channel in sorted(curves.items()):
        if not isinstance(gas,str) or not gas or set(by_channel)!=set(channels):
            continue
        compatible=True
        for channel in channels:
            entry=by_channel[channel]
            meta=sensor_metadata[channel]
            if (not isinstance(entry,dict) or not isinstance(meta,dict)
                    or not entry.get("source_id") or
                    (entry.get("r0_definition_id"),entry.get("heater_phase")) !=
                    (meta.get("r0_definition_id"),meta.get("heater_phase"))):
                compatible=False;break
        if not compatible:
            continue
        choices=[]
        for concentration in ppm:
            try:
                e=[(log10(observed_rs_r0[s])-log10(predict_ratio(by_channel[s]["points"],concentration)))**2
                   for s in channels]
            except (ValueError,OverflowError):
                continue
            choices.append((sqrt(sum(e)/len(e)),concentration))
        if choices:
            distance,best_ppm=min(choices)
            ranking.append({"candidate_gas":gas,"min_log_rmse":distance,
                            "hypothesis_ppm_at_min_distance":best_ppm,"sensors_used":channels,
                            "not_calibrated_ppm":True})
    ranking.sort(key=lambda x:(x["min_log_rmse"],x["candidate_gas"]))
    return {"ranking":ranking,"decision":"CANDIDATE_RANKING_ONLY" if ranking else "ABSTAIN_NO_COMPATIBLE_CURVES",
            "measured_gas_identity_verified":False,"Tank_G3":False,
            "note":"Optimization over candidates is NOT proof of calibrated concentration or molecular identity."}
