# G2 research continuation — Rs/R0 resistance-unit lineage guard (2026-10-26)

## Scope and source state
- Repository: `HazRob/HazRob-Tank`.
- Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- Source library: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`, pinned blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- No changes to `ChemCore Stable`, Android production code, Tank firmware, or G3 claims.
- Before this change: source-qualified independent G2 evidence records `0/80`; UCI1081 exploratory only due to source units and original physical grouping not established.

## Newly closed failure mode
`g2_signal_lineage_gate.py` previously allowed `feature_formula=RS_OVER_R0`, `source_signal_units=KOHM`, and `r0_ohm` without documenting the resistance-unit conversion. A literal calculation `Rs[kOhm] / R0[Ohm]` is off by a factor of 1000.

The research metadata gate now requires `rs_to_ohm_multiplier=1000.0` when `source_signal_units=KOHM`. For `OHM`, the optional multiplier, if provided, must equal `1.0`. Nonfinite, boolean, nonpositive, or inconsistent multipliers fail closed. This is a **metadata provenance guard** only: it does not prove that the downstream inference code actually applied the conversion. That requires an end-to-end numerical test with independently verified raw resistance and physical R0.

Expected dimensional expression:
```
ratio = (Rs_in_source_units * rs_to_ohm_multiplier) / r0_ohm
```

## Reproducible regression tests and CI
- Code commit: `c4cc351cf86e74d71e1e9fc20c586b4513dc4d9e`.
- Five new negative/positive tests commit: `336b0013bcef29acbfd23e4ce7920b3418cb33c4`.
- Workflow: `Tank V4 G2 — source signal and R0 evidence gate`.
- Verified workflow run: `40269187405` on test commit `336b0013bcef29acbfd23e4ce7920b3418cb33c4`.
- Job ID: `121398103094`; conclusion `success`.
- Python unittest log: **50 tests, 50 passed**, including five new unit-conversion cases.
- G2 audit output: `g2_pass_candidates=0`, `g2_blocked=80`.
- Actions artifact: `tank-v4-g2-signal-r0-gate`, artifact ID `12627692892`, uploaded ZIP SHA256 `539ff83a43c5539fa9541e7b9ecccad2f82c1efe4bbe784f9850676cdc8675aa`.
- CI run: https://github.com/HazRob/HazRob-Tank/actions/runs/40269187405

## Scientific interpretation and remaining blockers
- The regression checks guard against false numerical interpretation of Rs/R0 caused by resistance unit mismatch. They do not establish physical sensor calibration or gas identity.
- UCI1081 lacks independently verified original session/device group identifiers and source signal units for the Tank claim; `R0=NOT_USED` in its exploratory benchmark. The current change does not make that benchmark G2-eligible.
- `G2_INDEPENDENT_EVIDENCE.json` remains empty; no promotion is authorized. `G3 Tank=0/80`.
- Further work: obtain source-qualified physical exposure records, validate raw units and source grouping, lock blind holdouts, test conversion end-to-end with measured R0, and compare per-technique metrics against prespecified baselines.

## Status
`G2_BLOCKED_PENDING_INDEPENDENT_SOURCE_EVIDENCE`. Research-only progress; no scientific certification.
