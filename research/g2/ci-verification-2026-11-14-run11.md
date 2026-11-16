# G2 CI verification — 2026-11-14

Scope: research branch only. This record documents software CI observations, **not** scientific validation of any of the 80 techniques.

## Verified source

Commit `9d2c8b5ad311c1c5e5fa2bb4bd96cb3965d1401c` was queried through GitHub's commit workflow runs endpoint. Thirteen associated workflow runs were returned, each `completed` with `success` conclusion.

Relevant runs:
- G2 physical evidence intake preflight: 43575424790 — success.
- G2 independent raw-byte integrity regression: 43575424866 — success.
- G2 UCI1081 multi-channel open-set source benchmark: 43575424959 — success.
- G2 source signal and R0 evidence gate: 43575424906 — success.

See `https://github.com/HazRob/HazRob-Tank/actions/runs/43575424866` and `https://github.com/HazRob/HazRob-Tank/actions/runs/43575424790`.

## Interpretation and limitations

A passing CI workflow establishes only that its configured checks passed at that commit; it does not establish empirical accuracy, generalization, independent laboratory replication, or successful completion of G2. The independent physical evidence registry was not re-located by its filename at repository root in this inspection; no claim about its current contents is made here. The prior execution history reported an empty `records` array, but this must be rechecked at its actual path before any scientific gate is considered.

## Reproducibility and next gates

1. Record each test's input dataset provenance, SHA-256, acquisition device, calibration protocol, environmental conditions, train/test split, and exclusion rules.
2. Use held-out, independent physical measurements; preserve raw files unchanged and enforce device/session/source separation.
3. Publish per-technique acceptance criteria, confidence intervals, negative controls, abstention and false-alarm rates, and reproducible CI artifact hashes.
4. Keep ChemCore Stable, firmware, and application out of this research-only workstream.
5. Do not mark any technique scientifically qualified solely on software regression results.
