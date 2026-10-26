# ESP32-Tank firmware

**Latest development candidate: V4 (`0.1.4-v4-dev`)**. Not a stable/hardware-qualified release.

## Latest ESP32-Tank firmware source: V4 development candidate (2026-10-27)

**Current latest ESP32-Tank firmware on this branch:** `ESP32-Tank 0.1.4-v4-dev`, source at [this directory](./). This supersedes V3 as the **latest development firmware**, **not** as a hardware-validated stable release. The Android APK and ESP32-CAM have separate release status and are not replaced by this firmware package.

- **Arduino IDE ZIP:** [GitHub Actions run 40414404284](https://github.com/HazRob/HazRob-Tank/actions/runs/40414404284), artifact `tank-v4-UNRELEASED-compile-candidate` contains `HazRob-Tank-V4-Arduino-UNRELEASED.zip` and compiled ESP32 binaries. Extract the inner Arduino ZIP, then open `ESP32-Tank/ESP32-Tank.ino`. Board: ESP32 Dev Module / DOIT ESP32 DevKit V1, Arduino-ESP32 core **2.0.17**.
- **V4 changes:** eight MQ sensors through two ADS1115 modules; invalid/uncalibrated R0 is never silently replaced with historical defaults; runtime R0 loading; nonblocking calibration with configurable warm-up (default 300 seconds) followed by a full timed sampling window (default 60 seconds); all valid Rs readings in that window contribute to one mean-based R0 per sensor, with at least 10 valid samples required per sensor; calibration status/start/cancel HTTP `/r0` endpoint; eight-channel versioned/checksummed NVS record to avoid publishing partial new calibration; preserve previous calibration when a new one fails; no concurrent normal measurement/calibration. Rear dual continuous-rotation servo GPIO16/17 and direct BME280 GPIO13/14 preserved; existing relay/emergency stop and control watchdog preserved.
- **Verification:** GitHub Actions run 40414404284 **passed** source invariant checks, Arduino-ESP32 2.0.17 compilation and packaging. This does **not** validate MQUnifiedsensor equivalence, clean-air factors, ppm accuracy, power-loss behavior or real sensor/robot safety. MQUnifiedsensor library integration and bench calibration remain outstanding. **Do not treat this as a production or safety-certified firmware release.**
- **Detailed durable development record:** [V4_DEVELOPMENT_PLAN.md](V4_DEVELOPMENT_PLAN.md).

---


