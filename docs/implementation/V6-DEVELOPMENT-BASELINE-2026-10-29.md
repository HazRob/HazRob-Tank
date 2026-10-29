# HazRob Tank V6 — development baseline and release discipline

**Established:** 2026-10-29 (+03:00)
**Repository:** `HazRob/HazRob-Tank`
**Active V6 branch:** `feature/tank-v6-implementation`
**Exact V5 source baseline:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`
**Parent branch:** `feature/tank-v5-implementation`
**Status:** `V6_BRANCH_CREATED / REQUIREMENTS_INTAKE / NO_V6_SOURCE_CHANGES / NO_V6_APK`

## 1. Scope and isolation

This conversation/workstream concerns **the tracked HazRob Tank Android app V6**, inheriting V5 app logic, assets, embedded ChemCore baseline, tests, Android identity, ESP32/API4 compatibility contract, and firmware source as-is at the pinned baseline. V5 remains intact. No changes to the original wheeled HazRob or its SERVOC repository. Do not merge directly to `main` or `develop` before baseline reconciliation and explicit authorization.

V6 is a *development branch*, not a claim that Android versionName/versionCode, Gradle workflow, signer, Stable library or ESP32 firmware have been released. At branch creation `ReleaseAudit.VERSION` still reports `5.0` as inherited from V5. Version bump to `6.0` belongs to a later controlled implementation change.

## 2. Baseline evidence

- GitHub comparison `feature/tank-v5-implementation...feature/tank-v6-implementation` was `identical`, `ahead_by=0`, `behind_by=0`, prior to V6 documentation edits.
- Last V5 source change at baseline: `TankFirmwareCompatibility.java` checks ESP32 main-controller banner, `/diag` API4, `hardware_profile`, servo pins 16/17 and radio profile `direct`. Camera identity is a separate capability. This is **not** cryptographic device authentication.
- V5 diagnostic build workflow: `.github/workflows/build-tank-android-v5.yml`. It overlays Tank Java/resources/tests on a *read-only pinned* `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`, and reassigns application ID to `com.hazrob.tank`.
- V5 build notes authorize a **diagnostic** APK, not a scientifically or physically certified safety release. Verify the *actual* latest Actions run and APK signature before inheriting any claimed pass.
- V5 `ReleaseAudit.DATABASE_SCHEMA=10`; preserve V5 tables/data and approved cycle/report provenance. Database schema v11 is **not automatically required** for V6.
- Embedded Stable `1.0.0-hr069r1`, 652 identities and 49 reference priors are historical source expectations, **not** certified gas identity or ppm performance. Revalidate the precise V6 APK assets, payload digest and manifest when built.
- Existing user-reported robot connection trouble is an **open investigation**, not a proven cause. Investigate pairing/AP IP, HTTP `/` and `/diag` compatibility, API4 fields, controller vs camera URL, timeouts and E-STOP fail-closed before modifying safety logic.

## 3. V6 work packages (framework; owner changes pending)

| Stage | Key actions | Evidence / exit gate |
| --- | --- | --- |
| V6-W00 baseline | Freeze V5 commit and inventory Android, firmware, Stable, signing and reports | Source SHA, V5 behavior checklist, non-destructive branch comparison |
| V6-W01 requirements | Number approved changes `V6-001...`; define acceptance cases and affected modules | Requirement register with owner acceptance; nothing presumed approved |
| V6-W02 architecture | Dependency/compatibility review incl. ESP32 API4, camera, ChemCore, SQLite | Impact and rollback analysis, interface changes documented |
| V6-W03 implementation | Small traceable commits per approved change | Source diffs, updated tests and risk review |
| V6-W04 verification | JVM/Gradle, DB migration, PDF/XLSX, UI RTL/contrast, network, ESP32 hardware | Actual test outputs with pass/fail and CI link; no marker-only PASS |
| V6-W05 candidate | Conditional V6 diagnostic build, version 6.0, APK contents, signature and upgrade validation | VersionCode monotonic, same signer for in-place upgrade, checksum, install/upgrade and smoke tests |
| V6-W06 release | Owner acceptance, scientific safety labels, V6 report and release packaging | Explicit release approval; no manufactured certification |

## 4. Non-negotiable regression and safety gates

1. **Connectivity:** lost link inhibits drive; E-STOP remains available, fail-closed and independently testable. Compatibility must depend on verified firmware protocol contract, **not** Android version string. Do not claim HTTP/AP is authenticated.
2. **Measurement provenance:** retain raw samples, original R0 and selected library revision; no silent recomputation of approved historical analysis.
3. **R0:** staged `CAL_PENDING → accept/cancel` and NVS power-loss protection are preserved; test ESP32 firmware behavior against the real board.
4. **Field sessions:** site/session/cycle ownership, multiple approved cycles, interrupted attempts, exports to PDF/XLSX and SQLite v10 migration must not regress.
5. **ChemCore:** distinguish research screening M1/M2 and physically validated/approved calibration; no M4/G3 or safety-grade ppm declaration from simulation.
6. **UX:** RTL, controls, checkbox contrast, session screen stability, offline mode, landscape driving, refresh and status panels checked on actual Android where required.
7. **Distribution:** do not mutate V5 signing identity, overwrite any V5 artifact, or assume debug-signing upgrade continuity. Preserve backup/rollback path.
8. **Change control:** V6 work occurs only on V6 branch. No Gradle build, Actions build, firmware flash or APK release is implicitly authorized by establishing this branch.

## 5. Durable continuation contract

Update `docs/requirements/V6-CHANGE-REGISTER-2026-10-29.md` for each owner request, and append a chronological entry to `docs/implementation/V6-PROGRESS-LOG-2026-10-29.md` after every implementation batch: date, requirement IDs, Git SHA, files changed, tests actually executed, results, blocker, next action. Keep `NOT_RUN`, `BLOCKED`, `FAIL` distinct from `PASS`.

**Resume instructions:** read this document, requirement register, progress log, then inspect *live* V6 HEAD. Avoid reading only older V5 checkpoints as if they represented the latest V5 state.
