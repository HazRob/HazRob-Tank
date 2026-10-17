# TANK-APP-CHANGE-001 — Operator drive and obstacle policy

Status: requirements accepted; **Android source is not yet imported; no APK has been built**.

## Accepted requirements

- Tank app is independent of the original HazRob Android app. Do not edit `sohpany/SERVOC-Android`.
- Remove IR references, status, alerts and automatic-stop controls from **all** Tank Android screens, diagnostics and UI logic. Do not imply IR is installed.
- Echo is **distance telemetry and proximity warning only**. It must never veto drive commands or stop the robot due to proximity.
- Remove the obstacle/sensor automatic-stop toggle and all IR/Echo obstacle automatic-stop pathways from the Tank Android application.
- Keep deliberate manual STOP, latched E-STOP, release-to-stop, command timeout/deadman and loss-of-operator-control stop. These are operator-command safety mechanisms, not obstacle avoidance.
- Servo track outputs are left GPIO16 and right GPIO17, not the former L298N control pins.
- Preserve Direct/Remote mode selection and camera settings unless separately requested.

## Current firmware audit

`firmware/ESP32-Tank/Config.h`: dual servo GPIO16/17, ultrasonic TRIG23/ECHO39, warning at 10 cm. No IR GPIO is defined.
`firmware/ESP32-Tank/SafetyInterlock.cpp`: latched manual E-STOP only.
`firmware/ESP32-Tank/Hardware.cpp`: ultrasonic `ultraWarning` drives an alert, but observed proximity is not used as a motion-stop condition. 500ms command timeout remains.

## Implementation gate

Copy the pinned Android source revision `6281d3eb7c1c961ce63b0b5e4ae0792874626720` into this repository before editing. Update `HazRobHardwareProfile`, `SettingsTab`, `ControlTab`, relevant API/drive and tests; search all UI/resources for IR and auto-stop; compile and test the new APK. Do not claim Android changes are complete before that happens.
