# G2 research CI and independent-evidence checkpoint — 2026-11-14 15:20 UTC

## Immutable baseline
- Repository: HazRob/HazRob-Tank
- Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`
- Inspected head: `3f07c955f1b3fda416145282a61d75212974268f`
- Parent: `fa911a928f01fb76b5322e31958aa97ab1fba658`
- This checkpoint is research-only; no ChemCore Stable, Android, firmware, or production promotion.

## Reproducible CI inspection
GitHub Actions REST query: `GET /repos/HazRob/HazRob-Tank/actions/runs?head_sha=3f07c955f1b3fda416145282a61d75212974268f&per_page=100`.
Result at inspection: 14 runs, all `completed/success`.
Run IDs: 43599820861, 43599820831, 43599821070, 43599820815, 43599821136, 43599820809, 43599820811, 43599820910, 43599820918, 43599820913, 43599821087, 43599820871, 43599820852, 43599816858.
Independent raw-byte integrity: 43599821070 and 43599816858. Physical evidence intake preflight: 43599820809. Source/R0 gate: 43599820871.
To reproduce, query the above endpoint, filter `head_sha` exactly, and assert each run has `status=completed` and `conclusion=success`; do not count other commits or branches.

## Scientific evidence gate
Read `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` at the inspected commit. It declares `schema=hazrob.g2.independent-evidence.v1` and `records=[]`. Therefore independently verified physical holdout records = 0. CI success verifies software gates only, **not** empirical accuracy, physical calibration, generalization, or qualification of all 80 techniques. No scientific G2 approval is claimed.

## Next reproducible evaluation
1. Obtain independently measured raw sensor streams with provenance, instrument calibration, timestamps, reference concentrations and uncertainties, immutable SHA-256 checksums, and distinct physical sessions/devices.
2. Register the records through existing physical intake gates, keeping training/calibration and blinded holdout separated by source/session/device.
3. Freeze technique versions, preprocessing, seeds, environment and exclusions before unblinding; compute per-technique coverage, abstentions, errors, uncertainty calibration and confidence intervals.
4. Run existing CI plus physical-evidence verification on the research branch, archive exact commit and run IDs, and require independent review before any promotion.

No new measurement data were generated or fabricated in this checkpoint.