# G2 research restart checkpoint — 2026-11-03

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Verified base: `8da72aaa6fa87319b057db36a85593847ffbbbc5` (21/21 visible CI runs completed successfully, including both G2 workflows). Resume from the current research branch HEAD; never merge these changes to main or Stable as part of G2.

Change: detect NFKC-equivalent but byte-different reference gas labels across the full training/calibration/blind group manifest. Previous guard checked aliases within blind rows only; the new `GROUP_MANIFEST_REFERENCE_LABEL_NFKC_ALIAS` blocks spurious improvement over the training-only majority baseline when labels differ between partitions. Raw reference labels and all pre/post-unblinding digests remain unchanged. Four deterministic synthetic regression methods added to `test_g2_blind_target_diversity.py`. Method: `G2_REFERENCE_LABEL_CROSS_PARTITION_NFKC_2026-11-03_AR.md`.

CI status at documentation time: newly created workflow runs were queued/in progress. Do not declare the new commit CI-green until exact HEAD is checked. Investigate and repair any confirmed failures. Reproduce with:
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

Scientific gate unchanged: `G2_INDEPENDENT_EVIDENCE.json` has `records=[]`; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Missing independent, grouped, blinded and reference-calibrated physical Tank measurements. Synthetic CI does not certify gas identity, ppm, or G2/G3 readiness. No changes to ChemCore Stable, Android, firmware or main.
