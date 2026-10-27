# HazRob Tank V4 — Complete Developer Tabs and Laboratory Coverage Matrix
Date: 2026-10-27
Branch: `feature/tank-v4-mq-unified-r0`
State: **SCOPING/SOURCE AUDIT** — no Android V4 acceptance tests, Gradle package, or laboratory calibration success claimed.
Scope: the independent HazRob Tank Android app and Tank-specific ChemCore only. Do not change the four-wheel HazRob product or silently promote research data into Stable.

## Source evidence / inventory
- `android/app/src/main/java/com/servoc/app/ui/DeveloperTab.java` declares five PIN-protected destinations: **التجارب** (DeveloperExperimentsTab), **المختبر** (AnalyticalLabTab), **معايرة R0** (DeveloperR0Tab), **الاتصال** (DeveloperConnectionDiagnosticsTab), **المكتبة** (CentralLibraryDashboardTab). Unlock constructs all five eagerly, so a constructor exception may defeat the entire workspace. Preserve access/role isolation.
- `.../ui/ExperimentsTab.java` exists with reference capture, chemical chooser, eight-MQ relay, R0 edit, clean-air baseline/drift, MQDataScience model comparison, reference approval, PDF/XLSX exports and GitHub scientific-review publishing. **It does not prove the required DeveloperExperimentsTab is defined or wired**.
- `.../ui/DeveloperR0Tab.java` still uses `TankAutoR0Math` three-window / 20-minute app-side automatic R0, conflicting with firmware's configurable 300-s warmup + 60-s time-window averaging.
- `.../chemcore/ChemCoreAnalysisPresenter.java`, `TankInstantAnalyzer.java` and `TankStableScreeningModelRegistry.java` expose analysis presentation, hypothesis candidates, curve fit/residual/evidence and Stable verification paths; lab UI wiring needs end-to-end validation.
- `.../communication/RobotApi.java` and `RobotProtocolV4.java` have AP/remote telemetry and command handling; new firmware calibration `/r0` needs explicit compatibility.
- Source listing of `.../ui/` in the inspected branch contains `DeveloperTab`, `ExperimentsTab`, `DeveloperR0Tab` etc., but **does not contain** the expected `DeveloperExperimentsTab.java`, `AnalyticalLabTab.java`, `DeveloperConnectionDiagnosticsTab.java` or `CentralLibraryDashboardTab.java`. Treat as a **potential compile blocker**. Check other generated source sets, missing commits and branch origins before deciding how to restore. A source file naming mismatch is not proof of runtime failure without compilation evidence.
- Current tests include `TankV4SessionsCrashGuardTest`, `TankV4OutlinedContrastTest`, `TankV4NewHardwareCalibrationContractTest`, `TankAutoR0MathTest`, `TankStableScreeningModelRegistryTest`, `RobotProtocolV4DiagnosticsTest`. Their presence is not evidence of successful execution.

## Mandatory coverage, each item must receive reproducible tests

### DEV-01 — Developer access and tab navigation (P0)
1. PIN/role gate rejects invalid credentials; lock/unlock and user/developer separation preserved.
2. All five tabs construct, open, refresh, switch repeatedly, detach, return from background and reopen without exception, ANR or memory leaks; errors in one screen do not crash another.
3. Explicitly locate/restore missing tab classes or replace stale references with proven implementations; CI compilation required.
4. Validate RTL layout, black text and bordered controls in EVERY developer screen and modal, including disabled/spinner dropdown states.

### DEV-02 — Experiments and chemical reference laboratory (P0)
1. Confirm correct eight-MQ/ADS1115 sensor identity mapping, common relay power, warmup, telemetry freshness, valid sample count, ADC faults and unavailable channels.
2. Chemical search/selection, reference kind (fingerprint vs known-ppm calibration), required metadata, chamber environment and clean-air baseline; capture only complete acceptable averaged measurements, not stale snapshots.
3. Create/save/list/reload/select/delete-or-reject only through approved governance, with session identity and immutable source data; approval and reference promotion require source and quality gates.
4. Training fingerprints, calibration curves, model lab catalog (current UI labels 76 MQDataScience models), cross-model comparisons, experimental concentrations, uncertainty and drift handling; do not mistake a model catalog for 76 validated models.
5. PDF and XLSX exports contain original Rs/R0, calculated ratios, sample counts, units, calibration identity, timestamp, device, chemical/CAS if known, evidence flags and approval status.
6. GitHub review-area upload, encrypted credential storage, token revoke, failure/retry, duplicate suppression and audit trail; upload alone must NOT modify Stable ChemCore or publish production conclusions.
7. Remove duplicated/conflicting R0 editors or make any manual R0 change an explicit controlled override with provenance and confirmation.

### DEV-03 — Analytical Lab / ChemCore (P0)
1. Restore/verify AnalyticalLabTab actual implementation; enumerate its controls and individually test them.
2. Review all supported techniques exposed to UI: candidate ordering/uncertainty, fingerprints, reference curves, inverse models, residuals, mixture/open-set, multivariate/NN/QSPR/temporal where actually implemented. Label absent or research-only methods accurately; do not assume the central 80-technique research program is implemented in this Tank APK.
3. Test calibration-aware Rs/R0 input, eight-channel quality flags, timestamps, ambient temperature/humidity, missing sensor/zero denominator, saturation and drift; never impute nonexistent measurements.
4. Verify candidate score and ranking changes on verified reference inputs and concentration changes; known gas, nonmatching gas, mixtures, out-of-distribution and sensor-dropout controls; version, seed, and grouped holdouts recorded.
5. Compare inference on live operation vs frozen archived snapshot; old analyses must not change after R0 update. Show candidate evidence, equation/residual, fitted ppm only as appropriately qualified scientific screening, not regulatory proof.
6. Verify manifest hash and provenance of installed Stable models and resource files; distinguish research, benchmark, source-qualified reference and Tank-specific validation.
7. Export laboratory analysis with source/method/version/limitations, PDF/XLSX and truthful uncertainty. No safety-grade identity claim without physical G3 acceptance evidence.

### DEV-04 — R0 calibration and validation (P0)
1. Replace default 20-minute app sampler with firmware `/r0` state-machine operation; configurable warmup/capture lengths, all valid cycles in window, >=10 fresh samples **per sensor**, eight-channel commit gate.
2. Show states idle/warmup/sampling/ready/error/cancelled, source, progress, power status, per-channel count, provisional vs COMMITTED R0; no preview value may be accepted into ChemCore.
3. Resolve single R0 authority (ESP32 NVS vs manual app baseline), per-channel units kΩ, clean-air ratios, firmware/device identity and persisted provenance; protect archived ratios.
4. Test power loss, reconnect, inconsistent ADC1/ADC2, app foreground/background, cancellation, sensor power off, emergency stop and NVS readback.
5. Manual edits/reset require operator confirmation and audit; no silent overwrite, no substitution of research R0 for the installed unit.

### DEV-05 — Connection diagnostics / hardware tools (P0/P1)
1. Verify tab exists and exposes AP direct, STA/internet remote, MQTT, camera status, ESP32 and ESP32-CAM version/diagnostics, Wi-Fi provisioning and latency.
2. Validate safe connectivity tests, heartbeat, reconnect, timeouts, E-STOP, relay commands, watchdog, sensor power and refusal of unsupported remote calibration.
3. Confirm firmware V4 data model compatibility, stale-link/power-off/ADC diagnostics and no involuntary motion or replay after app backgrounding.
4. Diagnostics must not crash offline and must not expose Wi-Fi credentials or tokens in logs/reports.

### DEV-06 — Central library, Stable and scientific reference management (P0/P1)
1. Verify CentralLibraryDashboardTab exists, all library navigation/import/search/detail interfaces, Stable model asset availability, manifest digest, recorded version and local-first access.
2. Distinguish approved/research/unverified/external benchmark provenance. Preserve original measurements, R0 and source references; no cross-device calibration copying.
3. Check remote update/download integrity, offline mode, rollback, migrations, conflicts, duplicate handling, GitHub authentication permissions and broken-link error handling.
4. Reconcile Tank ChemCore scientific references, MQ curves, 76-model UI catalog and separate 80-technique development archive; only evidence-qualified, reviewed techniques enter Stable.

### DEV-07 — Shared usability + crash/exports regression (P0/P1)
1. Sessions tab create/site-select/session-select/close/approve, reports (individual/aggregate, PDF/Excel), schema migration and ANR/Logcat on old + fresh DB.
2. Uniform outlined controls, black text on white/light field background in user AND developer/lab pages, including spinner list/popups, dialog fields and edit controls.
3. Restore verified previous logo (launcher/adaptive icon AND ConnectionGate entry view), no fabricated substitute.
4. Verify the single instant-measurement operation/report lifecycle stays distinct from user field sessions and developer reference experiments.
5. Verify repeated switches among all user/developer tabs preserve selected record, state and exported data.

## Test/acceptance evidence templates
For every DEV-ID, attach:
- source paths + baseline commit SHA, implementation commit SHA, test names;
- exact Gradle command and unit/Android instrumentation/screenshot outcome, CI URL or local log hash;
- test fixture type (fresh DB / migrated DB / offline mock / AP real firmware / remote MQTT / real eight-MQ rig);
- raw inputs and expected vs actual outputs; screenshots or anonymized Logcat for UI crashes;
- status: TODO / IN_PROGRESS / BLOCKED / PASS, reviewer and remaining deviations.

**Mandatory release gates:** all five developer destinations navigable; no missing classes/compiler errors; zero reproductions of sessions crash across test matrix; exports parse and reopen; consistent R0 provenance; ChemCore replay correctness; permissions and safe controls; Gradle build + APK installation. Bench/field tests must be reported separately and never inferred from CI.

## Execution order / resumption
1. **V4-D01** source discovery and repair of missing developer-tab class/wiring blockers; run Gradle compile.
2. **V4-D02** add exhaustive per-screen developer UI navigation and contrast tests.
3. **V4-D03** experimental capture, approval, reports and GitHub publishing tests.
4. **V4-D04** R0 firmware contract, NVS reconciliation and calibration tests.
5. **V4-D05** Analytical Lab + ChemCore stable manifest, grouped benchmark and frozen replay tests.
6. **V4-D06** developer connection/hardware commands and remote/direct test matrix.
7. **V4-D07** central library provenance, updates, rollback, migration and offline tests.
8. **V4-D08** joint regression with Sessions, reports, logo, user permissions, all-tab navigation.
9. **V4-D09** final source audit, Gradle compile/tests, APK packaging and manual hardware acceptance.

Log every completed step and next exact action in `V4-APP-CHEMCORE-EXECUTION-LOG-2026-10-27.md`. Never change task status to PASS based only on a code search.


## Correction and implementation checkpoint — 2026-10-27
The early inventory's warning about potentially missing developer destinations was a **source-overlay inventory limitation**, not a confirmed Java dependency error. `DeveloperConnectionDiagnosticsTab` exists in the pinned original Android source (Git blob `65b728a511f796966d7d11ee2a17692889c33156`). The other listed Tank developer destinations have source overlays, and the earlier controlled Android run `40437121019` compiled main Java. This does not prove the new current revision compiles or that five tabs are navigable without crash on a phone.

New Tank override `DeveloperConnectionDiagnosticsTab.java` committed as `2ae12ca` replaces dark inherited colors with dark readable text on a light background, and takes `rememberWan` from the UI thread before WAN provisioning is dispatched to the worker. New Java contract `4277e11` is not yet Gradle-tested.

Developer review publication source was isolated from the four-wheel product at `e3344759` and `84471d9` with a dedicated Tank scientific-review branch and SHA-256 readback. `403fab7` records commit SHA and payload hashes; `879ef7d` supplies a source-contract assertion. An authenticated send, retry/409/422, permission failure, duplicate/idempotence, crash recovery and complete audit trail remain **PENDING**.

Proceed with the original DEV-01..DEV-07 acceptance matrix and preserve the distinction between `SOURCE_PRESENT`, `COMPILE_PASS`, `DEVICE_PASS` and `RELEASE_READY`. No release gate was promoted by this checkpoint.
