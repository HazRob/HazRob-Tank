"""Immutable-ish provenance record validation and canonical digest for research records."""
import json,hashlib
from datetime import datetime,timezone
from math import isfinite

REQUIRED=("schema_version","record_id","device_id","session_id","sensor_id",
          "timestamp_utc","firmware_version","calibration_id","heater_phase",
          "measurement_kind","measurement_unit","sample_count","processing_steps")
ALLOWED_KINDS=("adc","rs","rs_r0","feature")
def validate_provenance(record):
    if not isinstance(record,dict):raise ValueError("record must be mapping")
    if any(k not in record for k in REQUIRED):raise ValueError("missing provenance field")
    if record["schema_version"]!="1.0":raise ValueError("unsupported schema")
    for k in REQUIRED:
        if k in ("sample_count","processing_steps"):continue
        if not isinstance(record[k],str) or not record[k].strip():raise ValueError("invalid "+k)
    if record["measurement_kind"] not in ALLOWED_KINDS:raise ValueError("unsupported measurement")
    if not isinstance(record["sample_count"],int) or isinstance(record["sample_count"],bool) or record["sample_count"]<1:
        raise ValueError("invalid count")
    if not isinstance(record["processing_steps"],list) or any(not isinstance(x,str) or not x for x in record["processing_steps"]):
        raise ValueError("invalid steps")
    try:dt=datetime.fromisoformat(record["timestamp_utc"].replace("Z","+00:00"))
    except ValueError:raise ValueError("invalid timestamp")
    if dt.tzinfo is None or dt.utcoffset().total_seconds()!=0:raise ValueError("UTC timestamp required")
    if "environment" in record:
        env=record["environment"]
        if not isinstance(env,dict) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in env.values()):
            raise ValueError("invalid environment")
    if "parent_record_ids" in record and (not isinstance(record["parent_record_ids"],list) or any(not isinstance(x,str) or not x for x in record["parent_record_ids"])):
        raise ValueError("invalid parent ids")
    try:json.dumps(record,allow_nan=False,sort_keys=True)
    except (TypeError,ValueError):raise ValueError("non-serializable record")
    return True

def provenance_sha256(record):
    validate_provenance(record)
    payload=json.dumps(record,sort_keys=True,separators=(",",":"),ensure_ascii=False,allow_nan=False)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()
