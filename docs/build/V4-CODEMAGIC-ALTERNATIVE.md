# HazRob Tank V4 — Codemagic alternative to Railway/GitHub runners

Date: 2026-10-24. Status: **CONFIGURED IN REPO; EXTERNAL CONNECTION/BUILD NOT EXECUTED**.

## Why this alternative
The observed GitHub Actions run `40052873540` failed within seconds with **zero job steps**; its job logs endpoint returned `BlobNotFound`. This is not a Gradle compilation failure. Railway had not provided a functioning APK. Container execution here has Java 21 but **no Android SDK or Gradle distribution**, and its network cannot resolve Google/Gradle download hosts. There is no valid V4 APK generated from those attempts.

Codemagic directly supports native Android builds and downloadable APK artifacts. Bitrise's `build_apk` workflow is a secondary provider.

## Prepared repository assets
- [`codemagic.yaml`](../../codemagic.yaml): **tank-v4-diagnostic-apk** workflow, Java 17, macOS M2 worker, debug artifacts and test reports.
- [`tools/build_v4_codemagic.sh`](../../tools/build_v4_codemagic.sh): actual deterministic build with read-only source checkout pinned to `6281d3eb7c1c961ce63b0b5e4ae0792874626720`, overlay from the **Tank V4** branch, independent `com.hazrob.tank` app ID, Gradle 9.5.0 + AGP 9.3.0, Android SDK 36, unit tests, `:app:assembleDebug`, APK signature verification, embedded ChemCore Stable 652 records, and SHA256 checksum artifact.
- Emits `dist-v4/HazRob-Tank-v4-UNRELEASED-debug.apk` **only if all commands succeed**. This is a developer debug build, **not** a release-signed or robot-tested APK.

## One-time external account connection (user action, not completed)
1. Open https://codemagic.io/ and authorize GitHub access to private organization repository `HazRob/HazRob-Tank`. Ensure the app can see **that repo**; do **not** modify the original source repository.
2. Add application using **existing repository** `HazRob/HazRob-Tank`, choose branch `feature/tank-v4-implementation`, and choose configuration **codemagic.yaml**.
3. In the app's environment variables create group **`hazrob_source`** and a **Secret** `HAZROB_SOURCE_READ_TOKEN` with read-only access to **`sohpany/SERVOC-Android`**. Do not paste this token in commits, chat, URLs, or logs.
4. Run workflow **`tank-v4-diagnostic-apk`**. Download the APK from **Artifacts** on success; inspect test reports on failure. A saved file is a valid APK only after successful Gradle, signature and ChemCore-content checks.
5. Signing for app updates to installed V3 requires a **persistent Tank signing key**, not the ephemeral Gradle debug key. Real-phone Sessions tab, R0 across eight MQ channels, Echo distances and live TWA/LFL warning behavior remain separate release gates.

## Diagnostics
- Source repo permission failure: inspect `HAZROB_SOURCE_READ_TOKEN`; token must read original **sohpany/SERVOC-Android** and not grant write permission.
- Gradle 9.5 checksum mismatch or SDK missing: stop rather than bypassing integrity checks.
- JVM compile/test failure: inspect the **first compiler/test error**, patch only `HazRob/HazRob-Tank` source overlay and rerun.
- No artifacts: build failed; never publish an old APK under the V4 name.
- Original HazRob `sohpany/SERVOC-Android` is a pinned **read-only dependency**; it is never edited or pushed.

Official guidance: https://docs.codemagic.io/yaml-quick-start/building-a-native-android-app/ and https://docs.codemagic.io/yaml-basic-configuration/configuring-environment-variables/ .
