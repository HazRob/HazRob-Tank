# G2 research restart checkpoint — 2026-11-03

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Verified prior HEAD: `43b42345d65d586ea4d5bc1a9a252ea5a61ac9cf` (21/21 GitHub Actions runs completed successfully, including both G2 gates).

Change: MQ-EXT-10 ZIP member hierarchy and Unicode compatibility path integrity. Reject regular-file/implicit-directory conflicts in either member ordering, and reject NFKC-generated separators or dot segments before path identity checks. Five deterministic synthetic regressions added; explicit directories remain valid. Method: `G2_MQ_EXT10_ZIP_HIERARCHY_CONFUSABLE_GUARD_2026-11-03_AR.md`.

Reproduce:
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10_archive_intake.py' -v
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

CI status: inspect all workflow runs for the exact final commit and repair failures on research branch only. No success claimed before completion.

Scientific gate unchanged: independent Tank evidence `records=[]`; last verified `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. No independent grouped, blinded, reference-calibrated Tank exposure data. Synthetic archive integrity tests cannot qualify any technique, gas identity, ppm, G2 or G3. ChemCore Stable, Android, firmware and main remain untouched.
