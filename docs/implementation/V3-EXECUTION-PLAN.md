# HazRob Tank V3 — Implementation Plan and Durable Checkpoint
**Most recent successfully tested Android code:** `25d12dab1080f1dce8e0303b3adda78a1d977b69` — [CI 39478645439](https://github.com/HazRob/HazRob-Tank/actions/runs/39478645439) SUCCESS. Firmware build on `54728175` — [CI 39476938209](https://github.com/HazRob/HazRob-Tank/actions/runs/39476938209) SUCCESS. Subsequent ESP32 packaging workflow run may be pending.\n\n**Initiated:** 2026-10-20 | **Branch:** `feature/tank-v3-implementation` | **Base:** develop | **State:** IN PROGRESS — NOT RELEASED

## Governing requirements / provenance
- **Source of truth:** [V3 backlog](../requirements/V3-ANDROID-BACKLOG-2026-10-20.md) covering T3-001…T3-024, including owner-approved physical card UI, alert transitions, refresh icon, fixed 20-minute clean-air field R0 procedure, chart viewer, and independent PM tab.
- **Never modify the original application** `sohpany/SERVOC-Android`. The CI retrieves pinned baseline `6281d3eb7c1c961ce63b0b5e4ae0792874626720`, copies overlays in `android/app/src/main/java`, and builds Tank package `com.hazrob.tank`.
- **Last actually successful V2 APK:** `df92e9ccd2d7d8b69b83fff5515569d298734dc4`; later `develop` was not release-built. Preserve all Tank firmware hardware/safety decisions; no IR/L298N/automatic obstruction stops. Do not reintroduce deleted emulator/screenshot CI.
- **Definition of DONE:** committed independent code + tests + successful CI + appropriate real-phone / real-hardware acceptance. Static code implementation alone is marked IMPLEMENTED_UNVERIFIED, **never VERIFIED**.
- **Publishing:** V3 standalone APK only after completed gates; debug-only test artifact and unsigned build are not a signed V3 release. Stable signing secrets must be supplied by authorized admin and never committed.

## Phase plan and requirements mapping
| Phase | Priority | Requirements | Concrete work | Exit gate |
| --- | --- | --- | --- | --- |
| P0 Foundation | BLOCKER | T3-001,002,003,004,014,018 | V3 versionName=3.0 / versionCode=3, separate CI, persistent signing with explicit failure status, offline entry and full network/motion guard, independent branding, firmware API4 safety | unit tests + debug assemble; stable cert and physical fail-safe needed for release |
| P1 Operator design | HIGH | T3-005,016,020,021,022 | RTL/card consistency, physical cards two/row, emoji, context-valid red/gray instantaneous thresholds, compact refresh at upper left of six non-driving contents; none on drive | UI static tests + real-phone inspection |
| P2 Session lifecycle | HIGH | T3-006,007,008,017 | NEW/APPROVED field session lifecycle and atomic capture; approved-only reports, separate instant/developer data; migration/data integrity | DB and scope tests + test recovery |
| P3 Measurement / ChemCore | HIGH | T3-009,010,011,013,023 | target capability flags, chemistry evidence, user R0 5/3/3/3/3/3 (20 min) from valid Rs per channel and one confirmation/save; persist and ensure ChemCore uses saved app authority | numerical R0 tests + telemetry integration + no history mutation |
| P4 Reports and particles | HIGH | T3-012,015,024 | accessible selector of 20 data-backed charts, full-size viewer, genuine recorded series only, PM own tab and explicit not-installed condition, PDF/Excel snapshots | 20 chart test matrix + export checks |
| P5 Research separation | CONDITIONAL | T3-019 | proposals only; do not merge unvalidated research techniques into production ChemCore | explicit owner scientific acceptance |
| P6 Release qualification | BLOCKER | ALL | full unit/integration CI, manual device evidence, hardware safety review, stable APK signature, version, embedded ChemCore hash, upgrade, README change log, release asset | ALL BLOCKERS CLOSED before release |

## Interim execution checkpoint (2026-10-20, build verification ongoing)
- Source development has progressed beyond V3-01 to user field APPROVED lifecycle, approved-only field/site exports and physical PDF export; see [V3-EXECUTION-LOG.md](V3-EXECUTION-LOG.md).
- Implementation statuses in this ledger are **SOURCE STATE**, not acceptance/physical verification. All important UI, signing, ChemCore activation, hardware and real-device gates remain open.
- Most recent source at checkpoint: `dafa0101553be2a3874a79ad50684eb8d5e57a73`; [candidate CI run 39472037922](https://github.com/HazRob/HazRob-Tank/actions/runs/39472037922) pending final observation.

## Work-package dependency order
1. Establish this durable checkpoint on a **V3 feature branch**. No premature APK release.
2. Produce V3 CI as build/test/diagnostic pipeline and distinguish signed release vs development artifact; use GitHub Actions but no simulator.
3. Fix offline transport guard **end to end**, not only a menu button: EntryActivity mode contract, MainActivity boot/startLinkService/scheduler/drive and refresh, background lifecycle, and safety from service reconnects.
4. Implement layout improvements independently from chemical computations. Each change receives test evidence.
5. Implement datastore/state migrations, R0 computation and adoption across normalizer/ChemCore.
6. Test/verify, prepare signed release only with authorized keystore and physical checks.

## Status ledger (persist after every batch)
| ID | State | Note |
| --- | --- | --- |
| T3-001 | BLOCKED_EXTERNAL | Persistent signing keystore/secrets not established; CI build can check compilation without publishing a release |
| T3-002 | CI_VERIFIED_DEVICE_PENDING | V3 offline selection, service and RobotApi network hard gate; Android tests pass at 25d12dab, physical phone/network test pending |
| T3-003 | FIRMWARE_COMPILED_HARDWARE_PENDING | No autonomous replay; neutral stop on link loss or 500ms timeout; ESP32 Arduino-2.0.17 full compile SUCCESS run 39476938209; actual servos and E-STOP untested |
| T3-004 | PARTIAL_IMPLEMENTED | V3 strings and branding |
| T3-005 | PARTIAL_CI_VERIFIED | Measurements, Reports, Status, R0 developer and now Settings use light cards; Android build pending latest status; real phone layout test still required |
| T3-006 | CI_VERIFIED_DEVICE_PENDING | Session approval now requires completed READY USER capture with real samples and coherent terminal metadata; JVM guards passed CI 39480818787; real SQLite/device check pending |
| T3-007 | CI_VERIFIED_DEVICE_PENDING | SQLite approved-only report scope before LIMIT, history/graphs and site/session exports; CI 39480963016 passed, no real phone export validation |
| T3-008 | CI_VERIFIED_DEVICE_PENDING | Instant locally saved snapshot and independent log not linked to field sessions; Android CI 39477075161/39478645439 successful |
| T3-009 | IMPLEMENTED_UNVERIFIED | Preserve fingerprint/curve/formula/algorithm computation; display all available computed ppm estimates and method sources; device test pending |
| T3-010 | PARTIAL_IMPLEMENTED | Source/report evidence ranking and finite estimated ppm displayed; regulatory exceedance guards retained; physical/UI verification pending |
| T3-011 | CI_VERIFIED_DEVICE_PENDING | Physical PDF snapshot at click implemented and Android CI successful; verify Arabic rendering / data units on phone |
| T3-012 | PARTIAL_IMPLEMENTED | Existing PDF/Excel site/session writers, 20 real-source graph selector and large dialog compile; 20-case chart/export quality acceptance not done |
| T3-013 | CI_VERIFIED_DEVICE_PENDING | Stable snapshot install followed by molecular import and QSPR runtime rebuild at boot; Status shows active library/model counts, CI 39481514424 passed; runtime on phone pending |
| T3-014 | ESP32_COMPILED_HARDWARE_PENDING | Firmware API4 Arduino core compile run 39476938209 plus Android static route test; Tank peripheral/network hardware test pending |
| T3-015 | MAINTAIN_EXISTING | PM fourth subtab exists; no physical PM hardware |
| T3-016 | CI_REGRESSION_PASSED_DEVICE_PENDING | Android unit and UI source contracts passed CI 39478645439; real Android scrolling/layout/offline/performance untested |
| T3-017 | PARTIAL_IMPLEMENTED | Approved-only USER field reports and source-level developer isolation in place; full local-vs-central sync/data migration tests pending |
| T3-018 | BLOCKED_DEVICE | Android release installation and Play Protect diagnosis require stable keystore and physical target phone |
| T3-019 | DEFERRED_CONDITIONAL | Research integration is not a V3 ship blocker |
| T3-020 | IMPLEMENTED_UNVERIFIED | Physical cards 2/row |
| T3-021 | PARTIAL_IMPLEMENTED | Gray normal/red+white on valid instantaneous threshold crossing |
| T3-022 | IMPLEMENTED_UNVERIFIED | Single icon top-left non-drive tabs |
| T3-023 | CI_VERIFIED_DEVICE_PENDING | 20-minute R0 flow plus automatic source preservation in historical series; CI 39481674270 passed; real 20-minute sensor/ppm tests pending |
| T3-024 | IMPLEMENTED_CI_PENDING | 20 chart slots dynamically filtered for matching real MQ/environment/sound/chemcore evidence; old contract revised; latest CI pending, device plots pending |

## Resume after interruption — precise instructions
1. Read **this file first** and [V3 backlog](../requirements/V3-ANDROID-BACKLOG-2026-10-20.md); inspect branch HEAD SHA and all commits since creation.
2. Fetch most recent `docs/implementation/V3-EXECUTION-LOG.md` if present; check pending CI runs. Never infer completion from a planned step.
3. Continue **only from last VERIFIED commit/checkpoint**, focusing on the first queued batch. No edits to original repo. No simulation/emulator workflows.
4. If GitHub Actions fails, inspect **actual job log** (compilation, unit tests, signing) and update status without inventing a successful APK.
5. Do not request already supplied owner decisions again. V3 requirements and the R0 20-minute procedure are approved in backlog.
