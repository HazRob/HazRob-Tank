# HazRob Tank ChemCore — legacy migration ledger

Date: 2026-10-18. Scope: HazRob Tank only.

## Verified
- 103 local Python unit tests passed, no failures (2026-10-18).
- 38 local research source/test/report files packaged in a SHA-256-manifested ZIP, available as a conversation artifact, not yet committed to GitHub.
- Tank repository contains research roadmap, status ledger and resume checkpoint.
- Tank repository contains imported references for analytical equations and evidence fusion (technique 13).

## Pending — do not mark as transferred
- Commit the 38 source/test/report files to this repository and verify hashes.
- Import and review missing technique 12/14, sensor subset, and fuel/formalin references.
- Rerun tests from a clean checkout after import.
- Map prior prototypes to the new roadmap; do not promote them to chemical validation automatically.

## Research code inventory
fingerprint_v1, curve_evidence_v1, four_dimensional_v1, enose_v1, multivariate_v1, qspr_v1, open_set_v1, mixture_v1, ann_mlp_v1, temporal_response_v1, drift_compensation_v1, evidence_fusion_v1, tech14_validation_v1, uci1081_mq135_benchmark, uci308_raw_adapter_v1, uci308_adapter_validation_v2; plus unit tests and experiment reports.

Original HazRob production ChemCore, APK and firmware are outside scope.
