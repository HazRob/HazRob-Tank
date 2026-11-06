#!/usr/bin/env python3
"""Prepare immutable source-metadata test fixtures. No physical Tank measurements."""
import csv
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "research/chemcore_next/traceability/results"
OUT.mkdir(parents=True, exist_ok=True)
fpath = ROOT / "research/chemcore_next/fingerprints/chemcore_next_p03_3_source_fingerprints_24.json"
cpath = ROOT / "research/chemcore_next/source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"
tpath = ROOT / "research/v8_implementation/results/next_p00_80_activation.csv"
records = json.loads(fpath.read_text(encoding="utf-8"))["records"]
with cpath.open(encoding="utf-8", newline="") as f:
    curves = list(csv.DictReader(f))
with tpath.open(encoding="utf-8", newline="") as f:
    techniques = list(csv.DictReader(f))
assert len(records) == 24 and len(curves) == 109 and len(techniques) == 80, (
    len(records), len(curves), len(techniques))
def cell(v):
    if v is None: return ""
    return str(v).replace("\t", " ").replace("\r", " ").replace("\n", " ")
def arr(values): return ";".join(cell(v) for v in values)
rows=[]
for item in records:
    root=item["source_root_id"]
    model = "MQ135" if root.startswith("UCI1081") else (
        "MQ_SOURCE_ARRAY" if root.startswith("MQDS_") else "ROBOFUSION_ARRAY")
    rows.append(["F",item["id"],root,model,item["feature_space"],
                 item["source_label"],arr(item["vector"]),arr(item["source_scale_raw"]),
                 cell(item.get("canonical_uid")),cell(item.get("eligible_for_direct_tank_candidate"))])
for item in curves:
    eq = "RATIO" if item["forward_equation_definition"].startswith("RATIO") else "PPM"
    rows.append(["C",item["source_curve_uid"],item["source_root_id"],item["sensor_model"],
                 item["source_gas_or_class_label"],eq,item["coefficient_a"],item["coefficient_b"],
                 item["source_scale_ppm_known"],item["source_min_ppm"],item["source_max_ppm"],
                 item["canonical_chemical_uid"],item["candidate_eligibility"],
                 item["chemcore_next_model_activated"]])
(OUT/"source_cases.tsv").write_text(
    "\n".join("\t".join(map(cell,row)) for row in rows)+"\n", encoding="utf-8")
fields=["technique_id","technique_name","prior_v6_status","audit_classification",
        "technique_level_activated","qualification_g1","qualification_g2","qualification_g3",
        "replay_observation"]
with (OUT/"technique_80_execution_inventory.csv").open("w",encoding="utf-8",newline="") as f:
    w=csv.DictWriter(f,fieldnames=fields);w.writeheader()
    for r in techniques:
        w.writerow({**{k:r.get(k,"") for k in fields},
            "replay_observation":"NOT_INSTRUMENTED_IN_THIS_REFERENCE_TRACE" })
summary={
    "source_records":{"fingerprints":len(records),"curves":len(curves),
                      "research_technique_inventory":len(techniques)},
    "curve_source_active_in_reference_register":sum(
        r["chemcore_next_model_activated"]=="YES" for r in curves),
    "curve_source_canonical_uid_present":sum(bool(r["canonical_chemical_uid"]) for r in curves),
    "fingerprint_source_canonical_uid_present":sum(
        item.get("canonical_uid") is not None for item in records),
    "experiment_type":"metadata plus synthetic counterfactual actual Java core trace",
    "physical_tank_data":False,
    "missing_source_parameters_never_backfilled_in_original_mode":True
}
(OUT/"input_manifest.json").write_text(json.dumps(summary,indent=2)+"\n",encoding="utf-8")
print("REFERENCE_TRACE_FIXTURES_PREPARED",json.dumps(summary,sort_keys=True))
