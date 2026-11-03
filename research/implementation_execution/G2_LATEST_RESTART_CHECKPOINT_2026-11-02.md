# G2 research restart checkpoint — 2026-11-03

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Prior verified HEAD: `d224f4f604f0280bd5fefa93e33124e2af860c9b` — all 21 GitHub Actions runs completed successfully; 1242/1242 offline G2 tests passed in the preceding round.

New work: deterministic cross-snapshot provenance claim reconciliation. The pinned 14-source readiness matrix and 20-candidate inventory share the same central catalog blob but disagree on MQ-EXT-01 raw archive acquisition status. Added read-only reconciliation script, seven negative/real-snapshot regression tests, deterministic JSON report and Arabic methods document. Do **not** change the pinned source matrix or upgrade G2 from this receipt claim.

Reproduce:
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_catalog_snapshot_drift.py' -v
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_catalog_snapshot_drift.py --output /tmp/g2_source_snapshot_drift.json
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

Check all GitHub Actions runs for the final commit, repair only confirmed failures on this research branch. Scientific gate unchanged: independent Tank evidence `records=[]`, last verified `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. No independent, grouped, blinded, reference-calibrated Tank exposures. ChemCore Stable, Android, firmware, and main remain untouched.
