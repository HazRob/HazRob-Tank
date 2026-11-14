"""07.1: Partition complete sessions; never randomly split rows of one exposure."""
def holdout_sessions(records, *, source_id, heldout_session_ids):
    rows=list(records);selected=set(heldout_session_ids)
    if not rows or not selected or any(not isinstance(v,str) or not v for v in selected):
        raise ValueError("session holdout required")
    train=[];test=[];train_exp=set();test_exp=set()
    for row in rows:
        if not isinstance(row,dict) or row.get("source_id")!=source_id:
            raise ValueError("source must be consistent")
        for name in ("session_id","exposure_id","device_id"):
            if not isinstance(row.get(name),str) or not row[name]:
                raise ValueError(name+" is mandatory")
        if row["session_id"] in selected:
            test.append(row);test_exp.add(row["exposure_id"])
        else:
            train.append(row);train_exp.add(row["exposure_id"])
    if not train or not test or train_exp&test_exp:
        raise ValueError("missing partition or same exposure across sessions")
    if {x["session_id"] for x in train}&{x["session_id"] for x in test}:
        raise ValueError("session leakage")
    return {"train":train,"test":test,"heldout_sessions":sorted(selected),
            "status":"GROUPED_SOFTWARE_SPLIT_NOT_DEVICE_VALIDATION","Tank_G3":False}
