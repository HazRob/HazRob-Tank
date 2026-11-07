# G2 CI checkpoint — 2026-11-07 04:23 UTC

Scope: research branch only; no ChemCore Stable changes; no G2/G3 scientific promotion.

## Verified source state
- Branch HEAD: `9453713c66e13dc52f3aef1f08edff5305b75542`.
- `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json`: schema `hazrob.g2.independent-evidence.v1`, `records: []` (zero independently verified physical holdouts).
- Holdout preflight `g2_holdout_guard.py`: rejects absent train or sealed test, malformed group fields, duplicate raw SHA-256, and group leakage. A passing preflight is `GROUP_PREFLIGHT_ONLY`, never `g2_approved=true`.

## CI evidence
GitHub Actions API `GET /repos/HazRob/HazRob-Tank/actions/runs?branch=research%2Fchemcore80-central-evidence-gap-analysis-20261021&per_page=15` returned completed/success for the latest HEAD, including:
- G2 physical evidence intake preflight: run 42333603363.
- G2 source signal and R0 evidence gate: run 42333603518.
- G2 UCI1081 multi-channel open-set benchmark: run 42333603365.
- Actual UCI1081 MQ135 source stress test: run 42333603389.
- Other batch engineering workflows in the same query were also completed/success.

These are CI workflow conclusions, **not** independent accuracy measurements or evidence of G2 completion.

## Reproduction
```bash
git checkout research/chemcore80-central-evidence-gap-analysis-20261021
python -m unittest discover -s research/implementation_execution -p 'test_g2_holdout_guard.py' -v
python -m py_compile research/implementation_execution/g2_holdout_guard.py
```
To independently verify CI conclusions, query GitHub Actions runs by branch and compare `head_sha` to the checkpoint HEAD. Do not use PR-only run listings to infer the status of push-triggered workflows.

## Remaining scientific gate
No independently verified physical measurements, calibrated reference concentration labels, device/session provenance, or sealed independent test groups have been entered in the authoritative evidence register. Therefore the scientific G2 promotion gate is blocked for all 80 techniques; source-dataset software checks cannot substitute for Tank measurements. This checkpoint makes no promotion or performance claim.
