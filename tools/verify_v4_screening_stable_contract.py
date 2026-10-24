#!/usr/bin/env python3
"""Read-only V4 source policy regression; JVM, Gradle, device tests are separate."""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
folder = root / "android/app/src/main/java/com/servoc/app"

def java(name):
    return (folder / name).read_text(encoding="utf-8")

def has(source, needle):
    assert needle in source, f"missing V4 analytical contract: {needle}"

def absent(source, needle):
    assert needle not in source, f"regressed V4 analytical contract: {needle}"

stable = java("chemcore/TankStableScreeningModelRegistry.java")
analysis = java("chemcore/TankInstantAnalyzer.java")
warning = java("chemcore/TankScreeningExposureWarning.java")
limits = java("chemcore/TankExposureReferenceCatalog.java")
ui = java("ui/MeasurementsTab.java")
pdf = java("report/TankInstantReportPdf.java")

for part in (
    "ChemCoreLibraryStore store=new ChemCoreLibraryStore(context)",
    "store.activePayload()",
    "store.activeSha256()",
    'MessageDigest.getInstance("SHA-256")',
    "hash.equalsIgnoreCase(hex.toString())",
    "knowledge.optJSONObject",
    'knowledge==null?null:knowledge.optJSONObject("response_priors")',
    'counts.optInt("response_priors",-1)',
    "SELECT legacy_id,uid FROM chemicals",
    "model.minPpm()<prior.maxPpm()",
    "old.models",
    "replacement.contains(model.sensorKey())",
    "reportableQuantitative(){return false;}",
    "LOG10_LINEAR",
    "source_id",
):
    has(stable, part)
for part in (
    "TankStableScreeningModelRegistry.load(androidContext,db,sourceBacked)",
    "activeStable.candidates",
    'output.put("screening_m4_required",false)',
    'output.put("screening_ppm_not_a_certified_measurement",true)',
    'output.put("ppm_reportable",false)',
    "TankScreeningExposureWarning.evaluate(output)",
    "TankExposureReferenceCatalog.forAnalysis(db,output)",
):
    has(analysis, part)
for part in (
    'out.put("m4_required_for_screening_warning",false)',
    '"SUSTAINED_EXPOSURE_HYPOTHETICAL"',
    '"POSSIBLE_LFL_REACHED"',
    '"PRECAUTIONARY_LFL_APPROACH"',
    '"INSTANTANEOUS_REFERENCE_SCREENING"',
    'out.put("time_weighted_average_evaluated",false)',
    'out.put("no_safety_clearance",true)',
    '"mg/m3"',
):
    if part == '"mg/m3"':
        absent(warning, 'mgM3ToPpm(')
    else:
        has(warning, part)
has(limits, 'out.put("calibration_required_to_display",false)')
has(limits, 'out.put("comparison_with_estimated_ppm_permitted",true)')
for part in (
    "TankInstantAnalyzer.analyze(c,host.database(),frozen,now)",
    "screeningWarnings.setText(TankScreeningExposureWarning.summary",
    "referenceInventory.setText(TankExposureReferenceCatalog.summary",
    "showSelectedExposureReferences()",
    "styleScreeningWarning(screeningWarnings,p)",
):
    has(ui, part)
for part in ("screening_exposure_warnings", "exposure_reference_inventory",
             "active_stable_reference_models", 'doc.startPage(new PdfDocument.PageInfo.Builder(595,842,2)'):
    has(pdf, part)

print("PASS: active SHA-verified Stable non-M4 models, independent PPM, instant conditional TWA/LFL, unmodified regulatory gate, UI/PDF source provenance")
