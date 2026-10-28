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

## Retry 2
Initial GitHub compilation exposed two undefined `context` references in `ChemCoreAnalysisTab`.
Fixed in commit c2d546f3730630eec37f80d367f8e4b981e40372. Explicit user request remains in effect.
Diagnostic debug-key APK is permitted if the persistent V4 upgrade certificate is unavailable; not an in-place update or certified release.

## Retry 3
Android app compilation passed after context fix; JUnit source compilation found Java 11 Files.readString in a Java 8 Android test. Updated to Files.readAllBytes. A diagnostic APK is now explicitly allowed to compile even if unrelated legacy unit-test assertions fail, with an unrested NOT_RELEASE warning; report failures transparently.

## Retry 4 — complete 763-case JUnit regression matrix
On the previous diagnostic build, Java compilation and assembleDebug succeeded, but 16/763 source contracts failed. Updated the original legacy contracts for V5 multi-cycle DB, unified lab, new R0, twelve-sheet workbook and source-backed MQ7/MQ9 as well as copied the V5 build preparer to Gradle fixture location. Re-run tests and diagnostic compilation. Never represent a failed test suite as an accepted release.

## Retry 5 — final 763-case JUnit verification
The previous run completed 763 unit tests with only one remaining stale expectation for literal min_app_version in a dynamic V5 generator; fixed to check the bound-template loop. Re-run to confirm all actual scientific/source contracts before presenting diagnostic APK as test-pass (still not real-hardware validated).
