"""02.8: byte-verified external MQ raw/derived record with conservative ppm provenance."""
from hashlib import sha256
from datetime import datetime
from math import isfinite
from re import fullmatch
def audited_record(*,source_bytes, claimed_sha256, source_id, licence, source_url,
                   sensor_models, channel_unit, event_id, session_id, utc_timestamp,
                   identity_kind, parent_sha256=None, reference_ppm=None,
                   reference_certificate=None, doi=None, cas=None,
                   heater_phase=None, measured_rl_ohm=None, measured_r0_ohm=None,
                   vector_kind="SOURCE_RAW"):
    if not isinstance(source_bytes,bytes):
        raise ValueError("raw source bytes must be available")
    measured_hash=sha256(source_bytes).hexdigest()
    if claimed_sha256!=measured_hash:
        raise ValueError("source bytes differ from declared SHA256")
    if channel_unit not in ("ADC_CODE","RS_OHM","RS_R0_RATIO","VOLTS"):
        raise ValueError("measurement unit must be explicit; do not infer Rs from ADC")
    for s in (source_id,licence,source_url,event_id,session_id):
        if not isinstance(s,str) or not s.strip():raise ValueError("missing source metadata")
    if not sensor_models or any(not isinstance(s,str) or not s.strip() for s in sensor_models):
        raise ValueError("sensor models required")
    if identity_kind not in ("PURE_GAS","MIXTURE","COMPLEX_PRODUCT","OPERATIONAL_CLASS","UNKNOWN"):
        raise ValueError("not a known scientific class")
    try:ts=datetime.fromisoformat(utc_timestamp.replace("Z","+00:00"))
    except (ValueError,AttributeError):raise ValueError("invalid timestamp")
    if ts.tzinfo is None or ts.utcoffset().total_seconds()!=0:
        raise ValueError("UTC timestamp required")
    if parent_sha256 is not None and not fullmatch("[0-9a-f]{64}",parent_sha256):
        raise ValueError("invalid parent hash")
    if doi is not None and (not isinstance(doi,str) or not doi.strip()):
        raise ValueError("invalid DOI")
    if cas is not None and (not isinstance(cas,str) or not cas.strip()):
        raise ValueError("invalid CAS")
    if heater_phase is not None and heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("invalid heater reference phase")
    for key, value in (("RL",measured_rl_ohm),("R0",measured_r0_ohm)):
        if value is not None and (isinstance(value,bool) or not isinstance(value,(int,float)) or not isfinite(value) or value<=0):
            raise ValueError(key+" must be positive measured ohms")
    if vector_kind not in ("SOURCE_RAW","DERIVED"):
        raise ValueError("invalid source/derived distinction")
    if vector_kind=="DERIVED" and parent_sha256 is None:
        raise ValueError("derived content requires raw-parent SHA256")
    if channel_unit=="RS_R0_RATIO" and measured_r0_ohm is None:
        raise ValueError("ratio data require R0 definition/measurement context")
    if reference_ppm is not None:
        if identity_kind!="PURE_GAS" or not reference_certificate:
            raise ValueError("concentration standard not applicable or traceable")
        if isinstance(reference_ppm,bool) or not isinstance(reference_ppm,(int,float)) or not isfinite(reference_ppm) or reference_ppm<=0:
            raise ValueError("invalid ppm standard")
    return {"source_id":source_id,"source_sha256":measured_hash,"licence":licence,
            "source_url":source_url,"sensor_models":tuple(sensor_models),
            "channel_unit":channel_unit,"event_id":event_id,"session_id":session_id,
            "utc_timestamp":utc_timestamp,"identity_kind":identity_kind,
            "parent_sha256":parent_sha256,"reference_ppm":reference_ppm,
            "reference_certificate_claimed":reference_certificate,
            "doi":doi,"cas":cas,"heater_phase":heater_phase,
            "measured_rl_ohm":measured_rl_ohm,"measured_r0_ohm":measured_r0_ohm,
            "vector_kind":vector_kind,
            "actual_reference_gas_calibration_verified":False,
            "Tank_G3":False}
