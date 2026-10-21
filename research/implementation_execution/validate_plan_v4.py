"""Structural V4 work order audit; cannot certify any real chemical measurement."""
import json
from pathlib import Path


ROOT=Path(__file__).resolve().parents[1]


def audit():
    plan=json.loads((ROOT/"implementation_execution/PLAN_80_V4.json").read_text())
    ledger=json.loads((ROOT/"implementation_execution/EXECUTION_LEDGER_2026-10-21.json").read_text())
    progress=json.loads((ROOT/"PROGRESS_80.json").read_text())
    cross=json.loads((ROOT/"systems_analysis/TECH80_CENTRAL_REFERENCE_CROSSWALK_2026-10-21.json").read_text())
    assert plan["schema_version"]=="hazrob.tank.chemcore80.execution-plan-v4"
    assert len(plan["work_items"])==len(ledger["work_items"])==len(progress["techniques"])==len(cross["matrix"])==80
    ids=[x["technique_id"] for x in plan["work_items"]]
    assert len(set(ids))==80
    assert set(ids)=={x["id"] for x in progress["techniques"]}
    assert set(ids)=={x["id"] for x in cross["matrix"]}
    assert set(ids)=={x["id"] for x in ledger["work_items"]}
    assert plan["central_reference_bank"]["source_blob_sha"]==ledger["input_catalog_sha"]==cross["source"]["sha"]
    assert len(plan["batches"])==len(ledger["batch_state"])==8
    seen=set()
    for batch in plan["batches"]:
        ids_batch=batch["techniques"]
        assert len(ids_batch)==10
        assert not (set(ids_batch)&seen)
        seen.update(ids_batch)
    assert len(seen)==80
    assert [x["id"] for x in plan["batches"]]==list(range(1,9))
    assert ledger["baseline"]["G3_Tank_physical"]==0
    legal={"PLANNED_NOT_EXECUTED","G1_RESEARCH_TESTS_PASS_G2_G3_PENDING","CODE_COMMITTED_CI_PENDING","IMPLEMENTED_PENDING_CI","SOFTWARE_TESTS_CI_PASS_G2_PENDING",
           "SOFTWARE_TESTS_CI_PASS_G2_BLOCKED","BLOCKED_NEEDS_REFERENCE"}
    for entry in ledger["work_items"]:
        assert entry["new_implementation_status"] in legal
        assert entry["G3"]=="BLOCKED_REAL_TANK_REFERENCE"
        assert entry["source_refs"]
        assert all(isinstance(x,str) and x for x in entry["source_refs"])
    for entry in plan["work_items"]:
        assert entry["acceptance_criteria"]["g3"].startswith("Independent Tank")
        assert "production" in entry["acceptance_criteria"]
        assert len(entry["research_source_ids"])>0
    n=sum(x["new_implementation_status"]!="PLANNED_NOT_EXECUTED" for x in ledger["work_items"])
    assert sum(x["number_newly_implemented"] for x in ledger["batch_state"])<=n
    print("PASS_V4_STRUCTURE_ONLY: 80 unique IDs, eight ten-ID batches, pinned central references, G3=0")


if __name__=="__main__":
    audit()
