# HazRob Tank V4 — reproducible diagnostic build evidence

Date: 2026-10-27 UTC. Scope: independent Tank V4 Android APK and ESP32/CAM firmware packages.

## What actually passed
- Android source SHA `1f1ea7d84b30c26b5825299e39ab53ad9c90a836`.
- [Android Actions run 40552448040](https://github.com/HazRob/HazRob-Tank/actions/runs/40552448040): **SUCCESS**. Includes pinned original Android checkout, Tank overlays, Java test compilation, `:app:testDebugUnitTest`, `:app:assembleDebug`, APK signature verification, offline embedded ChemCore 4.0 compatibility, SHA256 verification and uploaded debug APK.
- The prior diagnostic run [40552169227](https://github.com/HazRob/HazRob-Tank/actions/runs/40552169227) reached 711 JVM tests with one failed stale RC2 staging-name test. The source contract was corrected in `d018a4d`; the final run returned **BUILD SUCCESSFUL** for both Gradle commands without skipping meaningful tests. The precise number of tests in the final run was not separately exported here; do not claim hardware/device coverage.
- Main ESP32 firmware build source SHA `a7e1ceb7879aab64f9e5fd2d26bbd87d15d68a80`, [Actions 40550815552](https://github.com/HazRob/HazRob-Tank/actions/runs/40550815552): **SUCCESS**, Arduino-ESP32 `2.0.17`, Arduino source ZIP and compiled BIN.
- ESP32-CAM build source SHA `1d11948a4fd534bc5e20d6c268ae4ba1b2391160`, [Actions 40552291396](https://github.com/HazRob/HazRob-Tank/actions/runs/40552291396): **SUCCESS**, AI Thinker ESP32-CAM, Arduino-ESP32 `3.3.11`; reused unchanged camera source from V3. Separate source ZIP and compiled binaries generated.

## Verified exported direct files (conversation attachments)
| Output | SHA256 | Evidence |
| --- | --- | --- |
| `HazRob-Tank-V4-UNRELEASED-debug.apk` | `a5115ef74efef5ce3a447230c774a3ce354e38c78459feaa3f211af133ab2b82` | Original Actions SHA256SUMS comparison and ZIP integrity |
| `HazRob-Tank-V4-ESP32-Arduino.zip` | `86340d9fe586aaca5a4d28f2d710657e51f3fcdfa026e50b4e33dfe8ccdd71d5` | Source ZIP CRC validation, 19 files |
| `HazRob-Tank-V4-ESP32-CAM-Arduino.zip` | `125d365eb10f01d08e9cfb3a953377903cd4c34be929692bc3fad6d8034ba339` | Original Actions SHA256SUMS and ZIP CRC, 7 source files |
| `HazRob-Tank-V4-Firmware-Complete.zip` | `534ea3957045377f6628bf58c2d77b6292702d9ff5cd406f7332c90984d76ea6` | Repacked both source trees and CI-produced binaries; each archive member SHA checked |

The direct files were extracted from verified CI artifacts and repackaged outside GitHub; they are attached to the ongoing ChatGPT conversation and not guaranteed to exist in the GitHub repository.

## ChemCore evidence
- APK embedded manifest: library `1.0.0-hr069r1`, `652` chemical identities, `0` accepted M4 calibration records.
- Manifest constrains Android app version to `4.0`; embedded payload bytes and SHA-256 match the manifest, verified by CI and again after downloading APK.
- This is research-grade screening data, **not** Tank reference-gas ppm certification.

## Release, signing and hardware boundaries
- Workflow printed `V4 compiled but persistent signing secrets are not configured. NO signed release candidate.`; persistent-signing steps were **skipped**. The user-facing APK is **debug-signed only**. Never describe it as a signed upgrade-stable production release.
- No on-device Android installation was conducted here; no fresh/migrated DB UI test, real PDF/XLSX export/open test, real-source GitHub developer upload, camera-streaming validation, R0 physical calibration, NVS power-loss interruption, safety interlock/E-STOP, Echo/IR measurement or servo tests occurred.
- Do not uninstall old app until user exports/backups sites, sessions, calibration and notes. PackageId must remain `com.hazrob.tank`; debug signatures may differ across builds.
- V4 has **BUILD_PASS / APK_CANDIDATE_AVAILABLE** but **RELEASE_READY=false, DEVICE_PASS=false, PHYSICAL_SAFETY_PASS=false, CERTIFIED_PPM=false**.

## Next release qualification
1. Configure and protect a persistent Tank-only signing keystore if an in-place-upgrade stable release is intended.
2. Test installed APK on Android including five developer tabs, Sessions fresh/legacy, SAF PDF/XLSX reopen, offline Stable activation and network failure UX.
3. Test firmware on safely restrained hardware: direct/remote modes, R0 300+60s / >=10 counts each, NVS interruption, BME, MQ heater, Echo/IR and 500ms neutral stop/E-STOP.
4. Only then change release labels; preserve all prior failed-run records.
