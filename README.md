# HazRob Tank — Independent Tracked Robot

> **Development handoff, 2026-10-18.** This README is the authoritative starting point for the next conversation and **v3 development**. Do not release another APK until the additional requested changes and acceptance tests are complete.

## V3 active implementation checkpoint — 2026-10-20 (NOT RELEASED)

- **Current work branch:** [`feature/tank-v3-implementation`](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v3-implementation). **No V3 release APK** has been published, the branch has not been merged into develop, and do not present any debug Actions artifact as a user-ready signed release.
- **Continue after interruption:** [V3 execution plan](docs/implementation/V3-EXECUTION-PLAN.md) and [chronological execution log](docs/implementation/V3-EXECUTION-LOG.md). Read these **before** making further edits; each records source commits, passing/failing CI, blockers, and per-requirement status.
- **Latest CI-tested Android V3 source:** `dda17d08bf2d2e82ca3c371cd686c9b85233cfae` — [run 39485877592](https://github.com/HazRob/HazRob-Tank/actions/runs/39485877592), **SUCCESS** for JVM regression, real SQLite approved-report scope, debug APK assembly and ChemCore snapshot checksum. All 20 chart selector evidence cases passed separately at [run 39485600871](https://github.com/HazRob/HazRob-Tank/actions/runs/39485600871). **Release signing skipped**, so the diagnostic APK is not a production update.
- **Implemented in V3 source (still unverified on real Tank):** offline mode with hard network block, 6 compact refresh overlays excluding drive, 2-up physical emoji cards, calibrated-only 90 dBA instantaneous red card display, 20-minute user clean-air R0 workflow and confirmed local save provenance/normalizer, dedicated site management vs approved reporting, approved-only field/site report data filtering, enlarged true-data report charts, frozen on-click physical PDF.
- **Tank V3 firmware:** `ESP32-Tank 0.1.3-v3-dev` has passed full Arduino-ESP32 2.0.17 build and been packaged as clearly **UNRELEASED** test .bin and Arduino files with SHA256, [run 39479359692](https://github.com/HazRob/HazRob-Tank/actions/runs/39479359692). The 500 ms neutral-stop and no autonomous reversing require real servo and link-loss trials before flashing/deployment.
- **Operator-facing Arabic acceptance matrix:** [V3 device test record](docs/implementation/V3-DEVICE-ACCEPTANCE-AR.md). All real-device outcomes remain NOT TESTED until witnessed and documented.
- **Outstanding release blockers:** stable Tank APK signing keystore/secrets, actual Android UI inspection (no emulator), complete site/session migration and safety tests, real 20-minute sensor calibration/ RL validation and ChemCore live activation, tracked robot motion watchdog/E-STOP/link-loss hardware tests, app installation/Play Protect diagnostics, plus still-pending backlog functionality.
- The original `sohpany/SERVOC-Android` remains read-only, pinned to `6281d3e`; no four-wheel code or deprecated simulator workflows should be introduced. Current V3 candidate `com.hazrob.tank` versionName `3.0`, versionCode `3` for CI.

## Project identity and separation

- Repository: **HazRob/HazRob-Tank**, working branch **develop**.
- Independent Android application ID: **`com.hazrob.tank`**; display name **HazRob Tank**.
- Independent tracked-robot firmware: **`firmware/ESP32-Tank/`**; Android overlays: **`android/app/src/main/java/`**; ChemCore stable data bundled at build time.
- The original four-wheel HazRob application in **`sohpany/SERVOC-Android`** must **not** be changed. Android baseline is pinned at commit **`6281d3eb7c1c961ce63b0b5e4ae0792874626720`** and copied into a separate build workspace. Changes belong in this Tank repository only.
- Never conflate HazRob Tank, HazRob Mini and the original HazRob. Firmware, Android app and ChemCore lifecycle must be handled independently.

## Verified build status versus development HEAD

| Component | Last known state | Verification |
| --- | --- | --- |
| Tank firmware | **`ESP32-Tank 0.1.1-dev`**, engineering revision **`TANK-SERVO-R1`**, **API 4** | Source in `firmware/ESP32-Tank/Config.h`; static API contract passed in earlier CI; **hardware validation pending** |
| Android version | **v2**, `versionName 2.0`, `versionCode 2`, package `com.hazrob.tank` | Generated from pinned Android baseline plus Tank overlays |
| Last **successful** APK build | Commit **`df92e9ccd2d7d8b69b83fff5515569d298734dc4`**, [Actions run 39084627573](https://github.com/HazRob/HazRob-Tank/actions/runs/39084627573) | Unit tests, APK assembly, signature verification and presence of embedded ChemCore assets passed. Artifact: `HazRob-Tank-v2-debug-apk`. **Debug-signed, not an upgrade-stable release** |
| Subsequent development | Commit **`ec8861da7a9c8141d4a6554cfb39a8b7cc5acaca` added offline entry; previous **`a2e8b4f0`** required persistent signing secrets | [Actions run 39086020081](https://github.com/HazRob/HazRob-Tank/actions/runs/39086020081) **failed**. **No validated APK containing offline entry** |
| V3 separate development branch | **V3 debug candidate assembled and firmware .bin compiled** | CI source verified; **NOT a signed/stable release**, no physical acceptance and no merge to `develop` |

**Do not claim that HEAD has passed CI simply because an earlier APK did.** Reinspect the latest Actions job logs and rerun the full pipeline after edits.

## Android v2 functionality implemented or prepared

1. Four measurement workspaces: chemical search/tracking, live ChemCore analysis, physical sensor measurements, and particle/PM view. The PM view must explicitly show unavailable hardware rather than fabricated concentrations.
2. Search and selection over the chemical library; ranked candidates and analysis results; session/site workflows, reports and developer/operator separation.
3. Light operator workspace with dark legible text, amber actions, Arabic/RTL layouts, scrolling lists and safety highlighting.
4. Chemical concentration and safety result fields turn **bold red** only for applicable, validated reference crossings; distinguish instantaneous comparison from time-integrated OSHA exposure. Do not invent PPM, LEL/LFL, chemical identity or confidence.
5. Physical reference comparison class: `android/app/src/main/java/com/servoc/app/measurements/PhysicalReferencePolicy.java`. It guards against unsupported instant comparisons. Current examples: WHO 2021 **PM2.5 15 µg/m³ / 24 h**, **PM10 45 µg/m³ / 24 h**, WHO UV index **≥3** when UV measurement is validated, indoor humidity guidance with explicit occupancy context; noise requires calibrated **dBA and duration**. This is **not** a completed regulatory standards engine. Physical tests: `android/app/src/test/java/com/servoc/app/measurements/PhysicalReferencePolicyTest.java`.
6. Clearing red/bold safety styling when readings disappear, become invalid or return to nonexceedance state.
7. **Pending release:** `android/app/src/main/java/com/servoc/app/auth/ConnectionGate.java` now adds **«الدخول دون اتصال • استعراض التطبيق»** on the entry screen. This should navigate for inspection **without** claiming a robot connection or enabling unsafe drive commands. Verify it on device and verify other screens in offline mode.
8. Entry screen still inherits the original HazRob branding/version footer; revise to **HazRob Tank v3** and verify independent branding in v3.

## ChemCore Stable in APK

- The build executes **`tools/build_hr069_embedded_stable.py`** from the pinned original Android source into Gradle's **`app/build/generated/hazrobAssets/generated/chemcore/embedded_stable/`**. **Do not** also generate it under `app/src/main/assets`: that caused a duplicate-assets failure.
- Manifest expected **`library_version = 1.0.0-hr069r1`**, **652 chemical identities**, **0 calibration records**; payload SHA-256 and length checked.
- APK verification checks `assets/generated/chemcore/embedded_stable/manifest.json` and `chemcore-stable-snapshot.json`.
- Tank `MainActivity` invokes **`ChemCoreEmbeddedStableInstaller.ensureInstalled(getApplicationContext())`** on startup. Embedded presence and build checks passed in the last successful APK, but **runtime installation, active-library status and analytical correctness still need on-device tests**. Embedded research priors are not a substitute for calibrated measurements.

## ESP32-Tank firmware contract

- Source of truth: `firmware/ESP32-Tank/ESP32-Tank.ino`, `Config.h`.
- API **4**, watchdog **500 ms**, endpoints include `/drive`, `/estop`, `/estop/reset`, `/relay`, `/measure`, `/data`, `/diag`, `/cfg`.
- Two rear continuous-rotation servos, eight MQ inputs through two ADS1115, BME environmental bus separate from ADS bus, Echo, microphone, IR/light and power diagnostics as implemented. Exact wiring and servo direction are provisional and require physical commissioning; see `Config.h`.
- No installed PM sensor: do not manufacture PM readings. Validate motor stop, watchdog, E-STOP, relay, sensor health and physical units with real hardware before deployment.

## Installation and signing — critical for v3

The v1 and successful v2 debug APKs used ephemeral GitHub Actions debug signing keys. Their signatures can differ, so Android may refuse an in-place upgrade. The user confirmed they are willing to remove **v1**; **do not** assume this solves signing for future updates.

Commit **`a2e8b4f0`** changed the v2 workflow to **require** a persistent Tank signing keystore through repository secrets:

- `TANK_KEYSTORE_BASE64`
- `TANK_KEY_ALIAS`
- `TANK_KEYSTORE_PASSWORD`
- `TANK_KEY_PASSWORD`

**These secrets must be configured securely by an authorized repository administrator.** Never commit the keystore, signing passwords or base64 keystore to the repository or a chat. Retain secure backups of the same keystore for all later releases. Current v2 pipeline may fail until these are provided. Keep application ID fixed and increment `versionCode` above 2 for v3 (e.g. 3, provided no higher version was already installed); use the **same signing certificate** for every future release. Test installing v3 over its signed predecessor without deleting data. Do not present the old debug-signed v2 as an upgrade-compatible baseline.

The user requested a **standalone APK download, not a ZIP**. GitHub Actions artifacts are ZIP archives. For v3, publish the signed APK as an **individual GitHub Release asset** or provide a separately verified standalone APK link. Do not falsely label an Actions artifact ZIP as a direct APK download.

## User-reported installation and entry issues

- Installing a new debug APK over v1 produced Android package conflict; removing v1 from Android Settings permitted installation.
- Play Protect displayed a **harmful app blocked** warning. **Cause is not established**; review signing, permissions (including `REQUEST_INSTALL_PACKAGES`), network behavior and package integrity; do not suggest bypassing protection as a routine installation procedure.
- User confirmed v2 launched, but its entry page lacked a **skip/offline entry button**. Code fix added in `ec8861da`, **not yet validated in an installable build**.

## v3 work plan / acceptance criteria

1. **Preserve the independent Tank repository and pinned baseline.** Begin from `develop`; inspect latest commits and failed workflow logs. Do not revert to older original HazRob application versions.
2. **Fix CI at HEAD**, confirm compilation and all applicable tests. The workflow excludes three legacy source-text tests that demand the original dark theme/old refresh implementation. Replace these with meaningful Tank-specific tests rather than treating exclusion as full UI validation.
3. **Finish persistent signing setup** (repository administrator), establish an upgrade-compatible baseline, increment version and correct v3 branding and entry footer.
4. **Test offline entry** on device: visible button, functional navigation, no false connection or telemetry, no drive commands without confirmed robot link.
5. **Complete physical standards coverage** by source, unit, calibration, exposure duration and context. Use WHO, OSHA, ASHRAE and other primary references where applicable; no universal limits for altitude, pressure, wind direction, luminous flux, etc. Add explicit averaging/validity provenance and tests. Do not call an instantaneous value a confirmed OSHA TWA exceedance.
6. **Review chemical/molecular safety emphasis** across all analysis panes, reports and alerts. Apply bold red only to valid threshold crossings, and reset styles on invalid/disconnected telemetry.
7. **Check ChemCore Stable runtime activation** and actual sensor/chemical analysis with controlled, calibrated data; maintain zero fabricated PM measurements.
8. **Validate firmware API4** against Android commands and on actual tracked hardware, especially emergency stop, drive neutral, watchdog, power supply and relay behavior.
9. **Build and sign v3**, verify signature/certificate fingerprint, package identity, versionCode, embedded ChemCore manifest/hash, tests and on-device upgrade. Publish **standalone APK** only after checks. Preserve prior app data across subsequent upgrades.
10. **Document every new change and test** in README and commit on `develop`; do not release prematurely.

## Instructions for a new ChatGPT conversation

> Continue independent **HazRob Tank v3** development in GitHub repository **`HazRob/HazRob-Tank`**, branch **`develop`**. **Read README.md and inspect the current repository and latest Actions results before editing.** Android base: `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` (read-only; never modify original). Firmware: `ESP32-Tank 0.1.1-dev`, API4. Last successful Android v2 debug APK: commit `df92e9cc`, Actions run `39084627573`. Later offline entry change `ec8861da` has **not** passed a full release build; persistent signing change `a2e8b4f0` requires GitHub secrets. ChemCore Stable `1.0.0-hr069r1` (652 identities) must remain embedded and verified. Preserve light Arabic RTL UI, real sensor data only, threshold provenance, emergency safety, separate project identity, and future in-place upgrade compatibility. **Do not release APK until requested v3 changes are finished and CI/device checks pass; provide a standalone signed APK, not an Actions ZIP.** Continue implementing user-requested changes with GitHub commits and update README.

