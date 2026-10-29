# ChemCore Tank V6 — Checkpoint 09 (N05 + N03-D)

**2026-10-29, Asia/Aden | branch: feature/tank-v6-implementation**

## Verified progress

- **54/80 PARTIAL_NEW_ENGINE_CODE_PRESENT**, **26/80 NOT_YET_PORTED**, **0/80 Tank G3 physically validated**.
- N03-D added 10 independent numerical research-stage methods; new unit tests: 44 assertions. CI [40952792580](https://github.com/HazRob/HazRob-Tank/actions/runs/40952792580) SUCCESS, total **1968 JDK assertions**.
- Android complete Java compile-only after N03-D source changes: [40952676989](https://github.com/HazRob/HazRob-Tank/actions/runs/40952676989) SUCCESS. This is not APK, UI or physical robot acceptance.
- Independent meat/fruit freshness isolation CI [40952676927](https://github.com/HazRob/HazRob-Tank/actions/runs/40952676927) SUCCESS. No food reference in gas ChemCore.

## V6-N05: ORIGINAL UCI1081 heldout Java benchmark COMPLETE

Source acquired directly at GitHub Actions from official UCI; archive SHA256 813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab and original gsalc.csv SHA256 d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3 verified before processing. Fixed 10-block layout corresponds to reshape(10,900).T. Derived feature CSV SHA256 c3f930b18b4d2ebfab8d691b1e2b5dcbe2ff5f94cd082a2e4fdd46d913a0d6da.

90 original exposures; 6 gases; 50/100/200 ppb x five replicates per gas. Leave each whole concentration level out, with 60 training and 30 heldout each fold. 4 new Java V6 classifiers x 2 sensor configurations x 3 folds = **24 evaluations / 720 heldout predictions**, [CI 40952186046](https://github.com/HazRob/HazRob-Tank/actions/runs/40952186046) SUCCESS. No source test observation participates in fitting.

| New V6 Java algorithm | MQ135 BA / macro-F1 | Full UCI 10-channel BA / macro-F1 |
|---|---|---|
| Diagonal LDA | 26.67% / 19.32% | 38.89% / 35.14% |
| KNN k=3 | 34.44% / 28.15% | 51.11% / 47.22% |
| Softmax logistic | 38.89% / 32.49% | **76.67% / 72.86%** |
| PCA2 then diagonal LDA | 37.78% / 32.42% | 32.22% / 25.72% |

These are performance results of Java V6 with fixed predeclared hyperparameters; NOT earlier sklearn results. They do not establish device/session/day independence, interchangeability of UCI MQ135 with installed Tank electronics, calibrated concentrations or unknown gas recognition. Softmax outputs were NOT calibrated as probabilities.

- Summary with all 24 folds: research/chemcore_v6/results/uci1081_v6_n05_java_original_source_results_2026-10-29.json
- GitHub Actions artifact **12931063894** stores source_manifest.json, fold_metrics.csv and **720 original holdout predictions** in heldout_predictions.csv. Artifact ZIP SHA256 04083f8a0398a0d1ae0e5a494b7e1551c161fa122e552dd94392e30ce054fac3.
- Durable personal Library copy: ChemCore_V6_N05_Original_UCI1081_Java_Results_2026-10-29.zip (libfile_4b7b621268f881919d097b6afda6d72d).

## V6-N03-D: ten more numerical research techniques

Source: android/app/src/main/java/com/hazrob/tank/chemcore/v6/SourceNumericsV6.java (commit 20eca8490b318de152e75e6d8b56c08bdde0512d).
Test: research/chemcore_v6/tests/java/V6FourthTenTechniquesTest.java (commit a33c6b35686d9369351f556d8f84d3292d92d309).

| ID | Technique | Precise implementation boundary |
|---|---|---|
| 05.5 | PLS regression | Train-only one-latent-factor PLS1, not PLS-DA |
| 05.6 | Tree regression | One-split CART regression stump only |
| 05.9 | Unseen concentrations | Monotone log-ratio interpolation with explicit extrapolation flag |
| 07.4 | Drift detection | Descriptive baseline-sequence OLS slope, not verified physical drift |
| 07.5 | Drift compensation | Explicit source/measured baseline ratio transfer, not fabricated R0 |
| 07.8 | Active learning / DoE | Entropy ordering of uncalibrated source-relative scores, not validated DoE |
| 07.10 | Resource profiling | p50/p95 from caller-supplied elapsed nanoseconds, not handset benchmark |
| 08.6 | Simulation | Seeded Gaussian feature perturbation, not validated gas physics |
| 08.8 | Mixture detection | Two-source NNLS residual vs best single-source fit, no confirmed mixture |
| 08.11 | Interference | Conditional nonnegative source interferer scale, no chemical identification |

CI run **40952792580** actually compiled and executed all N03-D fixtures, with **44 additional assertions**: total **1968**. Tests are mathematical and source-research-oriented, not independent physical sensor validation. Policy: keep all defined numerical source hypotheses available, record disagreements, prevent unsupported confident safety identity/ppm claims. All food data remain outside the new gas engine.

## Remaining gates and resumable instructions

- N05 original source benchmark: PASS for heldout source concentration only; no cross-device/day claim.
- N03-D ten new research primitives: PASS for CI, partial only.
- N06 Android live measurement, session approvals, PDF and instant result routing to V6-only: PENDING.
- N07 APK V6 packaging and physical Android acceptance: NOT BUILT / NOT TESTED.
- Tank G3 reference-gas validation: **0/80**, unchanged.
- MQ/Bosch first-minute transient research: deferred.

**Next:** N03-E, ten additional unported research techniques from the remaining 26; then N05 open-set/OOD unknown-gas evaluation with genuinely independent negatives and train/calibration/test separation; N06 Android new-engine routing. No new dataset result should be described as Tank device performance without G3 evidence.

**Resume token:** V6_CHECKPOINT09 / 54_PARTIAL / 26_UNPORTED / N05_JAVA_24_FOLDS_720_PREDICTIONS / JDK_1968_SUCCESS / G3_0_80 / NO_APK / FOOD_ISOLATED.
