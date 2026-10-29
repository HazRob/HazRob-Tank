# ChemCore Tank V6 — Checkpoint 12 / N03-G + N05-G

**Date:** 2026-10-29 (Asia/Aden)  
**Branch:** feature/tank-v6-implementation  
**Previous:** V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-11-2026-10-29_AR.md

## 1) Scientific status — verified

**78/80 PARTIAL_NEW_ENGINE_CODE_PRESENT; 2/80 NOT_YET_PORTED; 0/80 Tank physically validated (G3).** These 78 operations are only source/research stage, not production certification, not all integrated into the Android app. No V6 APK has been built or tested.

- Independent Java [CI 40958218641](https://github.com/HazRob/HazRob-Tank/actions/runs/40958218641): **SUCCESS**; 47 NEW N03-G numerical/causal/negative-input assertions; **2120 total Java assertions** (previous 2073+47), source parity and isolation checks PASS.
- Android Java compile-only [40957697411](https://github.com/HazRob/HazRob-Tank/actions/runs/40957697411): **SUCCESS** on N03-G implementation. No APK.
- Food freshness/gas engine isolation [40957697278](https://github.com/HazRob/HazRob-Tank/actions/runs/40957697278): **SUCCESS** on N03-G implementation.
- Earlier JDK CI attempts [40957820888](https://github.com/HazRob/HazRob-Tank/actions/runs/40957820888), [40958024652](https://github.com/HazRob/HazRob-Tank/actions/runs/40958024652), [40958133383](https://github.com/HazRob/HazRob-Tank/actions/runs/40958133383) failed due to a Java var compound declaration and overly strong saturated-gate test fixture. Both fixed (final fixture commit `14b1ed2b46cccef7fdf39ef809ad600d4a6023fa`). Only final successful CI is reported as passing evidence.

## 2) Four new research-stage partial neural architectures — N03-G

**Code:** `android/app/src/main/java/com/hazrob/tank/chemcore/v6/TemporalNeuralResearchV6.java`, commit `3684fb6765f8479371742387005fc142e8d60bb3`.  
**Tests:** `research/chemcore_v6/tests/java/V6SeventhNeuralTechniquesTest.java`, final commit `14b1ed2b46cccef7fdf39ef809ad600d4a6023fa`.  
**CI wiring:** `.github/workflows/chemcore-v6-independent-engine-ci.yml`, commit `a4a4f9332e8a6f8f334266ca6b0b7547a9320e3f`.

| ID | Actual numerical operation | Deliberate partial boundary |
|---|---|---|
| 08.1 1D-CNN | Four width-three fixed temporal convolution filters, ReLU positive part, source-channel global time mean, TRAINED binary logistic readout | Convolution filter bank NOT trained end-to-end; no calibrated probabilities |
| 08.2 TCN | 3 causal dilated temporal convolutions at dilation 1,2,4 with terminal/average features and TRAINED logistic head; future data excluded from prefix computations | Fixed dilation kernel coefficients; no backprop across convolution layers |
| 08.3 GRU | Real update/reset/candidate recurrent gate equations using fixed documented scalar per-channel coefficients; final/mean hidden state and TRAINED logistic head | NOT learned recurrent matrices or BPTT; **not full trained GRU/LSTM**, no automatic ppm |
| 08.4 GNN | Real two-hop graph neighbor aggregation and tanh update on an EXPLICIT undirected source sensor adjacency with trained logistic head | NOT learned message passing filters, and does not infer/make up missing physical graph |

Training is on entire source exposure rows; numeric time windows of one exposure are never presented as independent experiments. All models explicitly declare `encoderWeightsTrained=false`, `probabilitiesCalibrated=false`, `physicalTankGasIdentityValidated=false`. The GNN refuses missing/malformed graphs instead of synthesizing physical topology. **These four count as PARTIAL, not fully implemented deep learning techniques.**

## 3) N05-G original UCI1081 source temporal holdouts

**Workflow:** [40958081282](https://github.com/HazRob/HazRob-Tank/actions/runs/40958081282) = **SUCCESS**.

Verified official source archive SHA256 `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab` and original gsalc.csv `d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3`. Source ten channels each contain a contiguous 900-sample segment. Generated 90 original exposures with time-major 30 (nonoverlapping 30-second windows) × 10 channels per exposure. Each channel normalized against its same-exposure first 300-second source baseline. Derived temporal feature CSV SHA256 `d4a1bf654aa3d8e4cda85f7869c4d7a73ea2d78d4d2cfbd02f8e82830955d5da`.

- Three folds hold out entire original **50, 100, 200 ppb** concentration levels respectively (60 source exposures train, 30 heldout). All 30 temporal bins of an exposure are kept together. No device or day IDs inferred.
- Three algorithms CNN/TCN/GRU with fixed feature extractors and source-trained logistic heads; two UCI source-channel modes MQ135 vs all ten; **18 fold/model conditions and 540 original heldout predictions**. Six one-vs-rest logistic heads per fold (550 iterations, LR .12, L2 .002). GNN **NOT BENCHMARKED** on this source without a documented physical sensor graph.
- Original source replay code: `research/chemcore_v6/benchmarks/extract_uci1081_temporal_verified.py`; Java evaluator: `research/chemcore_v6/benchmarks/Uci1081N03GJavaBenchmark.java`.
- Permanent summary `research/chemcore_v6/results/uci1081_v6_n05g_temporal_java_original_source_2026-10-29.json`.

### Observed mean balanced accuracy / macro-F1

| Source algorithm | MQ135 alone | Full UCI ten-source channels |
|---|---|---|
| 1D-CNN fixed filters + trained head | 48.89% / 41.44% | 65.56% / 62.17% |
| TCN fixed filters + trained head | 26.67% / 18.95% | 53.33% / 47.68% |
| GRU fixed gates + trained head | 34.44% / 26.45% | **81.11% / 79.36%** |

Under this particular external-source concentration-grouped benchmark, frozen GRU encoders plus trained heads gave higher full-UCI balanced accuracy than the previous V6 Softmax on three-window summary features (76.67%). **The input features and model architectures differ, so this does NOT establish general superiority, independent-device reproducibility, calibration, Tank gas identity/ppm, or safety readiness.** It is important to preserve both conflicting/interpretable source-level results without overstating either.

**Full provenance and raw predictions:** Actions artifact 12934157263, `chemcore-v6-n05g-original-uci1081-temporal-java`, includes `source_manifest.json`, `fold_metrics.csv` and `heldout_predictions.csv` (540 lines). Artifact SHA256 `9cd3b2b4cbd480b73b2d383fa391574c2f23814e19b2d06f1df9b28958750aaa`, backup to personal Library `ChemCore_V6_N05G_UCI1081_Temporal_Java_Results_2026-10-29.zip` (`libfile_2e58d506e5008191aac25281b4e6248c`).

## 4) Two remaining unported methods

**01.4 Heater cycles** — development of MQ/Bosch initial heating cycles and first-minute transients is deferred by explicit user decision; DO NOT alter ESP32 heater firmware or claim the technique implemented.

**09.6 Motion decisions** — field-decision integration with safety controls, obstacles, lost communication, operator stop, emergency stop, and real robot testing is outstanding. The source-only geometrical `09.4` active sampling proposal is not robotic autonomous motion. No unqualified operational motion-control logic may be promoted without real acceptance and permission.

## 5) Research and release gates

N03-G mathematical coding and JDK source validation: PASS. N05-G original UCI temporal grouped holdout: PASS for **external source domain only**. Android Java compile and food separation: PASS. Android V6 real-time UI, instant measurements, sessions/approval, database migration, PDF/Excel reporting, signature/upgrade compatibility: **NOT INTEGRATED / NOT TESTED**. No V6 APK. Real Tank gas-reference G3 **0/80**.

**Next:** N05 independent-negative unknown gas evaluation with TRAIN/CALIBRATION/SEALED TEST; no threshold tuning on test. N06 connect only new V6 source engine with approved sessions and reports (no legacy fallback, food engine separate). N03 for final 01.4 and 09.6 is explicitly gated by user deferral and physical safety evidence.

**Resume token:** `V6_CHECKPOINT12 / 78_PARTIAL / 2_UNPORTED / N03G_JDK_47_PASS / TOTAL_JDK_2120 / CI_40958218641 / N05G_ORIGINAL_UCI_18_FOLDS_540_PREDICTIONS_CI_40958081282 / ANDROID_COMPILE_ONLY_PASS / FOOD_ISOLATED / G3_0_80 / NO_V6_APK`.
