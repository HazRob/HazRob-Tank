# HazRob Tank V5 — R0 staged / ESP32 NVS acceptance

**Date:** 2026-10-28 (+03:00). **Branch:** `feature/tank-v5-implementation`. **HEAD before audit:** `9cfe50ce2f815bb12acb7ed0f432b34a4279594e`.

## Authorization clarification

The owner authorized a V5 APK **only after the full V5 plan and its tests are complete**. This does not authorize a build of incomplete work. `APK=NOT_BUILT`, `BUILD_GATE=BLOCKED`.

## Verified original defect

The previous ESP32 V4 `calibrationUpdate()` calculated eight R0 values and immediately called `saveR0Record(rec)` before Android showed the operator an «اعتماد» control. Android could only decide whether to import the already-persisted values into its own preferences. Cancelling the phone screen therefore could not roll back the previously changed firmware NVS.

## V5 implementation saved in GitHub

1. `firmware/ESP32-Tank/Hardware.cpp`: `CAL_PENDING`, `R0Record pendingR0`, `pendingR0Valid`. After 300 seconds warmup + 60 seconds full capture, require >=10 distinct scans per each of 8 MQ and check validity before setting `CAL_PENDING`. **No NVS save during `calibrationUpdate`**.
2. `calibrationAccept()`: checks state/record checksum/all eight values, writes `r0_v4` NVS (backwards-compatible storage key), reads back and verifies checksum, then switches live R0 and enters `CAL_READY`. Failure does not advertise success.
3. `calibrationCancel()`: erases pending RAM and transitions to `CAL_CANCEL`, leaving earlier NVS unchanged. Relay-off and emergency-stop discard any pending candidates.
4. `calibrationJson()`: reports `schema=tank-r0-fw-v5/2`, `state=pending`, `pending=true`, `committed=false` before acceptance and a verified committed READY state after NVS readback. Historical approved R0 is still available after restart via existing NVS.
5. `firmware/ESP32-Tank/ESP32-Tank.ino` exposes `/r0?cmd=accept`. API4 other routes remain unchanged. `Config.h` marks the source `ESP32-Tank 0.1.5-v5-dev` and `TANK-V5-STAGED-R0-UNVERIFIED`.
6. `android/.../RobotApi.java` adds direct `calibrationAccept()` command.
7. `FirmwareR0Contract.java` recognizes new V5 `pending` separately from committed `ready`, validates all eight with at least ten samples, and rejects contradictory state flags.
8. `DeveloperR0Tab.java` (now owned by `SettingsTab`) presents pending results, sends acceptance to ESP32 and reads back NVS **before** phone `ManualR0Calibration.saveFirmware`. It verifies device-ID equality and each sensor R0 within readback precision. Cancellation sends ESP32 `cmd=cancel`, requires explicit acknowledgement and does not overwrite phone R0.
9. Tests: `TankV5PendingR0ProtocolTest.java` covers pending-vs-committed, incomplete 8-channel samples, contradictory flags, cancellation and firmware no-premature-commit. Updated `TankV5OperatorR0ContractTest` and `TankR0LifecycleContractTest` to the new navigation/protocol.
10. Created `.github/workflows/build-tank-android-v5.yml`, **manual dispatch only** with a mandatory absent acceptance marker `docs/implementation/V5_RELEASE_GATE_APPROVED.md`; testDebugUnitTest must pass before assembleDebug. No build or runner has been started.

## Verification actually performed

- **18/18** direct source assertions on staged R0 firmware and Android handshake.
- **9/9** C++/Java source files lexically balanced; **this does not compile C++/Java**.
- Tests written, **JUnit not executed**. ESP32 firmware compile/flash, real 300+60s R0 on eight channels, power loss during accept, and Android UI validation **NOT RUN**.
- Do not state that pending NVS write is crash-atomic or that calibration is physically validated by these checks.

## Open release gates (mandatory before build)

- Firmware bench tests: new staged protocol + existing approved R0 after cancel/power cycle, 8 MQ, retry, and timeouts.
- Android JUnit/instrumentation and compatibility with actual firmware V5; V4 firmware cannot comply with pending acceptance and will be rejected by the V5 protocol.
- End-to-end session/site PDF & XLSX and upgrade V4→V5 using original customer data without deletion.
- Stable active payload/embedded checksum, update manifest URL provisioning and offline recovery, 4 developer library pages.
- Navigation/video/servo/Echo/IR/STOP on physical robot, no overlay collisions.
- Source/binary signing and update-key continuity for in-place V4→V5 upgrade, security audit.
- No release marker shall be created while **any** gate lacks verifiable positive evidence. Current build permission is conditional and remains blocked.

`CURRENT_STATUS=SOURCE_IMPLEMENTED_R0_PENDING_APPROVAL / REAL_DEVICE_VALIDATION_PENDING / APK_NOT_BUILT`.
