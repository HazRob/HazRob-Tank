"""Research-only empirical blank-threshold LOD/LOQ screening; not certified limits."""
from math import isfinite,sqrt

def _number(x):
    return isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x)

def fit_blank_limits(blank_signals,*,sensor_id,gas_label,signal_unit="log10(Rs/R0)",
                     lod_sigma=3.0,loq_sigma=10.0,direction="decreasing"):
    """Thresholds in the same signal domain as blanks; NOT ppm without validated inversion."""
    data=list(blank_signals)
    if not isinstance(sensor_id,str) or not sensor_id or not isinstance(gas_label,str) or not gas_label:
        raise ValueError("sensor and gas labels required")
    if not isinstance(signal_unit,str) or not signal_unit:raise ValueError("signal unit required")
    if len(data)<10 or any(not _number(v) for v in data):
        raise ValueError("at least ten finite independent blank observations required")
    if direction not in ("increasing","decreasing"):raise ValueError("invalid direction")
    if not _number(lod_sigma) or not _number(loq_sigma) or lod_sigma<=0 or loq_sigma<=lod_sigma:
        raise ValueError("require 0 < LOD multiplier < LOQ multiplier")
    mean=sum(data)/len(data)
    sd=sqrt(sum((v-mean)**2 for v in data)/(len(data)-1))
    if sd<=0:raise ValueError("zero blank variance; cannot infer reliable limits")
    sign=1 if direction=="increasing" else -1
    return {"sensor_id":sensor_id,"gas_label":gas_label,"signal_unit":signal_unit,
            "blank_mean":mean,"blank_sd":sd,"n_blanks":len(data),
            "direction":direction,"lod_signal":mean+sign*lod_sigma*sd,
            "loq_signal":mean+sign*loq_sigma*sd,
            "lod_sigma":lod_sigma,"loq_sigma":loq_sigma,
            "warning":"Heuristic blank-sigma signal thresholds, not validated detection/quantitation limits or ppm."}

def classify_signal(signal,model):
    if not _number(signal):raise ValueError("finite observed signal required")
    sign=1 if model["direction"]=="increasing" else -1
    delta=sign*(signal-model["blank_mean"])
    lod=model["lod_sigma"]*model["blank_sd"]
    loq=model["loq_sigma"]*model["blank_sd"]
    if delta<lod:return {"status":"BELOW_SCREENING_LOD","quantifiable":False}
    if delta<loq:return {"status":"DETECTED_NOT_QUANTIFIABLE","quantifiable":False}
    return {"status":"ABOVE_SCREENING_LOQ","quantifiable":False,
            "warning":"Above heuristic LOQ signal threshold only; quantitative accuracy not established."}
