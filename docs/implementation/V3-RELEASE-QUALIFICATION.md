# HazRob Tank V3 — Release Qualification and Handoff Gates
**Prepared:** 2026-10-20 | **Branch:** `feature/tank-v3-implementation` | **Status:** RELEASE BLOCKED (intentional)

Refer to [execution log](V3-EXECUTION-LOG.md), [implementation plan](V3-EXECUTION-PLAN.md), and [owner-approved V3 requirements](../requirements/V3-ANDROID-BACKLOG-2026-10-20.md) for all 24 IDs.

## A. Proven build evidence (not physical proof)
| Item | Evidence | Status |
| --- | --- | --- |
| Tank ESP32-Tank sketch compiles on pinned Arduino-ESP32 2.0.17 | [CI run 39476938209](https://github.com/HazRob/HazRob-Tank/actions/runs/39476938209) on `54728175d2e...` | PASSED |
| Android V3 test + debug assembly + embedded 652 Chemical ChemCore payload manifest | [CI run 39478101674](https://github.com/HazRob/HazRob-Tank/actions/runs/39478101674) on `d3be7c82de...` | PASSED for that commit |
| Android UI test contracts, approved-only history and technical report changes on latest HEAD | [UI regression run 39478645439](https://github.com/HazRob/HazRob-Tank/actions/runs/39478645439) | PENDING at documentation checkpoint |
| Real stable signed APK with persistent administrator certificate | Private GitHub Actions secrets not yet configured | BLOCKED |
| Upgrade V2→V3 with intact sites, sessions, measurements, settings and R0 | Requires same signing identity + on-device data | BLOCKED |
| Real ESP32 hardware flash/run | Owner-held hardware required | NOT TESTED |

## B. Required safe firmware tests — DO NOT TEST UNDER LOAD
Perform initial motion tests with Tank **raised on a secure fixture and tracks free**, tested by a qualified operator, with a physical battery power cutoff accessible. Escalate to ground test only after full STOP behavior is verified.

- [ ] Confirm GPIO16 (left) and GPIO17 (right) servo directions, reversals, neutral at 1500 µs *for actual servos* and common ground. Detect neutral drift and prevent unintended movement.
- [ ] Test explicit Stop, E-STOP latch/reset and release-to-stop at positive and negative speed. Verify both outputs neutral promptly.
- [ ] Run 500 ms command silence / AP client disconnect / Remote MQTT disconnect tests. Both tracks MUST neutralize; no autonomous replay, reverse movement, restart or resumption without a fresh operator drive command.
- [ ] Direct AP boot profile, Direct↔Remote manual switch, offline app preview cannot drive or connect, one shared safety authority.
- [ ] Echo HC-SR04 warns below configured distance but **never automatically stops**; there is no IR sensor nor L298N Tank motor driver.
- [ ] Verify GPIO25 MQ relay OFF on reset, power loss and E-STOP; independently power the two servos, ADS + 8 MQ common supply, cable/fuse/ground checks.
- [ ] Verify all eight MQ channel Rs readings, two ADS1115 modules, BME280 on GPIO13/14, MAX4466 noise values, LDR threshold and optional camera relay.
- [ ] Confirm Remote MQTT TLS credentials and actual connection end-to-end. **NAPT Wi-Fi client Internet sharing is NOT validated/supported by pinned Arduino core 2.x**; do not imply working Wi-Fi Internet forwarding from the AP.

## C. Required real Android operator tests
- [ ] Install standalone Android APK, verify identity/package `com.hazrob.tank`, name Tank V3, app icon, versionName 3.0/versionCode 3 and authentic stable release signing certificate. V2 ephemeral debug APK can conflict; preserve/backup user data before any signature migration.
- [ ] OFFLINE selection gives full seven-tab UI with **zero network/motion requests**; reconnect mode is explicit and does not inherit offline danger.
- [ ] قيادة: live camera quality/flash, track joystick/dead-man, E-STOP/connection loss; no refresh icon; no nonexistent cliff/IR warning.
- [ ] Other six primary tabs: small refresh icon in **top-left of CONTENT**, never another full row or duplicate nested button, accessible and not obscuring text under RTL.
- [ ] القياسات > الفيزيائية: exactly two gray cards per row with correct icons and actual units; valid threshold crossing turns whole card red/white and returns immediately to gray; missing/uninstalled is neutral. **90 dBA threshold only with calibrated A-weighted audio, not raw MAX4466 dB SPL**.
- [ ] القياسات > الجزيئات is a **separate** subtab; PM1/PM2.5/PM10 not mixed with physical cards; without PM sensor show no-data, never fabricate particles.
- [ ] Chemistry search: fingerprint/curve/log equation/model estimators remain available. Display calculated finite PPM as estimates when applicable; distinguish confidence/reportability, not suppress the calculation.
- [ ] Developer > R0: record fixed 5-min ON warm-up + 3-min captures ×3 separated by 3-min ON pauses (20min total); show old/new each MQ in kΩ and a **single confirm/save**; verify cancel preserves old values, accepted values persist after restart, and new ChemCore uses them for subsequent analysis.
- [ ] User field session starts NEW/ACTIVE, exactly one documented complete acquisition, closes and explicitly approves; unapproved draft not visible in final reports and cannot be exported; approved report PDFs and XLSX scoped to one session or approved site children only. Developer lab/reference sessions never appear in end-user reports.
- [ ] Instant live snapshot is locally stored without a field session, separately listed; not included in approved site reports.
- [ ] Reports: all **20** plot selector types can be picked where real saved evidence exists; tap-to-enlarge chart opens correct full-size graph; appropriate clear no-data/failure state.
- [ ] PDF physical snapshot freezes values at click and notes timestamp/source/units; verify readability of Arabic/RTL text and report layout on real Android PDF viewer.
- [ ] PDF/XLSX field/session/site exports are consistent with approved scope, correct charts, known ppm methods/uncertainty, actual references, and no fabricated values.
- [ ] Scroll/performance, activity minimize/return during measurement, Wi-Fi disconnect, duplicate telemetry, corrupted/empty database, app restart and local backup/restore. Check data migrations and crash reports.

## D. Explicit definitions of done
- **SOURCE IMPLEMENTED:** Code committed on feature branch. No implied test.
- **CI VERIFIED:** Source SHA and URL to successful test/build required. Does NOT prove physical operation.
- **DEVICE VERIFIED:** Evidence from real phone and actual Tank hardware, including measured STOP/watchdog and sensor validity.
- **RELEASE READY:** Every P0 and owner-approved release-blocking P1 has evidence, stable signing and upgrade qualification. `T3-015` PM hardware is conditional, `T3-019` research 80-technique integration remains later scientific work.

## E. Resume command
If disconnected: read `docs/implementation/V3-EXECUTION-LOG.md` **first**, then this checklist; query current branch and CI status; act only on actual failures. Never reuse or guess an unsigned or ephemeral-debug APK as a release. No emulator workflow, no edits to original HazRob, no unattended robot motion tests.
