# G2 CI checkpoint — 2026-11-04 20:23 UTC

Scope: research-only observation for commit `d37be69bf4d51aebaeb875c29d8d0b43e5038fa6`. No ChemCore Stable, app, firmware or scientific promotion.

## Verified CI
GitHub Actions commit-run query returned 11 completed successful workflow runs: 41895914420, 41895914456, 41895914616, 41895914465, 41895914648, 41895914617, 41895914508, 41895914610, 41895914415, 41895914621, 41895914625.

Inspected job 126805248660 in run 41895914625 (`g2-signal-lineage-fail-closed`): all reported steps successful, including Python compilation, pinned MQ4 source receipt lineage, source ZIP absence/parity guards, offline regression tests, full 80-technique fail-closed audit, deterministic acquisition triage, replay byte identity and publication lineage validation.

These are *software and provenance gate passes*, not evidence that all 80 techniques passed scientific G2. MQ4 4-ppm references and ethanol liquid v/v labels are not interchangeable with calibrated Tank vapor ppm. No independent Tank session holdouts were established by this inspection.

## Outstanding scientific gates
- MQ-EXT-01 original archive bytes have not been independently rehashed in this workspace; prior historical CI hash is a logged observation only.
- Verify primary source signal units, physical session/device independence, concentration reference and preregistered grouped holdouts before candidate promotion.
- Preserve fail-closed scientific status. Last documented candidate tally: 0 pass, 80 blocked, 14 source-blocked; not independently recomputed here.

## Reproduction
Fetch commit workflow runs for SHA above; inspect run 41895914625 job 126805248660 and the uploaded deterministic gate artifact; compare replay hashes against pinned inputs. A green CI badge is insufficient for scientific promotion.
