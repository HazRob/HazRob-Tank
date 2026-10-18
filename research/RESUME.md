# Checkpoint 2026-10-18: 05.1 single-sensor calibration PROTOTYPE committed. Known gas, one MQ sensor, >=3 distinct independently known ppm standards; log-log power-law OLS; no extrapolation; R0/Rs ohms. 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.2 multipoint calibration. No production changes.

# Checkpoint 2026-10-18: 04.10 benchmarking PROTOTYPE committed. Grouped holdout partitioning and descriptive accuracy/coverage/selective accuracy/confusion/per-class metrics for externally supplied predictions; no real benchmark performed. 16 synthetic tests authored, clean checkout and real Tank evaluation pending. Stage 04 research code 04.1–04.10 present, with 04.5/04.7/04.9 PARTIAL_PROTOTYPE; not validated or production-ready. NEXT 05.1 single-sensor calibration. No production changes.

# Checkpoint 2026-10-18: 04.9 Physics + ML PARTIAL_PROTOTYPE committed. Candidate-specific MQ reference residual features at independently known concentration, 16 synthetic tests authored. ML model integration, unknown-concentration handling, clean-checkout test execution and real Tank validation pending. NEXT 04.10 benchmarking. No production changes.

# Checkpoint 2026-10-18: 04.8 MLP PROTOTYPE committed. Single-hidden-layer tanh/softmax network, full-batch backpropagation, train-only standardization, L2, 16 synthetic tests authored. Clean-checkout execution and real Tank validation pending. NEXT 04.9 physics + ML. No production changes.

# Checkpoint 2026-10-18: 04.7 XGBoost investigation PARTIAL_PROTOTYPE committed. Simplified multiclass gradient-boosted decision stumps, explicitly NOT XGBoost; 16 synthetic tests authored, clean checkout and real Tank evaluation pending. True XGBoost objective, Hessian-based splits, trees and validation remain outstanding. NEXT 04.8 MLP. No production changes.

# Checkpoint 2026-10-18: 04.6 Random Forest PROTOTYPE committed. Bootstrapped Gini decision trees with random feature subsets and deterministic seed; 16 synthetic tests authored, clean checkout and real Tank grouped holdout validation pending. NEXT 04.7 XGBoost. No production changes.

# Checkpoint 2026-10-18: 04.5 SVM PARTIAL_PROTOTYPE committed. Linear one-vs-rest hinge-loss subgradient baseline, 16 synthetic tests authored; kernel/RBF SVM and vetted solver comparison pending. Clean checkout and real Tank validation pending. NEXT 04.6 Random Forest. No production changes.

# Checkpoint 2026-10-18: 04.4 logistic regression PROTOTYPE committed. Multiclass softmax full-batch gradient descent, L2 regularization, train-only scaling, 16 synthetic tests authored; clean checkout and real Tank validation pending. Scores uncalibrated; convergence not independently verified. NEXT 04.5 SVM. No production changes.

# Checkpoint 2026-10-18: 04.3 KNN PROTOTYPE committed. Frozen training fingerprints, uniform and inverse-distance voting, deterministic tie flag, 16 synthetic tests authored; clean checkout and Tank grouped holdout evaluation pending. NEXT 04.4 logistic regression. No production changes.

# Checkpoint 2026-10-18: 04.2 fingerprint matching PROTOTYPE committed. Training-only multichannel exemplar library, scaled Euclidean nearest-reference per class and optional abstention threshold; 16 synthetic tests authored. Clean-checkout execution and real Tank held-out evaluation pending. NEXT 04.3 KNN. No production changes.

# Checkpoint 2026-10-18: 04.1 curve matching PROTOTYPE committed. Log-log piecewise interpolation of externally supplied MQ Rs/R0 curves and candidate ranking at independently known concentration; 16 synthetic tests authored, clean checkout pending. No real factory curves or measured Tank datasets bundled; unknown concentration/mixtures not handled. NEXT 04.2 fingerprint matching. No production changes.

# Checkpoint 2026-10-18: 03.8 identifiability limits PROTOTYPE committed. Empirical cross-class near-collision analysis and conservative query screening with uncalibrated distance threshold; 16 synthetic tests authored, clean checkout pending. Stage 03 has eight research implementations (03.4 PARTIAL_PROTOTYPE), no real Tank validation. NEXT 04.1 curve matching. No production changes.

# Checkpoint 2026-10-18: 03.7 concentration confounding PROTOTYPE committed. Within-gas Pearson correlation with log10 independently known ppm and insufficient-data flags; 16 synthetic tests authored, clean checkout pending. Does not establish causality, classification or concentration estimation. NEXT 03.8 identifiability limits. No production changes.

# Checkpoint 2026-10-18: 03.6 sensor subsets PROTOTYPE committed. Exhaustive nonempty sensor combinations and leave-one-out descriptive ambiguity, 16 synthetic tests authored. Selection bias warning; nested grouped CV and real Tank validation pending. NEXT 03.7 concentration confounding. No production changes.

# Checkpoint 2026-10-18: 03.5 sensor importance PROTOTYPE committed. Per-sensor univariate Fisher score, constant flag, train-only ranking and held-out projection; 14 synthetic tests authored, not verified from clean checkout. Requires Tank cross-session/device/concentration validation and ablation studies. NEXT 03.6 sensor subsets. No production changes.

# Checkpoint 2026-10-18: 03.4 LDA/PLS-DA PARTIAL_PROTOTYPE committed: binary ridge LDA and one-component binary PLS1-DA, 14 synthetic tests authored; no clean checkout verification. Full multiclass LDA and multicomponent PLS-DA, reference comparison and real Tank data validation pending. NEXT 03.5 sensor importance; return to complete 03.4 before any final scientific approval. No production changes.

# Checkpoint 2026-10-18: 03.3 class overlap PROTOTYPE committed. Descriptive leave-one-out nearest same/other class ambiguity and margins, 16 tests authored. Clean-checkout execution and controlled Tank cross-session data validation pending. NEXT 03.4 LDA/PLS-DA. No production changes.

# Checkpoint 2026-10-18: 03.2 similarity analysis PROTOTYPE committed. Cosine, Euclidean, Manhattan, scaled Euclidean and nearest reference. 16/16 local synthetic unit tests passed. Clean GitHub checkout and real Tank cross-session/concentration validation pending. NEXT 03.3 class overlap. No production changes.

# Checkpoint 2026-10-18: 03.1 PCA PROTOTYPE committed. Pure Python symmetric Jacobi covariance eigensolver, explained variance and projection, 14 tests authored; clean checkout and numerical reference verification pending. NEXT 03.2 similarity analysis. No production changes.

# Checkpoint 2026-10-18: 02.8 provenance schema PROTOTYPE committed. Metadata validation, UTC, calibration/device/session/sensor and canonical SHA-256 digest; 14 tests authored. Stage 02 now has eight research prototypes; clean checkout and real Tank validation pending. NEXT 03.1 PCA. No production changes.

# Checkpoint 2026-10-18: 02.7 feature selection PROTOTYPE committed. Training-only Fisher-like univariate ranking, deterministic indices, 14 synthetic tests authored; clean checkout and real-data validation pending. NEXT 02.8 provenance schema. No production changes.

# Checkpoint 2026-10-18: 02.6 3D/4D time-aligned response trajectories PROTOTYPE committed; 16 tests authored, clean checkout and real-data verification pending. This is not a reproduction of MQDataScience methods. NEXT: 02.7 feature selection. No production changes.

# Checkpoint 2026-10-18: 02.5 fingerprints PROTOTYPE committed. Ordered 8-sensor log-fold Rs response vector and cosine baseline, 14 test cases authored. Legacy fingerprint_v1 parity NOT verified; real Tank sensor order and heater-phase data pending. NEXT: 02.6 3D/4D features. No production changes.

# Checkpoint 2026-10-18: 02.4 temporal features PROTOTYPE committed, 16/16 synthetic local tests passed. Features include observed-peak rise and recovery times, slope and area. Actual Tank exposure timing and gas validation pending. NEXT: 02.5 fingerprints; inspect existing legacy fingerprint research before writing duplicate algorithms. No production changes.

# Checkpoint 2026-10-18: 02.3 static feature extraction PROTOTYPE committed; baseline/exposure Rs summary, relative and log-fold changes, 14 test cases authored. Clean-checkout test run and real Tank validation pending. NEXT: 02.4 temporal features. No production changes.

# Checkpoint 2026-10-18: 02.2 scaling PROTOTYPE committed under research/techniques/02.2/. Standard and robust scalers fit on training data only; 13 test cases authored but clean-checkout run not verified. Next sequential item 02.3 static features. Tank research only, no production changes.

# Checkpoint 2026-10-18: 02.1 filtering PROTOTYPE committed. Three causal filters (median, moving average, EMA), 15/15 local tests passed. Need real Tank time-series and clean checkout validation. Next 02.2 scaling. Tank research only; no production changes.

# Checkpoint 2026-10-18: 01.7 baseline drift monitoring prototype committed (robust median and log-change, stability gate; no automatic R0 changes). Twelve test cases authored, clean-checkout execution pending. Stage 01 research prototypes 01.1–01.7 now recorded; none chemically or hardware validated. NEXT sequential item 02.1 signal filtering. No production changes.

# Checkpoint 2026-10-18: 01.6 signal-quality prototype committed (missing data, saturation, out-of-range, insufficient data, MAD diagnostics). 13 test cases authored, not yet rerun in clean checkout. Hardware ADC configuration and real Tank data validation pending. NEXT sequential item 01.7 drift monitoring. No production changes.

# Checkpoint 2026-10-18: 01.5 environmental correction research prototype committed under research/techniques/01.5/. BME280 temperature, RH, pressure conditional normalization; coefficients must be fit from controlled data. Twelve test cases authored but clean checkout not yet run. No field validation or production changes. NEXT: 01.6 signal quality.

# Checkpoint 2026-10-18: 01.4 heater-phase segmentation prototype committed with 12 software test cases; test execution and physical heater profiles remain unverified. NEXT: 01.5 environmental correction (temperature/humidity/pressure) with BME280 and real calibration data; avoid unvalidated compensation coefficients. Tank only; no production changes.

# Research checkpoint 2026-10-18
01.1 ADC-to-Rs, 01.2 R0 and 01.3 sensitivity curves have research prototypes committed to Tank. 01.2 hardware calibration and 01.3 manufacturer-curve validation remain pending. Next sequential item: 01.4 heater cycles, especially MQ7 and MQ9. No production changes.

# Checkpoint update 2026-10-18
01.1 ADC-to-Rs: PROTOTYPE. Source, 11 unit tests, and README committed under research/techniques/01.1/. Actual Tank hardware topology/ADS1115 gain/RL not verified. Next sequential item: 01.2 R0 calibration audit; first inspect legacy code and verify sensor heater cycles, R0 units, calibration gas and actual data. No production changes.

# EXCLUSIVE SCOPE: HazRob Tank ChemCore
Never resume this roadmap in HazRob-Lab or the original HazRob repository. Current owner: HazRob/HazRob-Tank. Historical evidence links in HazRob-Lab are references only, not Tank implementation.

# ChemCore — resume checkpoint
Date 2026-10-18 | Roadmap 1.0 | Repo HazRob/HazRob-Tank

## NEXT
01.1 completed as research PROTOTYPE (not hardware validated); next 01.2 R0 calibration audit. Legacy results exist but have not been independently audited for these gates.

## Exact next actions
1. Read research/MASTER_PLAN.md and research/STATUS_LEDGER.md.
2. Inspect latest connected firmware and ChemCore measurement code for ADS1115 and ESP32 ADC scaling, RL, R0, reference voltages, attenuations, divider topology, units, clipping.
3. Verify real MQ board wiring and sensor-specific heater modes; derive and test Rs equations.
4. Create dated experiment log, code tests, decision, update ledger and this checkpoint with last commit and unresolved issues.

## Legacy pointers
references/tech12_nested_subset_validation_2026-10-18.md; references/tech13_evidence_fusion_prototype_2026-10-18.md; references/tech14_real_data_generalization_audit_2026-10-18.md; references/extracted_analytical_equations_batch01_2026-10-18.md.

## Restrictions
No production ChemCore/APK/firmware modifications. No claim of empirical validation from unit tests alone. Do not merge HazRob Tank with original HazRob.

## Prompt for a new conversation
استأنف خارطة ChemCore البحثية من HazRob/HazRob-Tank. اقرأ research/RESUME.md ثم research/MASTER_PLAN.md وresearch/STATUS_LEDGER.md. نفذ البند التالي بعد مراجعة الأدلة السابقة، ووثق الشيفرة والاختبارات والقرار وحدّث نقطة الاستئناف. لا تعدل الإصدارات الإنتاجية.
