# G2 CI and independent-evidence checkpoint — 2026-11-07 18:24 UTC

Scope: research branch only; no scientific promotion.

## Reproducible provenance
- Repository: HazRob/HazRob-Tank
- Branch: research/chemcore80-central-evidence-gap-analysis-20261021
- Examined head: `5354742e2387056641b8da37929e8d1bb383fdf8`
- GitHub REST query: `GET /repos/HazRob/HazRob-Tank/actions/runs?head_sha=5354742e2387056641b8da37929e8d1bb383fdf8&per_page=100`
- Observed 23 runs for this exact head: 12 pull_request and 11 push, all `completed/success`; zero failed among returned runs.
- Physical evidence intake: runs 42376339983 (PR), 42376337769 (push), both success.
- Source signal and R0 evidence gate: runs 42376340009 (PR), 42376337869 (push), both success.
- Research batch CI and UCI1081 checks were also successful; these do not establish physical G2 qualification.

## Independent evidence
- Read `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` on this branch; schema `hazrob.g2.independent-evidence.v1`, `records: []`.
- Read `G2_INDEPENDENT_EVIDENCE_ACQUISITION_PROTOCOL_2026-11-06.md`: frozen technique/baseline, blinded grouped independent acquisitions, calibrated reference identity and concentration, raw-byte SHA-256, and preregistered evaluation remain necessary.
- No new physical raw records or independently qualified holdout evidence were supplied or discovered in the ledger.
- No empirical performance metric, G2/G3 promotion, or extrapolation from green CI is authorized.

## Replay
1. Inspect the exact head's Actions runs with the GitHub REST query above.
2. Read the evidence ledger and verify `records` is an empty array.
3. Run `python3 research/implementation_execution/g2_physical_intake_gate.py --self-test` on a checkout of the head; this checkpoint **does not claim a local execution** of that command.
4. When independent calibrated blinded data become available, register raw hashes and acquisition provenance, then run intake and frozen grouped holdout evaluation.

## Isolation
Documentation only in research branch. ChemCore Stable, Android application, and firmware are untouched. The scientific G2 gate remains blocked by missing independent calibrated physical evidence.
