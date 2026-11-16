# G2 evidence-gate CI checkpoint — 2026-11-05

Scope: research-only evidence preflight, **not** scientific G2 approval.

## Reproducible checkpoint

- Parent commit: `f0366d3f50aa679dd32ea2a9ba21ab120e923890`.
- GitHub Actions commit-associated PR runs inspected: 12; all completed with conclusion `success`.
- Physical evidence intake workflow: run `42092532834`, conclusion `success`.
- Source file: `research/implementation_execution/g2_physical_intake_gate.py`.

Reproduce the preflight regression suite from this branch:

```bash
python3 research/implementation_execution/g2_physical_intake_gate.py --self-test
```

For a genuine manifest with raw files available relative to the manifest directory:

```bash
python3 research/implementation_execution/g2_physical_intake_gate.py /path/to/manifest.json
```

A successful result is `PREFLIGHT_ONLY`, **never** G2 approval. The preflight verifies raw-file hashes, nonempty evidence, metadata shape, and basic holdout leakage controls. It does not establish calibrated gas identity, quantitative accuracy, external reference traceability, or independent physical replication.

## Evidence still required

For each candidate technique, archive the original immutable acquisition files and SHA-256 checksums; calibrated reference instrument details and uncertainty; sensor and firmware metadata; acquisition site/day/device/session/exposure grouping; pre-registered train/validation/test partitioning; and blind independent test results including error bars, confusion matrices or concentration residuals as appropriate. No scientific promotion may be inferred from CI passing.

This checkpoint contains no new independent experimental data. ChemCore Stable, Android app, and firmware are out of scope.
