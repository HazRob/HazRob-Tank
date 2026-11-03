# HazRob Tank V8 — F05 field/site exports: 2026-11-03 continuation checkpoint

## Decision and source invariants
- Owner froze V8 changes and V7 visual style. No new functionality, no firmware mutation, no APK release.
- R03 production: `FieldSessionReportExporter.resolve` -> `SmartFieldSessionPdfWriter.writePdf`; never substitute `V7FieldSessionPdfWriter` (layout-only, blank charts).
- R04 production: `SiteSummaryReportExporter.writePdf`, verified approved sessions only; never substitute `V7SiteVisualPdfWriter` (blank visual template).
- The V7 `V7ReportLayoutKit` and `V7FieldChartLayout` Git blobs remain unchanged and are guarded in G1.
- R01/R02/R05/R06/R07 pixel-differential Android12 suite: run `41556632291` **SUCCESS**, meaning emulator fixture math and chart rendering, not physical Tank G3.

## F05 failure and corrective action
1. `41557242393` failed in `V8MultiSessionSiteFixtureActivity`, assertion 66 **before** second-session site verification; the source generated XLSX was wrongly expected to have 14 worksheets in total.
2. Production `SmartFieldSessionXlsxWriter` actually adds **14 baseline worksheets**:
   executive, session metadata, cycle ledger, hazards, panel 144, evidence, exposure, quality, time series, raw MQ wide, raw MQ long, technical readings, analytical contract, provenance.
3. The stored source-approved V6 appendix `V6ResearchXlsxSheets.append` adds **2 further worksheets** when the approved gas evidence is present (V6 gas research + source conflicts). Correct total: **16**.
4. Changed **test-only** activity `research/v8_implementation/tests/android/V8MultiSessionSiteFixtureActivity.java` (commit `eaf75b847`), shell QA `run_v8_f05_multi_session_site.sh` (commit `a9263f988`) and CI XML assertions `.github/workflows/v8-f05-approved-field-site-report-ci.yml` (commit `7316d6c79`). Production PDF/XLSX writers and the approved V7 layout were **not changed**.
5. Rerun `41557779295` in progress at this checkpoint. Never mark F05 PASS until it completes and verifies: original SHA unchanged, 6-page R03 PDF, 5-page R04 PDF for two independent approved sessions, 16-sheet XLSX, tamper detection, original source ledger and XML validity.

## Next actions
- Query `41557779295`; if FAIL inspect the exact Android fixture assertion/log, identify production defect vs obsolete test expectation, fix only the verified root cause.
- Verify Android12 source-bound report run `41556632291` after any writer modifications.
- Record F05 PASS in machine tracker only after CI verifies full files; obtain source artifacts and inspect chart/content consistency separately.
- Continue F01 UI navigation, F02 capture+R0+relay, F03 Next Stable parity, F08 XLSX and F11 actual Tank hardware.
