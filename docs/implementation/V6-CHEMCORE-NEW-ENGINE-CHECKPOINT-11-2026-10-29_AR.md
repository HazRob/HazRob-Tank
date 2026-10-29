# ChemCore Tank V6 — Checkpoint 11 / N03-F + N05-F

**Date:** 2026-10-29 | **Branch:** feature/tank-v6-implementation
**Supersedes as current status:** Checkpoint 10. Stable old gas engine and food freshness engine not modified.

## Status verified from CI and source-traceable artifacts

- **74/80 PARTIAL_NEW_ENGINE_CODE_PRESENT, 6/80 NOT_YET_PORTED, 0/80 Tank G3 PHYSICALLY_VALIDATED.** This is research-code coverage, not fully deployed/safety-certified functions.
- [Independent JDK CI 40956582213](https://github.com/HazRob/HazRob-Tank/actions/runs/40956582213): **SUCCESS**, 58 new N03-F source tests; **2073 total Java assertions** (60+1245+58+30+392+101+38+44+47+58); historical source-curve parity and independent engine boundary checks PASS.
- [Android full Java compile-only 40956403097](https://github.com/HazRob/HazRob-Tank/actions/runs/40956403097): **SUCCESS** on N03-F implementation source commit bc510a131e60d0e3c2d25b2fb0392d954aa65648; no V6 APK produced or installed.
- [Food freshness isolation 40956403090](https://github.com/HazRob/HazRob-Tank/actions/runs/40956403090): **SUCCESS** on same implementation commit. No food headspace references in gas inference.

## Batch N03-F: ten scientifically bounded implemented operations

Sources: 
- `android/app/src/main/java/com/hazrob/tank/chemcore/v6/SupervisedLearningResearchV6.java` commit `c17a6e4366d7cef3891b558ce1ee0aefb5ab8470`.
- `android/app/src/main/java/com/hazrob/tank/chemcore/v6/AdaptationEvidenceResearchV6.java` commit `bc510a131e60d0e3c2d25b2fb0392d954aa65648`.
- `research/chemcore_v6/tests/java/V6SixthTenTechniquesTest.java` commit `21b80aec6e0d89472a80160db81f1effb62a6898`.
- CI wiring `5be602c79623e8af04f444aa9d4a2db9b6027222`.

| ID | Exact partial implementation | What it does NOT demonstrate |
|---|---|---|
| 04.5 SVM | Binary hinge-loss linear soft-margin SGD | Nonlinear/kernel SVM or gas-class posterior calibration |
| 04.6 Random Forest | Seeded bootstrap ensemble of binary classification CART stumps | Full-depth forest or tuned production random forest |
| 04.7 XGBoost | Binary Newton gradient/Hessian logistic boosted stumps | Feature-complete XGBoost software or general concentration model |
| 04.8 MLP | One-hidden-layer seeded tanh network and batch backprop | Broad deep neural network generalization or Tank gas identity |
| 04.9 Physics+ML | Explicit anchored source log physical curve plus fitted log residual | Reference physical calibration on installed sensor |
| 06.8 SHAP | Exact fixed-baseline small-dimensional interventional Shapley attributions | Conditional SHAP for dependent sensors, causal explanation |
| 07.6 Domain adaptation | Diagonal source/target mean-variance alignment from separate training/calibration rows | Genuine Tank domain transfer or stable calibration |
| 07.7 Transfer learning | Prior-centred regularized linear fine-tune from labelled target rows | Full deep neural transfer learning |
| 07.9 Continual learning | One importance-weighted elastic incremental gradient step | Stable learning without catastrophic forgetting / holdout |
| 10.1 Negative Evidence | Provenance-retaining FAMILY-level qualified-negative sensor ledger | No hard candidate deletion, no UNKNOWN deletion, no unvalidated MQ-family exclusion |

### Critical negative-evidence safety boundary

The new source ledger annotates (never deletes) a candidate hypothesis only when an input evidence record **asserts** that its sensor is calibrated to Tank, operational, below a certified family-specific detection limit, with minimum independent stable measurements, sensor ID and provenance ID. This software checks declarations but CANNOT establish their factual validity. Missing conditions, MQ8 uncalibrated absence, sensor disconnection, inadequate stability, or unproven LOQ must never exclude a candidate. Keep original hypotheses (including UNKNOWN), all mathematically valid scores and source disagreements. Physical G3 data are still zero and must be required before real field assertions.

## N05-F original UCI1081 Java evaluation of four N03-F algorithms

Source: official UCI 1081 original ZIP SHA256 `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab`, gsalc.csv SHA256 `d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3`, verified original extraction `c3f930b18b4d2ebfab8d691b1e2b5dcbe2ff5f94cd082a2e4fdd46d913a0d6da` for 30 features. Original layout is ten contiguous 900-sample sensor blocks per 90 exposure rows, source concentration ppb levels 50/100/200, 6 gases x five replicates per level/gas.

Evaluation [Actions 40956838753](https://github.com/HazRob/HazRob-Tank/actions/runs/40956838753) **SUCCESS**. Four original Java V6 N03-F models trained one-versus-rest on 60 source rows, with a full 30-row heldout concentration level for each of three folds. MQ135 source-only vs all ten UCI source sensors; 24 fold-level evaluations and **720 heldout predictions**. Scalers/preprocessing are identical source features extracted without fitting to the sealed holdout; model hyperparameters fixed in code before the reported evaluation.

| Algorithm | Source MQ135 mean BA / macro-F1 | Full UCI 10-source mean BA / macro-F1 |
|---|---|---|
| Linear hinge SVM OVR | **18.89% / 8.03%** | **38.89% / 29.36%** |
| Bootstrap stump forest OVR | **25.56% / 16.92%** | **41.11% / 34.41%** |
| Newton boosted logistic stumps OVR | **30.00% / 22.61%** | **42.22% / 37.16%** |
| One hidden MLP OVR | **26.67% / 14.76%** | **42.22% / 34.49%** |

The previous actual Java V6 N05 Softmax on all ten source sensors scored BA **76.67%** (macro-F1 72.86%). The new four algorithms DO NOT outperform that reference under these declared fixed settings. **Do not adjust the new experiment to claim artificial superiority.** This comparison is only external source domain; original data lack proven independent device/day grouping, and UCI 10-source hardware is not the installed Tank sensor array. No new Android device or ppm measurement, no independent unknown-gas negative challenge or safety-grade threshold.

Code: `research/chemcore_v6/benchmarks/Uci1081N03FJavaBenchmark.java` commit `9112687c510806a0795162ae7313d208da8fce43`. Workflow: `.github/workflows/chemcore-v6-n05f-uci1081-java.yml` commit `a553728260374359ae868281c5738d5bebfed86a`. Pinned metrics: `research/chemcore_v6/results/uci1081_v6_n05f_n03f_models_original_source_2026-10-29.json` commit `576fc7f72b1158b27210287a70fe9a097b0d2ebb`. Raw all-fold predictions `heldout_predictions.csv`, source_manifest.json, metrics.csv in Actions artifact **12934415961**, ZIP digest `sha256:3e1530005ecac4373438d67b9143157ef7cea9468379343843fe496f41b7df0a`; durable library file `ChemCore_V6_N05F_Original_UCI1081_Java_Results_2026-10-29.zip` (`libfile_797166ba7778819181227a2846785900`).

## Remaining exactly SIX not ported IDs

1. **01.4 Heater cycles** — experimentally sensitive, first-minute MQ/Bosch transient work is deferred by the user, no firmware heater changes authorized.
2. **08.1 1D-CNN** — needs actual temporal trainable convolution and independent source labels.
3. **08.2 TCN** — causal dilated temporal layers and time-blocked validation.
4. **08.3 LSTM/GRU** — gated recurrent model with source-separated time sequences.
5. **08.4 GNN** — explicit graph semantics across physical sensor arrays and honest device/domain evaluation.
6. **09.6 Motion decisions** — no autonomous actuation without collision/lost-link/ESD and physical robot safety tests.

Status is **74 partial / 6 not ported / 0 Tank G3 / NO V6 APK**.

## Next execution

- **N03-G:** research implementation of the feasible remaining neural temporal methods (08.1–08.4), without numerical claims prior to real-source sequence benchmarks; preserve 01.4 heater transient deferral and 09.6 safety decision gate.
- **N05-open-set:** genuine source negative families and three independent partitions TRAIN/CALIBRATION/SEALED TEST with source provenance; no threshold optimization on test.
- **N06:** connect only NEW V6 gas engine to instant measurements, sessions/approval and reports; no old inference or food pipeline fallback.
- **G3 physical:** reference gas calibration, source traceability, session/device holdouts, sensor-specific LOQ and dedicated safety acceptance remain hard promotion gates.

**Resume token:** V6_CHECKPOINT11 / N03F_10_PARTIAL / 74_PARTIAL / 6_NOT_PORTED / JDK_2073_SUCCESS_40956582213 / N05F_UCI_24FOLDS_720PREDICTIONS_40956838753_SUCCESS / G3_0_80 / NO_APK / FOOD_SEPARATE.
