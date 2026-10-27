# HazRob Tank V4 — Android, developer tools and ChemCore continuation ledger

Checkpoint date: 2026-10-27 (Asia/Aden)  
Canonical branch: `feature/tank-v4-implementation`  
Status: `SOURCE_UPDATED / NEW_TESTS_NOT_RUN / NOT_READY_TO_BUILD / UNRELEASED`

This file is the canonical-app follow-up to [V4-PROGRESS-LOG-2026-10-24.md](V4-PROGRESS-LOG-2026-10-24.md). It reconciles the separate firmware/R0 research branch without destructive merging. The historical execution log originally added on `feature/tank-v4-mq-unified-r0` remains a separate source of previous decisions.

## Verified branch reconciliation
- App branch pre-resumption HEAD: `e4d4db6a3a944acd75402ed298f62004b9b7322f`.
- Research branch observed HEAD: `1d2bbc01308ab6ec59ad7007a10723395d7dfbab`.
- All four primary firmware file blob hashes are **identical** between branches at those heads: ESP32-Tank.ino, Config.h, Hardware.cpp, Hardware.h. No firmware transplant is needed.
- Five developer-tab destinations resolve from the composed build: `DeveloperExperimentsTab`, `AnalyticalLabTab`, `DeveloperR0Tab`, `DeveloperConnectionDiagnosticsTab`, `CentralLibraryDashboardTab`. Connection diagnostics existed in the pinned Android original; the Tank-specific overlay now corrects its visual/access-state defects.
- Last controlled run [40437121019](https://github.com/HazRob/HazRob-Tank/actions/runs/40437121019) passed main Java compilation, failed test Java compilation on two `Files.readString` uses; those were fixed later but no current Gradle validation followed.

## Evidence ledger
| ID | Scope | Latest evidence | Status |
| --- | --- | --- | --- |
| V4-A01/D01 | Baseline, dependency and five developer classes | Pinned-source diagnostics class found; main compile on *earlier tested SHA*, four firmware blobs equal | SOURCE_AUDIT_PASS / CURRENT_COMPILE_PENDING |
| V4-A02 | User Sessions P0 crash/ANR and migrated schema | Off-thread SQLite/Spinner guards present; no fresh/migrated phone Logcat | SOURCE_PRESENT / DEVICE_PENDING |
| V4-A03 | User site/session PDF/XLSX, approval, SAF | Worker and scope fixes present; no opened verified real exports | SOURCE_PRESENT / DEVICE_PENDING |
| V4-A04/D02 | Light theme, bordered controls and developer UX | `2ae12ca` corrected inherited developer network page; all-screen screenshot test missing | PARTIAL_SOURCE / DEVICE_PENDING |
| V4-A05 | Original approved launcher/entry logo | Previous pinned-source identity documented; launcher/entry images not visually verified on current install | REVIEW_PENDING |
| V4-A06/A07/D04 | ESP32 /r0 and 300s+60s state machine | Firmware and Android API source present; min 10 samples per channel; no physical collection or NVS power-cycle test | SOURCE_PRESENT / HARDWARE_PENDING |
| V4-A08 | Firmware/phone manual R0 authority and archived provenance | Confirmed/provisional distinction, per-device guards; migration and two-device replay pending | SOURCE_PRESENT / DEVICE_PENDING |
| V4-A09/D05 | ChemCore Stable embedded, candidate/PPM replay | Generator pins `1.0.0-hr069r1`, 652 identities/0 accepted M4; current APK integrity and gas reference benchmarks unverified | SOURCE_PRESENT / SCIENTIFIC_G3_PENDING |
| V4-D03 | Developer experiments, approval, signed review publication | `e3344759`, `84471d9`, `09ea5b7`: Tank-specific review branch, original route excluded, 201 + read-back hash; no authenticated PUT/read-back performed | SOURCE_FIXED / NETWORK_TEST_PENDING |
| V4-D06 | Connection diagnostics, direct/remote, credential UI | `2ae12ca`, `4277e11`: light text/fields, checkbox snapshot before async WAN worker; real network/permissions not exercised | SOURCE_FIXED / DEVICE_PENDING |
| V4-D07 | Library, Stable updates, rollback, provenance | Source inspection only; atomic remote activation/negative tests not established | IN_PROGRESS |
| V4-D08 | Cross-tab regression, title/logo, sessions/reports | Two new source-contract tests, static gate updated; complete test matrix pending | IN_PROGRESS |
| V4-A10/D09 | Full Java/Gradle CI, Android APK, signing and real hardware | No post-patch build, no real Android bench acceptance | BLOCKED_BY_OWNER_BUILD_HOLD_AND_ACCEPTANCE |

## Commits in 27 October resumption
- `e3344759`: new Tank-only publisher overlay and GET/readback SHA-256.
- `84471d9`: bounded readback for larger references.
- `09ea5b7`: Tank publication UX and remove visible app version on experiment title.
- `d986966`, `ceab99f`: JUnit contract source for staging.
- `2ae12ca`: inherited developer diagnostics page restored to light UI and WAN checkbox state confined to UI thread.
- `4277e11`: JUnit contract source for diagnostic UI/threading.
- `7ff040c`: future static preflight checks include publication isolation and developer WAN page.
- `65eb0a9`: appended evidence to canonical V4 progress log.

Static markers were independently read back from live GitHub source and test text: 18/18 true. This is **not** a JUnit, Python CLI, authenticated GitHub upload, Android/SQLite device, or hardware test.

## Next safe steps, in dependency order
1. Inspect/check test Java sources for correct assertions, source/manifest integration and API compatibility; correct any source-only defects without running Gradle or triggering workflows.
2. Verify the other developer pages and dialogs use dark readable text on light surfaces and outlined selection widgets; add focused regression source tests.
3. Review report scope, one-operation ChemCore/frozen R0 and Stable source provenance. Preserve all existing V3 field and developer data.
4. With explicit owner authorization, run a **diagnostic** build: run source/static/SQLite gates, `:app:testDebugUnitTest` then `:app:assembleDebug`; review exact logs and APK-embedded Stable SHA/signature; do not suppress meaningful failed tests.
5. Perform device matrix: freshly installed and migrated DB, 5 developer tabs, sessions crash Logcat, Arabic PDF/XLSX reopen, PIN/permissions/offline, signed in-place update, WAN errors and authenticated GitHub review PUT/GET against an approved test record.
6. Test eight physical MQ channels, RL, R0/300+60 windows and NVS interruption, MQ7/MQ9 heater, Echo against a ruler, 500 ms neutral stop/E-STOP and camera. Scientific G3 qualification is separate from software CI.

**Do not infer release readiness from compiler success on an older SHA or structural markers.**
