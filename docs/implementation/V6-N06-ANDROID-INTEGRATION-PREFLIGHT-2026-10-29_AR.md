# ChemCore Tank V6 — N06 Android integration preflight and no-legacy-fallback gate

**Date:** 2026-10-29 | **Branch:** feature/tank-v6-implementation  
**Status:** code-path audit performed, integration and APK still **NOT DONE**.

## Audited exact repository paths

1. **Existing independent V6 adapter:** `android/app/src/main/java/com/hazrob/tank/v6integration/TankV6TelemetryAdapter.java`.
   - Provides `analyzeSnapshot(JSONObject,...)` calling `com.hazrob.tank.chemcore.v6.ChemCoreTankV6.analyze`, and `toJson` serializing all underlying reference fits, source contradictions, independent fingerprint spaces, chemical evidence ledger.
   - Marks `physical_calibration_proven=false`, `reportable_ppm=false`, `food_models_included=false`, and source-hypothesis verification false.
   - **This source adapter already exists, but Android UI usage was not found in the inspected MainActivity, HazRobHost, MeasurementsTab or reports.** Presence of a bridge is not evidence that user-facing features invoke it.

2. **Current instant measurement flow:** `android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java` imports and invokes `com.servoc.app.chemcore.TankInstantAnalyzer`.
   - `android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java` still calls prior `ChemCoreInferenceEngine.analyze` and has a `legacy_fingerprint_curve_analysis` fallback via `AnalyticsSnapshot`. It uses AcceptedR0Normalizer, Stable models and legacy screeners. **This is not exclusively the new independent V6 gas engine.**
   - A direct wholesale replacement would risk losing calibrated R0 traceability, instant screenshot/report semantics, documented source hypotheses, exception handling and current UI controls. Integration must be deliberate and testable, not a blanket import rename.

3. **Current documented-session analysis:** `android/app/src/main/java/com/servoc/app/chemcore/CompletedSessionChemCoreAnalyzer.java` uses `ChemCoreInferenceEngine.analyze`, `TankStableRuntimeCatalog`, persisted decisions and 60-second archived source data aggregation; contains explicit no-snapshot fallback for field captures.
   - Preserve group-level original source samples, attached R0 per original sample, unique firmware scan-cycle identity and approved session identity. DO NOT replace 60s evidence with terminal snapshot.

4. **Current Android report paths:** `android/app/src/main/java/com/servoc/app/report/TankInstantReportPdf.java`, `FieldSessionReportExporter.java`, `SiteSummaryReportExporter.java`, `SmartFieldSessionPdfWriter.java`, `SmartFieldSessionXlsxWriter.java`.
   - `com.servoc.app.ui.FieldSessionsTab` controls sites, reports and session approval; `FieldSessionApprovalPolicy` is a separate acceptance check. Preserve existing date/site/session scoping, file permissions and export behavior.

5. **ChemCore controlled lab tab:** `android/app/src/main/java/com/servoc/app/ui/ChemCoreAnalysisTab.java` invokes previous `ChemCoreRuntimeCoordinator`, `ChemCoreInferenceEngine`, source libraries and temporal review. V6 gas inference integration must not override or silently change this lab process without explicit end-to-end migration tests.

## N05-H evidence constrains N06 UI claims

The original UCI external open-set experiment [40959780208](https://github.com/HazRob/HazRob-Tank/actions/runs/40959780208) accepted **151/270 genuinely untrained test unknown exposures (55.93%)** as known using all ten UCI source sensors and 96/270 (35.56%) using MQ135 source alone. These are source-domain results, not Tank hardware metrics. Nonetheless they definitively block using the current **source-derived** classification threshold as a certified unknown gas detection gate.

All gas analysis displays and reports must distinguish:
- `source_hypothesis` with raw sensor references, original measured Rs, original saved R0, timestamp, source root, curve metadata and explanatory comparison;
- `reported_identity=UNKNOWN` or `NOT_VALIDATED` unless separately supported Tank G3 physical identity acceptance evidence exists;
- `research_fit_ppm` clearly labelled non-reportable and never silently converted to occupational exposure limit exceedance;
- `open_set_status=SOURCE_BENCHMARK_FAILED_TANK_G3_NOT_VALIDATED` and no fabricated field confidence/LOD.

User-facing candidates must never be discarded solely because one related source fit conflicts, and the `UNKNOWN` placeholder must remain present. No food freshness (meat, fruit) headspace features may enter this path.

## Explicit migration steps and acceptance tests (NOT yet executed)

1. Add versioned `TankV6GasAnalysisRouter` as separate Android service that can invoke only `com.hazrob.tank.chemcore.v6.ChemCoreTankV6`; avoid imports from predecessor ChemCore engine. The adapter must consume the immutable existing telemetry snapshot and include measured and saved source metadata without inventing a missing R0.
2. Build a dedicated `V6ResearchReportModel` preserving all candidate fits, original source conflicts, `UNKNOWN` identity, `reportable_ppm=false`, source schema and version. It should be serializable consistently for instantaneous and approved sessions, with distinct session ID and evidence provenance. No field safety claim without G3.
3. New incremental integration into `MeasurementsTab`, `TankInstantReportPdf`, `CompletedSessionChemCoreAnalyzer` and field/site reports. Enforce **no legacy inference fallback for V6 results**, while retaining previous V5 code untouched for existing sessions until migration is validated.
4. Database migration must have fixtures for original and new records, versioned schema fields, idempotent session finalization, no approval state corruption, no duplicate snapshots, no loss of original Rs/R0 or timestamps, and readable historical reports.
5. Test with Android integration CI and device acceptance, including no connection, malformed telemetry, missing R0, large source disagreement, all-UNKNOWN, no sensor data, app resume/rotation, Chinese/Arabic Unicode text, PDF/Excel output, site deletion/approval and live UI crash regression.
6. Test `TankV6TelemetryAdapter` actual invocation path via counters/mocks; mere Android Java compile is insufficient. Verify all report and UI entry points use new V6 inference only in V6 mode and never fall back to legacy ChemCore for missing or failed V6 results.

**Hard gates:** DO NOT claim N06 complete, APK available, ppm accuracy, calibrated gas identity, field open-set safety or Tank G3. Do not change ESP32 heater behavior or robotic motion decisions.

**Resume:** N06_PRELIM_AUDIT_ONLY / N05H_GATE_HOLD / 78_PARTIAL / 2_NOT_PORTED / TANK_G3_0_80 / NO_V6_APK.
