#!/usr/bin/env python3
"""V8 UI/report/procedure static inventory. Not an Android device test.

Enumerates every actionable source occurrence in all Main/Entry/UI Java classes,
every report Java class and all exported procedures of measurement/field/chemcore/
calibration/communication. Writes an individual traceability record with every
verification status explicitly NOT_RUN. A source site != a rendered control:
Android UI-tree traversal and hardware acceptance are independent release gates.
"""
import argparse
import hashlib
import json
import pathlib
import re
from collections import Counter
ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA = ROOT / "android/app/src/main/java/com/servoc/app"
SCHEMA = "hazrob.tank.v8.every_UI_control_report_procedure_static_inventory/1"

PATTERNS = {
    "BUTTON": r"\b(?:UiKit\.button|new\s+(?:android\.widget\.)?Button)\s*\(",
    "BUTTON_ACTION": r"\.setOnClickListener\s*\(",
    "CHECKBOX": r"\bnew\s+(?:android\.widget\.)?(?:CheckBox|RadioButton|Switch)\s*\(",
    "CHECKBOX_ACTION": r"\.setOnCheckedChangeListener\s*\(",
    "DROPDOWN": r"\bnew\s+(?:android\.widget\.)?(?:Spinner|RadioGroup)\s*\(|\bUiKit\.outlinedSpinner\s*\(",
    "DROPDOWN_ACTION": r"\.setOnItemSelectedListener\s*\(",
    "INPUT": r"\b(?:UiKit\.input|new\s+(?:android\.widget\.)?EditText)\s*\(",
    "SLIDER": r"\bnew\s+(?:android\.widget\.)?(?:SeekBar|Slider)\s*\(",
    "SLIDER_ACTION": r"\.setOnSeekBarChangeListener\s*\(",
    "DIALOG": r"\bnew\s+(?:android\.app\.)?AlertDialog\.Builder\s*\(",
    "TOUCH": r"\.setOnTouchListener\s*\(|\.setOnLongClickListener\s*\(",
    "REFRESH": r"\.setOnRefreshListener\s*\(",
    "EXPORT_PICKER": r"\b(?:startActivityForResult|createDocument|openOutputStream|exportPdf|exportXlsx)\s*\(",
    "REPORT_ENTRYPOINT": r"\b(?:public|protected)\s+(?:static\s+)?(?:void|File|boolean|byte\[\]|String)\s+(?:write|export|render|generate|buildPdf|buildXlsx)\w*\s*\(",
    "DRAW": r"\b(?:drawLine|drawRect|drawCircle|drawPath|drawText|drawBitmap|drawArc|drawRoundRect)\s*\(",
    "SESSION_PROCEDURE": r"\b(?:public|protected)\s+(?:static\s+)?(?:void|boolean|[A-Za-z][A-Za-z0-9_<>\[\], ?]*)\s+(?:begin|complete|approve|retry|fail|cancel|stop|start|record|persist|recover|save|analyze)\w*\s*\(",
}
PATTERNS = {k: re.compile(v) for k, v in PATTERNS.items()}
SCENARIOS = [
    "normal_operation", "empty_or_invalid_input", "offline_or_hardware_absent",
    "repeat_tap_and_parallel_operation", "process_restart_state_and_navigation",
    "RTL_light_dark_contrast_visual", "result_reconciled_with_saved_decision_uid",
]
QA_STATUS = "NOT_RUN"

def files():
    roots = [JAVA / "MainActivity.java", JAVA / "EntryActivity.java"]
    for folder in ("ui", "report", "field", "measurement", "measurements",
                   "chemcore", "calibration", "chemical", "communication", "data"):
        roots += sorted((JAVA / folder).rglob("*.java")) if (JAVA / folder).exists() else []
    return [p for p in roots if p.is_file()]

def kind_eligible(kind, path):
    p = path.as_posix()
    if kind == "DRAW":
        return "/ui/" in p or "/report/" in p
    if kind == "REPORT_ENTRYPOINT":
        return "/report/" in p
    if kind == "SESSION_PROCEDURE":
        return any(f"/{d}/" in p for d in ("field", "measurement", "chemcore", "calibration"))
    return True

def main():
    arg = argparse.ArgumentParser()
    arg.add_argument("--output", default="build/v8-ui-full-inventory.json")
    args = arg.parse_args()
    entries, modules = [], []
    report_modules = 0
    for src in files():
        relative = src.relative_to(ROOT).as_posix()
        data = src.read_text(encoding="utf-8")
        digest = hashlib.sha256(data.encode("utf-8")).hexdigest()
        total = 0
        if "/report/" in relative:
            report_modules += 1
            entries.append({
                "id": f"MODULE-{src.stem}", "kind": "REPORT_MODULE",
                "file": relative, "line": 1, "offset": 0,
                "source_hash": digest, "source_excerpt": src.name,
                "test_status": {c: QA_STATUS for c in SCENARIOS},
                "rendered_output_verified": False, "android_test_run": None,
                "defect_id": None, "fix_commit_sha": None,
            })
            total += 1
        for line_no, line in enumerate(data.splitlines(), 1):
            for kind, pattern in PATTERNS.items():
                if not kind_eligible(kind, src):
                    continue
                for match in pattern.finditer(line):
                    entries.append({
                        "id": f"{src.stem}:{kind}:{line_no}:{match.start()}",
                        "kind": kind, "file": relative, "line": line_no,
                        "offset": match.start(), "source_hash": digest,
                        "source_excerpt": line.strip()[:240],
                        "same_line_label": (re.findall(r'"([^"]{1,95})"', line) or [None])[0],
                        "test_status": {c: QA_STATUS for c in SCENARIOS},
                        "rendered_output_verified": False,
                        "android_test_run": None,
                        "physical_Tank_test_run": None,
                        "defect_id": None, "fix_commit_sha": None,
                    })
                    total += 1
        modules.append({"file": relative, "source_sha256": digest,
                        "discovered_sites": total, "status": QA_STATUS})
    summary = Counter(x["kind"] for x in entries)
    result = {
        "schema": SCHEMA, "source_scope": "all app UI/handlers, reports, field operations, measurement and analytics",
        "static_inventory_not_runtime_coverage": True,
        "all_controls_verified": False,
        "scientific_G3_confirmed": False,
        "module_count": len(modules),
        "report_module_count": report_modules,
        "detected_control_event_report_procedure_sites": len(entries),
        "by_type": dict(sorted(summary.items())),
        "test_scenarios_required_per_site": SCENARIOS,
        "unexecuted_test_receipts": len(entries) * len(SCENARIOS),
        "dynamic_UI_row_and_popup_inventory_required": True,
        "physical_robot_device_test_required": True,
        "release_gate": "BLOCKED_UNTIL_TESTED_FOR_EACH_RENDERED_FUNCTION",
        "modules": modules, "sites": entries
    }
    assert report_modules >= 20, "V8 report coverage unexpectedly missing source files"
    assert len(entries) >= 100, "V8 UI function coverage unexpectedly small"
    assert any(x["file"].endswith("FieldSessionsTab.java") for x in entries)
    assert any(x["file"].endswith("MeasurementsTab.java") for x in entries)
    assert any(x["file"].endswith("SettingsTab.java") for x in entries)
    assert summary["BUTTON_ACTION"] > 20 and summary["DROPDOWN"] >= 5
    output = ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("V8_UI_FULL_SOURCE_INVENTORY_CREATED",
          "modules",len(modules),"reports",report_modules,"sites",len(entries),
          "unexecuted_scenarios",result["unexecuted_test_receipts"],
          "release_ready",False,"output",str(output.relative_to(ROOT)))

if __name__ == "__main__":
    main()
