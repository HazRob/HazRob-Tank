# HazRob Tank V4 — Firmware Development & Recovery Plan
Updated: 2026-10-27 UTC | Status: IN PROGRESS / NOT RELEASED
Repository: HazRob/HazRob-Tank
Branch: feature/tank-v4-mq-unified-r0
Scope: firmware/ESP32-Tank only. Tank remains independent from the original wheeled HazRob.

## Approved decisions
- Eight MQ sensors: MQ2, MQ4, MQ5, MQ8, MQ3, MQ7, MQ9, MQ135 via two ADS1115 (0x48, 0x49) on GPIO21/22.
- BME280 directly connected to ESP32 on independent I2C GPIO13/14 (TwoWire(1)); do not move it to ADS or a hub.
- MQUnifiedsensor externalADCUpdate(voltage) and calibrate(cleanAirRatio); confirm actual library version, factors, units and licensing before integration. Do not fabricate fixed factory R0 values.
- No legacy R0 fallback or artificial acceptance band. Uncalibrated R0 = NaN; report Rs, voltage, raw values, but null ratio/ppm until calibration.
- Five-minute warm-up, then a time-bounded calibration measurement window (e.g. 60 seconds) collecting ALL valid samples per channel throughout the full window (minimum 10 valid samples per channel for acceptance), nonblocking state machine. Compute ONE final R0 per sensor from the arithmetic mean of all N valid MQUnifiedsensor.calibrate(cleanAirRatio) results (equivalently mean Rs divided by the fixed clean-air ratio). Use a running sum/mean with overflow protection. Store only this final R0 per sensor in NVS; actual sample count N is metadata, not additional R0 values. Preserve prior NVS values on failure.
- Software calibration does not establish certified gas concentration or manufacturer-specific heater cycling for MQ7/MQ9. Clearly distinguish software R0 from physical validation.
- Keep existing dual-servo drive GPIO16/17, link-loss stop, ultrasonic, relay, alerts, camera/network and direct/remote functionality unchanged. Deferred: dual drive-mode redesign.
- Keep code and diagnostics concise; do not remove essential safety checks to reduce binary size.

## Implementation checkpoint 2026-10-27
- 0537174: replaced per-channel writes with a single versioned, checksummed eight-channel R0 NVS record. All eight channels must have >=10 valid readings and ADS health before commit; failure preserves prior record. Legacy per-channel NVS values remain readable as fallback. Build run 40413775523 pending at documentation time.
- This is a single-record commit, NOT proven power-loss atomicity on actual flash. Requires interruption/reboot test and post-write readback verification.

- 05b1c14: calibration window bounded; both ADS required at start; reject invalid channels.
- 1adbae6: new V4 GitHub Actions compile/safety workflow, run 40412870990 initiated (result pending at documentation time).
- Pending review: source currently computes Rs mean / clean-air factor directly; user-approved MQUnifiedsensor API still must be integrated and verified.
- Pending review: NVS multi-key update is not atomic; failure may yield mixed channel values. Do not promote to release before transactional policy and tests.

- 84f51b8: first nonblocking duration-based calibration collector in Hardware.cpp; one Rs mean / clean-air factor per sensor.
- 6a1b16f, 497b361: declarations and /r0 HTTP start/cancel/status route.
- 75c738f: avoid partial MQ scans and simultaneous measurement.
- IMPORTANT: these commits are UNBUILT and UNTESTED. MQUnifiedsensor dependency/API is NOT integrated yet. The provisional clean-air constants must be verified; NVS transactional semantics, provenance, sample timing, API safety and JSON require review. Do not release.

## Current verified code checkpoint
- 4829bc0: removed historical fixed R0 defaults in Config.h; default NaN.
- 543ac2b: blocked invalid Rs/R0 ratio and corrected displayed calibration flag.
- 31ba3f5: added activeR0[8], loading per-sensor mq_r0_<index> from NVS, and switched read/JSON ratio to runtime values.
- ebff389: corrected invented NVS sample count; unknown historical provenance now remains 0. Existing R0 values without provenance still require explicit migration/acceptance policy before release.
- First calibration state machine and API are now present but uncompiled/untested; MQUnifiedsensor integration and verified firmware binary still absent.
- No build, automated test, or hardware test success has been demonstrated.

## Execution checklist (update this file after each completed task)
- [x] D01 Remove fixed historical R0 defaults.
- [x] D02 Use runtime R0 and avoid divide-by-NaN.
- [ ] D03 Audit all R0/ratio/ppm references; remove stale static provenance, fix NVS sample-count provenance and JSON status. Ensure all paths compile. Partial: ebff389 fixes sample-count invention; remaining audit and compilation outstanding.
- [ ] D04 Pin and verify MQUnifiedsensor version; verify clean-air factors from examples and manufacturer documentation, RL units, ADC voltage, and ADS full-scale vs supply.
- [ ] D05 PARTIAL (84f51b8, 75c738f): Implement nonblocking warm-up and configurable measurement window (example: 60 seconds AFTER warm-up), collect all valid readings throughout the window, require at least ten valid samples per sensor, and calculate one averaged R0 per sensor. Warm-up and collection duration are separate parameters. Define power-cycle/restart/cancel semantics and invalid-channel behavior. No blocking delays in control loop.
- [ ] D06 PARTIAL (497b361): Add explicit calibration start/status/cancel API; structured per-sample readings (sensor, timestamp, raw, voltage, Rs, candidate R0, error); never claim calibration when only R0 is loaded.
- [ ] D07 Atomic/rollback-safe NVS update policy. Persist R0, count, method/version, timestamp/session as available; restore after reboot. Reject nonfinite/zero candidates without old-R0 proximity restrictions.
- [ ] D08 Ensure no gas ppm/rank is presented as validated from software-only R0. Test missing sensor, bad ADS, relay off, unstable power, NaN, restart, partial channels, concurrent measurement.
- [ ] D09 Regression-check BME280 direct I2C, servos, emergency stop, ultrasonic, AP/direct, remote mode, telemetry and relay.
- [ ] D10 Compile for actual ESP32 board/core using reproducible toolchain and capture logs, flash/RAM size and warnings. Add CI if needed.
- [ ] D11 Run automated host/API checks, bench tests with real sensors/ADS/BME and independent verification of clean-air environment. Hardware results must be labelled NOT RUN if no device is available.
- [ ] D12 Only after review and test gates, tag/release V4 binary with SHA256, source commit, dependencies, release notes and reproducible build evidence.

## Acceptance gate
1. Clean compilation, zero unresolved symbols, version-pinned dependencies and reproducible build.
2. Each MQ yields a finite positive R0 only after at least ten valid samples and warm-up; sample evidence is retained.
3. Power loss or calibration failure cannot overwrite last accepted R0; fresh devices remain uncalibrated.
4. Runtime and reboot NVS values agree; JSON never outputs NaN/Infinity as invalid JSON.
5. No safety/control regression; servo stop and emergency stop tested.
6. Actual-hardware calibration, ppm accuracy and MQ7/MQ9 heater compliance remain unverified until physically demonstrated. Do not claim certification.

## Recovery instructions
1. Open this file on branch feature/tank-v4-mq-unified-r0 and check latest commit/history.
2. Inspect Config.h, Hardware.cpp/.h, ESP32-Tank.ino, build workflow and pinned dependencies.
3. Resume at the first unchecked D-task; do not reapply completed commits blindly.
4. After each verified step: commit code, update this checklist with commit SHA and test evidence. Do not mark an item complete from intent alone.
5. Do not publish V4 binary until acceptance gate is satisfied. If only static/CI testing is possible, disclose hardware tests as pending.

## Evidence to retain
firmware/ESP32-Tank/docs/v4/BUILD.md
firmware/ESP32-Tank/docs/v4/TEST_RESULTS.md
firmware/ESP32-Tank/docs/v4/CALIBRATION_PROTOCOL.md
firmware/ESP32-Tank/docs/v4/RELEASE_GATE.md
Create these only as real evidence accumulates; record command, tool versions, result, timestamp and source commit.
