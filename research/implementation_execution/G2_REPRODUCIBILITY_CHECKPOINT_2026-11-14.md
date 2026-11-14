# G2 reproducibility checkpoint — 2026-11-14 (UTC)

Scope: research-only evidence intake and regression CI. This checkpoint does **not** certify any of the 80 techniques, change ChemCore Stable, or establish field accuracy.

## Provenance

- Repository: HazRob/HazRob-Tank
- Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`
- Tested commit: `32996521ad4182f27aca16d1480efac1b2a9e1cb`
- Previous issue: newline fixture for a source identifier was incorrectly escaped, causing two CI failures on the prior commit.
- Correction: the tested commit encodes the newline correctly in the Python regression fixture.
- Verified CI: 13 pull-request workflow runs reported `status=completed` and `conclusion=success` for the tested commit (including raw-byte integrity and physical-evidence intake preflight).
- Raw-byte integrity run: https://github.com/HazRob/HazRob-Tank/actions/runs/43561339127
- Physical intake run: https://github.com/HazRob/HazRob-Tank/actions/runs/43561339147

## Independent-evidence gate

At this checkpoint `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` has schema `hazrob.g2.independent-evidence.v1` and `records: []`. Consequently, no independent physical holdout sample was available for empirical G2 certification.

## Reproduction and interpretation

1. Check out the exact tested commit, not the moving branch tip.
2. Inspect the linked GitHub Actions runs and retain the run IDs, workflow names, statuses and conclusions as provenance.
3. Re-run the repository's raw-byte integrity and physical-intake regression workflows against the same commit; compare status and artifact hashes where artifacts exist.
4. Keep any synthetic fixture or public-source benchmark separate from independently measured holdout observations. A successful software gate verifies implementation behavior, **not** analytical identification or concentration accuracy.
5. Before claiming empirical G2 results, ingest independently acquired raw observations with immutable file SHA-256, device/sensor identifiers, acquisition timestamps, R0 and environmental conditions, calibration traceability, independent reference concentration/identity, and predeclared train/validation/holdout grouping.
6. Evaluate all 80 techniques against the predeclared acceptance criteria, including abstention, uncertainty, mixture identifiability, sensor drift, device/session leakage, and out-of-distribution exposures. Report denominators, confidence intervals and failures; do not promote a technique without adequate independent evidence.

## Open blocker

Independent physical evidence and sealed evaluation holdouts are missing. CI success cannot resolve this scientific evidence gap. No changes to Stable, app or firmware are authorized by this note.
