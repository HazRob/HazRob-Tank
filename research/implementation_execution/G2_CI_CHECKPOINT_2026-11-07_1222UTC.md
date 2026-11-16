# G2 research CI and evidence checkpoint — 2026-11-07 12:22 UTC

Scope: HazRob/HazRob-Tank research branch `research/chemcore80-central-evidence-gap-analysis-20261021`. No Stable/app/firmware changes and no scientific promotion.

## Verified revision
- HEAD: `649028a365e6c1474ff3f25cdad782a67eababc6`.
- GitHub Actions API `/actions/runs?branch=...&per_page=100`: 23 runs for this exact SHA; all `completed/success` at inspection. Includes push and pull_request runs.
- Representative G2 physical evidence intake preflight: `42359656972` (success).
- G2 source signal and R0 gate: `42359656916` (success).
- G2 UCI1081 multichannel open-set: `42359656871` (success).
- These are software/research gates, **not** independent physical validation.

## Evidence and reproducibility
- `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` contains `"records": []`; independent physical holdout count = 0.
- Guard: `research/implementation_execution/g2_holdout_guard.py`, with regression tests `test_g2_holdout_guard.py`.
- From repository root: `python -m unittest discover -s research/implementation_execution -p 'test_g2_holdout_guard.py' -v`.
- Inspect source evidence, immutable SHA-256, grouped device/session/source split, pre-registered blind test, and independent acquisition provenance before claiming empirical G2.
- Guard's `GROUP_PREFLIGHT_ONLY` explicitly means no G2 approval; `g2_approved` remains false.

## Blocking evidence gap
Without verified independent measured Tank sensor holdouts, empirical generalization, calibration drift, sensitivity/specificity, and per-technique scientific acceptance cannot be established. Keep G2/G3 scientific promotion blocked; do not substitute synthetic/UCI or passing CI for independent Tank measurements.

This checkpoint is documentation-only and intentionally confined to the research branch.
