# ChemCore status ledger
Version 2.3 | 2026-10-19 | G1: 80/80 research unit PASS (1261 original tests); G2 40 distinct technique IDs audited in 4 source-evidence batches (28 real external sensor array values, 11 blocked, 1 theoretical); Tank MQ8 G3=0/80. See G2_REAL_DATA_BATCH04_AR.md and PROGRESS_80.json.

| ID | Technique | Status | Evidence |
|---|---|---|---|
| 01.1 | ADC to Rs | PROTOTYPE | research/techniques/01.1/ (11/11 local unit tests; hardware audit pending); GitHub Actions batch01 11/11 verified unit PASS; empirical validation pending |
| 01.2 | R0 calibration | PROTOTYPE | research/techniques/01.2/ (hardware calibration pending) ; GitHub Actions batch01 11/11 verified unit PASS; empirical validation pending |
| 01.3 | Sensitivity curves | PROTOTYPE | research/techniques/01.3/ (synthetic tests; manufacturer curves pending) ; GitHub Actions batch01 17/17 verified unit PASS; empirical validation pending |
| 01.4 | Heater cycles | PROTOTYPE | research/techniques/01.4/ (software phase tagging; physical heater control unverified) ; GitHub Actions batch01 12/12 verified unit PASS; empirical validation pending |
| 01.5 | Environmental correction | PROTOTYPE | research/techniques/01.5/ (synthetic tests; real controlled exposures needed) ; GitHub Actions batch01 12/12 verified unit PASS; empirical validation pending |
| 01.6 | Signal quality | PROTOTYPE | research/techniques/01.6/ (13 unit cases authored; Tank ADC hardware validation pending) ; GitHub Actions batch01 13/13 verified unit PASS; empirical validation pending |
| 01.7 | Drift monitoring | PROTOTYPE | research/techniques/01.7/ (12 unit cases authored; controlled baseline validation pending) ; GitHub Actions batch01 12/12 verified unit PASS; empirical validation pending |
| 02.1 | Filtering | PROTOTYPE | research/techniques/02.1/ (15/15 local unit tests; Tank temporal response validation pending) ; GitHub Actions batch01 15/15 verified unit PASS; empirical validation pending |
| 02.2 | Scaling | PROTOTYPE | research/techniques/02.2/ (train-only scaling; real-data validation pending) ; GitHub Actions batch01 13/13 verified unit PASS; empirical validation pending |
| 02.3 | Static features | PROTOTYPE | research/techniques/02.3/ (14 test cases authored; real-data validation pending) ; GitHub Actions batch01 14/14 verified unit PASS; empirical validation pending |
| 02.4 | Temporal features | PROTOTYPE | research/techniques/02.4/ (16/16 synthetic local tests; real time-series validation pending); GitHub Actions batch02 16/16 unit PASS; empirical validation pending |
| 02.5 | Fingerprints | PROTOTYPE | research/techniques/02.5/ (8-sensor log-fold vector; legacy parity and real validation pending); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 02.6 | 3D/4D features | PROTOTYPE | research/techniques/02.6/ (time-aligned trajectories; 16 unit cases authored); GitHub Actions batch02 16/16 unit PASS; empirical validation pending |
| 02.7 | Feature selection | PROTOTYPE | research/techniques/02.7/ (training-only Fisher ranking; 14 tests authored); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 02.8 | Provenance schema | PROTOTYPE | research/techniques/02.8/ (canonical metadata digest; 14 tests authored); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 03.1 | PCA | PROTOTYPE | research/techniques/03.1/ (training-only PCA; 14 tests authored); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 03.2 | Similarity | PROTOTYPE | research/techniques/03.2/ (16/16 local tests; real Tank validation pending); GitHub Actions batch02 16/16 unit PASS; empirical validation pending |
| 03.3 | Class overlap | PROTOTYPE | research/techniques/03.3/ (nearest-neighbor ambiguity; 16 tests authored); GitHub Actions batch02 16/16 unit PASS; empirical validation pending |
| 03.4 | LDA/PLS-DA | PARTIAL_PROTOTYPE | research/techniques/03.4/ (binary-only baselines; multiclass/multicomponent pending); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 03.5 | Sensor importance | PROTOTYPE | research/techniques/03.5/ (training-only univariate ranking; 14 tests authored); GitHub Actions batch02 14/14 unit PASS; empirical validation pending |
| 03.6 | Sensor subsets | PROTOTYPE | research/techniques/03.6/ (exhaustive subset ambiguity; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 03.7 | Concentration confounding | PROTOTYPE | research/techniques/03.7/ (within-gas log-concentration correlation; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 03.8 | Identifiability limits | PROTOTYPE | research/techniques/03.8/ (cross-class near-collision and abstention candidate; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.1 | Curve matching | PROTOTYPE | research/techniques/04.1/ (log-log MQ reference interpolation; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.2 | Fingerprint matching | PROTOTYPE | research/techniques/04.2/ (nearest-reference fingerprint ranking; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.3 | KNN | PROTOTYPE | research/techniques/04.3/ (uniform/inverse-distance voting; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.4 | Logistic regression | PROTOTYPE | research/techniques/04.4/ (multiclass softmax L2; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.5 | SVM | PARTIAL_PROTOTYPE | research/techniques/04.5/ (linear OVR hinge-loss baseline; kernels pending); CI batch03 16/16 unit PASS; G3 pending |
| 04.6 | Random Forest | PROTOTYPE | research/techniques/04.6/ (bagged Gini trees; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.7 | XGBoost | PARTIAL_PROTOTYPE | research/techniques/04.7/ (simplified gradient-boosted stumps, NOT XGBoost; 16 tests authored); CI batch03 16/16 unit PASS; G3 pending |
| 04.8 | MLP | PROTOTYPE | research/techniques/04.8/ (single-hidden-layer tanh/softmax network; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 04.9 | Physics+ML | PARTIAL_PROTOTYPE | research/techniques/04.9/ (candidate-conditioned physics residual features only; no fitted ML model; 16 synthetic tests authored, not verified executed); CI batch04 16/16 unit PASS; G3 pending |
| 04.10 | Benchmarking | PROTOTYPE | research/techniques/04.10/ (grouped holdout and descriptive metrics; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.1 | Single-sensor calibration | PROTOTYPE | research/techniques/05.1/ (known-gas log-log calibration; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.2 | Multipoint calibration | PROTOTYPE | research/techniques/05.2/ (monotone piecewise log-log interpolation; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.3 | Nonlinear regression | PROTOTYPE | research/techniques/05.3/ (quadratic log-log calibration; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.4 | Multisensor regression | PROTOTYPE | research/techniques/05.4/ (known-gas multichannel ridge regression; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.5 | PLS regression | PROTOTYPE | research/techniques/05.5/ (known-gas PLS1 latent components; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.6 | Tree regression | PROTOTYPE | research/techniques/05.6/ (bagged CART log-ppm regression; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.7 | Identity-conditional estimates | PROTOTYPE | research/techniques/05.7/ (confidence/margin gate and gas-specific calibrator routing; 16 tests authored); CI batch04 16/16 unit PASS; G3 pending |
| 05.8 | LOD/LOQ | PARTIAL_PROTOTYPE | research/techniques/05.8/ (blank-sigma screening only; not validated ppm LOD/LOQ; 16 tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 05.9 | Unseen concentrations | PROTOTYPE | research/techniques/05.9/ (held-out ppm levels, interpolation/extrapolation metrics and abstention; 16 tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 06.1 | Evidence fusion | PARTIAL_PROTOTYPE | research/techniques/06.1/ (dependence-group log opinion pooling; 16 tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 06.2 | Bayesian inference | PARTIAL_PROTOTYPE | research/techniques/06.2/ (supplied priors and likelihoods; conditional independence assumption; 16 tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 06.3 | Probability calibration | PARTIAL_PROTOTYPE | research/techniques/06.3/ (temperature scaling and ECE; 16 tests authored, not run); CI batch05 16/16 unit PASS; G3 pending |
| 06.4 | Conformal prediction | PARTIAL_PROTOTYPE | research/techniques/06.4/ (split conformal closed-world candidate sets; 16 tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 06.5 | Open-set recognition | PARTIAL_PROTOTYPE | research/techniques/06.5/ (nearest-centroid open-set baseline; 16 synthetic tests authored); CI batch05 16/16 unit PASS; G3 pending |
| 06.6 | OOD detection | PARTIAL_PROTOTYPE | research/techniques/06.6/ (minimal baseline; smoke tests authored; real validation pending); CI batch05 15/15 unit PASS; G3 pending |
| 06.7 | Evidence conflicts | PARTIAL_PROTOTYPE | research/techniques/06.7/ (minimal baseline; smoke tests authored; real validation pending); CI batch05 16/16 unit PASS; G3 pending |
| 06.8 | SHAP | PARTIAL_PROTOTYPE | research/techniques/06.8/ (minimal baseline; smoke tests authored; real validation pending); CI batch05 15/15 unit PASS; G3 pending |
| 06.9 | Abstention | PARTIAL_PROTOTYPE | research/techniques/06.9/ (minimal baseline; smoke tests authored; real validation pending); CI batch06 18/18 behavioral tests PASS; device/gas G3 pending |
| 07.1 | Cross-session tests | PARTIAL_PROTOTYPE | research/techniques/07.1/ (minimal baseline; smoke tests authored; real validation pending); CI batch06 18/18 behavioral tests PASS; device/gas G3 pending |
| 07.2 | Cross-device tests | PARTIAL_PROTOTYPE | research/techniques/07.2/ (minimal baseline; smoke tests authored, not executed); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.3 | Cross-concentration tests | PARTIAL_PROTOTYPE | research/techniques/07.3/ (minimal baseline; smoke tests authored, not executed); CI batch06 15/15 behavioral tests PASS; device/gas G3 pending |
| 07.4 | Drift detection | PARTIAL_PROTOTYPE | research/techniques/07.4/ (minimal baseline; smoke tests authored, not executed); CI batch06 18/18 behavioral tests PASS; device/gas G3 pending |
| 07.5 | Drift compensation | PARTIAL_PROTOTYPE | research/techniques/07.5/ (minimal baseline; smoke tests authored, not executed); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.6 | Domain adaptation | PARTIAL_PROTOTYPE | research/techniques/07.6/ (minimal baseline; smoke tests authored, not executed); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.7 | Transfer learning | PARTIAL_PROTOTYPE | research/techniques/07.7/ (minimal baseline; synthetic smoke tests authored); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.8 | Active learning/DoE | PARTIAL_PROTOTYPE | research/techniques/07.8/ (minimal baseline; synthetic smoke tests authored); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.9 | Continual learning | PARTIAL_PROTOTYPE | research/techniques/07.9/ (minimal baseline; synthetic smoke tests authored); CI batch06 17/17 behavioral tests PASS; device/gas G3 pending |
| 07.10 | Resource profiling | PARTIAL_PROTOTYPE | research/techniques/07.10/ (minimal baseline; synthetic smoke tests authored); CI batch07 15/15 behavior checks PASS; research primitive, G3 pending |
| 08.1 | 1D-CNN | PARTIAL_PROTOTYPE | research/techniques/08.1/ (minimal baseline; synthetic smoke tests authored); CI batch07 16/16 behavior checks PASS; research primitive, G3 pending |
| 08.2 | TCN | PARTIAL_PROTOTYPE | research/techniques/08.2/ (minimal primitives; synthetic smoke tests authored); CI batch07 16/16 behavior checks PASS; research primitive, G3 pending |
| 08.3 | LSTM/GRU | PARTIAL_PROTOTYPE | research/techniques/08.3/ (minimal primitives; synthetic smoke tests authored); CI batch07 17/17 behavior checks PASS; research primitive, G3 pending |
| 08.4 | GNN | PARTIAL_PROTOTYPE | research/techniques/08.4/ (minimal primitives; synthetic smoke tests authored); CI batch07 16/16 behavior checks PASS; research primitive, G3 pending |
| 08.5 | Temporal ensemble | PARTIAL_PROTOTYPE | research/techniques/08.5/ (minimal primitives; synthetic smoke tests authored); CI batch07 17/17 behavior checks PASS; research primitive, G3 pending |
| 08.6 | Simulation | PARTIAL_PROTOTYPE | research/techniques/08.6/ (minimal primitives; synthetic smoke tests authored); CI batch07 16/16 behavior checks PASS; research primitive, G3 pending |
| 08.7 | Sim-to-real | PARTIAL_PROTOTYPE | research/techniques/08.7/ (minimal baseline; synthetic smoke tests authored); CI batch07 15/15 behavior checks PASS; research primitive, G3 pending |
| 08.8 | Mixture detection | PARTIAL_PROTOTYPE | research/techniques/08.8/ (minimal baseline; synthetic smoke tests authored); CI batch07 17/17 behavior checks PASS; research primitive, G3 pending |
| 08.9 | Unmixing | PARTIAL_PROTOTYPE | research/techniques/08.9/ (minimal baseline; synthetic smoke tests authored); CI batch07 17/17 behavior checks PASS; research primitive, G3 pending |
| 08.10 | Multi-analyte regression | PARTIAL_PROTOTYPE | research/techniques/08.10/ (minimal baseline; synthetic smoke tests authored); CI batch08 17/17 behavioral unit PASS; research-only, G3 not validated |
| 08.11 | Interference | PARTIAL_PROTOTYPE | research/techniques/08.11/ (minimal baseline; synthetic smoke tests authored); CI batch08 15/15 behavioral unit PASS; research-only, G3 not validated |
| 08.12 | Complex products | PARTIAL_PROTOTYPE | research/techniques/08.12/ (minimal baseline; synthetic smoke tests authored); CI batch08 16/16 behavioral unit PASS; research-only, G3 not validated |
| 09.1 | Robotic olfaction | PARTIAL_PROTOTYPE | research/techniques/09.1/ (minimal baseline; synthetic smoke tests authored); CI batch08 16/16 behavioral unit PASS; research-only, G3 not validated |
| 09.2 | Gas mapping | PARTIAL_PROTOTYPE | research/techniques/09.2/ (minimal baseline; synthetic smoke tests authored); CI batch08 16/16 behavioral unit PASS; research-only, G3 not validated |
| 09.3 | Source localization | PARTIAL_PROTOTYPE | research/techniques/09.3/ (minimal baseline; synthetic smoke tests authored); CI batch08 16/16 behavioral unit PASS; research-only, G3 not validated |
| 09.4 | Active sampling | PARTIAL_PROTOTYPE | research/techniques/09.4/ (minimal baseline; synthetic smoke tests authored); CI batch08 16/16 behavioral unit PASS; research-only, G3 not validated |
| 09.5 | Spatiotemporal fusion | PARTIAL_PROTOTYPE | research/techniques/09.5/ (minimal baseline; synthetic smoke tests authored); CI batch08 17/17 behavioral unit PASS; research-only, G3 not validated |
| 09.6 | Motion decisions | PARTIAL_PROTOTYPE | research/techniques/09.6/ (minimal baseline; synthetic smoke tests authored); CI batch08 18/18 behavioral unit PASS; research-only, G3 not validated |

Update rows only with verified evidence, preserve historical decisions, and update RESUME.md each session.

Additional research proposal: [Negative Evidence & Sensor-Guided Exclusion](NEGATIVE_EVIDENCE_PROPOSAL.md) — PROPOSED, to be studied with 06.1/06.2/06.7/06.9; no hard chemical-family elimination.

## Technique 80 — new research extension

| ID | Technique | Status | Evidence |
|---|---|---|---|
| 10.1 | Negative Evidence & Sensor-Guided Exclusion | PARTIAL_PROTOTYPE | research/techniques/10.1/ (8/8 local synthetic unit tests executed; sensor detection-power calibration, correlated evidence correction and Tank validation pending); CI batch08 25/25 behavioral unit PASS; research-only, G3 not validated |

**Scope of 10.1:** calibrated sensor-specific non-detection likelihoods conditional on chemical identity, concentration, sensor detection capability, response latency, environmental conditions, cross-sensitivity and quality flags. Output should down-weight supported hypotheses, not hard-eliminate chemical families. Requires real blank/positive controls and grouped validation before use. Prior research primitive in 06.7 does not make 10.1 complete.

**Audit outcome:** 80 items are **on the roadmap**, not 80 validated or fully implemented techniques. All preexisting ledger entries retain their evidence tier. Technique 04.9 correction reconciles ledger with its actual README and RESUME history. The reference benchmark starter is not proof of any 79/80-technique execution or accuracy.

## First empirical source-data benchmark — IMPORTANT NON-PROMOTION (2026-10-19)

Evidence: [G2_REAL_DATA_BATCH01_AR.md](G2_REAL_DATA_BATCH01_AR.md), source doi 10.24432/C5CK6F, verified action runs [classification](https://github.com/HazRob/HazRob-Tank/actions/runs/39180196732) and [diagnostics](https://github.com/HazRob/HazRob-Tank/actions/runs/39180489713). Ten research modules ran on 90 measured external-array exposures using non-overlapping experimental-row tests and held-out concentration levels. Device and session independence NOT established; the UCI source sensors are not the Tank MQ8 array. Their status remains PROTOTYPE: **G2 partial external reference evidence**, NOT G3 device validation or calibrated gas accuracy. See individual evidence links in PROGRESS_80.json.

## G2-02 experimental reference batch — 2026-10-19

- [G2_REAL_DATA_BATCH02_AR.md](G2_REAL_DATA_BATCH02_AR.md) records 9 research techniques exercised with real external-source measurements, and 02.5 blocked because external 10-channel data are not actual eight-MQ Tank resistance/baseline recordings. The 9 exercised: 03.3,03.6,03.7,03.8,04.10,05.9,06.3,06.5,07.3. **05.9 is concentration-blind metric integration only**, not a calibrated ppm model.
- Evidence: [successful CI #39182059102](https://github.com/HazRob/HazRob-Tank/actions/runs/39182059102), source archive SHA-256 verified, 8/8 new integration/invariant tests passed, structured numerical evidence [here](benchmarks/results/uci1081_g2_batch02_2026-10-19.json).
- Findings include poor open-set rejection of actually unseen gas labels (11/90), and failed transfer of temperature-calibrated softmax across two concentration-holdout test levels. These are research negative findings, **not operational validation**.
- Preserve implementation statuses as before and keep all G3 **NOT_VALIDATED**. No original HazRob, APK, ESP32 or production ChemCore updates.

## G2-03 — physical evidence eligibility (2026-10-19)
Reviewed exactly 10 technique IDs `01.1,01.2,01.3,01.4,01.5,01.6,01.7,02.1,02.3,05.1`. Only generic numerical causal filtering `02.1` can run without interpreting undefined source units. Successful real UCI1081 90-exposure/900-channel-window GitHub Actions run [#39184775420](https://github.com/HazRob/HazRob-Tank/actions/runs/39184775420), 13/13 method tests passed, SHA-verified original source. Remaining nine lack source MQ Rs/R0 or ADC topology, heater schedules and/or BME; their experimental G2 is BLOCKED, software G1 remains PASS. Full empirical and invalid-input risk table [G2_REAL_DATA_BATCH03_AR.md](G2_REAL_DATA_BATCH03_AR.md), machine status [PROGRESS_80.json](PROGRESS_80.json). These are external-array numerical diagnostics and must not promote any technique to G3.

## G2-04 external chronological-drift evidence (2026-10-19)

**Official UCI270:** 13,910 measured external sensor records across ten time batches, 128 precomputed attributes from 16 sensor channels. [Detailed Arabic report](G2_REAL_DATA_BATCH04_AR.md), [commit-pinned numeric summary](benchmarks/results/uci270_g2_batch04_2026-10-19.json), [successful GitHub CI #39201607716](https://github.com/HazRob/HazRob-Tank/actions/runs/39201607716).

- 03.4: actual binary LDA/PLS1DA time-held-out, accuracy 619/1200 and 332/1200, NOT multiclass completed.
- 07.1: ten source temporal-batch proxy folds (NOT true independent session ID); 07.2: BLOCKED one original instrument, cannot hold out an unseen device.
- 07.4,07.5: matched acetone 200 ppm shift and offset checks, 50 separate target test exposures, no physical attribution of drift.
- 07.6: unlabeled marginal alignment, heldout centroid movement diagnostic only.
- 07.7: target-labelled 60-reference nearest-centroid adaptation 923/3540 -> 978/3540; not neural transfer learning.
- 07.8: uncalibrated offline uncertainty ranking; no acquisition decision verified.
- 07.9: sequential labels after evaluation, overall 4327/13465 fixed vs 4980/13465 incremental; severe batch-level variance.
- 07.10: theoretical 448-byte prototype estimate only; NO device RAM, timing, RF/camera interaction or power measurement.

**All above preserve PROTOTYPE/PARTIAL_PROTOTYPE and `empirical_gate=NOT_VALIDATED`.** UCI224 is the same original 13,910-measurement drift source; must not count again. Current external-source registered exposures across UCI1081 and UCI270 = 14,000, all outside Tank MQ8 and with no demonstrated device-transfer qualification.
