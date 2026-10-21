#!/usr/bin/env python3
"""Offline reproducibility checks for the HazRob Tank 80-technique work plan.

Structural consistency is a G0 record integrity check, not validation of gas
detection, measured accuracy, or safety-critical production performance.
"""
from pathlib import Path
from collections import Counter
import json

ROOT = Path(__file__).resolve().parents[2]
SA = ROOT / "research/systems_analysis"
RESEARCH = ROOT / "research"

def read(path):
    with path.open(encoding="utf-8") as file:
        return json.load(file)

def main():
    matrix = read(SA / "CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json")
    plan = read(SA / "IMPLEMENTATION_PLAN_80_2026-10-21.json")
    baseline = read(RESEARCH / "PROGRESS_80.json")
    ids = [x["id"] for x in matrix["techniques"]]
    ids_planned = [x["id"] for x in plan["techniques"]]
    ids_baseline = [x["id"] for x in baseline["techniques"]]
    assert len(ids) == len(ids_planned) == len(ids_baseline) == 80
    assert len(set(ids)) == 80
    assert set(ids) == set(ids_planned) == set(ids_baseline)
    assert len(matrix["central_library"]["source_id_snapshot"]) == 80
    source_ids = set(matrix["central_library"]["source_id_snapshot"])
    assert len(source_ids) == 80
    assert matrix["source_link_count"] == 179
    assert all(set(x["source_ids"]) <= source_ids for x in matrix["techniques"])
    expected = {"REDESIGN":26, "ENHANCE":33,"RETAIN":14,"DEFER":7}
    assert dict(Counter(x["decision"] for x in matrix["techniques"])) == expected
    assert dict(Counter(x["decision"] for x in plan["techniques"])) == expected
    batches = plan["batch_schedule"]
    assert len(batches) == 8 and all(len(b["technique_ids"]) == 10 for b in batches)
    assert len(set(x for b in batches for x in b["technique_ids"])) == 80
    assert set(x for b in batches for x in b["technique_ids"]) == set(ids)
    assert all(x["G3_Tank"] == "NOT_VALIDATED" for x in plan["techniques"])
    assert all(x["tank_g3_validated"] is False for x in matrix["techniques"])
    assert plan["inventory"]["G3_Tank_validated"] == 0
    assert matrix["readiness"]["Tank_G3_scientifically_validated"] == 0
    assert baseline["total_techniques"] == 80
    assert baseline["verified_unit_tests"] == 1261
    assert plan["coordination"]["canonical_progress"].startswith("research/PROGRESS_80")
    arabic = (SA/"IMPLEMENTATION_PLAN_80_2026-10-21_AR.md").read_text(encoding="utf-8")
    comparison = (SA/"CHEMCORE80_REFERENCE_COMPARISON_2026-10-21_AR.md").read_text(encoding="utf-8")
    tick = chr(96)
    assert all(tick + x + tick in arabic and tick + x + tick in comparison for x in ids)
    assert len(plan["techniques"]) == len(matrix["techniques"])
    print(json.dumps({"status":"PASS_80_TECHNIQUE_PLAN_STRUCTURE_ONLY",
                      "techniques":len(ids),"batches":len(batches),
                      "reference_source_ids":80,
                      "source_mapping_links":179,
                      "decision_counts":expected},sort_keys=True))

if __name__ == "__main__":
    main()
