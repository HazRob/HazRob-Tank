# ESP32-Tank — HazRob Tank V6 controller candidate

**Source version:** `ESP32-Tank 0.1.6-v6-quickr0-dev` (`Config.h`), API 4. This is a development/acceptance candidate, **not** a completed hardware qualification or user-ready firmware release.

The controller targets ESP32 DOIT DevKit V1 with two continuously rotating rear servos: GPIO16 left, GPIO17 right. ADS1115 buses, direct environmental I²C, ultrasonic Echo warning only, independent alert and 500 ms lost-command motor watchdog are defined in `Config.h`, `Hardware.cpp` and `SafetyInterlock.cpp`. There is **no** IR cliff sensor or L298N DC motor driver in the Tank configuration.

**R0 V6 operator contract:** one **180-second warmup + 60-second real sampling window** for all eight MQ channels; a successful verified device clean-air result is stored with V6 NVS provenance and explicit acceptance. Do not use external R0, published curves or fabricated normal air as a physical device calibration.

Compile target is Arduino-ESP32 **3.3.11** on board `esp32:esp32:esp32`. Source/firmware CI on this branch confirms the sketch compiles as a software check only. Before safe field operation, bench-verify servo polarity and neutral positions, supply rails and level shifting, camera link, timeout stop, flame and environmental sensors, real R0, and all emergency functions.

V6 deliverable must be packaged with `Cam-tank` only after Android V6 acceptance and the project owner's conditional release instruction becomes effective. GitHub Actions compile artifacts are **test evidence**, not approved installation packages. See `docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json` and `docs/firmware/TANK-FW-001-CHECKPOINT.md`.
