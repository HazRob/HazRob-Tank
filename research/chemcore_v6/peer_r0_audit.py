#!/usr/bin/env python3
"""Recompute historical R0 equations from pinned peer studies, research only.

External authors' measured R0 values are NEVER substituted into the Tank device.
Detect contradictory paper arithmetic and sensor-specific circuit variation.
"""
from __future__ import annotations
import hashlib
import json
import math
from pathlib import Path
from typing import Any
from source_audit import EvidenceError, git_blob_sha1, require_positive_finite, SENSORS

ROOT = Path(__file__).resolve().parent / "source_snapshots" / "r0_peers"
PINS = {
 "MQ_R0_RS_CROSS_STUDY_2026-10-21.json": "03198302d478c9f54cf84a79b5bdec1b600612ca",
 "MQ_R0_ADDITIONAL_5_SOURCES_2026-10-21.json": "34b3cbc64cbc881444fbeefa54fda92448cbedfb",
 "mq_r0_rs_peer_sources_2026-10-21.json": "8565d6f2d9c27bf459d3f63f4350b4b19d9ecf3b",
}

def sources(root: Path = ROOT) -> dict[str,dict[str,Any]]:
    if {x.name for x in root.iterdir()} != set(PINS):
        raise EvidenceError("unrecognized or missing R0 external study source")
    docs = {}
    for name,sha in PINS.items():
        blob=(root/name).read_bytes()
        if git_blob_sha1(blob) != sha:
            raise EvidenceError("peer R0 source SHA modified: "+name)
        docs[name]=json.loads(blob)
    return docs


def report(root: Path = ROOT) -> dict[str,Any]:
    docs=sources(root)
    paper=docs["MQ_R0_RS_CROSS_STUDY_2026-10-21.json"]["paper_2018"]
    issues=[];rows=[]
    for original in paper["rows"]:
        sensor=original["sensor"]
        rs=require_positive_finite(original["Rs_air_ohm"],"paper Rs air")
        f=require_positive_finite(original["ratioAir_paper"],"paper clean-air ratio")
        published=require_positive_finite(original["R0_reported_ohm"],"paper R0")
        computed=rs/f
        relative=(published-computed)/computed
        mismatch=abs(relative) > 0.01  # source arithmetic screen, NOT physical measurement tolerance
        row={"sensor":sensor,"paper_RL_ohm":original["RL_ohm"],
            "paper_Rs_air_ohm":rs,"published_R0_ohm":published,
            "arithmetic_R0_ohm":computed,"relative_discrepancy":relative,
            "arithmetic_mismatch_over_1pct":mismatch,
            "peer_device_only_not_Tank_R0":True}
        rows.append(row)
        if mismatch:
            issues.append(sensor)
    if set(issues)!={"MQ7","MQ8"}:
        raise EvidenceError("unexpected published-source arithmetic discrepancy set")
    if len(rows)!=9 or len(set(r["sensor"] for r in rows))!=9:
        raise EvidenceError("invalid historical sensor list")
    additional=docs["MQ_R0_ADDITIONAL_5_SOURCES_2026-10-21.json"]
    if len(additional["sources"])!=5:
        raise EvidenceError("historical source count changed")
    return {
      "schema":"chemcore-v6-peer-r0-recalculation/1",
      "research_only":True,"device_Tank_R0_authority":False,
      "sources_pinned":len(PINS),
      "paper_2018_sensor_rows":len(rows),"paper_2018_Tank_sensor_models":sum(r["sensor"] in SENSORS for r in rows),
      "paper_arithmetic_issue_sensors":issues,
      "study_specific_RL_ohm":{r["sensor"]:r["paper_RL_ohm"] for r in rows},
      "historical_additional_source_count":len(additional["sources"]),
      "paper_rows":rows,
      "warning":"A paper's R0 is tied to its device, RL, reference gas/heater and ambient conditions; cannot be transferred to a Tank sensor instance."
    }

if __name__=="__main__":
    print(json.dumps(report(),indent=2,sort_keys=True))
