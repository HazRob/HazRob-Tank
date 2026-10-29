# HazRob Tank V6 — chronological progress log

**Branch:** `feature/tank-v6-implementation`
**Established:** 2026-10-29
**Frozen parent baseline:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`

## 2026-10-29 — V6 branch initialization

- **Action:** Created `feature/tank-v6-implementation` directly from `feature/tank-v5-implementation`.
- **Verification:** GitHub compare returned `identical`; `ahead_by=0`, `behind_by=0`, `total_commits=0` before V6 documentation.
- **Baseline SHA:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b` — V5 ESP32 API4/Direct compatibility source fix.
- **Functional changes:** NONE. No APK versionCode/versionName, firmware, database, Stable, signing or Android source modified during branch creation.
- **Tests executed for V6:** GitHub branch/compare metadata only. No JVM, Gradle, firmware compile, Android device test or robot physical test.
- **Release state:** `V6_NOT_BUILT / NO_RELEASE_APPROVAL`.
- **Next:** register the owner's V6 change requests, triage connectivity issue if raised as a V6 item, design tests, then implement in isolated commits.

## 2026-10-29 — V6-001 Direct admission patch

- **Owner request:** no app/firmware authentication or matching release versions; reported screen shows direct AP `192.168.4.1` responds `ESP32-Tank 0.1.4-v4-dev` but entry is denied.
- **Confirmed source defect:** ConnectionGate invoked the baseline `LocalNetworkContract.isExpectedFirmwareBanner(banner, ReleaseAudit.VERSION)` which requires `HazRob 5.0`; it cannot recognize `ESP32-Tank 0.1.4-v4-dev`.
- **Source patch:** commit `0d4e9ce245c95274e9e155eb393e26a42b18054f`; replace release comparison with `TankFirmwareCompatibility.looksLikeTank(banner)`, stop sending Direct mode change when `/remote/status` is unreadable, remove stale hard-coded V3 login footer, update failure instructions.
- **JVM test:** `TankV6DirectVersionIndependentEntryTest.java` added; specifically checks V4/V5/V6 firmware banners, non-Tank rejection and app-side source admission policy.
- **Assurance:** code review/source inspection only; `JUNIT=NOT_RUN / GRADLE=NOT_RUN / APK=NOT_BUILT / ESP32_DEVICE=NOT_TESTED`. No firmware changes or flashing, and no new release authorization.
- **Next:** device replay with physical firmware V4, then confirm read-only telemetry and E-STOP/watchdog. Keep controller protocol and safety checks separate from release labels.

## 2026-10-29 — V6-002 ChemCore scientific and source audit

- **Request:** compare all accessible manufacturers, MQUnifiedsensor R0 method, MQDataScience, scientific libraries/reference bank, gas curves, fingerprints, logarithms and PPM with inherited Tank ChemCore.
- **Source inputs inspected:** Tank V5 inherited `Hardware.cpp` and `Config.h`; `AcceptedR0Normalizer`, `AcceptedReferenceResponseModel`, `TankStableScreeningModelRegistry`, `TankMultiSourceResponseModel`, `InverseMolecularSolver`, `TankInstantAnalyzer`, `TankFingerprintScreening`, legacy `ChemCoreInferenceEngine` and uncertainty/fingerprint/mixture/open-set rules; organizational central reference bank and R0 peer comparison; public Winsen, MQSensorsLib, MQDataScience, peer-reviewed reviews and UCI.
- **Report:** `docs/research/V6-002-CHEMCORE-MANUFACTURER-R0-PPM-FINGERPRINT-GAP-AUDIT-2026-10-29.md` — tables G01–G13 and proposed tests T01–T12.
- **Key findings:** current engine already joint-inverts and marks research ppm; priority constraints are physical per-board RL/R0/VC, exact MQ7/MQ9 heater phases, chemical label specificity, source-to-curve comparability, statistical identification and independent reference-gas accuracy.
- **Code or firmware edits:** NONE for V6-002. **Verification:** source/literature review; no runtime/JUnit/Android/robot experiment executed; 80 entries are reference catalog, Tank G3 remains not validated.
- **Next:** choose controlled implementation changes, build a numeric model/source audit with pinned refs and physical device measurements, then test against explicit grouped holdouts.
