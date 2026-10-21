"""02.4: segmental mean derivative on one source-labelled MQ time trace.
Research interpretation of the segmentation concept, not reproduction of unpublished source code.
"""
from math import isfinite, log
from statistics import mean, median

def segmental_difference(times_s, rs_ohm, baseline_rs_ohm, *, segments=4,
                         sensor_instance_id, exposure_group_id, source_id,
                         heater_phase, max_gap_factor=3.0):
    for name, value in (("sensor_instance_id",sensor_instance_id),
                        ("exposure_group_id",exposure_group_id),("source_id",source_id)):
        if not isinstance(value,str) or not value.strip():
            raise ValueError(name+" required")
    if heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("known heater phase required")
    if not isinstance(segments,int) or isinstance(segments,bool) or segments<2:
        raise ValueError("invalid segment count")
    if not isinstance(max_gap_factor,(int,float)) or isinstance(max_gap_factor,bool) or not isfinite(max_gap_factor) or max_gap_factor<1:
        raise ValueError("invalid sample gap tolerance")
    if not isinstance(baseline_rs_ohm,(int,float)) or isinstance(baseline_rs_ohm,bool) or not isfinite(baseline_rs_ohm) or baseline_rs_ohm<=0:
        raise ValueError("baseline Rs must be finite positive ohms")
    t=list(times_s);r=list(rs_ohm)
    if len(t)!=len(r) or len(t)<2*segments:
        raise ValueError("at least two samples per segment")
    if any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) or x<=0 for x in r):
        raise ValueError("invalid Rs measurements")
    if any(not isinstance(x,(int,float)) or isinstance(x,bool) or not isfinite(x) or x<0 for x in t):
        raise ValueError("invalid timestamps")
    gaps=[y-x for x,y in zip(t,t[1:])]
    if any(g<=0 for g in gaps):
        raise ValueError("timestamps must increase")
    if max(gaps)>max_gap_factor*median(gaps):
        raise ValueError("sampling gap too large to interpret derivative")
    offsets=[len(t)*i//segments for i in range(segments+1)]
    levels=[mean(log(x/baseline_rs_ohm) for x in r[a:b]) for a,b in zip(offsets,offsets[1:])]
    centers=[mean(t[a:b]) for a,b in zip(offsets,offsets[1:])]
    slopes=[(levels[i+1]-levels[i])/(centers[i+1]-centers[i]) for i in range(segments-1)]
    return {"sensor_instance_id":sensor_instance_id,"exposure_group_id":exposure_group_id,
            "source_id":source_id,"heater_phase":heater_phase,
            "segment_mean_log_ratio":levels,"segment_difference_per_second":slopes,
            "segment_center_s":centers,"ppm_validated":False,
            "method":"SEGMENTAL_MEAN_DIFFERENCE_RESEARCH_VARIANT"}
