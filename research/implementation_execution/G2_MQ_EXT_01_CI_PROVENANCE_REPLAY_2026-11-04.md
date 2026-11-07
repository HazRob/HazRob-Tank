# MQ-EXT-01 CI provenance replay — 2026-11-04

Research-only follow-up to G2_MQ_EXT_01_SOURCE_STATUS_RECONCILIATION_2026-11-04.md. No G2/G3 promotion.

## Independently inspected GitHub Actions evidence
- Run: https://github.com/HazRob/HazRob-Tank/actions/runs/40287072270
- Head SHA: `26b7e7cc90bb5818848239182ea48953cdff0cda`; run created 2026-10-26T07:38:54Z; conclusion `success`.
- Job ID `121453143021` (`source-fileday-benchmark`): completed successfully.
- Job log: 10 unit tests passed in 0.582s. The source benchmark logged archive SHA-256 `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`, `n_files=50`, 12 channel configurations and an explicit `g2_qualified=false`.
- Workflow asserted `source_md5_verified`, matching SHA-256, `grouping_verified_as_independent_physical_session=false`, `source_signal_units_verified=false`, `g3_tank=false`, `safety_grade=false`.
- Uploaded artifact: `tank-g2-mq01-fileday-exploratory`, ID `12634895827`, 2 files, archive ZIP SHA-256 `d83fef34f67ce9a3e7169e5eebd844fd2ed616d454888df56d6eb0c3946c7a19`. This digest is the *artifact ZIP*, NOT the source archive.

## Narrow reconciliation
The older catalog's claim that a source archive was checksum-verified is supported by the historical CI job log and its assertions. The newer catalog's `archive_listed_not_downloaded` entry is inconsistent with that historical CI execution, but may reflect a different local workspace/snapshot. Do not silently rewrite either record. Treat acquisition as `HISTORICAL_CI_VERIFIED_LOCAL_RETRIEVAL_NOT_REPLAYED` until original bytes and archive retrieval provenance are independently reproduced in the current workspace.

## Reproduction / evidence gate
1. Re-run the exact historical commit and verify archive SHA-256 and publisher MD5 against original bytes, retaining retrieval URI, timestamp, license and filename.
2. Verify that artifact ID 12634895827 can be downloaded, hash it separately, and inspect JSON predictions; artifact existence alone is not raw source availability.
3. Verify physical experiment/session independence, signal units, and concentration references from primary documentation. A file/day split is not automatically an independent physical holdout.
4. Only then evaluate preregistered grouped holdouts. Keep all 80 G2 promotions blocked pending technique-specific independent evidence.

This note is an audit of logs, not an independent rehash of the original source bytes.