# HazRob Tank V4 — Firmware ↔ Android ↔ ChemCore compatibility audit
Date: 2026-10-27
Scope: independent HazRob-Tank only. Status: source audit and implementation backlog; NOT a validated V4 APK.

## Inspected source
- firmware/ESP32-Tank/{ESP32-Tank.ino,Hardware.cpp,Hardware.h,Config.h}; firmware V4 branch feature/tank-v4-mq-unified-r0; verified compilation Actions 40414404284.
- android/app/src/main/java/com/servoc/app/communication/{RobotApi.java,RobotProtocolV4.java}
- android/app/src/main/java/com/servoc/app/calibration/{TankAutoR0Math.java,AcceptedR0Normalizer.java}
- android/app/src/main/java/com/servoc/app/chemcore/{TankInstantAnalyzer.java,TankStableScreeningModelRegistry.java}
- android/app/src/main/java/com/servoc/app/measurement/TankAcquisitionPolicy.java
- docs/requirements/V4-UPDATE-BACKLOG-2026-10-24.md and prior V4 execution plan.

## Confirmed compatibility gaps
1. **Calibration schedule conflict (P0):** firmware `/r0?cmd=start&warm=300&duration=60` runs nonblocking 300 s power-on warmup + 60 s sample window (defaults). Android TankAutoR0Math still hard-codes a 20-minute three-window procedure (5-minute heat, three 3-minute captures separated by rests). Do not silently combine results or show both as the same workflow.
2. **Dual R0 authority (P0):** firmware stores 8-channel checksummed NVS record `r0_v4`, while AcceptedR0Normalizer resolves app-side ManualR0Calibration/FieldR0Baseline. The app must explicitly reconcile firmware values with app state; never silently overwrite a confirmed firmware calibration with an old app override.
3. **Firmware calibration response lacks durable provenance (P0):** `calibrationJson()` currently exposes state, error, elapsed_ms, duration_ms, and per-channel samples/provisional r0, but not committed-vs-preview flag, calibration ID, clean-air factors, firmware build SHA, full window start/end, warmup, or readback checksum. Android cannot safely infer that a preview value is accepted.
4. **Frozen measurement provenance (P0):** TankInstantAnalyzer uses AcceptedR0Normalizer.recordedRatio and freezes its input; firmware/app integration must ensure captured `rs`, `r0`, `ratio`, source, sample count, and calibration identity are present in each immutable operation. Historical reports must never be recomputed with today's R0.
5. **Analysis validity (P0):** do not infer chemical identity/ppm or occupational exposure compliance solely from MQ response. Keep uncalibrated or absent sensors out of strict inference. Preserve evidence weights and Stable manifest SHA verification; research models cannot be promoted without source-qualified validation.
6. **Protocol parity (P1):** RobotProtocolV4 normalizes /diag and /data, but firmware /r0 state is not yet a first-class normalized calibration model; add explicit parser/transport API with offline guard, request timeout, and states idle/warmup/sampling/ready/error/cancelled.
7. **Safety/UX (P1):** while calibration is active, normal measurement must be blocked (firmware already guards it); Android should disable competing controls and clearly display progress, valid sample counts per channel, ADC errors and cancellation; never confuse 60-second calibration capture with the 30-second analysis measurement window.
8. **Data migration (P1):** preserve legacy SQLite sites/sessions/experiments and R0 history; introduce schema/provenance fields additively. No deletion or rewriting archived values.
9. **Network mode (P1):** direct AP and remote modes must distinguish unsupported /r0 operations; no implicit GET mutation without clear operator action. Verify existing offline preview guard.
10. **Release gate (P0):** build/install tests for Android, schema migration, calibration mock transport tests, ChemCore golden cases, stale R0 regression, reconnect/cancel/estop, real robot/ADC bench test. No claim of a completed V4 APK until these pass.

## Proposed implementation sequence
A. Extend firmware /r0 JSON with committed record, clean-air factors, warmup/window, versioned provenance and NVS readback; retain legacy fields for compatibility.
B. Add RobotApi calibration start/status/cancel and strict RobotProtocolV4 calibration normalization, including offline mode and timeout handling.
C. Replace the user-facing TankAutoR0Math workflow with firmware-authoritative timed calibration; keep legacy 20-minute scheme only as explicitly named historical mode if necessary. Add deterministic unit tests for 300+60 timing, >=10 valid samples, partial failure, cancellation, and ADC outage.
D. Create calibration acceptance transaction in Android DB with firmware ID, timestamp, sensor name, R0 in kΩ, sample count, clean-air factor, device ID and source. Explicitly handle conflict with existing manual R0; no silent precedence.
E. Ensure every live snapshot freezes Rs/R0/ratio and calibration provenance before ChemCore. Run replay tests verifying old results do not change after recalibration.
F. Validate Stable manifest integrity, source-backed models, unknown/open-set behavior, uncertainty and noncertified ppm messaging; benchmark known gases with grouped holdouts, not only self-consistency.
G. Run Gradle/JVM/instrumented tests, manual UI/device tests, publish unsigned debug APK only after compile, and retain signed-release gate separately.

## Acceptance criteria
- App renders exactly the firmware's calibration state and per-channel valid sample counts; does not accept tentative R0.
- Accepted R0 source is explicit and persisted; no silent cross-device or cross-session substitution.
- Normal/instant/archived ChemCore uses frozen measurement calibration, with audit fields and deterministic replay.
- Existing safety stop/relay and independent Tank app identity preserved.
- Every change has a test result and commit in the durable V4 progress log.

## Evidence status
- Firmware V4 compile/ZIP: PASS, Actions 40414404284.
- Android V4 integration build: NOT RUN in this audit.
- Device calibration, ChemCore accuracy and Android field acceptance: NOT VERIFIED.

## Additional mandatory owner requirements (2026-10-27)
11. **Sessions crash/hang P0:** pressing Sessions must not crash, freeze, or trigger a callback loop. Audit FieldSessionsTab.refresh()/renderSessionUi(), Spinner reentrancy, FieldSessionRepository.verifySessionSchema(), SQLite migration, lifecycle, main-thread DB access, and exception logging. Existing catch block is not proof of fix. Verify on upgraded pre-V4 database and fresh install; collect Logcat, UI tests and ANR traces. Never delete existing sites/sessions.
12. **Sessions functionality and reports P0:** verify site create/select, dependent session list, start/close/approval, session switching, empty/error state, PDF and Excel exports, aggregated site report, persisted results and reopened PDFs. Validate no duplicate entries, no orphan sessions, correct RTL and content, and SAF permissions. Include regression test on a populated legacy DB.
13. **Black text + outlined controls P1:** all Spinner selected rows and popup rows, dropdowns, selection fields and EditText controls must use visible dark/black text and a clear outline on light backgrounds in light/dark Android themes. UiKit.outlinedSpinner and ensureLightRow exist, but require coverage audit of every screen, popup and dynamically created row; screenshot tests at multiple densities.
14. **Restore previous HazRob logo P1:** restore the previously approved original logo as Android launcher/adaptive icon and entry screen logo, without replacing it with an invented image. Identify authoritative asset from repository history or documented design file; inspect density variants, AndroidManifest and launch/entry layouts. Current drawable contains ic_hazrob_tank_v3.xml; do not assume this is the requested original. If authoritative asset is unavailable, mark blocked and request it only after searching project history.

## Durable resumable implementation plan (checkpoint discipline)
Every task must be recorded in `docs/implementation/V4-APP-CHEMCORE-EXECUTION-LOG-2026-10-27.md` with unique ID, source path, original defect, patch commit SHA, test command, test output or CI run URL, status (TODO/IN_PROGRESS/BLOCKED/PASS), remaining risks and next exact action. Commit after each cohesive task. Do not mark PASS based on static inspection alone.

**Execution order and gates:**
- V4-A01 baseline inventory: branch SHA, manifest/version, app navigation, DB migrations, logo candidates, existing Gradle tests; preserve current data and baseline artifacts.
- V4-A02 reproduce and fix Sessions crash/ANR on legacy/fresh DB, guard Spinner reentrancy and offload expensive DB queries if found; unit/instrumentation tests and Logcat evidence.
- V4-A03 exercise sessions/site report lifecycle PDF/XLSX, SAF, RTL and re-open regression; protect session identity and existing records.
- V4-A04 audit and normalize outlined dropdowns, black text in fields, popup rows, dialogs and selection widgets; screenshot/contrast tests.
- V4-A05 restore original logo from verified asset to launcher and entry UI; inspect on actual Android launcher and login screen.
- V4-A06 firmware `/r0` protocol contract, accepted-vs-preview values, persistent calibration metadata and error/cancel behavior; mock API tests.
- V4-A07 integrate calibration state machine in Android; remove conflicting 20-minute automatic path from default workflow; verify 300s/60s and configurable duration.
- V4-A08 migrate and reconcile R0 authority, preserving old R0 provenance and archived records.
- V4-A09 ChemCore immutable per-snapshot R0, source validation, Stable SHA integrity, unknown/open-set and grouped holdout evaluation; no unsupported ppm certification.
- V4-A10 full app regression (offline/AP/remote, UI, DB, reports, emergency stop, watchdog), Gradle debug build, installation and on-device acceptance; then V4 APK packaging and README update. Firmware remains separately versioned.

**Resume procedure:** open this audit, then the execution log; inspect the latest commit and CI run; resume the first IN_PROGRESS/BLOCKED/TODO task. Never assume an interrupted action succeeded without checking the repository. Keep APK status UNRELEASED until required gates pass.
