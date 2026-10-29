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

## 2026-10-29 — V6-002 additional Pending scientific tools review

- Confirmed the **62** `ADOPTED / PORT PENDING` records are `MQDataScience 6.0.0` model ports absent from Android, not missing Tank-80 research source code.
- Source: pinned SERVOC `ModelRuntimeRegistry`, `MqDataScience76Catalog`, `Mq76ModelRoleRegistry`, `ExperimentsTab`, `CalibrationRegressionModels` and `tools/hazrob_mq76_upstream.py`.
- Verified structural `HazRob/HazRob-Tank:main/research/techniques`: 80 directories, 80 README, research scripts and tests; `PROGRESS_80.json` historical G1 1,261 software tests, G2 49/29/2, Tank G3 0/80.
- Confirmed central `HazRob/build-harness` 80 catalog entries cover curated sources only; `LEGACY_COLLECTIONS.md` explicitly does not claim all old source files were individually indexed; 15 external entries have raw files pending, 24 analytic studies marked not imported.
- Documentation: `docs/research/V6-002-CHEMCORE-MANUFACTURER-R0-PPM-FINGERPRINT-GAP-AUDIT-2026-10-29.md`, section 10, commit `6fa4f9a6a2ef1b165fea1bc31ce0c646848f5606`.
- No Android port, tests, APK or firmware changed in this Pending investigation.

## 2026-10-29 — V6-002 scientific development plan and traceability artifacts

- **Produced:** master plan `docs/implementation/V6-CHEMCORE-SCIENTIFIC-MASTER-PLAN-2026-10-29_AR.md` (architecture AS-IS/TO-BE; source R0 physics; simulation SIM-A/SIM-B; dataset groups; G3 policy; Android integration and rollback), source sampling protocol and machine-readable execution tracker P00–P11.
- **Source-pinned matrices:** 80 real research technique IDs and their recorded G1/G2/G3 statuses, 76 MQDataScience model names and recorded native/pending state, 80 curated bibliography entries and their raw-state. GitHub files `docs/research/V6-CHEMCORE-TECHNIQUE-80-TRACEABILITY.csv`, `docs/research/V6-CHEMCORE-MQDATASCIENCE-76-RUNTIME-MATRIX.csv`, `docs/research/V6-CHEMCORE-CENTRAL-REFERENCE-80-PROVENANCE.csv`.
- **Simulation:** `docs/research/V6-CHEMCORE-SIMULATION-SCENARIOS.csv` — **24 planned** scientific/engineering stress cases including a required separate real-device G3 entry.
- **External source verification:** UCI1081, UCI270, UCI308, UCI309 official descriptions and ISO 6145-1:2019, ISO 6143:2025, ISO/IEC 17025:2017, JCGM 100:2008 pages checked; UCI/MQ raw experiments not downloaded or re-run in this planning task.
- **Executed tests:** format/readback of GitHub source content; no new numerical simulation, model training, calibration or Android device test.
- **State:** `PLAN_PREPARED; ALL_PHASES_NOT_STARTED; G3_DEVICE_VALIDATION=0/80; V6_APK_NOT_BUILT`.
- **Next controlled implementation:** baseline/versions and regression tests (P00), source matrices/eligibility (P01), and math-only simulation (P04) while missing hardware parameters are tracked as blockers.

## 2026-10-29 — V6-002 source expansion: MQUnifiedsensor R0 + legacy curves and fingerprints

- **Change request:** treat models used for local R0 calibration plus all usable source curves and fingerprints as **mandatory ChemCore Tank V6 scientific inputs**, not an optional postrelease enhancement.
- **Read / inspected:** MQUnifiedsensor `calibrate`, `readSensor`, `readSensorR0Rs`; central source cross-study and peer R0 JSON; organization `HazRob/build-harness/library/research/hr069/current-manifest.json` with 14 byte-identical legacy scientific assets; 43 source curves, 13 derived prior fingerprint families, 59 food-headspace sessions, 4 MQ odor classes from 6400 ADC rows, 18 MQ135 gas/concentration summaries from UCI1081.
- **Finding:** `MQUnifiedsensor` uses dimensionless `RatioCleanAir` in `Rs_air/R0`; its `readSensorR0Rs` inverts the ratio, requiring a `ratio_direction` field. A 2018 R0 paper has MQ7/MQ8 calculation discrepancies; 6 MQ3 curves require a source-provenance interpretation of `output_scale=50`. None is a portable device-R0 reference.
- **Commits:** `c54da0d2132bbc0e1409c6f96cdd8952680a573b` 43 curves; `59e44fc926f7fd9a8be2dc8d8dc12c4f1f203adc` 14 assets; `ca63e5b8e91484005eff3f15e1167d35573377d0` 8-R0 matrix; `f11d365cc126001330125c332dfab5ed50d6ecdd` scientific addendum; `f1f1c3c3bc0ea591825fb45e752725fceafe2575` seven new simulation scenarios. Master plan/sampling protocol/tracker updated.
- **State:** `P01=IN_PROGRESS_SOURCE_METADATA_ONLY / SOURCE_RAW_PARITY_PENDING / SIMULATION=NOT_RUN / V6_CHEMCORE_CODE=NOT_CHANGED / G3=0_OF_80`. No firmware or Android source modified for this expansion. No release/certified ppm claim.
- **Next:** audit sources individually to resolve duplicate source/curve relationships, then implement `R0CurveCompatibilityGate`, run source-crossing SIM-R cases, and test group-safe researcher data.
