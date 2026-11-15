# G2 blinded holdout execution checklist (research only)

Status: BLOCKED until independently acquired physical evidence is registered. This is an execution checklist, not a qualification certificate.

## Pre-registration (before collecting or revealing holdout labels)
- [ ] Freeze candidate commit SHA, all 80 technique identifiers and eligibility rules, configuration, R0 policy, sensor map, reference-library hashes, and dependencies.
- [ ] Specify analyte classes, unknown/OOD classes, target concentration bands, mixture conditions, reference methods and uncertainties.
- [ ] Predefine primary and secondary endpoints, acceptance thresholds, uncertainty interval methods, and handling of invalid, missing, and censored readings.
- [ ] Predefine independent groups by physical exposure session, instrument, day and operator; never split correlated windows across training and test.
- [ ] Assign independent custodian for blinded labels, and hash the frozen evaluation plan.

## Acquisition and chain of custody
- [ ] Acquire traceable reference measurements independently of the MQ sensor array; record reference calibration and uncertainty.
- [ ] Include randomized blanks, repeats, cross-interference, drift, recovery, device/day variation, and previously unseen compounds.
- [ ] Record timestamp, device serial, sensor age, environmental covariates, operator, reference instrument and custody history.
- [ ] Retain failed exposures and exclusion reasons.
- [ ] Preserve raw bytes without modification and register SHA-256 and canonical paths in G2_INDEPENDENT_EVIDENCE.json.

## Blind evaluation and audit
- [ ] Verify metadata and byte integrity in CI; treat passing CI as integrity only.
- [ ] Generate predictions with frozen code before the custodian releases reference labels; hash prediction artifacts.
- [ ] Audit leakage and provenance independently; compare baselines on identical groups.
- [ ] Report per-technique coverage, abstention, unknown rejection, confusion matrix, false alarm/miss rates and uncertainty intervals.
- [ ] Report quantitative concentration error only where independently traceable ground truth exists.
- [ ] Record failures, unsupported techniques and all exclusions without hiding them.
- [ ] Require an independent reviewer to sign off on provenance and prespecified criteria before any scientific claim.

## Decision states
- BLOCKED: no independently measured holdout, no verified custody or no blind labels.
- INTEGRITY_PASS_ONLY: raw data and metadata validated, but scientific performance not established.
- EVALUATED_NOT_QUALIFIED: independent blind assessment completed but acceptance criteria not met.
- ELIGIBLE_FOR_INDEPENDENT_REVIEW: criteria met; scientific qualification still requires independent review.

Never change ChemCore Stable, firmware or production application as part of this checklist. No synthetic or legacy benchmark substitutes for independent physical evidence.
