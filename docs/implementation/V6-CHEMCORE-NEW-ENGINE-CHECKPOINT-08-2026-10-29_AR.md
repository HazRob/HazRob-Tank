# ChemCore Tank V6 — Checkpoint 08 / N03-C

**Date:** 2026-10-29 (Asia/Aden)  
**Branch:** feature/tank-v6-implementation  
**Status:** independent gas-engine JDK CI PASS; Tank G3 still 0/80; no V6 APK.

## Verified milestone

10 additional partially implemented research techniques: **04.10, 06.2, 06.3, 06.4, 06.5, 06.6, 06.9, 07.1, 07.2, 07.3**.

**Totals:** 44 PARTIAL, 36 NOT_YET_PORTED, 0 PHYSICALLY_VALIDATED. Partial means source-tested research primitives, NOT 44 deployable production techniques.

- Implementation: android/app/src/main/java/com/hazrob/tank/chemcore/v6/SourceValidationV6.java — commit 33a43d3963662b52f10735a8b51b69ba7a1db7fa.
- JDK tests: research/chemcore_v6/tests/java/V6ThirdTenTechniquesTest.java — commit fec898fcd8f6abb9b0198ac95875f714854499eb.
- CI wiring: .github/workflows/chemcore-v6-independent-engine-ci.yml — commit cecdfd56d8461a306aaf5313b6fee1d41fede32b.
- **JDK CI run 40949107155 SUCCESS**: 60+1245+58+30+392+101+38 = **1924 assertions**, including the new batch's **38 checks**. Parity of 43 source coefficients and isolation from former engine/Android dependencies also passed.
- **Android compile-only run 40948928576 SUCCESS** on the implementation source commit. It produced **no APK**.
- **Food-engine isolation run 40948928574 SUCCESS** on implementation source commit.

## Scientific and implementation boundaries

| ID | Research-stage algorithm | Not yet established |
|---|---|---|
| 04.10 | Balanced accuracy, macro-F1 and accuracy on labeled held-out predictions; abstentions remain errors | No fresh Java V6 UCI1081 benchmark |
| 06.2 | Bayes normalization from explicit priors and likelihoods | Validated physical gas posterior |
| 06.3 | Temperature fitting by NLL on a separate calibration set | Independently calibrated Tank probability |
| 06.4 | Finite-sample split-conformal quantile and conservative all-label fallback | Exchangeability or coverage on Tank |
| 06.5 | UNKNOWN decision using externally supplied best-score and margin limits | Validated unknown rejection rate |
| 06.6 | Training-only diagonal Mahalanobis OOD score with regularized variance | Calibrated OOD cutoff |
| 06.9 | Abstain from identity reporting while preserving every numerically valid hypothesis score | Field safety performance |
| 07.1 | Entire-source-session holdout without frame leakage | Real independent Tank sessions |
| 07.2 | Entire-physical-device holdout using genuine device IDs | Inter-device validation |
| 07.3 | Entire-source-concentration-level holdout | Original-source Java model cross-concentration results |

Keep all mathematically valid research evidence, even when metadata are incomplete. Only exclude undefined numeric operations (NaN/Infinity, denominator zero etc.) from that computation; document the reason. UNKNOWN/ABSTAIN suppresses an unsupported confident *claim*, not source candidate calculations. Preserve independent source disagreements.

**Food freshness remains an independent meat/fruit engine.** Food headspace sources must never join ChemCore gas candidate ranking. MQ/Bosch first-minute transient research stays deferred.

## UCI1081 evaluation gate

Correct 10-channel raw order: reshape(10,900).T (NOT reshape(900,10)). Previous corrected sklearn reference benchmark: Logistic 28.89% (MQ135) versus 71.11% (10 channels); LDA 34.44% versus 78.89%. These are **not** accuracies of the new Java classes.

Required source archive SHA256: 813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab. Raw archive must be independently checked before each reproduction. Level-blocked 50/100/200 ppb evaluation and training/calibration separation are mandatory. Earlier MQ135 open-set heldout unknowns were rejected only 4/90; thresholds are **not** validated for Tank.

## Gates

| Gate | Outcome |
|---|---|
| JDK core and 38 new scientific unit assertions | PASS, run 40949107155 |
| Android Java compile for V6 N03-C source | PASS, run 40948928576 |
| Food separation | PASS, run 40948928574 |
| New Java classifier on real UCI1081 sealed folds | NOT RUN |
| V6 UI, instant/approved sessions, PDF routing | NOT INTEGRATED |
| V6 installable APK | NOT BUILT |
| Physical Tank gas-reference G3 | 0/80 |

## Next steps

1. **V6-N05:** implement and run an independently reproducible source ZIP benchmark harness for Java V6 PCA/LDA/KNN/softmax, then OOD/open-set, with all source SHA, complete concentration holdouts, per-fold predictions and train-only fitting. No invented source data.
2. **V6-N03-D:** choose ten among the 36 not ported; actual mathematical implementation plus JDK unit/source validation for each.
3. **V6-N06:** connect only new V6 inference to instantaneous and approved-session reports, exposing evidence and noncertification notices without calling the predecessor inference engine.
4. **V6-N07/G3:** physical Android/Tank acceptance with real calibration evidence, then consider release. No unsupported gas ppm/identity assurance.

**Resume token:** V6-N03-C_DONE / 44_PARTIAL / 36_NOT_PORTED / 1924_JDK_ASSERTIONS / CI_40949107155_SUCCESS / NO_UCI_JAVA_METRICS / G3_0_80 / NO_APK / FOOD_ISOLATED.
