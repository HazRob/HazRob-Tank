"""07.2: Strict leave-physical-device-out; batch labels are not devices."""
def split_physical_devices(rows, *, source_id, heldout_device_ids):
    data=list(rows);held=set(heldout_device_ids)
    if not data or not held: raise ValueError("physical device holdout required")
    train=[];test=[];session_device={}
    for x in data:
        if not isinstance(x,dict) or x.get("source_id")!=source_id:
            raise ValueError("source mismatch")
        for k in ("physical_device_id","physical_sensor_instance_id","session_id","exposure_id"):
            if not isinstance(x.get(k),str) or not x[k]:
                raise ValueError("physically grounded "+k+" required")
        if x["physical_device_id"]=="UNKNOWN" or x["physical_device_id"].startswith("batch-"):
            raise ValueError("do not substitute batch for physical device")
        if x["session_id"] in session_device and session_device[x["session_id"]]!=x["physical_device_id"]:
            raise ValueError("ambiguous session-to-device assignment")
        session_device[x["session_id"]]=x["physical_device_id"]
        (test if x["physical_device_id"] in held else train).append(x)
    if not train or not test: raise ValueError("both partitions must contain physical devices")
    if {x["physical_sensor_instance_id"] for x in train}&{x["physical_sensor_instance_id"] for x in test}:
        raise ValueError("identical physical sensor instance in both devices")
    if {x["exposure_id"] for x in train}&{x["exposure_id"] for x in test}:
        raise ValueError("duplicated exposure across devices")
    return {"train":train,"test":test,"source_id":source_id,
            "status":"PHYSICAL_DEVICE_GROUP_SPLIT_ONLY","Tank_G3":False}
