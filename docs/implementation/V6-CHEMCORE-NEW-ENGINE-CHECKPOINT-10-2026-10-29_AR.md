# ChemCore Tank V6 — Checkpoint 10 / N03-E

**Date:** 2026-10-29 (Asia/Aden)  
**Branch:** feature/tank-v6-implementation  
**Previous:** V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-09-2026-10-29_AR.md

## 1. Verified scope and actual count

The new gas-only research engine now has **64/80 PARTIAL_NEW_ENGINE_CODE_PRESENT, 16/80 NOT_YET_PORTED, and 0/80 PHYSICALLY_VALIDATED on Tank (G3)**. These are partial research implementations; neither test counts nor source fits constitute operationally deployed / certified techniques.

**CI scientific JDK runner [40954841693](https://github.com/HazRob/HazRob-Tank/actions/runs/40954841693) SUCCESS** with:
- Independent source curves: 60 JDK assertions.
- Source gas fingerprints: 1245.
- ChemCore V6 evidence coordinator: 58.
- V6 source signal features: 30.
- N03 first batch: 392.
- N03-B: 101.
- N03-C: 38.
- N03-D: 44.
- **N03-E: 47**.
- **Total: 2015 Java assertions (verified from same run logs)**.
- Reference parity and isolation checks PASS in this job.

Android compile-only [40954696816](https://github.com/HazRob/HazRob-Tank/actions/runs/40954696816) SUCCESS on implementation commit, **no APK**. Food freshness isolation [40954696500](https://github.com/HazRob/HazRob-Tank/actions/runs/40954696500) SUCCESS on same source. These workflow runs do not test physical hardware or Android UI runtime.

## 2. Ten distinct research-stage partial operations

New module: android/app/src/main/java/com/hazrob/tank/chemcore/v6/TemporalSpatialResearchV6.java (commit 42d9e3ea2d50fb15d791c29521247c68716248aa).

New test: research/chemcore_v6/tests/java/V6FifthTenTechniquesTest.java (commit 877c66a35540bc85d0bbea1621d7e8aea929f4f6).

Workflow registration: .github/workflows/chemcore-v6-independent-engine-ci.yml (commit a02bcb7a1cc57bc8b3c42fbf3f5ec8b60662aa0c).

| ID | Implementation | Claim boundary |
|---|---|---|
| 08.5 Temporal ensemble | Weighted averaging of multiple source-time scores on common class axes | Not an independent probabilistic evidence ensemble |
| 08.7 Sim-to-real | Paired residual bias and RMSE for externally provided simulated and measured rows | Does not generate real measurements or prove sim-to-Tank fidelity |
| 08.9 Unmixing | Nonnegative multi-prototype coordinate descent with fit residual and bounded iterations | A numerical decomposition does not confirm a chemical mixture or identity |
| 08.10 Multi-analyte regression | Training-only single-feature ridge regression for several response targets | Not full multivariate calibration and not ppm |
| 08.12 Complex products | Additive signature expectation versus supplied measured fingerprint; report residual | No complex-product chemical identification |
| 09.1 Robotic olfaction | Immutable pose/time/signal measurement record preserving source IDs and temporal order | No robot geolocation accuracy, motor control, or physical olfaction certification |
| 09.2 Gas mapping | Distance-weighted interpolation of sensor response in a caller-specified coordinate system | Does not generate a calibrated gas concentration/ppm plume map |
| 09.3 Source localization | Baseline-relative positive response centroid and peak original observation | Wind, airflow and geometry can confound source location |
| 09.4 Active sampling | Geometric farthest-site proposal from supplied candidate points | Does not issue autonomous navigation, controls, sampling or safe-route decisions |
| 09.5 Spatiotemporal fusion | Gaussian spatial-temporal kernel weighted response from original observations | No physical dispersion/transport model or concentration accuracy |

All methods explicitly reject undefined mathematical inputs and preserve source evidence without silently imputing gas identities. No chemical source or nutritional/food-freshness profiles are blended into the gas inference engine.

## 3. Scientific policy and previous N05 results

N05 original UCI1081 Java results remain unchanged and reproducible (CI 40952186046; raw SHA verified). Four Java model families with 24 whole-concentration holdout model/fold combinations; 720 predictions retained separately. The best tested full UCI source ten-sensor mean balanced accuracy was 76.67% for Softmax (macro-F1 72.86%); the same Java configuration for source MQ135 only was 38.89%. These are external source-discrimination experiments, **not** measurements by the installed Tank sensor array and not proof of Tank ppm or gas-identity accuracy. G3 stays **0/80**.

MQ/Bosch first-minute heater transients remain deferred. No modification to ESP32 or Android application routing occurred in N03-E.

## 4. Remaining 16 not ported

01.4 Heater cycles; 04.5 SVM; 04.6 Random Forest; 04.7 XGBoost; 04.8 MLP; 04.9 Physics+ML; 06.8 SHAP; 07.6 Domain adaptation; 07.7 Transfer learning; 07.9 Continual learning; 08.1 1D-CNN; 08.2 TCN; 08.3 LSTM/GRU; 08.4 GNN; 09.6 Motion decisions; 10.1 Negative Evidence & Sensor-Guided Exclusion.

Heater-start transient research is deferred by user decision; adding an independent scientific heater-cycle policy must not revise firmware/heater control without separate authorization. Safety-related negative evidence and motion decisions cannot be promoted from MQ-style tests or numeric source-model simulations alone.

## 5. Next verified task and release gates

1. **N03-F**: Select next ten of 16 unported techniques, avoiding any false claim of complete SVM/forest/XGBoost/CNN/etc when only a submodel is implemented. Each is partial until external-source and physical device evidence exist.
2. **N05-open-set**: design independent-negative (unseen gases) source benchmark with distinct training / calibration / test groups; evaluate false-known acceptance. Do not fit/rechoose a rejection threshold on sealed test data.
3. **N06**: Android facade for new V6 gas-only engine: instant readings, approved field sessions, PDF and chemCore candidate reports with preserved source provenance, no old inference fallback and no food references. Test real SQLite migration, data permissions, crash regression and report correctness.
4. **N07**: complete phone/robot verification, properly signed and versioned V6 APK, and actual referenced gas experiments under G3; physical validation remains a hard release-claim barrier.

**Resume token:** V6-CHECKPOINT-10 / N03-E_JDK_47_PASS / TOTAL_JDK_2015 / CI_40954841693_PASS / 64_PARTIAL / 16_NOT_PORTED / N05_UCI_JAVA_SOURCE_ONLY / G3_0_80 / NO_V6_APK / FOOD_ISOLATED.
