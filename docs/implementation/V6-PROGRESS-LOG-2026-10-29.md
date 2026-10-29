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

## 2026-10-29 — ChemCore V6-002 first execution batch, verified

- **Historical assets** 14/14 SHA-equivalent original HR069 files copied read-only as Tank-local research inputs; three R0 paper datasets copied with matching Git blob source hash.
- **Code written** `source_audit.py` 43 source curves × 9 roundtrip, 172 synthetic R0 variations, 14 hash checks, 4-source-class fingerprint mask; `peer_r0_audit.py` confirms original-paper MQ7/MQ8 arithmetic errors and never substitutes R0 physical Tank.
- **External actual data** UCI1081 original zip SHA `813ebb7d00ed9172114b68d27ab3210d6c220b8b23907dce91e9838cd48ab`: 90 physically recorded source exposures (NOT Tank), 6 gases, 3 ppb concentration levels, 5 reps; local grouped concentration holdout with 4 model/channel comparisons. Measured balanced accuracies in `docs/research/V6-CHEMCORE-UCI1081-REAL-DATA-PILOT-2026-10-29_AR.md`; no G3 and no independent device/day validation.
- **Android-facing source contract** `TankV6SourceCompatibility.java` created, enhanced fail-closed M4 policy. Source exists but not active runtime integration.
- **CI** [40912404109](https://github.com/HazRob/HazRob-Tank/actions/runs/40912404109), head `42732b2cd51b3e1d0567cda686d0291f8b75a909`: **30 Python tests PASS + 31 Java assertions PASS**, 387/387 numerical model self-check, 172 R0 counterfactuals, 94 out-of-domain rejected. Artifact `12914686568`.
- **Durable resume point** `docs/implementation/V6-CHEMCORE-IMPLEMENTATION-CHECKPOINT-2026-10-29.md` and JSON execution tracker (P00,P01,P02,P03,P04,P05 partial; P06–P11 not started). **No V6 APK built, no field firmware changed, no G3.**
- **Next work** read-only V5-replay and P03 schema/data migration, then P04 additional SIM-B, P05 UCI270/308/309 real raw sampling, followed by P06 engine integration.

## 2026-10-29 — V6-002 executable implementation checkpoint after user status request

- **GitHub CI run:** `40912404109`, **success**, head commit `42732b2cd51b3e1d0567cda686d0291f8b75a909`.
- **Completed before this status check:** 14/14 pinned HR069 snapshots verified; 3 pinned peer R0 studies; 30/30 Python source/calibration tests passed; Java/JDK `TankV6SourceCompatibility` contract compiled and its standalone tests executed successfully; 387 self-consistent curve roundtrips; 172 synthetic R0 counterfactuals (94 out-of-domain); external UCI1081 90-exposure research pilot documented.
- **V6 source:** `android/app/src/main/java/com/servoc/app/chemcore/TankV6SourceCompatibility.java` rejects incompatible R0 definitions, invalid sensor/source domain, and reportable M4 absent independently verified G3 evidence. **Not yet wired into Android production inference.**
- **Research continuity:** detailed checkpoint `docs/implementation/V6-CHEMCORE-IMPLEMENTATION-CHECKPOINT-2026-10-29.md`, machine-readable `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json`.
- **Not done:** physical RL/ADC/heater verification; remaining multiple-source benchmark program; core fusion upgrade; application-wide Gradle test/APK; actual Tank G3. No unattended background execution.

## 2026-10-29 — V6-002 continuation, checkpoint 02 (executed)

- **Engineering code commit:** `0eb56670339d93057c27ec3a96f1e8affe39c76c`, `android/app/src/main/java/com/servoc/app/chemcore/TankV6EvidenceConsensus.java`: Research-only independence-aware consensus; one model contribution per physical sensor, root-lineage counts, source conflicts, abstention if unqualified, no probability/M4/ppm claims. JDK tests `50f70c124f1c03c01f303dba26e61b0539026a77` (33 assertions).
- **G2 pinned history records:** UCI270, UCI308, UCI309 and trained-model summary copied byte-identically from Tank research main to `research/chemcore_v6/source_snapshots/g2_research/` with original Git blob pins. These are not new raw replays. `research/chemcore_v6/g2_portfolio_audit.py` and eight Python tests verify counts, reuse, unit semantics and G3=0. Historical 14,238 distinct exposures in four external UCI sources; UCI224 duplicates UCI270.
- **Defect found by CI:** Archived UCI309 `scientific_Tank_G3_validated` is integer `0` not boolean `false`; strict scientific no-promotion policy now accepts only explicit numeric zero/boolean false. Fix `70bfaf86ee787cce5c1f738e08265a3996dfb26a` after failing CI `40916199674` and `40916243379`; successful `40916327597`.
- **SIM-B engineering:** `research/chemcore_v6/temporal_simulation.py` commit `c5a05072f84a72e756456a885e16374f08173bd7`, 8 deterministic synthetic stress scenarios, 12 Python unit tests `6a9671703a19b13bab91726d1013fcd18ad68b55`. All coefficients **assumed**, no Tank exposures or gas phase ppm claims.
- **CI:** `40916864090`, head `3f81daa2913c3c679e1c8327d6fd188b183ca1f7`, **success**: 50/50 Python unit cases, 31 JVM compatibility assertions + 33 JVM evidence-consensus assertions, 387 curve numerical roundtrips, 172 synthetic R0 cases, eight SIM-B scenario outputs and G2 history audit. Machine evidence artifact `12915184303`. `Tank_G3=0/80`.
- **Checkpoint:** `docs/implementation/V6-CHEMCORE-CHECKPOINT-02-2026-10-29_AR.md` and `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json` updated.
- **Not executed/complete:** V6 Android active runtime/Gradle/APK, experiments on physical robot, new UCI270/308/309 raw files/benchmark, P06/P07 selection/validation, field G3 promotion. Do not promote P08 prototype to production decision from these tests.

## 2026-10-29 — V6-002 Checkpoint 03: source curve Java numerical engine

- **Implemented:** `TankV6CurveKernel.java` in `android/app/src/main/java/com/servoc/app/chemcore/`, commit `85ee3934b620dc58a737a485316d3ae17d74699d`: source-pinned power-law forward/inverse function with source ppm bounds, `outputScale`, explicit R0 definition and ratio direction through `TankV6SourceCompatibility`. All outcomes research-only, never reportable ppm or G3 by a model flag.
- **Test code:** `research/chemcore_v6/tests/java/TankV6CurveKernelTest.java` commit `ec67ee5283dadce154bc535ebb374cfed642e940`: 15 JDK assertions including MQ3 50× source-axis algebra, cross-device denial, domain and invalid R0/concentration checks. Test coefficient fixture is synthetic, not a manufacturer's physical calibration.
- **CI:** [run 40917593589](https://github.com/HazRob/HazRob-Tank/actions/runs/40917593589), head `1e52e2f7605b2ef7de5555ffea78ca402ed101cc`, **success**: 50 Python tests, 31 R0 compatibility + 33 consensus +15 bounded curve JVM assertions = 79 Java assertions. Research ZIP artifact `12916292465`.
- **Permanent checkpoint:** `docs/implementation/V6-CHEMCORE-CHECKPOINT-03-2026-10-29_AR.md`; phase tracker `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json` updated. `P06=SOURCE_CURVE_JAVA_MATH_JDK_TESTED_NOT_STABLE_INTEGRATED`.
- **Untested:** Android-specific Gradle/APK, real device sensors RL/Vc/heater, actual source-curve applicability, full Stable registry and P08/P09 runtime integration. **G3 Tank=0/80**.

## 2026-10-29 — V6-003 user-approved DEFER and cleanup

- **Owner instruction:** defer startup transient MQ and Bosch scan technique until after the core ChemCore Tank V6 scientific plan is completed. **Phone-side storage** remains a requirement should the idea be reopened; no ESP32 buffering or added request rate.
- **Immediate rollback:** `MainActivity.java` restored byte-identical to pre-experiment blob `e1885a3b806c6b375b19e129880a3ef126ce5b68` (commit `2e948b57c454f193ab9b4fa39de55c4c5162ec21`); scientific CI restored byte-identical to prior blob `3ea9ac7ca1aa308df4def0283307d88608073fc2` (commit `ba70fc50a0cb72b9c6a5aea4bb272742ba5077b8`).
- **Discarded deferred implementation:** `TankV6StartupWindow.java`, `PhoneMqStartupRecorder.java`, Java startup unit test, Python startup feature prototype and Python startup unit test. `V6-003` source study retained in `docs/research/`, marked `DEFERRED_BY_OWNER`.
- **Effect:** no phone-side first-minute recording is active, no new ESP32 request/flash/SPI/heap pressure; baseline V5 telemetry, R0 and normal analytics preserved.
- **Main work resumed:** P06 `SourceCurveRegistryV6`, P03 provenance mapping, P08 validation; G3 remains `0/80`.

## 2026-10-29 — Open-source research policy and P06/P08 source registry implementation

- **Owner requested:** use all available numerically usable observations, curves and fingerprints; no restrictive scientific provenance/R0/heater/domain/margin gates that hide research calculations. Exception: corrupt/nonfinite/undefined numerical operations. Keep uncertainty annotations and do not mislabel research estimates as physical calibration.
- **43 original curves:** `research/chemcore_v6/manifests/source_curve_registry_43.json` revised from HOLD to `USABLE_RESEARCH_WITH_CAVEATS` for all 43, commit `4ba4e691cae421304299c498e605aa2e2203eed3`. Source parameters unchanged.
- **Python source registry audit:** `research/chemcore_v6/curve_registry_audit.py`, commit `c5091c4c0338354e1e29dfa5725e11628d9796df`; provenance remains pinned while incomplete source details are advisory. Seven unit regressions added in `test_curve_registry_audit.py`, commit `c1092a4dc541e1574205067e127a5f5575417fd9`.
- **Android V6 numerical research inventory:** `SourceCurveRegistryV6.java`, commit `e5e576b93b9bbde6c24bdbe6dc31d619c2c7db52`. All 43 model coefficients exposed in `all()`, `forSensor()`, and `explore()` with `ppmResearch` and `risk_flags`, never a fabricated calibration. Java test `SourceCurveRegistryV6Test.java` commit `84b6f1d24ba0c401960a132f71232a38692442d5`.
- **Evidence consensus:** `TankV6EvidenceConsensus.java` commit `7b6f7355174b391ba079240da3d794a46f22fcae` now retains every valid research observation. Missing source IDs are grouped as `source_unattributed`, out-of-domain and source conflict are included as explanatory flags, not dropped. JDK test updated `1417beecfbcd9ec9a08b2b884f06ab22791d34f4`. Initial CI before updated tests failed as expected; new tests run passed.
- **CI:** `40923146562`, head `1417beecfbcd9ec9a08b2b884f06ab22791d34f4`, **success** with **57 Python unit cases and 414 JVM assertions** (R0=31, consensus=40, math=15, registry=328). Artifact `12919626294`.
- **Scientific classification:** numeric estimates remain research estimates; physical G3=0/80, V6 APK not built, V5 production active inference unchanged.
- **Resumption:** `docs/research/V6-002-INCLUSIVE-ANALYTICS-POLICY-2026-10-29_AR.md` and machine tracker, then P03/P06 Android research integration and source data experiments.

## 2026-10-29 — Clean-slate architecture approved: new ChemCore Tank V6, not an extension

- **Owner correction:** ChemCore Tank on app V6 is a completely **new** analytical engine. Previous engine logic, source code, decisions or result-envelope semantics are no longer the architecture baseline. Scientific curves and experimental research datasets are still available as independent literature evidence.
- **Reverted experimental attachment in existing Android analyzer:** `TankInstantAnalyzer.java` restored exactly to pre-experiment Git blob `a24b8e7e433e24565adb400cc3f605cfd02ac3df`, code commit `8ed61a4bec10b543208185e743e964be8fd1de08`.
- **New package / independent inference:** `com.hazrob.tank.chemcore.v6`.
  - `SourceCatalog.java` commit `aa74ce279b1fd5e4d77cdfc73ec039ae9cc3eaa1`, all 43 MQ curve coefficients pinned to source blob `bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1`.
  - `Engine.java` commit `45262cb04d3ebecd6595027befc66a707adca17e`, then raw Rs preservation `4d242cd5004167959c823d367e2d351fba921d80` (absent R0 no longer drops valid raw Rs).
  - New regression tests `NewChemCoreTankV6EngineTest.java` and `test_independent_v6_catalog_parity.py`; no source imports from earlier ChemCore packages, no Android dependency for the pure kernel.
- **Engine semantics:** converts eligible Rs and R0 into source-specific log-ratio, computes all mathematically valid source inversions and multi-sensor least-squares candidates. All output rankings/fit PPM are **research estimates**. Missing R0 results in retained raw observation, no fabricated ppm. No arbitrary source/heater/threshold exclusions. Synthetic CO example at 200 ppm is algebraic fixture, not real gas calibration.
- **JDK CI:** [run 40925694019](https://github.com/HazRob/HazRob-Tank/actions/runs/40925694019) **success**, 59 Java assertions and 3 Python parity/independence tests.
- **Android full Java compilation:** [run 40925657334](https://github.com/HazRob/HazRob-Tank/actions/runs/40925657334) **success**, source-compatible with frozen Tank Android overlay. This is not full UI/app/physical test and did not create an APK.
- **Architecture ADR authoritative:** `docs/architecture/CHEMCORE-TANK-V6-CLEAN-SLATE-ADR-2026-10-29_AR.md` (commit `ac772ef040ebfc823a738c4cd87a47001edf56db`), the master plan has been amended to supersede earlier attempts to attach V6 as a supplementary output.
- **Resumption:** `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json` contains V6-N01..N07 independent milestones. Proceed with V6-N02 literature/fingerprint/baseline sources and V6-N03 analytical technique adapters; no automatic re-activation of startup-heater technique; no previous inference engine coupling.
- **Remaining:** new V6 Android input adapter, view/reports/session integration, cross-source validation and actual Tank G3 exposures. `APK_NOT_BUILT; G3=0/80`.

## 2026-10-29 — Owner clarification: food freshness is NOT part of gas ChemCore

- **Binding decision:** food freshness analysis and its curves/fingerprints are an independent Android feature. Both may read the robot's raw sensor telemetry; no food-model ingestion into `com.hazrob.tank.chemcore.v6`, no mixing with gas candidate ranking or gas ppm.
- **Moved byte-identical reference asset (Git blob `959aae7574307a0030d218d4b86eb6618a6f72b0`)** from `android/app/src/main/assets/chemcore_v6/reference/` into `android/app/src/main/assets/food_freshness/reference/`. Likewise moved the archival copy from `research/chemcore_v6/source_snapshots/hr069/` into `research/food_freshness/source_snapshots/`; verified exact Git blob SHA before deleting old paths.
- **Gas source manifest and tests:** removed food entry from `research/chemcore_v6/source_manifest.json` (now **13 gas archival files**, original 14 across both modules). Removed food parsing/counters from `research/chemcore_v6/source_audit.py`; updated source audit regression tests and scientific CI count to 13.
- **Food reference manifest:** `research/food_freshness/source_manifest.json` preserves DOI `10.5281/zenodo.17285312`, **59 eight-channel food headspace profiles**, with `quantitative_ppm=false`.
- **Boundary CI:** `research/food_freshness/test_food_isolation.py`, `.github/workflows/food-freshness-isolation-ci.yml`. [Food module isolation run 40928648698](https://github.com/HazRob/HazRob-Tank/actions/runs/40928648698) **success (5 tests)**. [Gas source verification run 40928767861](https://github.com/HazRob/HazRob-Tank/actions/runs/40928767861) **success** with 13 gas-only source copies. [Independent ChemCore engine run 40928967267](https://github.com/HazRob/HazRob-Tank/actions/runs/40928967267) **success**; workflow includes six additional MQ135 equations in Java compilation.
- **Documented architectural rule:** `docs/architecture/FOOD-FRESHNESS-MODULE-BOUNDARY-2026-10-29_AR.md`. The separate food UI, food analyzer and report generator remain **pending implementation**; isolation and source placement are complete.

## 2026-10-29 — V6 Checkpoint 05: independent multi-source gas ChemCore execution

- **Direction:** continue new standalone Tank ChemCore V6 (NOT an extension of any previous ChemCore). Food freshness remains isolated; MQ startup/heater transient feature remains deferred.
- **24 gas-only numeric fingerprints implemented:** `FingerprintEngineV6.java` commit `ba648638ec13d4911c807dd6f61ea91e078fbc00`; uses `GasFingerprintCatalogV6.java` across three mathematically separate spaces (4 ADC class patterns / 2 RoboFusion operational patterns / 18 UCI1081 MQ135 50,100,200 ppb temporal research patterns). No fabricated MQ4/MQ9 input or automatic conversion of Rs to source ADC; raw input scale mismatch carried as advisory.
- **Independent evidence coordinator:** `ChemCoreTankV6.java` commit `ce96500a11e2b114a2bb4dbd8b7132201a133824`; maintains all 49 curve estimates, all submitted source-fingerprint hypotheses and a source-root-unique ledger by chemical reference ID. No conflation of fingerprint similarity with ppm or a probability. Operational class labels do not assert molecular identity.
- **Source parity:** `test_gas_fingerprint_reference_parity.py` fixed repo-root bug commit `e6ebd287477c92e6c8d69ececcca6102b3eca77e`; verifies all 24 numeric signatures against pinned original JSON and no food/predecessor engine imports.
- **Android-only input boundary:** `TankV6TelemetryAdapter.java` commit `91bc3792404f9c3c6ad5450c05b2fd7b669af995`: consumes existing snapshot and outputs new V6 research JSON; retains positive raw Rs even without R0; does not produce UCI temporal features from a one-shot reading or contact ESP32.
- **Four native signal algorithms:** `SignalFeaturesV6.java` commit `ca439d320985d785cd15585b65f709e4ce49bbc0`: trailing median, log-geometric normalization, mean/median/std and sample-time slope/AUC. Its JDK test was corrected and passed, commit `f92404f870d26958e96c3be20e3591b4e7038759`. No startup warmup sampling.
- **CI evidence:** [independent engine 40935647665](https://github.com/HazRob/HazRob-Tank/actions/runs/40935647665) **success**: Java assertions 60 response +1245 fingerprint +55 coordinator +30 signals = **1390**, and 8 Python source/independence tests. [scientific CI 40934854637](https://github.com/HazRob/HazRob-Tank/actions/runs/40934854637) **66 Python tests PASS**. Android Java compilation [40935139045](https://github.com/HazRob/HazRob-Tank/actions/runs/40935139045) (new V6 adapter) and [40935496430](https://github.com/HazRob/HazRob-Tank/actions/runs/40935496430) (signal module) **success**. Neither is an APK nor phone/device run.
- **Full 80 coverage:** `docs/implementation/V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json` generated by comparison to the actual 80-entry scientific list: **14 partially implemented in NEW V6**; **66 not yet ported**; no physically validated techniques. Prior old app G1/G2 tests do not count as current V6 port.
- **Permanent resumption document:** `docs/implementation/V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-05-2026-10-29_AR.md`; main tracker has been updated. **New ChemCore app interface and laboratory/report switch-over, signed APK V6, physical MQ gas benchmarks and Tank G3=0/80 all still pending.**

## 2026-10-29 — V6 checkpoint 06: 10 more scientific technique ports and corrected UCI1081 source layout

- **Owner instruction:** continue developing the completely new independent ChemCore Tank V6, preserving all mathematically usable gas curves and fingerprints. Food remains its own completely separate algorithm, references and UI. Postpone MQ heater-start transient feature.
- **10 new research-stage technique operations** implemented in `MeasurementDiagnosticsV6.java` (commit `cb19ff7db1d0070d93155b256e656147b36c6df3`) and `ResearchDiagnosticsV6.java` (commit `a82234b6ac738933d6dc2a135c45fa1cbedc1132`): `01.1` ADC-to-Rs; `01.2` R0 clean-air source ratio; `01.6` signal quality; `01.7` drift; `02.6` 4D context; `02.7` variance-ranked features; `03.3` pairwise class overlap; `03.5` leave-one-feature-out importance; `03.7` UCI concentration confounding; `03.8` per-physical-sensor identifiability. Reports all finite source-comparable results without score threshold censorship.
- **JDK tests:** `V6BatchTenDiagnosticsTest.java` commit `fcd19fd285bc7337c86cb114e76e8b3a323b04ef`, **392 assertions PASS** in [CI run 40938234785](https://github.com/HazRob/HazRob-Tank/actions/runs/40938234785). Combined independent V6 core: 1,782 Java assertions. Android full Java compile with both new classes passed [40938031796](https://github.com/HazRob/HazRob-Tank/actions/runs/40938031796). This is not APK/field acceptance.
- **Actual 80 coverage updated:** `V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json` commit `0075b0566b64df6690a3c8376ead7a57bedcb402` = **24 partial / 56 unported / 0 physically validated**, not “24 fully complete.”
- **CRITICAL UCI1081 correction:** original `gsalc.csv` stores **ten contiguous sensor blocks of 900 consecutive samples**, NOT 900 interleaved time points with 10 sensors. Verified on exact source archive `sha256:813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab`. Correct `reshape(10,900).T` reproduces independent pinned original MQ135 acetone/50ppb mean endpoint ratio **0.9471726656594102**. Incorrect orientation gives **5.277230002555443**. Prior UCI pilot accuracy **retracted**.
- **UCI real-source 90 exposure, concentration-held-out re-run:** MQ135-only Logistic **28.89%** balanced accuracy; MQ135-only LDA **34.44%**; original full ten sensor Logistic **71.11%**, full ten sensor LDA **78.89%**. Results logged in `research/chemcore_v6/results/uci1081_corrected_sensor_blocked_benchmark_2026-10-29.json` commit `367d53790ff0660771ab6fbf2366e65b41c1f9a2`. Corrected benchmark reader `research/chemcore_v6/benchmarks/uci1081_v6_benchmark.py` commit `2317fe6bb4fbc81349f437809d37fb8a2c1a5752`. Old report prominently marked superseded `3b522baf67fded9d36ca38dfa3df22add06d7d99`; source layout regression `test_uci1081_layout_contract.py` commit `044f88e2bf34ab8b4c0fa8a4f235a55ed27d2a92`.
- **Scientific CI:** [40939360159](https://github.com/HazRob/HazRob-Tank/actions/runs/40939360159) **69 Python tests PASS**, raw UCI 90-exposure replay was executed locally (original archive not shipped in CI). No Tank G3 accreditation or new APK.
- **Resumption checkpoint:** `docs/implementation/V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-06-2026-10-29_AR.md`. Next: V6-N03-B new ten techniques, correct UCI1081 holdout on new source-native matching algorithms, V6 UI/report switch-over, Android runtime and physical Tank tests. Do not depend on old ChemCore.
