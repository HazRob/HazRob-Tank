# HazRob Tank V4 Android + ChemCore — durable execution log
Date: 2026-10-27
Branch: feature/tank-v4-mq-unified-r0
Status: PLANNED / SOURCE AUDIT IN PROGRESS. No Android V4 APK approved.
Authoritative requirements: [V4 compatibility audit](V4-FIRMWARE-ANDROID-CHEMCORE-COMPATIBILITY-AUDIT-2026-10-27.md).
Firmware reference: GitHub Actions 40414404284 (compile success only).

## Resumption protocol
1. Check branch HEAD, this log and latest GitHub Actions before changing files.
2. Find first IN_PROGRESS task, else first TODO; verify previous commits and tests before trusting statuses.
3. Record each change: task ID, files, commit SHA, test invocation, observed result, known limitations, next exact step. Update this file in the same work period.
4. Preserve user SQLite data and independent Tank app identity. No claim of device tests without evidence.
5. Do not publish a stable APK until release gate V4-A10 is PASS.

## Task board
| ID | Scope | Status | Evidence / next action |
|---|---|---|---|
| V4-A01 | Baseline inventory: branch, DB, UI, logo and Gradle | IN_PROGRESS | Inspected FieldSessionsTab, UiKit, EntryActivity, RobotApi, TankAutoR0Math, AcceptedR0Normalizer, TankInstantAnalyzer; next inspect Android manifest/resources, schema, tests and logo history |
| V4-A02 | Sessions crash/ANR reproduction and fix | TODO | Test fresh and upgraded SQLite, Spinner callbacks, main-thread load, Logcat |
| V4-A03 | Sites/sessions lifecycle and PDF/XLSX reports | TODO | Full regression + SAF file reopen |
| V4-A04 | Outlined dropdowns and black text | TODO | Audit all screens and dialogs, screenshot verification |
| V4-A05 | Restore verified original logo launcher + entry | TODO | Find authoritative asset, do not invent substitute |
| V4-A06 | Firmware /r0 contract and parser | TODO | Device/API mock contract tests |
| V4-A07 | New timed firmware calibration UX | TODO | Replace conflicting 20-minute default, unit tests |
| V4-A08 | R0 authority reconciliation and migration | TODO | Preserve historical values and source |
| V4-A09 | ChemCore frozen snapshots and Stable validation | TODO | Replay/golden tests, grouped holdout |
| V4-A10 | Full regression, Gradle, Android device tests, APK | TODO | Publish only after evidence-based acceptance |

## Initial source observations
- FieldSessionsTab.refresh() guards recursive refresh and catches RuntimeException; not proof that reported crash/ANR is resolved.
- UiKit.outlinedSpinner() and ensureLightRow() exist; no all-screen proof.
- Existing drawable ic_hazrob_tank_v3.xml is not proof of original approved logo.
- TankAutoR0Math uses a 20-minute three-window procedure, inconsistent with new firmware /r0 300s + 60s default.
- AcceptedR0Normalizer uses application-side accepted R0; firmware now persists an 8-channel record.
- TankInstantAnalyzer freezes input JSON, but calibration provenance must be verified end-to-end.

## Completed documentation commits
- 719dc081: created initial source-traceable firmware/app/ChemCore audit.
- 7de30038: added owner's four UI/session/logo requirements and resumable execution gates.
- This log creation: consult Git history for SHA.

## Immediate next action
Inventory manifest launcher icon and EntryActivity branding, session SQLite schema and migration, tests and CI Gradle baseline. Then reproduce/fix V4-A02 and record objective evidence.

## Developer and laboratory scope expansion — 2026-10-27
New authoritative checklist: [V4-DEVELOPER-LAB-COVERAGE-MATRIX-2026-10-27.md](V4-DEVELOPER-LAB-COVERAGE-MATRIX-2026-10-27.md).
- V4-D01 IN_PROGRESS: verify source definitions/wiring of all five developer tabs; investigate four referenced destinations not found in inspected ui directory; run Gradle compile.
- V4-D02 TODO: navigation, role gate, lifecycle and black-text/outlined-control verification on all developer screens.
- V4-D03 TODO: experiment capture, reference approval, reports, model comparisons and scientific-review publishing.
- V4-D04 TODO: firmware-managed R0, app persistence and provenance.
- V4-D05 TODO: analytical laboratory, full supported ChemCore methods, model evidence, benchmark and replay tests.
- V4-D06 TODO: connection diagnostics in direct/remote/offline modes, camera and safety checks.
- V4-D07 TODO: central scientific library and Stable integrity, updates and rollback.
- V4-D08 TODO: Sessions/report/logo cross-tab regression.
- V4-D09 TODO: Gradle build, UI test and physical device acceptance before APK.
Evidence: Source files DeveloperTab, DeveloperR0Tab, ExperimentsTab, MainActivity, RobotApi, TankInstantAnalyzer, ChemCoreAnalysisPresenter, ConnectionGate inspected; existing unit test names inventoried, but none executed in this step. Latest completed change is documentation-only. Next exact step: resolve developer screen source inventory and compile baseline.
