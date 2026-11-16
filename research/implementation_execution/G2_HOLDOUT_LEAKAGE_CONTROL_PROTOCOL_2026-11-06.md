# G2 independent evidence: preregistered holdout and leakage-control protocol (2026-11-06)

Status: RESEARCH ONLY; no G2/G3 scientific promotion. The independent evidence register currently has `records: []`.

## Evidence ingestion contract
Every independently measured record must include: immutable source identifier and license; raw file path and SHA-256; measurement timestamp (UTC), device ID, sensor model/serial, firmware and ADC configuration; ambient temperature, relative humidity, supply voltage; calibration R0 provenance and date; known analyte identity and reference concentration with units and uncertainty; sampling/blank/warmup durations; replicate and exposure-series IDs; chamber, operator, session, and batch IDs; and traceable reference instrument/method where quantitative concentration is claimed. Missing fields are recorded as missing, not fabricated or imputed across holdout partitions.

## Before any tuning
1. Freeze a source manifest with file hashes and a prespecified task list for each of 80 techniques.
2. Deduplicate by raw hash, exposure-series ID, and near-identical timestamps/trajectories. Keep related observations in one group.
3. Allocate groups (not individual rows) to train, validation, and sealed test partitions. Prefer device/day/source-level external holdout; no overlap of reference curves, repeated captures, or calibration episodes between partitions.
4. Freeze feature transformations, R0 selection rules, thresholds, candidate universe, exclusion/abstention logic and scoring scripts using training/validation only. Do not inspect test labels for tuning.
5. Run baseline comparisons on the same sealed groups, with per-compound and per-concentration strata. Record seed, commit SHA, environment, exact commands, input hashes, and outputs.

## Minimum evaluation outputs
For identification: confusion matrix, top-1/top-k accuracy, macro-F1, per-class recall, unknown-class false-accept rate, abstention/coverage tradeoff, calibration error and 95% group-bootstrap confidence intervals. For quantitative predictions: MAE, RMSE, bias, relative error where valid, calibration slope, limits of quantification, and concentration-stratified error. Report missing/unsupported classes and failures explicitly; never pool synthetic, literature-curve, and measured hardware data into one empirical score.

## Gate
CI green means only that checks ran successfully. Promotion to empirical G2 requires independently measured, provenance-complete, non-leaking holdout evidence and prespecified acceptance criteria, reviewed independently. G3 requires separate device/chamber validation. Zero records means no scientific qualification regardless of workflow success.

## Reproducibility checklist
- Verify SHA-256 before parsing; reject malformed JSON, duplicate keys, NaN/Inf, numeric over/underflow.
- Preserve untouched raw files; derive normalized tables into separate versioned outputs.
- Produce machine-readable split manifest, metrics JSON, failure-case CSV, and execution log.
- Record all exclusions with reasons and sample counts; never silently discard failures.
- Block any script from writing to Stable or production firmware/application paths.

## CI observation
Commit `9ea1fb0a5f43f35f2d3f204e5bbe2f3f3d2e5e98`: 12 pull-request workflow runs returned by connector, all completed with conclusion success. This is a scoped observation, not a complete repository-wide CI audit.
