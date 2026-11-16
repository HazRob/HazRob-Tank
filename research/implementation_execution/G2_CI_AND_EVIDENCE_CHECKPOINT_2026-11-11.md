# G2 reproducibility checkpoint — 2026-11-11 (research only)

## Verified baseline
- Repository: HazRob/HazRob-Tank
- Branch: `research/chemcore80-central-evidence-gap-analysis-20261021`
- Inspected HEAD: `a445a2d49808376eff8f20b18c225c52c75316ef`
- GitHub Actions workflow run `42971401715`: **success**, `G2 independent evidence provenance regression (research only)`, on the inspected HEAD.
- The 23 most recent workflow runs returned for that HEAD all reported `completed/success`. This is a CI result, **not** an independent scientific validation.
- `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` has `records: []`; no independent physical holdout measurements were registered at this checkpoint.
- No change to ChemCore Stable, app, or firmware is authorized by this checkpoint.

## Reproduction
From a checkout of the research branch at the inspected HEAD:

```sh
python3 -m unittest discover -s research/implementation_execution -p 'test_audit_g2_independent_evidence.py' -v
python3 research/implementation_execution/audit_g2_independent_evidence.py --json
```

Expected behavior: regression tests pass; the second command emits `status: BLOCKED`, `record_count: 0`, `scientifically_validated: false`, and exits with code 2 because independent evidence is absent. A blocked evidence gate is the **correct** outcome until authentic records are supplied.

## Evidence needed to unblock empirical G2
1. Immutable, attributable raw physical sensor measurements with actual file SHA-256 digests, instrument and calibration provenance, and collection dates.
2. A documented independent holdout split separated by acquisition session/device/source as appropriate, with leakage controls and blinded labels.
3. Independent reference measurements and uncertainty analysis sufficient to test concentration estimates and classification/abstention behavior.
4. Per-technique acceptance criteria and reproducible evaluation outputs, reviewed separately from software CI.

The manifest audit only checks the supplied metadata; it does **not** verify that files exist, recompute their hashes, establish independent sampling, or validate scientific accuracy. Synthetic examples and successful CI must never be represented as empirical G2 approval.
