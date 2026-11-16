"""08.7: compare source-matched synthetic and real *paired* sensor observations.
No sim-to-real approval if original physical reference provenance is unverified.
"""
from math import sqrt,isfinite
def paired_source_gap(pairs, *,source_id,sensor_instance_id,heater_phase):
    data=list(pairs);ses=set();exp=set();errs=[];observed=[]
    if len(data)<5 or heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("physical matched source cohort required")
    for x in data:
        if not isinstance(x,dict) or any(x.get(k)!=v for k,v in (
            ("source_id",source_id),("sensor_instance_id",sensor_instance_id),
            ("heater_phase",heater_phase))):
            raise ValueError("source/instance/heater mismatch")
        for k in ("exposure_id","session_id","reference_trace_id"):
            if not x.get(k):raise ValueError("source reference required")
        if x["exposure_id"] in exp:raise ValueError("reused exposure")
        exp.add(x["exposure_id"]);ses.add(x["session_id"])
        sim=x.get("simulated_rs_over_r0");real=x.get("observed_rs_over_r0")
        for value in (sim,real):
            if isinstance(value,bool) or not isinstance(value,(int,float)) or not isfinite(value) or value<=0:
                raise ValueError("finite positive Rs/R0 in same domain required")
        errs.append((sim-real)**2);observed.append(real)
    if len(ses)<3:raise ValueError("independent sessions required")
    return {"rs_over_r0_rmse":sqrt(sum(errs)/len(errs)),
            "relative_rmse_to_mean_observed":sqrt(sum(errs)/len(errs))/(sum(observed)/len(observed)),
            "pairs":len(data),"external_reference_certificate_independently_verified":False,
            "deployment_ready":False,"Tank_G3":False}
