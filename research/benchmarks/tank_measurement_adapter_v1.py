"""Research-only HazRob Tank exposure schema and leak-resistant cohort gate.

No file I/O, no sensor control, no ppm estimation, no certified safety clearance.
This schema is provisional until the exact eight physical MQ devices/wiring is
inspected. Do not interpret parser success as empirical scientific validation.
"""
from datetime import datetime,timezone
from math import isfinite

SCHEMA="tank-exposure-1.0"
REFERENCE_TYPES=("certified_gas","reference_instrument","blank","unknown")
HEATER_PHASES=("HIGH","LOW","CONSTANT","UNKNOWN")

def _nonempty(x):
    return isinstance(x,str) and bool(x.strip())

def _finite(x, *, positive=False, zero_ok=True):
    return (isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x)
            and ((x>0) if positive else (x>=0 if zero_ok else True)))

def _utc_timestamp(value):
    if not _nonempty(value):return False
    try:
        dt=datetime.fromisoformat(value.replace("Z","+00:00"))
        return dt.tzinfo is not None and dt.utcoffset()==timezone.utc.utcoffset(dt)
    except (TypeError,ValueError,AttributeError,OverflowError):
        return False

def validate_exposure(item):
    if not isinstance(item,dict) or item.get("schema_version")!=SCHEMA:
        raise ValueError("schema_version must be tank-exposure-1.0")
    for key in ("record_id","device_id","session_id","exposure_id",
                "firmware_version","measurement_protocol_id"):
        if not _nonempty(item.get(key)):raise ValueError("missing "+key)
    if not _utc_timestamp(item.get("timestamp_utc")):
        raise ValueError("UTC acquisition timestamp required")
    source=item.get("reference_type")
    label=item.get("gas_label")
    concentration=item.get("concentration_ppm")
    if source not in REFERENCE_TYPES or not _nonempty(label):
        raise ValueError("invalid reference_type/gas_label")
    if source=="unknown":
        if label!="UNKNOWN" or concentration is not None:
            raise ValueError("unknown identification cannot contain inferred concentration")
    elif source=="blank":
        if label!="BLANK" or not _finite(concentration) or concentration!=0:
            raise ValueError("blank requires independent true blank, zero ppm")
        if not _nonempty(item.get("reference_id")):
            raise ValueError("blank reference identity required")
    else:
        if label in ("UNKNOWN","BLANK") or not _finite(concentration,positive=True):
            raise ValueError("positive independently referenced gas ppm required")
        if not _nonempty(item.get("reference_id")):
            raise ValueError("gas reference identity required")
    fields=item.get("sensors")
    if not isinstance(fields,list) or not fields:
        raise ValueError("sensors must contain actual physical device readings")
    seen=set()
    for s in fields:
        if not isinstance(s,dict):
            raise ValueError("sensor row required")
        for key in ("sensor_id","sensor_instance_id","r0_calibration_id"):
            if not _nonempty(s.get(key)):raise ValueError("missing "+key)
        if s["sensor_instance_id"] in seen:
            raise ValueError("duplicate physical sensor instance")
        seen.add(s["sensor_instance_id"])
        if not _finite(s.get("rl_ohm"),positive=True):
            raise ValueError("RL must be positive ohm")
        if not _finite(s.get("r0_ohm"),positive=True):
            raise ValueError("R0 must be positive ohm, not mistaken kOhm")
        if s.get("heater_phase") not in HEATER_PHASES:
            raise ValueError("heater phase must be explicitly tagged")
        ts=s.get("time_s");rs=s.get("rs_ohm")
        if not isinstance(ts,list) or not isinstance(rs,list) or len(ts)<2 or len(ts)!=len(rs):
            raise ValueError("time/rs arrays missing or misaligned")
        if any(not _finite(t) for t in ts) or any(ts[i]>=ts[i+1] for i in range(len(ts)-1)):
            raise ValueError("finite increasing time required")
        if any(not _finite(v,positive=True) for v in rs):
            raise ValueError("positive finite Rs required")
        if "raw_adc" in s:
            codes=s["raw_adc"]
            full=s.get("adc_full_scale_code")
            if not isinstance(full,int) or isinstance(full,bool) or full<1:
                raise ValueError("ADC scale code missing")
            if not isinstance(codes,list) or len(codes)!=len(ts) or any(
                    not isinstance(x,int) or isinstance(x,bool) or not 0<=x<=full for x in codes):
                raise ValueError("ADC code range/time mismatch")
            if not _finite(s.get("adc_reference_v"),positive=True) or not _finite(
                    s.get("voltage_divider_gain"),positive=True):
                raise ValueError("ADC reference and divider mapping required")
    env=item.get("BME280")
    if env is not None:
        if not isinstance(env,dict) or not _finite(env.get("humidity_percent")) or env["humidity_percent"]>100:
            raise ValueError("invalid BME humidity")
        if not _finite(env.get("temperature_c"),zero_ok=False) or not _finite(env.get("pressure_hpa"),positive=True):
            raise ValueError("invalid BME temperature/pressure")
    sha=item.get("source_sha256")
    if not isinstance(sha,str) or len(sha)!=64 or
                            any(c not in "0123456789abcdef" for c in sha):
        raise ValueError("source digest must be 64 lowercase SHA256 hex chars")
    return {
        "schema_valid":True,"record_id":item["record_id"],
        "exposure_key":(item["device_id"],item["session_id"],item["exposure_id"]),
        "n_sensor_instances":len(fields),
        "n_sample_values":sum(len(s["rs_ohm"]) for s in fields),
        "sensor_ids":[s["sensor_id"] for s in fields],
        "r0_kohm_by_instance":{s["sensor_instance_id"]:s["r0_ohm"]/1000. for s in fields},
        "environment_present":env is not None,
        "independent_gas_reference_present":source!="unknown",
        "heater_phases_all_known":all(s["heater_phase"]!="UNKNOWN" for s in fields),
        "tank_g3_scientific_validation":False,
        "note":"Parser integrity is not gas identity, concentration accuracy, or device validation.",
    }

def audit_cohort(exposures):
    rows=list(exposures)
    if not rows:raise ValueError("exposures required")
    checks=[validate_exposure(x) for x in rows]
    ids=[r["record_id"] for r in checks]
    keys=[r["exposure_key"] for r in checks]
    if len(set(ids))!=len(ids):
        raise ValueError("duplicate source record IDs")
    if len(set(keys))!=len(keys):
        raise ValueError("duplicate exposure events cannot be counted twice")
    sessions={(r["device_id"],r["session_id"]) for r in rows}
    devices={r["device_id"] for r in rows}
    return {"valid_exposures":len(rows),"distinct_devices":len(devices),
            "distinct_device_sessions":len(sessions),
            "time_scalar_readings":sum(r["n_sample_values"] for r in checks),
            "independent_gas_reference_exposures":sum(r["independent_gas_reference_present"] for r in checks),
            "environment_complete_exposures":sum(r["environment_present"] for r in checks),
            "ready_for_independent_session_splits":len(sessions)>=2,
            "ready_for_independent_device_splits":len(devices)>=2,
            "tank_g3_scientific_validation":False}

def holdout_session(exposures,held_out_device_id,held_out_session_id):
    """Partition BY COMPLETE (device,session); never random signal time points."""
    rows=list(exposures)
    audit_cohort(rows)
    group=(held_out_device_id,held_out_session_id)
    train=[r for r in rows if (r["device_id"],r["session_id"])!=group]
    test=[r for r in rows if (r["device_id"],r["session_id"])==group]
    if not train or not test:raise ValueError("nonempty whole-session folds required")
    if set((r["device_id"],r["session_id"]) for r in train)&set(
            (r["device_id"],r["session_id"]) for r in test):
        raise AssertionError("session leakage")
    if {r["exposure_id"] for r in train}&{r["exposure_id"] for r in test}:
        raise ValueError("same exposure_id in training and test")
    return {"train":train,"test":test,"group_key":group,
            "warning":"Session split is NOT an independent-device or safe chemical validation."}
