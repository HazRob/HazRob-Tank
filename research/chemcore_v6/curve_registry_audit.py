#!/usr/bin/env python3
"""P06 source-curve inventory audit, not a Tank calibration or model promotion."""
from __future__ import annotations
import argparse
import json
import math
from pathlib import Path
from source_audit import EvidenceError, git_blob_sha1, verify_source_snapshot

BASE=Path(__file__).resolve().parent
ORIGINAL=BASE/"source_snapshots/hr069/mq_research_curves_43.json"
REGISTRY=BASE/"manifests/source_curve_registry_43.json"
BLOB="bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1"
CLASS_LABELS={"lpg","smoke","alcohol"}
EXPECTED_SENSOR_COUNTS={"MQ2":7,"MQ3":6,"MQ4":6,"MQ5":5,"MQ7":5,"MQ8":5,"MQ9":3,"MQ135":6}

def validate(registered=REGISTRY, original=ORIGINAL):
    source_bytes=original.read_bytes()
    if git_blob_sha1(source_bytes)!=BLOB:
        raise EvidenceError("upstream curve set changed or corrupted")
    upstream=json.loads(source_bytes)
    registry=json.loads(registered.read_text(encoding="utf-8"))
    records=registry.get("records",[])
    if (upstream.get("curve_count")!=43 or len(upstream["curves"])!=43
        or upstream["m4"] is not False or registry.get("curve_count")!=43
        or len(records)!=43 or registry.get("original_git_blob")!=BLOB):
        raise EvidenceError("source size, reference or M4 policy invalid")
    if registry.get("source_group_count")!=1:
        raise EvidenceError("43 curves share a research source family, not 43 independent experiments")
    counts={}
    ids=set()
    cases=[]
    for idx,(entry,original_row) in enumerate(zip(records,upstream["curves"]),1):
        sensor=original_row["sensor"]
        counts[sensor]=counts.get(sensor,0)+1
        required={
          "record_id":"MQDATA43-%03d"%idx,
          "dataset_root_id":"MQDATASCIENCE_6_0_0",
          "source_id":upstream["source"]["id"],
          "source_commit":upstream["source"]["commit"],
          "canonical_original_git_blob":BLOB,
          "sensor_model":sensor,
          "chemical_legacy_id":original_row["legacy_id"],
          "original_gas_label":original_row["gas_label"],
          "ratio_a":original_row["ratio_a"],
          "ratio_b":original_row["ratio_b"],
          "min_source_ppm":original_row["min_ppm"],
          "max_source_ppm":original_row["max_ppm"],
          "output_scale":original_row.get("output_scale",1),
          "source_ratio_direction":"RS_OVER_R0",
          "source_r0_definition":None,
          "source_r0_definition_verified":False,
          "source_output_scale_verified":False,
          "source_heater_profile_verified":False,
          "molecular_identity_qualified":False,
          "tank_device_reference_validated":False,
          "status":"USABLE_RESEARCH_WITH_CAVEATS",
          "model_use_status":"RESEARCH_EXPLORATORY",
        }
        for k,value in required.items():
            if type(entry.get(k)) != type(value) or entry.get(k)!=value:
                raise EvidenceError("registry diverges from source / premature promotion: %s %s"%(idx,k))
        if not isinstance(entry.get("risk_flags"),list) or len(entry["risk_flags"])<3:
            raise EvidenceError("missing explanatory risk flags: %s"%idx)
        if entry["record_id"] in ids: raise EvidenceError("duplicate source record ID")
        ids.add(entry["record_id"])
        if entry["original_gas_label"].lower() in CLASS_LABELS and entry["label_semantics"]!="AGGREGATE_OR_CLASS":
            raise EvidenceError("gas class mislabeled as pure chemical")
        if entry["original_gas_label"].lower()=="nh4/nh3" and entry["label_semantics"]!="AMBIGUOUS_CHEMICAL_LABEL":
            raise EvidenceError("NH4/NH3 not a verified molecular label")
        for number in ("ratio_a","ratio_b","min_source_ppm","max_source_ppm","output_scale"):
            if not math.isfinite(entry[number]):raise EvidenceError("nonfinite source parameter")
        if entry["ratio_a"]<=0 or entry["ratio_b"]==0 or entry["min_source_ppm"]<=0 or entry["max_source_ppm"]<=entry["min_source_ppm"]:
            raise EvidenceError("invalid curve parameter/domain")
        cases.append(entry["status"])
    if counts!=EXPECTED_SENSOR_COUNTS:
        raise EvidenceError("source sensor coverage is not original 8-channel MQ family")
    scaled=[r["record_id"] for r in records if r["output_scale"]!=1]
    if len(scaled)!=6 or {r["sensor_model"] for r in records if r["output_scale"]!=1}!={"MQ3"}:
        raise EvidenceError("MQ3 output_scale=50 source cases missing")
    return {
        "schema":"hazrob-v6-curveregistry-audit/1",
        "total_source_curves":len(records),"verified_source_git_blob":BLOB,
        "source_dataset_root_count":1,
        "sensor_counts":counts,
        "mq3_nonunity_output_scale_count":len(scaled),
        "source_r0_qualified_for_tank":0,
        "physically_validated_tank_curves":0,
        "curves_open_to_research_exploration":len(records),
        "research_with_caveats":cases.count("USABLE_RESEARCH_WITH_CAVEATS"),
        "r0_definition_unverified":sum(not r["source_r0_definition_verified"] for r in records),
        "rejection_policy":"NONFINITE_OR_MATHEMATICALLY_UNDEFINED_ONLY",
        "source_metadata_integrity":"PASS",
        "same_origin_curves_not_independent_exposures":True,
        "G3_Tank_validations":0,
        "research_only":True
    }

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",type=Path)
    args=ap.parse_args()
    data=validate()
    if args.out:
        args.out.parent.mkdir(exist_ok=True,parents=True)
        args.out.write_text(json.dumps(data,indent=2,sort_keys=True)+"\n",encoding="utf-8")
    print(json.dumps(data,indent=2,sort_keys=True))
