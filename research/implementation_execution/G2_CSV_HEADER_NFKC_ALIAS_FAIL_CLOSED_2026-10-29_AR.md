# G2 — NFKC identity gate for evidence CSV headers (2026-10-29)

## Scope and reproducibility
- Repository: `HazRob/HazRob-Tank`; branch: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- Resumed from `46dd80de1bc29d118aec72b8f24924b720355173` after verifying the prior G2 workflow #40848957297: 327/327 tests PASS, double replay and publication gate PASS.
- Central source index: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`, Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`; 80 catalog entries, 14 external measurement datasets.
- Source-readiness matrix: `G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json` (Git blob `bd69304fc35175b90199c6cc6ad4a88fc10ae93f`), 0/14 qualified. Independent evidence register: `G2_INDEPENDENT_EVIDENCE.json` (Git blob `c44f7cbf9aa767c7aeabb750e34eaa339868e43e`), `records=[]`.

## Engineering defect and change
`csv.DictReader` allows visually confusable NFKC-compatible field names (for example `exposure_id` versus `ｅxposure_id`, and `device_id` versus `ｄevice_id`). Previously, the CSV gate rejected duplicate headers, control characters and non-NFC text, but did not reject NFKC compatibility aliases. An extra confusable column could carry conflicting physical identity information while the gate consumes the canonical column, misleading a downstream reviewer.

The `_read_csv` schema gate in `research/implementation_execution/g2_cross_artifact_gate.py` now requires every header to be NFKC-stable and fails closed with `CSV_HEADER_AMBIGUOUS`. This is intentionally scoped to CSV **header names**; it does not rewrite source measurements or chemical labels. Existing controls for row width, duplicate columns and Unicode control characters remain.

Added six deterministic synthetic tests in `test_g2_csv_header_nfkc_gate.py`: fullwidth exposure header; duplicate-looking canonical/compatibility exposure headers; device header alias; ordinary ASCII schema; multilingual canonical extra header; decomposed header. Synthetic fixtures are not gas measurements.

## Verified CI evidence
- Code/test commit: `ff04494fc99377bf673dd3564c544b594a385466`.
- [G2 workflow #40857166203](https://github.com/HazRob/HazRob-Tank/actions/runs/40857166203), job `123390916550`: **333/333 PASS**; 80-technique audit and byte-identical replay PASS; publication guard PASS.
- Results: `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Artifact: `12889014481` (`tank-v4-g2-signal-r0-gate`); ZIP SHA256 `2b418c362804a37bb879c98f6a78010ef3f490b2a944202a128f0f1c1d0bcdbe`.
- No G2/G3 scientific promotion; no modifications to ChemCore Stable, Android or firmware.

## Remaining scientific blocker
Acquire and independently audit original physical exposure data, traceable reference concentrations, source/device/session/exposure provenance and preregistered grouped blind holdouts. Software checks alone cannot establish empirical qualification. Recheck CI on this documentation checkpoint before proceeding.
