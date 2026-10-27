# HazRob Tank V4 — LEDC servo API compatibility fix (2026-10-27)

Status: `CI_COMPILE_PASS_ON_BOTH_CORES / REAL_ROBOT_TEST_PENDING / NOT_SAFETY_CERTIFIED`.

## Field report and cause
The user reported Arduino IDE errors at `firmware/ESP32-Tank/Hardware.cpp`, line 280: `ledcSetup was not declared` and `ledcAttachPin was not declared`. Their selected board was DOIT ESP32 DEVKIT V1.

These symbols were removed in Arduino-ESP32 core 3.x; Arduino-ESP32 2.x code used the legacy channel-based API. The initial V4 firmware build had been validated **only on core 2.0.17**, hence its earlier success did not guarantee compiling against a user's newer board manager core.

Official upstream guidance: https://docs.espressif.com/projects/arduino-esp32/en/latest/migration_guides/2.x_to_3.0.html and https://docs.espressif.com/projects/arduino-esp32/en/latest/api/ledc.html.

## Production firmware correction
- Main firmware branch: `feature/tank-v4-mq-unified-r0`, commit `a4af298e5893db22b4b02573834b361faa0c772e`.
- Matched source propagated to canonical V4 app branch: commit `6db0af8fcf08e1218cb87b5e903fe56e73bdc5b2`.
- Both corrected source copies share Git blob SHA `0015d2ea2c1db474d3f556fee7e443d430608ac9` for `Hardware.cpp`.
- Added `<esp_arduino_version.h>` and conditional PWM initialization/write in `servoPwmBegin` and `servoPwmWrite`:
  - Arduino-ESP32 2.x: `ledcSetup(channel,50,16)`, `ledcAttachPin(pin,channel)`, `ledcWrite(channel,duty)`.
  - Arduino-ESP32 3.x: `ledcAttachChannel(pin,50,16,channel)`, `ledcWriteChannel(channel,duty)`.
- Preserved distinct channels **6 and 7** and physical servo pins **GPIO16/17**.
- If attaching PWM fails, the affected pin is explicitly configured OUTPUT LOW rather than asserting a working pulse train. Motion and safety logic were otherwise left unchanged.

## Reproducible CI evidence
- Updated workflow: `7b395508e5fc88db1d168eb4bd01721468130165`; pins both 2.0.17 and 3.3.11 on independent runners.
- GitHub Actions run: https://github.com/HazRob/HazRob-Tank/actions/runs/40558674621.
- **2.0.17:** SUCCESS (core installation, invariant checks, ESP32 Dev Module sketch compilation, Arduino ZIP and firmware artifact upload).
- **3.3.11:** SUCCESS (same gates, independent compilation, source ZIP and compiled firmware artifacts).
- Artifacts: `tank-v4-UNRELEASED-core-2.0.17` (#12759125030) and `tank-v4-UNRELEASED-core-3.3.11` (#12758077579).
- The source ZIP extracted from the 3.3.11 CI artifact has 19 files; CRC check PASS; `Hardware.cpp` blob SHA matches both corrected branches. Source ZIP SHA-256: `72c7361ecdf8b0eb5ed8616a0f0e2e91d86ad70c2fa0c5d9809a6fd44bbd6263`.
- `2.0.17` main image: 995248 bytes; `3.3.11` main image: 1134960 bytes. These binaries are core-specific; **do not mix** bootloaders, partition tables, and application images from different cores.

## User installation
1. Extract direct sketch ZIP, open `ESP32-Tank/ESP32-Tank.ino`.
2. In Boards Manager install Espressif ESP32 `2.0.17` or `3.3.11`; board ESP32 Dev Module / compatible DOIT ESP32 DEVKIT V1.
3. Compile and upload with the same core version selected for that local build.
4. Before enabling propulsion, keep the chassis raised and test both servos at neutral; test each axis and E-STOP, link-loss watchdog, GPIO wiring, ultrasonic interlocks and MQ relay. Software compilation alone does not prove PWM neutrality or physical safety.

**Explicitly not tested:** actual user board, servo pulse timings and neutral calibration, 5 V servo power integrity, R0 known-gas evidence, NVS power interruption, physical E-STOP, GPIO/pin failure, and the Android app talking to the newly flashed robot.
