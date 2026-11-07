# G2 independent evidence acquisition protocol — 2026-11-06

Status: **research-only protocol; no scientific promotion**. Baseline branch head before this document: `93f0cb26eea545f3efbfc9da7619bff591af5813`.

## Verified starting state
- All 12 commit-associated GitHub Actions PR workflows for the baseline head completed successfully, including physical intake preflight run `42155840592`.
- `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` contains `"records": []`; **zero independently verified physical holdout records**. Software checks are not substitutes for experimental evidence.

## Preregister before acquisition
1. Freeze the candidate technique identifier, code commit, feature extraction, threshold, abstention policy, and acceptance criteria *before* independent test labels are disclosed.
2. Define target analyte, matrix, concentration range, interfering compounds, humidity and temperature range, device population, and intended operating domain. Do not treat one gas/source as evidence for all 80 techniques.
3. Assign immutable acquisition IDs; preserve raw ADC/time-series, R0 method and values, instrument calibration certificates, firmware and board versions, site, date, device, session, exposure and operator metadata. Compute SHA-256 on original raw bytes.
4. Group train/validation/test by acquisition source and correlated device/session/day/exposure units. Hold out entire independent acquisition groups; never split replicates from one exposure across partitions.
5. Include clean air, blanks, known positive exposures, interferents, mixtures where relevant, sensor drift, and out-of-distribution challenges. Record failed acquisitions and missing data without selective deletion.
6. Obtain reference concentration and identity from an independently calibrated measurement method, with stated uncertainty and detection limit. Manufacturer MQ curves and synthetic data are **priors/engineering checks**, not independent ground truth.
7. For identification report per-class support, confusion matrix, macro-F1, precision/recall, false alarm rate, abstention coverage, calibration, and grouped confidence intervals. For concentration report bias, MAE/RMSE, relative error only where defined, calibration slope, uncertainty coverage and concentration-stratified residuals.
8. Compare frozen candidate to frozen baseline on the *same* blind groups, with paired group-level uncertainty intervals. Declare failures and indeterminate outcomes; no threshold tuning on blind test.
9. Run `python3 research/implementation_execution/g2_physical_intake_gate.py --self-test` and then run intake on a real manifest with original raw files. Record command, commit, SHA-256 inventory, workflow run IDs, and outputs.
10. Promote only after all predeclared technique-specific criteria are met by independent evidence and a separate review; CI green alone means **preflight only**.

## Current blocking evidence gap
The independent evidence ledger is empty. No claim of 90% identification accuracy, concentration accuracy, G2 completion, or G3 readiness is justified. Next empirical step: collect and register a first independently calibrated, blinded, grouped physical acquisition bundle, then run the frozen intake and analysis protocol.

No Stable, app, or firmware changes are authorized by this document.
