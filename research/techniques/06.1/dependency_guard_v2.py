"""06.1 — conservative physical-evidence group gate before log-opinion pooling.

Merely naming two methods does not create independent evidence when they
derive from the same MQ array and recording/session.
"""
from evidence_fusion_v1 import fuse_evidence

def guarded_dependency_fusion(sources, *, prior, minimum_groups=2):
    items=list(sources)
    if not items:
        return {"status":"ABSTAIN","reason":"NO_SOURCE_EVIDENCE","Tank_G3":False}
    seen={}
    for s in items:
        if not isinstance(s,dict):
            raise ValueError("evidence row must be a dict")
        for key in ("group","source_id","physical_session_id","sensor_array_id"):
            if not isinstance(s.get(key),str) or not s[key].strip():
                raise ValueError("required physical dependency provenance: "+key)
        key=(s["source_id"],s["physical_session_id"],s["sensor_array_id"])
        prev=seen.setdefault(key,s["group"])
        if prev!=s["group"]:
            raise ValueError("same physical source/session/array cannot assert different independence groups")
    evidence=[{k:v for k,v in s.items() if k in ("group","probabilities","weight")} for s in items]
    out=fuse_evidence(evidence,prior=prior,minimum_sources=minimum_groups)
    out["source_dependency_keys"]=len(seen)
    out["Tank_G3"]=False
    out["calibrated_probabilities"]=False
    out["warning"]="Heuristic evidence pooling; dependence-group labels need independent physics audit."
    return out
