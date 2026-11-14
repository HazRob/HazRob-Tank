# ChemCore 80 — G2 reproducibility checkpoint (2026-11-08)

Status: **NOT QUALIFIED**. Research branch only; no changes to ChemCore Stable.

## Provenance and CI
- Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- Reviewed predecessor: `7277009dbe3bc6a3b47f455af8d48b13ddf21672`.
- Its 12 GitHub Actions workflow runs returned `completed/success`, including physical evidence intake preflight, R0 source evidence gate, and UCI1081 benchmarks.
- Successful CI proves software checks ran, **not** independent scientific validity.
- `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` currently contains `records: []`. No independent physical holdout can be evaluated.

## Repeatable negative control
From the repository root at this branch:
```sh
python -m unittest discover -s research/implementation_execution -p 'test_g2_empty_evidence_negative_control.py' -v
```
The synthetic nonempty-register case tests *presence detection*, not scientific qualification. Do not count it as an independent measurement.

## Required independent evidence before G2 evaluation
For each physical campaign preserve: immutable source filename and SHA-256; device and sensor serials; firmware revision; ADC and resistance raw time series with units and sampling times; manufacturer and measured R0, including calibration temperature/humidity and timing; certified analyte identity, reference concentration and uncertainty; blank/clean-air controls; sample preparation, chamber conditions and contamination controls; session/day/device grouping; operator and acquisition timestamps; and reference instrument calibration traceability.

Freeze grouping and a holdout manifest **before** fitting, tuning, feature selection or threshold optimization. Prevent sample/session leakage and report per-analyte confusion, false alarms, abstentions, concentration errors and confidence intervals by group. Distinguish manufacturer-curve tests, public-source UCI benchmarks, synthetic negative controls, and truly independent Tank measurements. Any missing metadata or absent physical records is an explicit failure to qualify, never an implicit pass.

## Next executable gate
1. Ingest genuinely independent raw measurements with provenance and verify schema/intake checks.
2. Freeze training/validation/holdout separation by device, day and exposure session.
3. Execute all 80 technique-specific evaluations and record per-technique pass/fail/insufficient-evidence, dataset hashes, exact commit, commands and CI run IDs.
4. Independently review evidence and only then consider G2 decisions. G3 remains a separate physical validation gate.

This checkpoint documents the evidence gap and reproducible software checks; it asserts no scientific approval.
