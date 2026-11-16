"""04.10: byte-locked reference experiment split and prediction evaluation.

Requires JSONL source records; hash proves *these supplied bytes* are unchanged,
not that measurements, physical groups or gas labels were independently audited.
"""
import hashlib
import json
from benchmarking_v1 import classification_metrics

def source_split_report(source_bytes, claimed_sha256, *, source_id, sensor_ids):
    if not isinstance(source_bytes,bytes) or not source_bytes:
        raise ValueError("source experiment bytes required")
    real_hash=hashlib.sha256(source_bytes).hexdigest()
    if not isinstance(claimed_sha256,str) or real_hash!=claimed_sha256:
        raise ValueError("source bytes hash mismatch")
    ids=tuple(sensor_ids)
    if not ids or len(set(ids))!=len(ids):
        raise ValueError("fixed channel map required")
    lines=source_bytes.decode("utf-8").strip().splitlines()
    records=[json.loads(line) for line in lines]
    if not records:
        raise ValueError("empty source")
    observed=set();train=set();test=set();train_sessions=set();test_sessions=set();truth=[];pred=[]
    for item in records:
        if item.get("source_id")!=source_id or tuple(item.get("sensor_ids",()))!=ids:
            raise ValueError("source or sensor definition drift")
        group=item.get("exposure_id");split=item.get("partition")
        session=item.get("session_id")
        if not isinstance(session,str) or not session.strip():
            raise ValueError("session_id required for independent heldout validation")
        if not isinstance(group,str) or not group or split not in ("TRAIN","TEST"):
            raise ValueError("exposure split required")
        if (group,item.get("sample_id")) in observed:
            raise ValueError("duplicate temporal or exposure record")
        observed.add((group,item.get("sample_id")))
        if split=="TRAIN":
            train.add(group)
            train_sessions.add(session)
        else:
            test.add(group)
            test_sessions.add(session)
            truth.append(item.get("gas_label"));pred.append(item.get("prediction"))
    if train&test or train_sessions&test_sessions:
        raise ValueError("same exposure or session appears in both partitions")
    if len(train)<3 or len(test)<2:
        raise ValueError("insufficient independent exposure groups")
    metric=classification_metrics(truth,pred)
    return {"source_id":source_id,"raw_byte_sha256_verified":real_hash,
            "sensor_ids":ids,"train_exposure_groups":len(train),
            "train_sessions":tuple(sorted(train_sessions)),
            "test_sessions":tuple(sorted(test_sessions)),
            "heldout_exposure_groups":len(test),"heldout":metric,
            "physical_group_identity_independently_verified":False,
            "G2_real_source_approved":False,"Tank_G3":False,
            "note":"Hash verifies exact submitted JSONL bytes; independent source and instrument certification are separate."}
