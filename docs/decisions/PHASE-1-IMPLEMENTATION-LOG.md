# Phase 1 implementation log — HazRob Tank
Last updated: 2026-10-17 23:56 UTC. Work branch: `develop`. Original SERVOC-Android is untouched.

## Checkpoint P1-01 — requirements APPROVED
- Commit `7253eb6a5fe2df47a44da3a3e29b7690b9bb10ac`
- `docs/requirements/PHASE-1-ANDROID-MEASUREMENT-SESSIONS.md`
- Defines instant vs documented acquisition, four measurement panes, two field-session panes, one approved measurement per field session, generic live MQ warnings, physical data integration, PM placeholder, smart report boundaries.

## Checkpoint P1-02 — lifecycle policy and tests COMMITTED (NOT YET CI-VERIFIED)
- Policy commit `e2d9e4a082708cf64a01cbdf74f4f8145cdd0bb3`
- Test commit `8a5c8ec1f96a7434abc7e08cc9ef3c005137412b`
- `TankAcquisitionPolicy.java` models NEW/WARMUP/MEASURING/COMPLETED/CANCELLED/APPROVED; only approved is reportable; user-visible status collapses to جديدة/معتمدة.
- `TankAcquisitionPolicyTest.java` covers visibility, approval, retries, deletion precondition, generic warnings.
- These are initial policy contracts, not yet wired into database and Android UI.

## Checkpoint P1-03 — measurement navigation shell COMMITTED (NOT YET CI-VERIFIED)
- Commit `ef1e6bfe26d3bc9087730a644905d9f8b384f403`
- `android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java`
- Four mobile RTL panes: search/tracking landing, analysis (existing live/analysis/results reachable through nested buttons), existing physical, PM placeholder.
- Existing ChemCore candidate controls and live telemetry remain reachable.
- Search landing is intentionally NOT yet a functional chemical search; no simulated gas values.
- Particle pane explicitly marks hardware unavailable; no fake measurements.
- UI shell must be compiled and device-tested before calling this completed.

## Next implementation order
1. Trigger existing GitHub Actions workflow on `main` to checkout `develop` and run Gradle tests/build; fix any compile or test regressions.
2. Implement functional chemical-family + name/CAS search using actual ChemicalRepository and availability flags; connect target selection to existing tracking workflow and top-ten candidate selection.
3. Implement field session 1:1 acquisition + approval persistence and migration; unit tests; distinguish instant from documented recorder sessions.
4. Replace FieldSessionsTab with إدارة/التقارير, dynamic dropdowns, approved-only reporting, confirmation dialogs, proper result checking.
5. Physical instantaneous PDF snapshot, ChemCore environmental input verification, PM placeholders and future interface contract.
6. Review smart report templates separately with owner, end-to-end regression and APK build.

## Important limits
- No new APK has been built for these commits yet.
- No device/hardware integration testing has been performed for this phase.
- Do not claim search, session approval, smart reports or source direction are complete based on the shell.

## Checkpoint P1-04 — requested light UI, chemical lookup, PM layout (COMMITTED, NOT CI-VERIFIED)
- `784bf2adbb1bf0ae053dc918503a509195dcc4f1`: `MeasurementsTab.java` adds real ChemicalRepository search by name/CAS, chemical-family dropdown, selectable chemicals and tracking hook for legacy-identified chemicals, RTL scroll views with always-visible scrollbars, PM1/PM2.5/PM10 structured table, chart selector and report control.
- `4b3dcb4c7c2073284fda84c1a79c1198d5fefeb7`: `UiKit.java` shared light palette: white/gray surfaces, black text, amber-yellow buttons, RTL inherited.
- PM values remain blank when hardware absent; chart selector is wired to a placeholder only, not real series yet. Reports remain disabled without PM data.
- Chemical family filter uses actual `chemicalClass` strings but is heuristic, pending verified taxonomy; analytical-capability columns still need database-backed availability mapping.
- Original `MainActivity` calls `enter()` at startup already; offline review requires an explicit UI affordance and guarding transport-only features, not yet implemented.
- Other legacy panes still contain explicit dark backgrounds; palette harmonization and contrast regression remain open.
- No build/test run has been triggered after these commits. All changes require compilation and device verification.

## Checkpoint P1-05 — v2 packaging and offline preview (COMMITTED, NOT VERIFIED)
- `8e94f349e8b09462e2d7ee30f49d4582f0deaab6`: dedicated `.github/workflows/build-tank-android-v2.yml` with Android versionName 2.0/versionCode 2 and Tank unit-test overlay copied into original pinned checkout. Workflow requires `HAZROB_SOURCE_READ_TOKEN`.
- `a0b252e63dfdffc6a716b52643bcb7222a3010b0`: MainActivity overlay adds an upper header **↪ دخول** preview shortcut to measurements even without robot link, plus light shell header. Existing startup already calls `enter()`; the shortcut is navigation, not an authentication bypass.
- Workflow has NOT been dispatched; no CI run, compile verification, emulator/device validation, firmware-in-the-loop test, or v2 APK artifact exists yet.
- Release gate: run Gradle unit tests and assembleDebug, verify APK signature, test all UI screens and navigation offline, then ESP32 API4 protocol/motor/sensor/camera tests with hardware; do not publish a 'tested v2' until gates pass.
- Pending: ensure ChemicalRepository-backed search is UI-thread safe at scale, check chemical taxonomy and capability flags, wire chart dialogs to actual chart data, full visual contrast sweep, session lifecycle and smart reports.

## Checkpoint P1-06 — search correctness and automatic CI trigger
- `aacec9ccf09579424ef5b3887ebf1806b78ea288`: chemical search now debounced 250ms to reduce repeated database reads while typing; selecting a row no longer starts tracking immediately. Only the positive confirmation action invokes `host.setTrackingTarget` for compatible legacy IDs.
- `83fd2386a9623888f7e751e28a3e2126942c47b7`: v2 workflow configured to run on pushes to `develop` when Android or workflow files change, in addition to manual dispatch.
- **Verification is not complete:** GitHub workflow status could not be retrieved from the available GitHub interface. Check repository Actions for `Build HazRob Tank v2 Android APK`. Do not claim build success or provide an APK before confirming the run.
- Current release blockers: functional ChemCore capability mapping; chart data/dialog verification; field-session one-to-one approval lifecycle; physical snapshot export; whole-app palette regression; firmware API4 contract and hardware tests.

## Checkpoint P1-07 — ESP32-Tank API4 source compatibility audit
- Commit `b542a72fca227ce31796c2ef6fe501ce0ce2b3d2`: `docs/firmware/ANDROID-V2-ESP32-TANK-API4-COMPATIBILITY.md`.
- Inspected Tank firmware `Config.h`, `ESP32-Tank.ino`, `SettingsOps.cpp` against Android `RobotApi.java` and `RobotProtocolV4.java`.
- Direct HTTP command paths and query parameters align for drive, stop, ESTOP, relay, measurement, telemetry and settings. Settings bounds, watchdog and measurement state contract documented.
- **Not tested:** actual JSON payloads from powered hardware, remote MQTT end-to-end, Wi-Fi resilience, camera or servo direction. Static route matching is not a substitute for integration testing.
