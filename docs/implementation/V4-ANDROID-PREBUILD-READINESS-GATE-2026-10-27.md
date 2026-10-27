# HazRob Tank V4 — prebuild readiness gate
Date: 2026-10-27
Scope: independent HazRob Tank Android app on `feature/tank-v4-implementation`.
Status: **NOT READY_TO_BUILD** (not a stable/release gate).
Owner instruction: notify when fully ready to start build; **do not start Gradle/CI/APK packaging without explicit authorization**.

## Last objective build evidence
- [GitHub Actions 40437121019](https://github.com/HazRob/HazRob-Tank/actions/runs/40437121019), 2026-10-27 02:12 UTC, **FAIL** in `compileDebugUnitTestJavaWithJavac`, after main app Java compilation succeeded.
- Cause: two test sources used `Files.readString(Path,Charset)`, unavailable with current Android JUnit Java-API configuration: `TankV4FieldSessionsAsyncAndExportContractTest` and `TankV4UiChartClarityContractTest`.
- Both files in the inspected current branch now use `new String(Files.readAllBytes(Paths.get(...)), StandardCharsets.UTF_8)`. **This was verified by reading source; a fresh build has NOT verified this fix.**
- Historic success of ESP32 V4 Arduino build is not Android validation.

## Required source-level gates before owner is informed of READY_TO_BUILD
| ID | Gate | Status 2026-10-27 | Evidence needed |
|---|---|---|---|
| PB-01 | Source consistency: pinned original Android at `6281d3e...` plus Tank overlays and app identity | CHECK SOURCE | Review all missing base classes, dependency/API interfaces, duplicate signatures, Android version 4.0 |
| PB-02 | Unit-test Java API portability | SOURCE FIXED, UNRETESTED | Files.readAllBytes replacement present, recheck all test source paths for unsupported Java APIs |
| PB-03 | Sessions and report workspace main-thread SQLite, selected IDs, lifecycle and schema migration | SOURCE FIXED, UNRETESTED | FieldSessionsTab retains background queries and release guards; ReportsTab now loads sites, approved sessions, archived reports and selected ChemCore snapshot on a worker, applying only generation-matched UI results. Still requires migrated/fresh database and ANR tests on Android. |
| PB-04 | Reports PDF/XLSX by site/session and SAF | SOURCE ROUTED, UNTESTED | Exact-ID scope tests, completed/approved sessions, cancellation, reopen and real file checks. PDF/XLSX formatting must be physically viewed. |
| PB-05 | R0 contract with ESP32 | SOURCE INTEGRATED, UNTESTED | `/r0` schema/version, 300s+60s, >=10 readings per MQ, 8-channel atomic acceptance, cancel/reconnect and phone/ESP32 owner binding. No PPM accuracy claims. |
| PB-06 | All five developer tabs and laboratory workflows | INCOMPLETE QA | Check PIN, DeveloperExperimentsTab/AnalyticalLabTab/DeveloperR0Tab/Connection/Library, reference approval, drift, model comparisons, archived replay, uploads and diagnostics. |
| PB-07 | ChemCore Stable embedded offline | SOURCE PREPARED, APK UNVERIFIED | `prepare_v4_stable_source.py` updates manifest version boundaries to 4.0; SHA256 required in workflows. Confirm current Stable reference/identity, verify actual APK assets after owner-authorized build. The pinned `1.0.0-hr069r1` bundle is a screening research snapshot with 652 identities and 0 M4 calibrations, not new Tank-certified gas calibration. |
| PB-08 | UI logo, bordered controls, black text and chart accessibility | SOURCE UPDATED, DEVICE UNTESTED | UI screenshots/visual and touch-flow check on Android, RTL and dark/light themes. |
| PB-09 | Complete reproducible checkpoint and explicit owner authorization | PENDING | Mark `READY_TO_BUILD` **only** after all known source blockers reviewed and fixed; log branch SHA and static checks. Authorization still required before any build. |

## Distinguish build readiness from release readiness
- READY_TO_BUILD means all **known source-level blockers** are resolved and a diagnostic build may be started with owner's permission. It does NOT mean an APK exists, all Gradle tests pass, Android UI is validated, or sensors meet accuracy standards.
- A diagnostic build must run all meaningful unit/contract tests before generating an APK and inspect embedded Stable payload and APK signing. Actual Android/emulator and hardware testing remain mandatory before stable release.

## Resumption checkpoint
1. Recheck the latest branch SHA and this file; do not rely on outdated CI results when newer commits exist.
2. Run the dedicated ReportsTab async source-contract tests and inspect UI/SQLite lifecycle under a populated upgraded DB; source work added but untested.
3. Review Java source/test compatibility and all previously reported failing tests without suppressing functional/clinical safety checks.
4. Validate stable generator manifest and reference provenance from canonical source; keep source-only checks from accidentally triggering Gradle.
5. Mark each PB gate with SHA, objective evidence and next action. Notify owner once a responsible review confirms READY_TO_BUILD, **not** based solely on a favorable source string test.

## Source checkpoint — 2026-10-27 (no build)
- `7da95c0`: ReportsTab database-intensive site/session scope refresh, approved report index and selected ChemCore snapshot moved to background executor with stale UI-result guards. No longer synchronous SQLite queries from those tab interactions.
- `6de131a`: added `TankV4ReportsAsyncContractTest` for scope, frozen analysis selection and exact PDF/XLSX actions.
- `1203b52`, `a2b2922`: updated the build-free static gate and existing report test to validate the revised worker-thread scope implementation.
- Independently inspected 11 live source/workflow files and checked 10 targeted invariants: **10/10 matched**, including background field sessions/reports, device R0, Stable integrity and manual-only build trigger. This is **NOT** Java compilation, UI test or APK acceptance.
- Last authorized diagnostic build `40437121019` remains **FAIL** at test Java compilation. Its two `Files.readString` mistakes have been corrected in current source but no follow-up compile was started under the build hold.
- **Current readiness: NOT READY_TO_BUILD** pending complete per-feature audit and objective successful future build preflight; no APK created.
