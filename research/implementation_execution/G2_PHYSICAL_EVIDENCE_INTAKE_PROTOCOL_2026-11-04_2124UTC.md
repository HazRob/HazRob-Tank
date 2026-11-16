# G2 research checkpoint and physical-evidence intake protocol — 2026-11-04 21:24 UTC

Scope: research branch only. Parent checkpoint: 42422a2348dbc4eea7e299389ef7e2cf13669074. This record does not qualify any technique and must not alter ChemCore Stable.

## Verified CI at parent commit
GitHub Actions branch query returned 21 completed successful workflow runs for head 42422a23: 41906265724, 41906265674, 41906265676, 41906265784, 41906265717, 41906265905, 41906265760, 41906265734, 41906265605, 41906265643, 41906265670, 41906252535, 41906252635, 41906252462, 41906252527, 41906252551, 41906252729, 41906252638, 41906252663, 41906252461, 41906252524.

Inspected run 41906265605, job 126841618247. All reported steps succeeded: Python compilation, pinned MQ4 lineage/4-ppm source-reference checks, MQ-EXT-10 liquid-v/v source guards, offline G2 regression suite, 80-technique fail-closed audit, deterministic triage, byte-identical replay and publication lineage. This is a software/provenance result only. It does not demonstrate calibrated Tank ppm or independent physical-session validation.

## Reproducible physical evidence intake (required before G2 promotion)
For every source, preserve original immutable bytes and SHA-256, archive URL/DOI, retrieval timestamp, license, device model and serial/board identifier, sensor model and serial, firmware revision, ADC resolution/reference voltage, supply and heater voltage, circuit load resistance, raw ADC-to-voltage conversion, temperature/humidity, sensor warm-up and sampling cadence, calibration gas identity, concentration value **and physical units**, traceable concentration standard and uncertainty, exposure start/stop timestamps, session/run/day/device/site IDs, and blank/control conditions. Explicitly mark unknown fields as UNKNOWN, not inferred.

Prespecify grouped train/validation/test splits by physical session and device/day/site where applicable. All readings from the same physical exposure, overlapping temporal windows, calibration sequence, or shared raw file must remain in one partition. Detect content duplicates by SHA-256 and investigate near-duplicates by lineage; distinct file hashes alone do not prove independent experiments. Hold out at least one genuinely independent device/session cohort and record its identities before model fitting. Never select thresholds on the test set.

Report per-technique classification confusion matrices, macro-F1, abstention/unknown rate, per-family error, calibration reliability, and confidence intervals; for concentration regression report MAE/RMSE, bias, interval coverage and errors by concentration/humidity/temperature strata. Record prespecified pass criteria and negative controls before observing test labels. Reject unsupported conversions: liquid ethanol %v/v is not vapor ppm; external MQ4 4-ppm reference is not Tank ppm calibration. A single benchmark or green CI run cannot substitute for independent evidence.

## Current scientific status
Last recorded tally (not recomputed): g2_pass_candidates=0; g2_blocked=80; source_blocked=14. MQ-EXT-01 historical CI archive receipt does not replace a new byte-level source verification. Required physical raw data, source-unit reconciliation, independent grouped holdouts and uncertainty evidence remain outstanding. Do not promote G2/G3 without all required evidence.

## Reproduction pointers
- Workflow run: https://github.com/HazRob/HazRob-Tank/actions/runs/41906265605
- Job: 126841618247
- Branch: research/chemcore80-central-evidence-gap-analysis-20261021
- Repeat the branch's G2 gate workflow against pinned inputs and compare output hashes; preserve failed or missing source statuses instead of substituting simulations.
