# HazRob Tank — ChemCore V6 Checkpoint 18 / N06-B: approved-session read-only research annex

**Date:** 2026-10-30 (Asia/Aden)  
**Branch:** `feature/tank-v6-implementation`  
**Prior checkpoint:** [Checkpoint 17 — instant Android V6](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-17-N06-ANDROID-2026-10-30_AR.md)

## 1. Delivered code changes

### A. Explicit READ-ONLY gas analysis from truly archived field scans

New Android Java class `android/app/src/main/java/com/servoc/app/report/V6FieldSessionResearchAppendix.java`, commit `9fa723bd7cad1dab34b1feb910b6d74220a8aaee`, compilation fix `93104708866f77af12929d99445e5fde87a1f2aa`.

- This is invoked ONLY after the **approved** Field Session is resolved by `FieldSessionReportExporter.resolve`. That resolver verifies every approved cycle is included exactly once and `FieldCycleRepository.verifyApprovedEvidence(cycle)` has validated the saved original SHA-256 of cycle/decision/source telemetry.
- Iterates **approved gas-including cycles only**, never failed, superseded, deleted or particle-only cycles. Selects archived `telemetry_samples` ordered by `t_ms,id`; reads exactly the original `mq_payload`/`env_payload` and `scan_cycle` from each source row.
- Per MQ sensor, de-duplicates repeated Android polls of the same firmware scan; requires **10 distinct eligible source scans**; remembers sample timestamp, original `Rs`, original `R0` if supplied (never current/latest phone R0), and the source scan ID.
- Evaluates an **actually recorded middle-index scan**, not a fabricated median of readings or composite of nonsynchronous sensors; invokes `TankV6TelemetryAdapter.analyzeSnapshot` then `ChemCoreTankV6` only, separately per sensor. No fallback to `ChemCoreInferenceEngine` or Stable for new V6 results.
- Builds a distinct `research_source_sha256` over the exact archived `t_ms + mq_payload + env_payload` rows for independent rechecking; this is NOT the canonical V5 approval digest and never replaces it.
- All displayed chemical source curves are **unverified source hypotheses** with `reported_identity=UNKNOWN`, `reportable_ppm=false`, `G3=false`. If a channel lacks 10 distinct real firmware scans, it yields `INSUFFICIENT_UNIQUE_SOURCE_SCANS`, never terminal-snapshot fallback.
- NO SQLite writes/schema edits/approval changes; existing V5 `CompletedSessionChemCoreAnalyzer`, `ChemCoreDecisionRepository`, Field Sessions site logic, historical approval digest, firmware heater, safety and food modules remain untouched.

**Scientific caveat:** This is a **read-only analytical report appendix**, not replacing primary historical V5 decisions or reclassifying an already-approved field session as V6-validated. It does NOT analyze a simultaneous full eight-MQ time-window/multisensor fusion; each source channel has an independent real archived snapshot.

### B. PDF export wiring

`android/app/src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java`, commits `fdd13b4c6ab0a6b33be1ac35b9aa6a6a3e9fdd73`, `3ba4218aa0eea8d5b0dd03776933b6dd88ad2674`, final PDF pagination `a416fdf2d27707dd6316acb2cd14f2af33d48926`.

- After `FieldSessionReportExporter.resolve` validates approved source data, adds V6 **research-only** PDF appendix pages with original per-channel source scan count/selected Rs/R0 and source SHA trace.
- Retains every existing original V5 report page, charts and approved cycle ledger.
- **Max 3 approved gas cycles per V6 PDF appendix page** so each may show up to 8 MQ sensor lines without exceeding 595-point PDF height. New page total includes appended pages.
- Explicit UNKNOWN, no certified ppm/occupational limits, no automatic chemical identity.

### C. XLSX export wiring

`android/app/src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java`, commits `03c4b5e4f514e5fb957949b6387f25723a86a69f` and `497c17589f9f8ef06ee86d37503984168b735dc7`.

Adds **two independent, research-only** sheets when a verified approved session contains gas cycles, without overwriting any V5 sheet:
1. `V6 بحثي غازات`: per cycle, per sensor, original unique `scan_cycle` count, exact selected source Rs/R0, timestamp, source SHA256, number of source fits, insufficiency notes and `UNKNOWN`.
2. `V6 مصادر وتعارضات`: all individual source curve hypotheses, original reference IDs, source roots, gas/class labels, **research-only** curve ppm, out-of-source-range flags, and source disagreement counts. This is explanatory data, NOT certified concentration or ranking.

### D. Structural/source fixture tests

`research/chemcore_v6/tests/test_v6_approved_session_report_contract.py`, commit `0a1fef9f8d87ff17f681d345a3c501415b21664d`, corrected check `3bff406cf83badb2b5513cb0503e1a376b0cfe75` and added hypothesis/pagination guards through `64cb96f585b6bca9324a0816f3cd178fea3976a4`.

Seven contract checks:
- Required canonical SHA before read-only V6 analysis.
- No writable SQL, no legacy engine fallback, no schema migration.
- Original `scan_cycle` de-duplication, 10 distinct scans minimum, original R0/Rs preserved.
- Approved and gas cycles only.
- PDF V6 appendix additive, pagination guarded 3 cycles/page.
- XLSX **two** separate sheets, source provenance/unknown.
- Synthetic **SQLite** fixture with duplicate polls, malformed JSON, absent R0 and source-session isolation; confirms the SQL read behavior without modifying the DB. **This fixture is NOT a real Android UI test or real field acquisition.**

`.github/workflows/chemcore-v6-android-compile.yml` updated commit `874fbf879e803c53a46648df7af16076e8ff65e4`, which runs Gradle `:app:compileDebugJavaWithJavac` plus the previous 7 instant tests and **7 new** approved-session tests. Source verification workflow also runs the new Python fixture tests.

### CI evidence

- Initial Android compile attempts failed due to PDF color constant (`Color.DARK_GRAY` vs `Color.DKGRAY`) and missing checked `JSONException` declaration from immutable `JSONObject` copy. Both are **fixed**; do not count early failing runs as successful.
- **Final source / final code CI:** [Android Gradle compile + 14 tests 40973174549](https://github.com/HazRob/HazRob-Tank/actions/runs/40973174549) **SUCCESS**, [scientific/source CI 40973174716](https://github.com/HazRob/HazRob-Tank/actions/runs/40973174716) **SUCCESS**, both on final tested source commit `64cb96f585b6bca9324a0816f3cd178fea3976a4` (includes PDF max-three-cycle pagination and both V6 XLSX sheets). Log: `BUILD SUCCESSFUL in 26s`, **7 instant contract checks and 7 approved-session contract/SQLite fixture checks PASS**. Earlier run [40972877496](https://github.com/HazRob/HazRob-Tank/actions/runs/40972877496) passed before the final XLSX and pagination additions.
- The tests are Java compilation and **source/SQL fixture checks only**. PDF bitmap rendering, actual XLSX ZIP inspection after Java exporter execution, Android device/emulator and physical Tank firmware remain **UNTESTED**.

## 2. Scientific and safety status

| Item | Status |
|---|---|
| Current instant Android V6 | Source integration + compile verified Checkpoint 17 |
| Approved field session V6 research appendix | Code wired to PDF/XLSX; compile/structural gates, **not device-tested** |
| Historical V5 decisions & approved source SHA | **Unmodified**; checked before V6 appendices |
| Field capture new V6 primary decision persistence | **NOT ported**; original V5 primary decision remains |
| Multi-sensor simultaneous temporal V6 fusion | **NOT implemented in N06-B** |
| Physical Tank gas identification / concentration | **G3=0/80**, no ppm certification |
| Technique coverage | **78/80 PARTIAL research**, 2 not ported (01.4 heating transient deferred and 09.6 motion safety gated) |
| Food freshness | **Independent and unchanged** |
| APK | **No V6 APK produced by compile-only workflow** |

## 3. Next stage N06-C

- End-to-end approved session **PDF and XLSX rendering tests** via Android emulator or fully compatible instrumented environment; inspect Arabic text, page counts (including gas appendix pages), workbook sheets and original source SHA fields.
- Android database V9/V10 **migration fixtures** with approved historical snapshots and repeated scan_cycle, missing R0/invalid sensor, completed/failed/deleted/approved cycles. No mutation or lost approval permitted.
- Evaluate actual Tank G3 gas reference Rs/R0 measurements with distinct devices/days and concentration holdouts; do not infer ppm safety from external studies.
- Build APK only when the user approves release readiness; current work remains research integration.

**Resume token:** `V6_CHECKPOINT18_N06B_APPROVED_REPORT_READONLY / V5_SHA_UNMODIFIED / ORIGINAL_SCAN_CYCLE_DEDUP_10 / PDF_V6_UNKNOWN_APPENDIX_MAX_3_CYCLES_PAGE / XLSX_TWO_V6_SOURCE_SHEETS / NO_SQLITE_SCHEMA_MIGRATION / NO_LEGACY_FALLBACK_FOR_NEW_V6 / JDK_SOURCE_AND_ANDROID_COMPILE_CHECKS / 78_PARTIAL_2_UNPORTED_G3_0 / NO_APK`.
