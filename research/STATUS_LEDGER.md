# ChemCore status ledger
Version 1.0 | 2026-10-18 | 79 items. Legacy research not automatically complete.

| ID | Technique | Status | Evidence |
|---|---|---|---|
| 01.1 | ADC to Rs | PROTOTYPE | research/techniques/01.1/ (11/11 local unit tests; hardware audit pending) |
| 01.2 | R0 calibration | PROTOTYPE | research/techniques/01.2/ (hardware calibration pending) |
| 01.3 | Sensitivity curves | PROTOTYPE | research/techniques/01.3/ (synthetic tests; manufacturer curves pending) |
| 01.4 | Heater cycles | PROTOTYPE | research/techniques/01.4/ (software phase tagging; physical heater control unverified) |
| 01.5 | Environmental correction | PROTOTYPE | research/techniques/01.5/ (synthetic tests; real controlled exposures needed) |
| 01.6 | Signal quality | PROTOTYPE | research/techniques/01.6/ (13 unit cases authored; Tank ADC hardware validation pending) |
| 01.7 | Drift monitoring | PROTOTYPE | research/techniques/01.7/ (12 unit cases authored; controlled baseline validation pending) |
| 02.1 | Filtering | PROTOTYPE | research/techniques/02.1/ (15/15 local unit tests; Tank temporal response validation pending) |
| 02.2 | Scaling | PROTOTYPE | research/techniques/02.2/ (13 unit cases authored; clean-checkout and real-data validation pending) |
| 02.3 | Static features | PROTOTYPE | research/techniques/02.3/ (14 test cases authored; real-data validation pending) |
| 02.4 | Temporal features | PROTOTYPE | research/techniques/02.4/ (16/16 synthetic local tests; real time-series validation pending) |
| 02.5 | Fingerprints | PROTOTYPE | research/techniques/02.5/ (8-sensor log-fold vector; legacy parity and real validation pending) |
| 02.6 | 3D/4D features | PROTOTYPE | research/techniques/02.6/ (time-aligned trajectories; 16 unit cases authored) |
| 02.7 | Feature selection | PROTOTYPE | research/techniques/02.7/ (training-only Fisher ranking; 14 tests authored) |
| 02.8 | Provenance schema | PROTOTYPE | research/techniques/02.8/ (canonical metadata digest; 14 tests authored) |
| 03.1 | PCA | PROTOTYPE | research/techniques/03.1/ (training-only PCA; 14 tests authored) |
| 03.2 | Similarity | PROTOTYPE | research/techniques/03.2/ (16/16 local tests; real Tank validation pending) |
| 03.3 | Class overlap | PROTOTYPE | research/techniques/03.3/ (nearest-neighbor ambiguity; 16 tests authored) |
| 03.4 | LDA/PLS-DA | PARTIAL_PROTOTYPE | research/techniques/03.4/ (binary-only baselines; multiclass/multicomponent pending) |
| 03.5 | Sensor importance | PROTOTYPE | research/techniques/03.5/ (training-only univariate ranking; 14 tests authored) |
| 03.6 | Sensor subsets | PROTOTYPE | research/techniques/03.6/ (exhaustive subset ambiguity; 16 tests authored) |
| 03.7 | Concentration confounding | PROTOTYPE | research/techniques/03.7/ (within-gas log-concentration correlation; 16 tests authored) |
| 03.8 | Identifiability limits | PROTOTYPE | research/techniques/03.8/ (cross-class near-collision and abstention candidate; 16 tests authored) |
| 04.1 | Curve matching | PROTOTYPE | research/techniques/04.1/ (log-log MQ reference interpolation; 16 tests authored) |
| 04.2 | Fingerprint matching | PROTOTYPE | research/techniques/04.2/ (nearest-reference fingerprint ranking; 16 tests authored) |
| 04.3 | KNN | PROTOTYPE | research/techniques/04.3/ (uniform/inverse-distance voting; 16 tests authored) |
| 04.4 | Logistic regression | PROTOTYPE | research/techniques/04.4/ (multiclass softmax L2; 16 tests authored) |
| 04.5 | SVM | PARTIAL_PROTOTYPE | research/techniques/04.5/ (linear OVR hinge-loss baseline; kernels pending) |
| 04.6 | Random Forest | PROTOTYPE | research/techniques/04.6/ (bagged Gini trees; 16 tests authored) |
| 04.7 | XGBoost | PARTIAL_PROTOTYPE | research/techniques/04.7/ (simplified gradient-boosted stumps, NOT XGBoost; 16 tests authored) |
| 04.8 | MLP | PROTOTYPE | research/techniques/04.8/ (single-hidden-layer tanh/softmax network; 16 tests authored) |
| 04.9 | Physics+ML | NOT_STARTED | — |
| 04.10 | Benchmarking | PROTOTYPE | research/techniques/04.10/ (grouped holdout and descriptive metrics; 16 tests authored) |
| 05.1 | Single-sensor calibration | PROTOTYPE | research/techniques/05.1/ (known-gas log-log calibration; 16 tests authored) |
| 05.2 | Multipoint calibration | PROTOTYPE | research/techniques/05.2/ (monotone piecewise log-log interpolation; 16 tests authored) |
| 05.3 | Nonlinear regression | PROTOTYPE | research/techniques/05.3/ (quadratic log-log calibration; 16 tests authored) |
| 05.4 | Multisensor regression | PROTOTYPE | research/techniques/05.4/ (known-gas multichannel ridge regression; 16 tests authored) |
| 05.5 | PLS regression | PROTOTYPE | research/techniques/05.5/ (known-gas PLS1 latent components; 16 tests authored) |
| 05.6 | Tree regression | NOT_STARTED | — |
| 05.7 | Identity-conditional estimates | NOT_STARTED | — |
| 05.8 | LOD/LOQ | NOT_STARTED | — |
| 05.9 | Unseen concentrations | NOT_STARTED | — |
| 06.1 | Evidence fusion | NOT_STARTED | — |
| 06.2 | Bayesian inference | NOT_STARTED | — |
| 06.3 | Probability calibration | NOT_STARTED | — |
| 06.4 | Conformal prediction | NOT_STARTED | — |
| 06.5 | Open-set recognition | NOT_STARTED | — |
| 06.6 | OOD detection | NOT_STARTED | — |
| 06.7 | Evidence conflicts | NOT_STARTED | — |
| 06.8 | SHAP | NOT_STARTED | — |
| 06.9 | Abstention | NOT_STARTED | — |
| 07.1 | Cross-session tests | NOT_STARTED | — |
| 07.2 | Cross-device tests | NOT_STARTED | — |
| 07.3 | Cross-concentration tests | NOT_STARTED | — |
| 07.4 | Drift detection | NOT_STARTED | — |
| 07.5 | Drift compensation | NOT_STARTED | — |
| 07.6 | Domain adaptation | NOT_STARTED | — |
| 07.7 | Transfer learning | NOT_STARTED | — |
| 07.8 | Active learning/DoE | NOT_STARTED | — |
| 07.9 | Continual learning | NOT_STARTED | — |
| 07.10 | Resource profiling | NOT_STARTED | — |
| 08.1 | 1D-CNN | NOT_STARTED | — |
| 08.2 | TCN | NOT_STARTED | — |
| 08.3 | LSTM/GRU | NOT_STARTED | — |
| 08.4 | GNN | NOT_STARTED | — |
| 08.5 | Temporal ensemble | NOT_STARTED | — |
| 08.6 | Simulation | NOT_STARTED | — |
| 08.7 | Sim-to-real | NOT_STARTED | — |
| 08.8 | Mixture detection | NOT_STARTED | — |
| 08.9 | Unmixing | NOT_STARTED | — |
| 08.10 | Multi-analyte regression | NOT_STARTED | — |
| 08.11 | Interference | NOT_STARTED | — |
| 08.12 | Complex products | NOT_STARTED | — |
| 09.1 | Robotic olfaction | NOT_STARTED | — |
| 09.2 | Gas mapping | NOT_STARTED | — |
| 09.3 | Source localization | NOT_STARTED | — |
| 09.4 | Active sampling | NOT_STARTED | — |
| 09.5 | Spatiotemporal fusion | NOT_STARTED | — |
| 09.6 | Motion decisions | NOT_STARTED | — |

Update rows only with verified evidence, preserve historical decisions, and update RESUME.md each session.
