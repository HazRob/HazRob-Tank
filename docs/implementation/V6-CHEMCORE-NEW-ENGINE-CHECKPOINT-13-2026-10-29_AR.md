# ChemCore Tank V6 — Checkpoint 13 / N05-H: Original UCI1081 unknown-gas open-set stress test

**Date:** 2026-10-29, Asia/Aden  
**Branch:** feature/tank-v6-implementation  
**Previous:** Checkpoint 12.

## Verified current status

- **Research code:** **78 of 80 PARTIAL**, 2 NOT YET PORTED (01.4 heating transients deferred, 09.6 physical motion safety gated), **0/80 G3**.
- **Independent JDK CI:** [40959501615](https://github.com/HazRob/HazRob-Tank/actions/runs/40959501615) SUCCESS, **28 new source Open-set calibration assertions**, 2148 total Java assertions and source parity/isolation PASS.
- **Android Java compile-only:** [40959245040](https://github.com/HazRob/HazRob-Tank/actions/runs/40959245040) SUCCESS (source V6 OpenSetResearchV6.java); no APK or installed test.
- **Food freshness isolation:** [40959244989](https://github.com/HazRob/HazRob-Tank/actions/runs/40959244989) SUCCESS. No meat/fruit signals or recipes in the gas-only engine.
- **Original-source N05-H:** [40959780208](https://github.com/HazRob/HazRob-Tank/actions/runs/40959780208) SUCCESS as an *execution*, but **FAILED AS A SAFETY PROMOTION GATE**: the actual unknown-gas false-known acceptance is far too high, particularly in the full ten-UCI-channel model. Neither the fitted threshold nor the learned model may be represented as a Tank reference calibration.

## 1. Reproducibility and sample identity

Original UCI1081 downloaded inside CI and checked BEFORE reading:

- Archive ZIP SHA256: `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab`
- Raw source CSV SHA256: `d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3`
- Derived feature CSV SHA256: `c3f930b18b4d2ebfab8d691b1e2b5dcbe2ff5f94cd082a2e4fdd46d913a0d6da`
- Source layout: ten contiguous 900-point sensor channels per exposure, equivalent to `reshape(10,900).T`.
- 90 source exposures: six named gases, three concentration levels (50/100/200 ppb), five original exposures per gas/level.

36 evaluation folds = **2 source channel configurations × 3 heldout known-gas concentration levels × 6 different sealed-test unknown gas identities**. For every fold, the six source gas identities are allocated as **four known gases**, **one calibration UNKNOWN gas**, and **one DIFFERENT sealed-test UNKNOWN gas**:

| Partition | Original source exposures / fold | Rule |
|---|---:|---|
| TRAIN known | 40 | Four known gases × other two concentration levels × five repetitions |
| CAL known | 8 | Four known gases × two original repetitions at withheld concentration |
| CAL unknown | 15 | All 15 original source exposures of a FIFTH gas |
| SEALED TEST known | 12 | Four known gases × remaining three original repetitions at withheld concentration |
| SEALED TEST unknown | 15 | All 15 original source exposures of a SIXTH, DIFFERENT gas |

All 90 original source row IDs are accounted for in each fold with **zero cross-partition exposure leakage**. The sealed unknown gas identity never appears in TRAIN or CAL, and calibration-negative unknown gas identity never appears in TRAIN or TEST. The known model is four-class Java V6 Softmax fitted on ONLY the 40 source TRAIN exposures using train-only standardization (iterations 800, learning rate 0.15, L2 0.005). The threshold is fitted on ONLY 8 CAL known + 15 CAL unknown max-relative-scores using balanced source known-retention plus unknown-rejection. **No test data used to select thresholds**.

**Limitation:** The source CSV does NOT establish independent acquisition device/day, nor physical equivalence of its electronics to Tank MQ sensors. Multiple outer folds share parts of the original UCI study, so don't treat the 36 folds as independently sampled device campaigns.

## 2. Actual sealed test results — no promotion

Each mode: 18 folds × (12 test known + 15 test unknown) = 486 original-source test decisions. Entire experiment: **972 decisions**, with **3,240 source-row partition audit entries** and complete per-candidate scores recorded on every decision.

| Outcome | MQ135 source alone | All ten UCI source channels |
|---|---:|---:|
| TEST unknown accepted incorrectly as known | **96/270 = 35.56%** | **151/270 = 55.93%** |
| TEST unknown rejected as UNKNOWN | 174/270 = 64.44% | 119/270 = 44.07% |
| TEST known retained with gas report | 103/216 = 47.69% | 123/216 = 56.94% |
| TEST known correctly classified before gate | 113/216 = 52.31% | 178/216 = 82.41% |
| TEST known correctly classified after gate (of ALL known tests) | 67/216 = 31.02% | 114/216 = 52.78% |
| Combined correct open-set decisions across source known+unknown | 241/486 = 49.59% | 233/486 = 47.94% |

**Interpretation:** more source channels improved closed-set discrimination of the four known gases but DID NOT transfer reliably to unknown-gas rejection. In the ten-channel test, more than half the entirely unknown test-gas exposure decisions were falsely reported as known. Calibration thresholds and fitted relative scores are **not physically valid Tank safety/identity thresholds**.

## 3. Implemented and auditable work

- Gas-only research threshold and abstention with full candidate preservation: `android/app/src/main/java/com/hazrob/tank/chemcore/v6/OpenSetResearchV6.java`, commit `e8a71c394280a273891a3a5c876c1e11bb5268d9`.
- Original UCI gas-disjoint experiment and per-row partitions: `research/chemcore_v6/benchmarks/Uci1081OpenSetV6Benchmark.java`; final score-preserving update commit `0a71790eea1ab6d2b7f6e4a1afa435ccb6ca229b`.
- JDK negative/stability/abstention tests: `research/chemcore_v6/tests/java/V6OpenSetResearchTest.java`, commit `deae0c35e7df155d55b2b3fbf0025ffafa065aaf`; CI registration commit `675efaf32accbb7803e26b1721b1943e2f6ada66`.
- Source CI: `.github/workflows/chemcore-v6-n05h-open-set-uci.yml`, final commit `bb2f3114a40e573af582eac45baefef1c0bb0317`.
- Structured permanent JSON results: `research/chemcore_v6/results/uci1081_v6_n05h_sealed_unknown_gas_original_source_2026-10-29.json`, commit `c61ec0b87dc012f5ede4fff9c13313691d252209`.
- Source CI artifact ID **12934992995**, ZIP SHA256 `ee26ed0973b4fdf7b59c350e60bd9f1eb9068a7a48988f805013fe007abd8804`. Contents: source_manifest.json, fold_metrics.csv (36), heldout_decisions.csv (972 with ALL four class scores), source_partition_audit.csv (3240).
- Permanent personal Library backup: `ChemCore_V6_N05H_UCI1081_OpenSet_Results_2026-10-29.zip`, ID `libfile_35fb7eda29b8819199eabfc4d4283b4e`.

Source scores and hypotheses are NOT deleted when research reporting abstains. Even a known class label surviving the source cutoff is NOT promoted to safety-grade device gas identity.

## 4. Gate policy for application integration

- **Open-set unknown-gas identity gate: HOLD / NOT VALIDATED for Tank.** An Android research screen may display candidate evidence + UNKNOWN + source-study status, but must not claim that a confident source-trained Softmax class is field-confirmed or that the UCI threshold controls real Tank detection.
- **No real Tank calibration Rs/R0, gas-family detection limit, temperature/humidity compensation, device/day source validation or G3 benchmarks are available to authorize safety-grade identity/ppm.**
- **Food-freshness engine stays a completely separate meat/fruit source and processing path.**
- First-minute MQ/Bosch sensor-heater transient task remains deferred, and no automatic motion decisions are activated.

## 5. Next work

**N06-Prep:** examine Android instant/session/reports structure, build independent gas-only research result adapter with evidence/source labels, UNKNOWN/NOT_VALIDATED markers, and tests. Any Android activity integration must preserve existing sessions/approval, SQLite migration, file share and crash handling; no fallback to old gas inference or mixing food-headspace profiles. Do not declare APK or phone test without actual evidence.

**N05-I:** explore better genuinely heldout known/negative calibration, domain-separated scores and false-known-rejection stress with a new experiment, without reusing the *sealed* N05-H scores for tuned model-selection and then calling the result independent. Physical Tank G3 remains decisive.

**Resume token:** `V6_CHECKPOINT13 / N05H_SOURCE_OPENSET_36_FOLDS / 972_TEST_DECISIONS / FALSE_KNOWN_MQ135_35.56_PERCENT / FALSE_KNOWN_FULL_UCI_55.93_PERCENT / 78_PARTIAL / 2_UNPORTED / JDK_2148 / CI_40959501615_PASS / N05H_40959780208_PASS_EXECUTION_FAIL_SAFETY_GATE / G3_0_80 / FOOD_ISOLATED / NO_APK`.
