# HazRob Tank V4 — firmware build authorization, 27 October 2026

The owner explicitly authorized producing ESP32 Tank V4 Arduino source ZIP
and an Android V4 APK. This file triggers the existing firmware-only verification
workflow on `feature/tank-v4-mq-unified-r0` without changing physical controller code.

The requested deliverable is a directly downloadable Arduino sketch ZIP
containing `ESP32-Tank/ESP32-Tank.ino`, `.h` and `.cpp` sources. A successful
toolchain compile is not evidence of actual MQ sensor calibration, heater regime,
safe driving or flash/power-loss reliability. Do not flash the robot automatically.
