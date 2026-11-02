# G2 research restart checkpoint — 2026-11-03

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Verified prior HEAD: `0465067a78548e7f13b331ac029798b38468ca2f` (21/21 GitHub Actions runs completed successfully, including both G2 gates).

Change: MQ-EXT-10 whole-ZIP member integrity. Previously non-Ethanol members were skipped without decompression/CRC verification; now every non-Ethanol file is streamed in bounded chunks and checked against declared size, while directory entries with payload are rejected. Three deterministic synthetic regressions added. Method: `G2_MQ_EXT10_WHOLE_ZIP_MEMBER_INTEGRITY_2026-11-03_AR.md`.

Reproduce:
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10_archive_intake.py' -v
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

CI status: check all workflow runs for the exact final commit and inspect/repair any failures on the research branch only; no success claimed before completion.

Scientific gate unchanged: independent Tank evidence `records=[]`; last verified `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. No independent grouped, blinded, reference-calibrated Tank exposure data. Synthetic ZIP integrity tests cannot qualify any technique, gas identity, ppm, G2 or G3. ChemCore Stable, Android, firmware and main remain untouched.
