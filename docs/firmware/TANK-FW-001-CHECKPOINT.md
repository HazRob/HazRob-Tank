# Tank firmware refactor checkpoint — TANK-FW-001
Date: 2026-10-17
Branch: develop
Baseline: ESP32-Tank 0.1.0 (copied HR069 source). Target: ESP32-Tank 0.1.1-dev.

## Approved scope
1. Remove L298N motor pins and implementation; drive two continuous-rotation servos from adjacent GPIO outputs. Proposed GPIO16 (left), GPIO17 (right), subject to board and electrical verification. Rear-mounted servos; mechanical placement does not change code.
2. Implement short nonblocking acceleration/deceleration ramps (e.g. update every 20 ms), neutral dwell before reversing (e.g. 100 ms), and a 500 ms command-loss watchdog that ramps to neutral. Explicit E-stop remains immediate.
3. Remove IR completely: config, acquisition, JSON, alarms, and safety gating.
4. Retain BME280 on direct ESP32 hardware I2C GPIO13/14 (already implemented); no ADC or I2C hub.
5. Echo warning-only; remove all movement-blocking and stop-zone authority; preserve diagnostic distance and warning.
6. Delete unused code, avoid disabled legacy branches. Keep existing HTTP drive API and gas acquisition unaffected.

## Execution plan
- [x] P0: inspect Config.h, Hardware.cpp, SafetyOptions.cpp, Alerts.cpp and current hardware pin map.
- [x] P1: write approved scope and checkpoint before edits.
- [x] P2: replace Config.h motor/IR/ultrasonic stop definitions and update version.
- [x] P3: replace Hardware.cpp L298N with two servo PWM outputs, short ramps, reverse dwell, watchdog ramp; delete IR and motion blocking; simplify JSON.
- [x] P4: remove obsolete IR alert and safety-auto-stop code and endpoints; inspect dependent files for references.
- [ ] P5: static search for orphan symbols and verify endpoint compatibility.
- [ ] P6: compile with pinned Arduino-ESP32 board/toolchain; hardware bench tests with servos disconnected from drivetrain.
- [ ] P7: record release 0.1.1-dev changelog and actual test status.

## Technical constraints
Servo model, neutral pulse, rotation polarity, PWM frequency, speed limits and current draw are unverified. Provisional standard continuous-servo values: 50 Hz, 1500 us neutral, 1000-2000 us endpoints; calibrate physically before drive tests. Power servos from dedicated 5V outputs with shared ground, not ESP32 3.3V. GPIO16/17 must be confirmed available on selected ESP32 board. IR removal also changes Android telemetry schema: Android must be updated separately.

## Status
P0-P1 complete. No source modifications claimed by this checkpoint.

## Confirmed board and physical-direction contract (2026-10-17)
- Main controller: ESP32 DOIT DevKit V1; provisional servo signal GPIO16 left and GPIO17 right.
- Both continuous-rotation servos mounted at rear, on opposite sides; do not infer shaft polarity from logical wheel direction.
- API contract: `driveSet(+L,+R)` advances both tracks; `(-L,-R)` reverses; `(+L,-R)` pivots right and `(-L,+R)` pivots left, subject to real-world verification.
- Independent `SERVO_LEFT_INVERT` and `SERVO_RIGHT_INVERT` configuration, with neutral pulse calibration for each servo. Default polarity must be validated with tracks raised off the ground; correct in firmware rather than swapping UI controls.
- Bench test: neutral, forward, reverse, pivot left/right, braking, direction reversal, link-loss watchdog, emergency stop. Test without load first.

## Implementation checkpoint (2026-10-17)
- Commits: 67966b7 (main refactor), 9f45d1a (cleanup), ef59852 (last IR JSON removal).
- Source edited on develop only. Config and drive implementation updated; old L298N GPIO code removed; independent PWM channels 6/7 on GPIO16/17; nonblocking ramp, reverse dwell and 500ms watchdog deceleration; E-stop immediate.
- IR acquisition, alerts, safety gating, old safety option files/endpoints removed; Echo warning-only. BME remains direct GPIO13/14.
- P5 static inspection is INCOMPLETE. No compilation, no bench tests, no APK compatibility validation. Arduino-ESP32 2.x LEDC API assumed; GPIO16/17 and polarity require physical verification.
- IMPORTANT: hardware neutral calibration, servo power isolation, and cloud/app protocol review still pending. Do not flash on assembled drivetrain until tested.

## Reverse recovery implementation — 2026-10-17
- GitHub commits: 8106162 (recovery configuration), c9ed41d (history and reverse playback), aad57a0 (idle guard).
- ESP32-Tank firmware now has a 5-second rolling record, 5-second wait, reverse replay at 50% speed for twice each segment duration, and cancellation by explicit stop/E-stop or heartbeat.
- INCOMPLETE: actual transport-loss detection/reconnection across AP and MQTT, source compilation, servo neutral/polarity tests, timing under load and physical obstacle-safety tests. No claim of readiness for flashing.
