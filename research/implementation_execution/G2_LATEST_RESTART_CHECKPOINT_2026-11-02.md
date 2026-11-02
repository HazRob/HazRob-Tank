# G2 research restart checkpoint — 2026-11-03

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Verified previous HEAD: `9112ff1c9bbce3776b4736bc79cc9b7b00bdbd10` (21/21 visible GitHub Actions workflow runs completed successfully, including both G2 workflows).

Change: MQ-EXT-10 ZIP member path identity hardening. NFKC/casefold path aliases are rejected without modifying raw member names; case/fullwidth aliases of the published Ethanol folder cannot silently hide measurements; Unicode control/format characters in member names are rejected. Six deterministic synthetic regression methods added. Method: `G2_MQ_EXT10_ZIP_UNICODE_MEMBER_IDENTITY_2026-11-03_AR.md`.

Reproduce:
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10_archive_intake.py' -v
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

CI status: verify workflow runs for the exact final commit; no success is asserted before CI completes. If any job fails, inspect the job log and repair on this research branch only.

Scientific gate remains unchanged: independent Tank evidence register `records=[]`; last verified `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. No independent grouped, blinded, reference-calibrated Tank exposure dataset is present. Synthetic tests and source hygiene do not certify gas identity, ppm, G2 or G3. No changes to ChemCore Stable, Android, firmware or main.
