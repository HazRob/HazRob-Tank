# G2 research restart checkpoint — 2026-11-02

Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
Base: `c2a946355f4fa90d99b701cd23b5bbc8b168c083`. This checkpoint is committed together with the NFKC blind-label collision guard; use the branch HEAD as the actual resume SHA.

Review: 21/21 visible GitHub Actions runs on base SHA succeeded, including both G2 gates. Change: canonicalize blind reference labels only for class-diversity accounting, block compatibility aliases and collapsed single-class holdout. Tests: 3 deterministic synthetic regression methods in `test_g2_blind_target_diversity.py`. Method: `G2_BLIND_LABEL_NFKC_ALIAS_2026-11-02_AR.md`.

Next: verify G2 CI on exact new HEAD and repair any failure; keep `records=[]`, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14` until independently measured grouped and blinded reference-calibrated evidence is actually acquired. Never modify Stable, app, firmware or main; never promote scientific readiness from synthetic CI.
