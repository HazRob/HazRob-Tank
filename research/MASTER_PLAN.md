# SCOPE: HAZROB TANK CHEMCORE ONLY
This roadmap belongs exclusively to the independent HazRob Tank project. It does NOT authorize development or changes to the original HazRob ChemCore. External HazRob-Lab studies are historical research evidence only and must be reviewed and explicitly imported before use.

# HazRob ChemCore — Scientific execution plan
Version 1.0 | 2026-10-18 | Research only

## Dependencies
01 measurement -> 02 signal processing -> 03 identifiability -> 04 classification and 05 quantification in parallel -> 06 uncertainty and evidence fusion. Stage 07 validation starts from day one and continues throughout; advanced adaptation follows baselines. 08 temporal neural and mixture models requires validated baselines; 09 robotic mapping needs pose/time/airflow ground truth. Revisit prior stages whenever evidence warrants.

## Complete inventory: 79 techniques across 9 stages

### Stage 01 — Measurement
- 01.1 ADC to Rs
- 01.2 R0 calibration
- 01.3 Sensitivity curves
- 01.4 Heater cycles
- 01.5 Environmental correction
- 01.6 Signal quality
- 01.7 Drift monitoring

### Stage 02 — Signal and features
- 02.1 Filtering
- 02.2 Scaling
- 02.3 Static features
- 02.4 Temporal features
- 02.5 Fingerprints
- 02.6 3D/4D features
- 02.7 Feature selection
- 02.8 Provenance schema

### Stage 03 — Identifiability
- 03.1 PCA
- 03.2 Similarity
- 03.3 Class overlap
- 03.4 LDA/PLS-DA
- 03.5 Sensor importance
- 03.6 Sensor subsets
- 03.7 Concentration confounding
- 03.8 Identifiability limits

### Stage 04 — Classification
- 04.1 Curve matching
- 04.2 Fingerprint matching
- 04.3 KNN
- 04.4 Logistic regression
- 04.5 SVM
- 04.6 Random Forest
- 04.7 XGBoost
- 04.8 MLP
- 04.9 Physics+ML
- 04.10 Benchmarking

### Stage 05 — Quantification
- 05.1 Single-sensor calibration
- 05.2 Multipoint calibration
- 05.3 Nonlinear regression
- 05.4 Multisensor regression
- 05.5 PLS regression
- 05.6 Tree regression
- 05.7 Identity-conditional estimates
- 05.8 LOD/LOQ
- 05.9 Unseen concentrations

### Stage 06 — Decision uncertainty
- 06.1 Evidence fusion
- 06.2 Bayesian inference
- 06.3 Probability calibration
- 06.4 Conformal prediction
- 06.5 Open-set recognition
- 06.6 OOD detection
- 06.7 Evidence conflicts
- 06.8 SHAP
- 06.9 Abstention

### Stage 07 — Robustness
- 07.1 Cross-session tests
- 07.2 Cross-device tests
- 07.3 Cross-concentration tests
- 07.4 Drift detection
- 07.5 Drift compensation
- 07.6 Domain adaptation
- 07.7 Transfer learning
- 07.8 Active learning/DoE
- 07.9 Continual learning
- 07.10 Resource profiling

### Stage 08 — Advanced modeling
- 08.1 1D-CNN
- 08.2 TCN
- 08.3 LSTM/GRU
- 08.4 GNN
- 08.5 Temporal ensemble
- 08.6 Simulation
- 08.7 Sim-to-real
- 08.8 Mixture detection
- 08.9 Unmixing
- 08.10 Multi-analyte regression
- 08.11 Interference
- 08.12 Complex products

### Stage 09 — Robotic olfaction
- 09.1 Robotic olfaction
- 09.2 Gas mapping
- 09.3 Source localization
- 09.4 Active sampling
- 09.5 Spatiotemporal fusion
- 09.6 Motion decisions

## Required workflow for EACH technique
1. Define scientific question, sensor physics, inputs, units, outputs, assumptions and failure modes.
2. Audit data provenance, license, calibration, heater protocol, gas/concentration/session/environment metadata and leakage.
3. Predefine baseline, split strategy, primary metrics, reproducibility seeds and computational budget.
4. Implement isolated versioned research source and unit tests; do not change production.
5. Validate on real data where available, fit preprocessing on training folds only, nested select models/subsets, use held-out days/devices/concentrations where possible; report uncertainty, confusion and per-gas errors.
6. Audit cross-sensitivity, open-set errors, and separation of identity, concentration and safety claims.
7. Record decision ACCEPT_RESEARCH / NEEDS_DATA / BLOCKED / FAILED; production qualification is a separate explicit approval.
8. Commit source, tests, results, experiment log, update STATUS_LEDGER.md and RESUME.md in same session.

## Statuses
NOT_STARTED; IN_PROGRESS; PROTOTYPE; VALIDATING; ACCEPT_RESEARCH; NEEDS_DATA; BLOCKED; FAILED; PRODUCTION_QUALIFIED. Previous work is LEGACY_UNMAPPED until re-audited; passing unit tests alone does not constitute empirical validation.

## Existing research references — NOT auto-completed
references/tech12_nested_subset_validation_2026-10-18.md (03.6); references/tech13_evidence_fusion_prototype_2026-10-18.md (06.1); references/tech14_real_data_generalization_audit_2026-10-18.md (06.5/07.3); references/extracted_analytical_equations_batch01_2026-10-18.md (01.3/05.1). Verify files and methods before promoting status.

## Limitations and restrictions
UCI 1081 has 10 sensors and 90 experiments; it is not the HazRob 8-MQ array (MQ2 MQ3 MQ4 MQ5 MQ7 MQ8 MQ9 MQ135). MQ7/MQ9 heater cycles require special treatment. No extrapolation from replicate CV to independent field performance. Do not edit production ChemCore, APK or firmware; HazRob Tank stays independent.

## Per-item deliverables
Question; data manifest; baseline; source/tests; held-out results or explicit data gap; decision; verifiable commit; ledger row; RESUME checkpoint.
