#!/usr/bin/env python3
"""Source-byte pinned arithmetic QA of published MQ R0/Rs studies.

This is a G2 literature/number audit, NOT physical Tank R0 calibration.
No medical/occupational gas-safety use is authorized.
"""
from pathlib import Path
import hashlib
import json
import math

HERE = Path(__file__).resolve().parent
SOURCE_PATH = HERE / "source_snapshots/mq_r0_rs_peer_8source_2026-10-21.json"
PINNED_ORIGINAL_GIT_BLOB = "8565d6f2d9c27bf459d3f63f4350b4b19d9ecf3b"


def load_original():
    raw = SOURCE_PATH.read_bytes()
    prefix = b"blob " + str(len(raw)).encode("ascii") + b"\0"
    actual = hashlib.sha1(prefix+raw).hexdigest()
    if actual != PINNED_ORIGINAL_GIT_BLOB:
        raise ValueError("source changed relative to pinned central library original")
    payload = json.loads(raw)
    if payload.get("source_count") != 8:
        raise ValueError("incorrect count of sources")
    return payload


def audit():
    data = load_original()
    rows = data["baseline_2018_table"]
    if len(rows) != 9 or len({r["model"] for r in rows}) != 9:
        raise ValueError("2018 source table not complete/unique")
    calculations = []
    conflicts = []
    for item in rows:
        rs = float(item["Rs_air_kohm"])
        ratio = float(item["RatioAir"])
        printed = float(item["R0_reported_kohm"])
        if min(rs, ratio, printed) <= 0 or not all(map(math.isfinite,(rs,ratio,printed))):
            raise ValueError("bad published source resistances")
        predicted = rs / ratio
        relative = 100*(predicted/printed - 1)
        flagged = abs(relative) > 1.0
        calculations.append({"sensor":item["model"],
                             "rs_air_kohm":rs, "ratio_air":ratio,
                             "r0_published_kohm":printed,
                             "r0_recomputed_kohm":round(predicted,6),
                             "r0_recompute_disagreement_percent":round(relative,3),
                             "table_internal_conflict":flagged})
        if flagged:
            conflicts.append(item["model"])
    if set(conflicts) != {"MQ7","MQ8"}:
        raise ValueError("expected paper conflicts MQ7 and MQ8 absent or changed")
    mq9 = next(x for x in calculations if x["sensor"]=="MQ9")
    peer = next(x for x in data["comparison_observations"]
                if x["source_id"]=="R0-PEER-02" and x["sensor_model"]=="MQ9")
    diff = 100*(float(peer["R0_kohm"])/mq9["r0_published_kohm"]-1)
    return {
       "scientific_scope":"SOURCE_TABLE_ARITHMETIC_NOT_TANK_CALIBRATION",
       "original_source_git_blob":PINNED_ORIGINAL_GIT_BLOB,
       "original_source_sha256":hashlib.sha256(SOURCE_PATH.read_bytes()).hexdigest(),
       "n_studies_documented":data["source_count"],
       "baseline_sensor_models":len(rows),
       "baseline_rows":calculations,
       "inconsistent_models":conflicts,
       "mq9_cross_source_difference_percent":round(diff,3),
       "mq9_near_match_interpretation":"NUMERIC_AGREEMENT_ONLY_NOT_A_REFERENCE_R0_STANDARD",
       "independent_Tank_exposures":0,
       "new_empirically_validated_Tank_methods":0
    }


def main():
    result = audit()
    dest = HERE / "results/mq_r0_rs_2018_vs_peer_arithmetic_2026-10-21.json"
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":"PASS_R0_SOURCE_ARITHMETIC_QC_ONLY",
        "original_studies":result["n_studies_documented"],
        "conflicts":result["inconsistent_models"],
        "mq9_percent_difference":result["mq9_cross_source_difference_percent"]}))


if __name__ == "__main__":
    main()
