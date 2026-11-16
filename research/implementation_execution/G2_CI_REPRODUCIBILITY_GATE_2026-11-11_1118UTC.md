# G2 CI and physical-evidence gate — 2026-11-11 11:18 UTC

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Inspected commit: `0664b3971750d71aa79b40d134a3cba7273f8192`.

## Observed GitHub Actions state
GitHub REST endpoint `GET /repos/HazRob/HazRob-Tank/actions/runs?head_sha=0664b3971750d71aa79b40d134a3cba7273f8192&per_page=100` returned **23 completed successful runs**. This includes `G2 independent evidence provenance regression (research only)` (run 43002168562) and `G2 physical evidence intake preflight` (run 43002178750). These are software/metadata checks, not empirical validation.

## Reproduction
```sh
git checkout research/chemcore80-central-evidence-gap-analysis-20261021
python3 -m unittest discover -s research/implementation_execution -p 'test_audit_g2_independent_evidence.py' -v
python3 research/implementation_execution/audit_g2_independent_evidence.py --json
```
The final command is expected to exit with status 2 (BLOCKED) while the manifest has no records; that exit is a *correct scientific fail-closed outcome*, not a CI defect. Record the exact SHA and run IDs when comparing future results.

## Evidence required before scientific qualification
The manifest `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` contains `records: []` at the inspected commit. The current audit checks **declared metadata**, including digest format and duplicate declarations; it does not independently open and hash raw files, establish measurement truth, prove blinded split independence, or quantify sensitivity/specificity. A passing CI workflow therefore must not be interpreted as G2 completion.

For each genuinely independent acquisition, archive raw instrument/ADC traces, acquisition timestamps, instrument identity, calibration logs and conditions, controlled reference analyte and concentration with uncertainty, negative controls, and predeclared held-out group IDs. Compute SHA-256 over the original bytes and verify against the manifest before scoring. Freeze group assignment before model selection; prevent shared runs, sessions, devices or samples from leaking across training and holdout. Pre-register per-technique performance criteria, uncertainty intervals, abstention/unknown-class behavior, and negative findings. A second party should independently reproduce the scoring from raw bytes.

Do not modify ChemCore Stable or treat any metadata-only success as scientific approval.
