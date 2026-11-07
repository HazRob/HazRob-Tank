# G2 — Original MQ-EXT-01 Zenodo source acquisition and file-day benchmark (2026-10-26)

## Scientific scope
This report records a **source-domain exploratory benchmark** on original externally measured MQ sensor signals. It does not qualify HazRob Tank G2, G3, gas identity, ppm, or occupational safety.

## Source provenance
- Central catalog: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`, pinned Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- Candidate: `MQ-EXT-01` / `https://zenodo.org/records/17285312` / Food Freshness Electronic Nose Dataset, published 2025-10-07.
- Original file: `Food-Freshness-E-Nose-Dataset.zip`.
- Publisher-listed MD5: `008b8ccb62d7a7d51ff9ffcfd698a1e5`, **verified against downloaded bytes**.
- Computed source ZIP SHA256: `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`.
- ZIP inventory: **59 CSV files**, verified CRC, no extraction; 9 MQ columns: MQ2, MQ3, MQ4, MQ5, MQ6, MQ7, MQ8, MQ9, MQ135, plus `Ticks`.
- Five CSV headers and sample records inspected from original source, not inferred from literature.
- Original ZIP is not committed to the repository; SHA and manifest plus inventory are stored as CI artifacts.

## Exploratory measured-data experiment
- A filename-based subset contains **50 single-produce files**, 10 produce classes × D1–D5. The other nine CSVs describe mixtures or differently named samples and are excluded by a prespecified filename rule.
- Two prespecified feature sets: Tank-overlap 8 MQ channels (without MQ6) versus all source 9 MQ channels.
- One vector per CSV file: median of each raw channel. Source signal units are **unverified**. No R0 normalization and no ppm conversion.
- Five folds: hold out all files marked D1, D2, ..., D5 in turn (10 files per fold), train on the remaining 40; training-only per-channel standard deviation and 1-nearest-neighbor Euclidean distance, no tuning on test folds.
- Results: **8-channel 11/50 = 22.0%**, **9-channel 11/50 = 22.0%**. Nominal balanced-majority baseline 5/50 = 10.0%.
- This experiment **does not show a benefit from adding MQ6** for this specific source task and method. It does not establish that MQ6 is redundant in other settings.
- D1–D5 are labels in filenames; their independence as physical exposures, sessions or devices is **not established**. The 50 files may share equipment, source material, time trends and environmental confounders. Do not treat the 50 test outcomes as independent trials for confidence intervals.

## CI reproducibility and artifacts
1. `research/implementation_execution/g2_zenodo_mq01_raw_inventory.py` and `test_g2_zenodo_mq01_raw_inventory.py`; `.github/workflows/tank-g2-mq01-source.yml`.
   - Source acquisition/inventory run: `40269682952` success; **4/4** offline source tests.
   - Artifact `12627940011`: `raw_manifest.json`, `archive_inventory.csv`, `source_qualification.json`, `source_csv_preview.json`.
2. `research/implementation_execution/g2_mq01_fileday_benchmark.py` and `test_g2_mq01_fileday_benchmark.py`; `.github/workflows/tank-g2-mq01-fileday.yml`.
   - Source benchmark run: `40269823330` success; **3/3** offline negative/positive controls.
   - Artifact `12627599504`: `source_file_features.json`, `fileday_results.json` with all 50 per-file predictions for each configuration.
   - ZIP artifact SHA256: `bada0f855b9e091c50c6de8e85684c1b1783221bdabee4fea7cab876f18dcbde`.
3. G2 R0 unit-lineage gate: run `40269187405` success, **50/50** tests, `0/80` G2 candidates, `80/80` blocked. The newly introduced guard requires explicit kOhm-to-Ohm conversion for Rs/R0.

## Why no G2 promotion
- No independently verified physical session/device/exposure group IDs in this dataset; filename day labels alone are insufficient.
- Raw channel units, physical MQ instance identifiers, heater operation and device R0 provenance are not verified.
- Target labels represent produce/food freshness, not reference pure-gas identities or traceable gas ppm.
- File-level holdout avoids **row-level leakage**, but cannot establish independence of biological or physical acquisitions.
- The measured 22% accuracy is exploratory, not a qualified model performance estimate.

## Next scientific actions
- Inspect source documentation for device/session and unit definitions; request original experimental metadata if absent.
- Reuse this source only for source-domain time-series/feature robustness and group-leakage demonstrations until provenance is established.
- Prioritize original gas-reference datasets with documented physical exposure group IDs, known signal units, and blind holdout; no model promotion from synthetic tests or manufacturer-only curves.
- Keep `G2_INDEPENDENT_EVIDENCE.json` empty until the source qualification gate and independent review are satisfied. Stable remains untouched.

**Status:** `SOURCE_ARCHIVE_VERIFIED`, `EXPLORATORY_FILEDAY_EVALUATED`, `G2_BLOCKED`, `G3_TANK_BLOCKED`.
