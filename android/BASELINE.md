# HazRob Tank Android — Baseline 1 (pre-modification)

**Status:** immutable upstream reference, **not yet a copied Android source tree or APK binary**.

- Application: HazRob Android 0.10.2 / HR069-R1 / RC2 (diagnostic working build, unapproved).
- Source repository: `sohpany/SERVOC-Android`
- Exact source commit: `6281d3eb7c1c961ce63b0b5e4ae0792874626720`
- Upstream branch for reference: `release/hazrob-0.10.2-rc2` (branch HEAD may change).
- User's installed APK filename: `HazRob-0.10.2-HR069-R1-RC2-WORKING-359-6281d3eb7c1c961ce63b0b5e4ae0792874626720.apk`
- This is **Baseline 1** for Tank app adaptations. Do not substitute HR067/HR068 or 0.10.3.
- Preserve original Android package and analytical modules before introducing Tank-specific motor, camera, telemetry or API changes.
- Do not assume the installed APK's bytes equal any separate CI build: verify hash or inspect uploaded APK first.

## Import status

The pinned reference and provenance have been recorded here. The actual 526+ Android app files, supporting Gradle/branding/chemcore/reference resources, and the APK binary **have not been imported**. GitHub's cross-repository source copy is still pending. No claim is made that Tank now builds an Android APK.

## Required next step

Copy the exact upstream commit's full Android build source and required modules into a Tank Android subdirectory, preserving provenance, then build/test against Tank firmware. Avoid overwriting the firmware directories.
