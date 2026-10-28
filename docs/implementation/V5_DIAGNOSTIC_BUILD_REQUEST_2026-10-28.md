# HazRob Tank V5 — explicit owner diagnostic build request

OWNER_DIAGNOSTIC_BUILD_AUTHORIZED=2026-10-28T03:17:21Z

The user explicitly requested **now** that the V5 Android APK and V5 firmware ZIP be prepared.
This authorizes a diagnostic GitHub Actions compilation/test run and separate ESP32/CAM
Arduino source ZIP packaging. It does **not** assert robot G3/M4 calibration, safety
certification, physical R0 testing, production signing, safe field usage, or that
the V5 release gate has passed.

Workflow: `.github/workflows/build-tank-android-v5.yml`.
Test JUnit before assemble; if compilation or tests fail, no manufactured APK
or fake success. Firmware ZIP may be produced independently of Android build.

RELEASE_GATE=NOT_YET_MET
